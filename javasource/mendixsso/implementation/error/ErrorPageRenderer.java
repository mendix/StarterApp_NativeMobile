package mendixsso.implementation.error;

import com.mendix.core.Core;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

public class ErrorPageRenderer {

    private static final String ERROR_PAGE_TEMPLATE;
    private final ErrorPageContent errorPageContent;
    private final Object[] messageParameters;
    private final Locale locale;

    static {
        ERROR_PAGE_TEMPLATE = Paths.get(Core.getConfiguration().getResourcesPath().getPath(), "mendixsso", "templates", "sso-error.html").toString();
    }

    private ErrorPageRenderer(ErrorPageContent errorPageContent, Object[] messageParameters, Locale locale) {
        this.errorPageContent = errorPageContent;
        this.messageParameters = messageParameters;
        this.locale = locale;
    }


    private String render() {
        TemplateVariables templateVariables = new TemplateVariables();

        templateVariables.putString("{{METATITLE}}", errorPageContent.getMetaTitle(locale));
        templateVariables.putString("{{TITLE}}", errorPageContent.getTitle(locale));
        String message = errorPageContent.isMessageParameterized() ? errorPageContent.getMessage(locale, messageParameters) : errorPageContent.getMessage(locale);

        if (errorPageContent.isMessageHtml()) {
            templateVariables.putHtml("{{MESSAGE}}", message);
        } else {
            templateVariables.putString("{{MESSAGE}}", message);
        }

        templateVariables.putString("{{HOMEPAGE}}", errorPageContent.getHomePage());
        templateVariables.putString("{{BUTTONTITLE}}", errorPageContent.getButtonTitle(locale));
        return TemplateRenderer.render(ERROR_PAGE_TEMPLATE, templateVariables);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ErrorPageContent errorPageContent;
        private Object[] messageParameters;
        private Locale locale;

        public Builder errorPageContent(ErrorPageContent errorPageContent) {
            this.errorPageContent = Objects.requireNonNull(errorPageContent, "errorPageContent must not be null");
            return this;
        }

        public Builder messageParameters(Object... messageParameters) {
            this.messageParameters = Objects.requireNonNull(messageParameters, "messageParameters must not be null");
            return this;
        }

        public Builder locale(Locale locale) {
            this.locale = Objects.requireNonNull(locale, "locale must not be null");
            return this;
        }

        /**
         * Single use method to render the error page HTML content using the provided parameters.
         *
         * @return String containing the rendered HTML content of the error page.
         * @throws IllegalArgumentException if required parameters are missing
         */
        public String render() {
            if (errorPageContent == null) {
                throw new IllegalArgumentException("errorPageContent must not be null");
            }
            if (errorPageContent.isMessageParameterized() && messageParameters == null) {
                throw new IllegalArgumentException("messageParameters must not be null when errorPageContent is parameterized");
            }
            if (locale == null) {
                throw new IllegalArgumentException("locale must not be null");
            }
            return new ErrorPageRenderer(errorPageContent, messageParameters, locale).render();
        }
    }


}
