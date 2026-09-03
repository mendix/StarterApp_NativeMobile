package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import mendixsso.proxies.microflows.Microflows;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public final class LocaleExtractor {
    private static final String LOCALE_HEADER = "Accept-Language";
    public static final Locale DEFAULT_LANG = Locale.forLanguageTag("en-US");

    private LocaleExtractor() {
    }

    public static Locale getLocale(IMxRuntimeRequest request) {
        final var lang = request.getHeader(LOCALE_HEADER);
        if (StringUtils.isNotBlank(lang)) {
            try {
                final var languageRanges = Locale.LanguageRange.parse(lang);
                final var highestPriorityLang = languageRanges.get(0);

                Locale locale = Locale.forLanguageTag(highestPriorityLang.getRange());
                // Check if the locale is supported by the application
                if (Microflows.retrieveLanguage(Core.createSystemContext(), getMendixLanguageTag(locale) ) != null) {
                    return locale;
                } else {
                    MendixSSOLogger.warn("Locale[%s] is not supported by the application, using default[%s] locale.", locale, LocaleExtractor.DEFAULT_LANG);
                    return LocaleExtractor.DEFAULT_LANG;
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                MendixSSOLogger.error(e, "Invalid Accept-Language header: %s", lang);
            }
        }
        return DEFAULT_LANG;
    }

    /**
     * @param locale The Locale object to convert.
     * @return Mendix language tag in the format "language_country" (e.g., "en_US"). Otherwise, just the language code (e.g., "en").
     */
    public static String getMendixLanguageTag(Locale locale) {
        // Mendix uses a language tag format of "language_country" (e.g., "en_US").
        return StringUtils.isNotBlank(locale.getCountry()) ? locale.getLanguage() + "_" + locale.getCountry() : locale.getLanguage();
    }
}
