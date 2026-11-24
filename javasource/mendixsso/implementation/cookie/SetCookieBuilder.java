package mendixsso.implementation.cookie;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SetCookieBuilder {

    public static final long OMIT_MAX_AGE = Long.MIN_VALUE;

    // IMF-fixdate per RFC 7231 §7.1.1.1: fixed 2-digit day, English month/day, GMT.
    // NOT DateTimeFormatter.RFC_1123_DATE_TIME, which emits a single-digit day.
    private static final DateTimeFormatter IMF_FIXDATE =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);

    // Byte budget for the fixed "; Attr=..." scaffolding (Max-Age + 19-digit long,
    // Expires IMF-fixdate, Path/Domain attribute names, Secure, HttpOnly, SameSite).
    // Path/Domain values are added separately so the buffer is sized in one allocation.
    private static final int ATTR_OVERHEAD = 128;

    private static final String SECURE_PREFIX = "__Secure-";
    private static final String HOST_PREFIX = "__Host-";
    private static final int DOMAIN_MAX_LENGTH = 253;
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
        "^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*$"
    );

    private final String name;
    private final String value;
    private String path;
    private String domain;
    private long maxAge = OMIT_MAX_AGE;
    private boolean secure;
    private boolean httpOnly;
    private SameSite sameSite;

    private SetCookieBuilder(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static SetCookieBuilder of(String name, String value) {
        return new SetCookieBuilder(name, value);
    }

    public SetCookieBuilder path(String path) {
        this.path = path;
        return this;
    }

    public SetCookieBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    public SetCookieBuilder maxAge(long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public SetCookieBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public SetCookieBuilder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public SetCookieBuilder sameSite(SameSite sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    public String build() {
        return build(System.currentTimeMillis());
    }

    public String build(long nowMillis) {
        validateName(name);
        validateValue(value);
        if (path != null) {
            validatePath(path);
        }
        boolean domainPresent = domain != null && !domain.isEmpty();
        if (domainPresent) {
            validateDomain(domain);
        }
        enforcePrefixInvariants(domainPresent);
        enforceSameSiteSecure();
        validateMaxAge();

        int estimate = name.length() + value.length() + ATTR_OVERHEAD
            + (path != null ? path.length() : 0)
            + (domainPresent ? domain.length() : 0);
        StringBuilder sb = new StringBuilder(estimate);
        sb.append(name).append('=').append(value);

        if (maxAge != OMIT_MAX_AGE) {
            sb.append("; Max-Age=").append(maxAge);
            sb.append("; Expires=");
            long expiresMillis = nowMillis + (maxAge * 1000L);
            sb.append(IMF_FIXDATE.format(Instant.ofEpochMilli(expiresMillis).atZone(ZoneOffset.UTC)));
        }
        if (path != null) {
            sb.append("; Path=").append(path);
        }
        if (domainPresent) {
            sb.append("; Domain=").append(domain);
        }
        if (secure) {
            sb.append("; Secure");
        }
        if (httpOnly) {
            sb.append("; HTTPOnly");
        }
        if (sameSite != null) {
            sb.append("; SameSite=").append(sameSite.attributeValue());
        }
        return sb.toString();
    }

    private static void validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("cookie name must not be null");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("cookie name must not be empty (RFC 6265 §4.1.1)");
        }
        if (name.charAt(0) == '$') {
            throw new IllegalArgumentException("cookie name must not start with '$' (reserved by RFC 2109 convention): " + name);
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '\r' || c == '\n') {
                throw new IllegalArgumentException("CR/LF not allowed in cookie name (header injection) at index " + i);
            }
            if (!isValidNameChar(c)) {
                throw new IllegalArgumentException(
                    "cookie name contains invalid char 0x" + Integer.toHexString(c) + " at index " + i + " (RFC 6265 §4.1.1 / RFC 2616 token)");
            }
        }
    }

    private static void validateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("cookie value must not be null");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n') {
                throw new IllegalArgumentException("CR/LF not allowed in cookie value (header injection) at index " + i);
            }
            if (!isValidValueChar(c)) {
                throw new IllegalArgumentException(
                    "cookie value contains invalid char 0x" + Integer.toHexString(c) + " at index " + i + " (RFC 6265 §4.1.1 cookie-octet)");
            }
        }
    }

    private static void validatePath(String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\r' || c == '\n') {
                throw new IllegalArgumentException("CR/LF not allowed in Path (header injection) at index " + i);
            }
            if (c < 0x20 || c > 0x7E || c == ';') {
                throw new IllegalArgumentException(
                    "Path contains invalid char 0x" + Integer.toHexString(c) + " at index " + i + " (RFC 6265 §5.2.4 path-value)");
            }
        }
    }

    private static void validateDomain(String domain) {
        for (int i = 0; i < domain.length(); i++) {
            char c = domain.charAt(i);
            if (c == '\r' || c == '\n') {
                throw new IllegalArgumentException("CR/LF not allowed in Domain (header injection) at index " + i);
            }
        }
        if (domain.length() > DOMAIN_MAX_LENGTH) {
            throw new IllegalArgumentException("Domain exceeds " + DOMAIN_MAX_LENGTH + " octets (RFC 1035 §2.3.4)");
        }
        if (!DOMAIN_PATTERN.matcher(domain).matches()) {
            throw new IllegalArgumentException("Domain is not a valid LDH hostname (RFC 1034 §3.5 / RFC 1035 §2.3.4): " + domain);
        }
    }

    private void enforcePrefixInvariants(boolean domainPresent) {
        if (startsWithIgnoreCase(name, HOST_PREFIX)) {
            if (!secure) {
                throw new IllegalArgumentException("__Host- prefix requires Secure (RFC 6265bis §4.1.3.2)");
            }
            if (!"/".equals(path)) {
                throw new IllegalArgumentException("__Host- prefix requires Path=\"/\" (RFC 6265bis §4.1.3.2)");
            }
            if (domainPresent) {
                throw new IllegalArgumentException("__Host- prefix forbids Domain (RFC 6265bis §4.1.3.2)");
            }
        } else if (startsWithIgnoreCase(name, SECURE_PREFIX)) {
            if (!secure) {
                throw new IllegalArgumentException("__Secure- prefix requires Secure (RFC 6265bis §4.1.3.1)");
            }
        }
    }

    private void enforceSameSiteSecure() {
        if (sameSite == SameSite.None && !secure) {
            throw new IllegalArgumentException("SameSite=None requires Secure (RFC 6265bis §5.7 step 19)");
        }
    }

    private void validateMaxAge() {
        if (maxAge != OMIT_MAX_AGE && maxAge < 0) {
            throw new IllegalArgumentException("Max-Age must be non-negative (RFC 6265 §5.2.2); use OMIT_MAX_AGE to skip");
        }
    }

    private static boolean isValidNameChar(char c) {
        if (c < 0x21 || c > 0x7E) {
            return false;
        }
        switch (c) {
            case '(': case ')': case '<': case '>': case '@':
            case ',': case ';': case ':': case '\\': case '"':
            case '/': case '[': case ']': case '?': case '=':
            case '{': case '}':
                return false;
            default:
                return true;
        }
    }

    private static boolean isValidValueChar(char c) {
        return c == 0x21
            || (c >= 0x23 && c <= 0x2B)
            || (c >= 0x2D && c <= 0x3A)
            || (c >= 0x3C && c <= 0x5B)
            || (c >= 0x5D && c <= 0x7E);
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.length() >= prefix.length()
            && s.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
