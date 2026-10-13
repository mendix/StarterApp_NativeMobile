
package mendixsso.implementation;

import mendixsso.implementation.utils.OpenIDUtils;
import mendixsso.proxies.constants.Constants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ConfigurationManager {

    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    private ConfigurationManager() {
    }

    private static class SingletonInstanceHolder {
        private static final ConfigurationManager INSTANCE = new ConfigurationManager();
    }

    public static ConfigurationManager getInstance() {
        return SingletonInstanceHolder.INSTANCE;
    }

    public String loadValueFromEnvOrDefault(String envVarName, String defaultValue) {
        return loadValueFromEnvOrDefault(envVarName, defaultValue, Function.identity());
    }

    public <T> T loadValueFromEnvOrDefault(String envVarName, T defaultValue, Function<String, T> convertFromString) {
        //noinspection unchecked
        return (T) cache.computeIfAbsent(envVarName, _key -> {
            var envVariableVal = System.getenv(envVarName);
            if (null != envVariableVal) {
                return convertFromString.apply(envVariableVal);
            } else {
                return defaultValue;
            }
        });
    }

    public String getEnvironmentPassword() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_EnvironmentPassword",
                Constants.getEnvironmentPassword()
        );
    }

    public String getEnvironmentUUID() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_EnvironmentUUID",
                Constants.getEnvironmentUUID()
        );
    }

    public String getOpenIDPrefix() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_OpenIDPrefix",
                "https://mxid2.mendixcloud.com/mxid2/");
    }

    public String getIndexPage() {

        return OpenIDUtils.ensureStartsWithSlash(
                loadValueFromEnvOrDefault(
                        "MendixSSO_IndexPage",
                        Constants.getIndexPage()
                )
        );
    }

    public String getOpenIdConnectProvider() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_OpenIdConnectProvider",
                Constants.getOpenIdConnectProvider()
        );
    }

    public String getOpenIDConnectScopes() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_OpenIdConnectScopes",
                Constants.getOpenIdConnectScopes()
        );
    }

    public String getRolesLocation() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_RolesLocation",
                Constants.getRolesLocation()
        );
    }

    public String getDefaultSignupHint() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_SignupHint",
                Constants.getSignupHint()
        );
    }

    public boolean getSilentAuthentication() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_SilentAuthentication",
                Constants.getSilentAuthentication(),
                Boolean::parseBoolean
        );
    }

    public boolean getLocalizationEnabled() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_LocalizationEnabled",
                Constants.getLocalizationEnabled(),
                Boolean::parseBoolean
        );
    }

    public Long getTokenValidatorMaxClockSkew() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_TokenValidatorMaxClockSkew",
                Constants.getTokenValidatorMaxClockSkew(),
                Long::parseLong
        );
    }

    public String getAllowedContinuationURLs() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_AllowedContinuationURLs",
                ""
        );
    }

    /**
     * The HTTP connect timeout for JWK set retrieval, in
     * milliseconds. Default Set to 500 milliseconds. zero for infinite. Must not be negative.
     */
    public Integer getRemoteJWKSConnectTimeout() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_RemoteJWKSHttpConnectTimeout",
                500, Integer::parseInt
        );
    }

    /**
     * The HTTP read timeout for JWK set retrieval, in
     * milliseconds. Default Set to 500 milliseconds.  zero for infinite. Must not be negative.
     */
    public Integer getRemoteJWKSReadTimeout() {
        return loadValueFromEnvOrDefault(
                "MendixSSO_RemoteJWKSHttpReadTime",
                500, Integer::parseInt
        );
    }

}