package mendixsso.implementation.error;

import mendixsso.implementation.ConfigurationManager;
import mendixsso.proxies.ErrorPageMessage;
import mendixsso.proxies.ErrorPageTitle;

import java.text.MessageFormat;
import java.util.Locale;

import static mendixsso.implementation.utils.LocaleExtractor.getMendixLanguageTag;

public enum ErrorPageContent {
    REQUESTED_RESOURCE_PATH_NOT_FOUND(ErrorPageTitle.RESOURCE_NOT_FOUND, ErrorPageMessage.RESOURCE_NOT_FOUND),
    OOPS_UNEXPECTED_EXCEPTION_OCCURRED(ErrorPageTitle.OOPS, ErrorPageMessage.UNEXPECTED_EXCEPTION_OCCURRED, true),
    INTERNAL_SERVER_ERROR_UNEXPECTED_EXCEPTION_OCCURRED(ErrorPageTitle.OOPS, ErrorPageMessage.UNEXPECTED_EXCEPTION_OCCURRED, true),
    UNAUTHORIZED(ErrorPageTitle.UNAUTHORIZED, ErrorPageMessage.UNAUTHORIZED, false, true),
    INCOMPATIBLE_USER_TYPE_ERROR(ErrorPageTitle.INCOMPATIBLE_USER_TYPE_ERROR, ErrorPageMessage.INCOMPATIBLE_USER_TYPE_ERROR, true),
    FAILED_TO_REGISTER_YOUR_ACCOUNT(ErrorPageTitle.INTERNAL_SERVER_ERROR, ErrorPageMessage.FAILED_TO_REGISTER_YOUR_ACCOUNT),
    FAILED_TO_INITIALIZE_SESSION(ErrorPageTitle.INTERNAL_SERVER_ERROR, ErrorPageMessage.FAILED_TO_INITIALIZE_SESSION),
    UNEXPECTED_IDP_RESPONSE(ErrorPageTitle.INTERNAL_SERVER_ERROR, ErrorPageMessage.UNEXPECTED_IDP_RESPONSE)
    ;

    private final ErrorPageTitle errorPageTitle;
    private final ErrorPageMessage errorPageMessage;
    private final boolean isMessageHtml;
    private final boolean isMessageParameterized;
    private static final String HOME_PAGE = ConfigurationManager.getInstance().getIndexPage();

    ErrorPageContent(ErrorPageTitle errorPageTitle, ErrorPageMessage errorPageMessage) {
        this(errorPageTitle, errorPageMessage, false, false);
    }

    ErrorPageContent(ErrorPageTitle errorPageTitle, ErrorPageMessage errorPageMessage, boolean isMessageParameterized) {
        this(errorPageTitle, errorPageMessage, isMessageParameterized, false);
    }

    ErrorPageContent(ErrorPageTitle errorPageTitle, ErrorPageMessage errorPageMessage, boolean isMessageParameterized, boolean isMessageHtml) {
        this.errorPageTitle = errorPageTitle;
        this.errorPageMessage = errorPageMessage;
        this.isMessageHtml = isMessageHtml;
        this.isMessageParameterized = isMessageParameterized;
    }


    public String getTitle(Locale locale) {
        return errorPageTitle.getCaption(getMendixLanguageTag(locale));
    }


    public String getMessage(Locale locale) {
        return errorPageMessage.getCaption(getMendixLanguageTag(locale));
    }

    public String getMessage(Locale locale, Object... parameters) {
        // Single quote character (') has a special meaning in MessageFormat pattern; it is used to represent a section within the message pattern that will not be formatted.
        // We need to escape by using double quote on the fly instead of using double quotes in .properties files.
        String message = errorPageMessage.getCaption(getMendixLanguageTag(locale)).replace("'", "''");
        return MessageFormat.format(message, parameters);
    }

    public String getHomePage() {
        return HOME_PAGE;
    }

    public String getMetaTitle(Locale locale) {
        return ErrorPageTitle.META_TITLE.getCaption(getMendixLanguageTag(locale));
    }

    public String getButtonTitle(Locale locale) {
        return ErrorPageTitle.BUTTON_TITLE.getCaption(getMendixLanguageTag(locale));
    }

    public boolean isMessageHtml() {
        return isMessageHtml;
    }

    public boolean isMessageParameterized() {
        return isMessageParameterized;
    }

}
