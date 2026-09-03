package mendixsso.implementation.error;

import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import mendixsso.implementation.ConfigurationManager;
import mendixsso.implementation.utils.LocaleExtractor;
import mendixsso.implementation.utils.MendixSSOLogger;
import mendixsso.proxies.ErrorPageTitle;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ErrorPageHandler {

    private ErrorPageHandler() {
    }

    public static void serveErrorPage(IMxRuntimeRequest request, IMxRuntimeResponse response, ErrorPageContent errorPageContent, Object... errorPageParameters) {
        try {
            response.setContentType("text/html");
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            response.setStatus(HttpURLConnection.HTTP_OK);

            // Determine the locale based on the request or use default if localization is disabled
            Locale locale = ConfigurationManager.getInstance().getLocalizationEnabled() ? LocaleExtractor.getLocale(request) : LocaleExtractor.DEFAULT_LANG;

            String pageContent;
            try {
                pageContent = ErrorPageRenderer.builder()
                        .errorPageContent(errorPageContent)
                        .messageParameters(errorPageParameters)
                        .locale(locale)
                        .render();
            } catch (Exception e) {
                MendixSSOLogger.error(e, "Error while rendering error page: %s", e.getMessage());
                // Fallback to a simple error message if rendering fails - Unexpected error
                pageContent = String.format("<html><body><h1>%s</h1></body></html>", ErrorPageTitle.INTERNAL_SERVER_ERROR.getCaption());
            }

            try (OutputStream out = response.getOutputStream()) {
                out.write(pageContent.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | IllegalArgumentException e) {
            MendixSSOLogger.error(e, "Error while serving error page: %s", e.getMessage());
        }
    }
}
