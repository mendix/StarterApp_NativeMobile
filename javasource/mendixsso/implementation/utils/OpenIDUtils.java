package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.MendixRuntimeException;
import com.mendix.systemwideinterfaces.core.ISession;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import mendixsso.implementation.ConfigurationManager;
import mendixsso.implementation.ContinuationURLManager;
import mendixsso.implementation.handlers.OpenIDHandler;
import mendixsso.proxies.constants.Constants;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mendixsso.implementation.handlers.OpenIDHandler.CALLBACK;
import static mendixsso.implementation.handlers.OpenIDHandler.OPENID_CLIENTSERVLET_LOCATION;


public final class OpenIDUtils {

    private static final String SET_COOKIE = "Set-Cookie";

    private OpenIDUtils() {
    }

    private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String LOCATION_HEADER_NAME = "location";
    private static final String NUM = "0123456789";
    private static final String SPL_CHARS = "!@#$%^&*_=+-/";
    private static final String NONCE_COOKIE_NAME = "MENDIXSSONONCE";

    private static final String NONCE_COOKIE_PATH = "/";
    private static final String NONCE_COOKIE_DOMAIN = "";
    public static final String COOKIE_HOST_PREFIX = "__Host-";


    private static final Pattern OPENID_UUID_REGEX = Pattern.compile("mxid2/id\\?id=(\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12})$");

    private static final ILogNode LOG = Core.getLogger(Constants.getLogNode());

    public static String getApplicationUrl(final IMxRuntimeRequest req) {
        final String serverName = req.getHttpServletRequest().getServerName();
        if (serverName == null) {
            LOG.warn("Something went wrong while determining the server name from the request," + " defaulting to the application root URL.");
            return getDefaultAppRootUrl();
        }

        try {
            // Because the Mendix Cloud load balancers terminate SSL connections, it is not possible to
            // determine
            // the original request scheme (whether it is http or https). Therefore we assume https for
            // all connections
            // except localhost (to enable local development).
            final String scheme = serverName.toLowerCase().endsWith(".test") || "localhost".equalsIgnoreCase(serverName) ? HTTP : HTTPS;
            final int serverPort = req.getHttpServletRequest().getServerPort();
            // Ports 80 and 443 should be avoided, as they are the default, therefore we pass in -1
            final URI appUri = new URI(scheme, null, serverName, serverPort == 80 || serverPort == 443 ? -1 : serverPort, "/", null, null);
            return appUri.toString();
        } catch (URISyntaxException e) {
            LOG.warn("Something went wrong while constructing the application URL," + " defaulting to the application root URL.", e);
            return getDefaultAppRootUrl();
        }
    }

    private static String getDefaultAppRootUrl() {
        return ensureEndsWithSlash(Core.getConfiguration().getApplicationRootUrl());
    }

    public static String getOpenID(String uuid) {
        return ensureEndsWithSlash(ConfigurationManager.getInstance().getOpenIDPrefix()) + "id?id=" + uuid;
    }

    public static String extractUUID(final String openID) {
        if (openID != null) {
            final Matcher m = OPENID_UUID_REGEX.matcher(openID);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    public static String getRedirectUri(final IMxRuntimeRequest req) {
        return getApplicationUrl(req) + OPENID_CLIENTSERVLET_LOCATION + CALLBACK;
    }

    public static void redirectToIndex(final IMxRuntimeRequest req, final IMxRuntimeResponse resp, final String continuation) {
        resp.setStatus(IMxRuntimeResponse.SEE_OTHER);

        // no continuation provided, use index
        if (continuation == null) {
            resp.addHeader(LOCATION_HEADER_NAME, OpenIDHandler.INDEX_PAGE);
        } else {
            if (continuation.trim().startsWith("javascript:")) {
                throw new IllegalArgumentException("Javascript injection detected!");
            } else if (!continuation.startsWith("http://") && !continuation.startsWith("https://")) {
                resp.addHeader(LOCATION_HEADER_NAME, getApplicationUrl(req) + continuation);
            } else if (ContinuationURLManager.getInstance().isRedirectionAllowedForUrl(continuation)) {
                resp.addHeader(LOCATION_HEADER_NAME, continuation);
            } else {
                throw new IllegalArgumentException("Redirection to the specified URL is not allowed!");
            }
        }
    }

    private static String base64Encode(final byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String getFingerPrint(final IMxRuntimeRequest req) {
        final String agent = req.getHeader("User-Agent");
        if (agent != null) {
            return base64Encode(agent.getBytes());
        }
        return "";
    }

    public static String getFingerPrint(final ISession session) {
        final String agent = session.getUserAgent();
        if (agent != null) {
            return base64Encode(agent.getBytes());
        }
        return "";
    }

    public static String ensureStartsWithSlash(final String text) {
        return text.startsWith("/") ? text : "/" + text;
    }

    public static String ensureEndsWithSlash(final String text) {
        return text.endsWith("/") ? text : text + "/";
    }

    public static String randomStrongPassword(final int minLen, final int maxLen, final int noOfCAPSAlpha, final int noOfDigits, final int noOfSplChars) {
        if (minLen > maxLen) {
            throw new IllegalArgumentException("Min. Length > Max. Length!");
        }
        if ((noOfCAPSAlpha + noOfDigits + noOfSplChars) > minLen) {
            throw new IllegalArgumentException("Min. Length should be at least sum of (CAPS, DIGITS, SPL CHARS) Length!");
        }

        final SecureRandom secureRandom = getStrongSecureRandom();
        final int len = secureRandom.nextInt(maxLen - minLen + 1) + minLen;
        final char[] pswd = new char[len];
        int index;
        for (int i = 0; i < noOfCAPSAlpha; i++) {
            index = getNextIndex(len, pswd);
            pswd[index] = ALPHA_CAPS.charAt(secureRandom.nextInt(ALPHA_CAPS.length()));
        }
        for (int i = 0; i < noOfDigits; i++) {
            index = getNextIndex(len, pswd);
            pswd[index] = NUM.charAt(secureRandom.nextInt(NUM.length()));
        }
        for (int i = 0; i < noOfSplChars; i++) {
            index = getNextIndex(len, pswd);
            pswd[index] = SPL_CHARS.charAt(secureRandom.nextInt(SPL_CHARS.length()));
        }
        for (int i = 0; i < len; i++) {
            if (pswd[i] == 0) {
                pswd[i] = ALPHA.charAt(secureRandom.nextInt(ALPHA.length()));
            }
        }
        return String.valueOf(pswd);
    }

    private static int getNextIndex(final int len, final char[] pswd) {
        int index;
        final SecureRandom secureRandom = getStrongSecureRandom();
        //noinspection StatementWithEmptyBody
        while (pswd[index = secureRandom.nextInt(len)] != 0) ;
        return index;
    }

    public static String convertInputStreamToString(final InputStream is) {
        final Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static SecureRandom getStrongSecureRandom() {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new MendixRuntimeException("Could not get an instance of a strong secure random generator.", e);
        }
    }

    public static void unsetNonceCookie(IMxRuntimeRequest request, IMxRuntimeResponse response) throws URISyntaxException {
        HttpServletResponse servletResponse = response.getHttpServletResponse();
        String cookieHeader = createCookie(request, "", 0);

        servletResponse.addHeader(SET_COOKIE, cookieHeader);
    }


    public static void setNonceCookie(IMxRuntimeRequest request, IMxRuntimeResponse response, String nonce) throws URISyntaxException {
        HttpServletResponse servletResponse = response.getHttpServletResponse();
        String cookieHeader = createCookie(request, nonce, (int) (Constants.getAuthRequestExpiryDurationInMinutes() * 60));

        servletResponse.addHeader(SET_COOKIE, cookieHeader);
    }

    private static String createCookie(IMxRuntimeRequest request, String nonce, int maxAge) throws URISyntaxException {
        var isHttps = isHttps(request);
        var cookieName = isHttps ? COOKIE_HOST_PREFIX + NONCE_COOKIE_NAME : NONCE_COOKIE_NAME;

        /*
        Netty http codec used to encode cookie to string. Here, our intention is building cookie without underlying Core module, Jetty or something.
        Thus, that give ability of specifying same site policy. Of course, it was possible to write cookie encoder to comply with RFC standard but
        maintaining is another concern.
        If Mendix runtime upgrades the Jetty version 12.0 or higher, remove the netty external dependency and replace this with built-in solution.
         */
        DefaultCookie nettyCookie = new DefaultCookie(cookieName, nonce);
        nettyCookie.setHttpOnly(true);
        nettyCookie.setSecure(isHttps);
        nettyCookie.setPath(NONCE_COOKIE_PATH);
        nettyCookie.setSameSite(CookieHeaderNames.SameSite.Lax);
        nettyCookie.setDomain(NONCE_COOKIE_DOMAIN);
        nettyCookie.setMaxAge(maxAge);

        // Use ServerCookieEncoder to format the cookie
        return ServerCookieEncoder.STRICT.encode(nettyCookie);
    }


    private static boolean isHttps(IMxRuntimeRequest request) throws URISyntaxException {
        var hasProtoHttps = hasHeaderValue(request, "X-Forwarded-Proto", "https");
        var hasSchemeHttps = hasHeaderValue(request, "X-Forwarded-Scheme", "https");
        return isSecure(Core.getConfiguration().getApplicationRootUrl()) || hasProtoHttps || hasSchemeHttps;
    }

    private static boolean hasHeaderValue(IMxRuntimeRequest request, String headerName, String value) {
        return value.equalsIgnoreCase(request.getHeader(headerName));
    }

    public static String getNonceFromCookie(IMxRuntimeRequest request) throws URISyntaxException {
        HttpServletRequest servletRequest = request.getHttpServletRequest();
        var isHttps = isHttps(request);
        var cookieName = isHttps ? COOKIE_HOST_PREFIX + NONCE_COOKIE_NAME : NONCE_COOKIE_NAME;
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies == null)
            return null;

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(cookieName))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    public static String sha256Hex(String input) {
        try {
            // Get an instance of the SHA-256 MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Compute the hash of the input string
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing SHA-256 hash", e);
        }
    }

    private static boolean isSecure(String rootUrl) throws URISyntaxException {
        URI uri = new URI(rootUrl);
        return uri.getScheme().equals("https");
    }

}
