package mendixsso.implementation.oidp;

import com.mendix.core.Core;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import mendixsso.implementation.ConfigurationManager;
import mendixsso.implementation.utils.OpenIDUtils;
import mendixsso.proxies.constants.Constants;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static mendixsso.proxies.constants.Constants.getLogNode;

public class IdentityProviderMetaDataCache {

    private static IdentityProviderMetaDataCache instance;

    private IdentityProviderMetaData identityProviderMeta;


    public static synchronized IdentityProviderMetaDataCache getInstance() {
        if (instance == null) {
            instance = new IdentityProviderMetaDataCache();
        }
        return instance;
    }

    private static IdentityProviderMetaData loadConfiguration() throws IOException, ParseException {

        try {
            final String discoveryUrl = OpenIDUtils.ensureEndsWithSlash(ConfigurationManager.getInstance().getOpenIdConnectProvider()) + Constants.getOpenIdConnectDiscoveryPath();

            final URI providerConfigurationURL = new URI(discoveryUrl);

            final String providerInfo;
            try (final InputStream stream = providerConfigurationURL.toURL().openStream()) {
                providerInfo = OpenIDUtils.convertInputStreamToString(stream);
            }

            final OIDCProviderMetadata providerMetadata = OIDCProviderMetadata.parse(providerInfo);
            final String decryptedClientSecret = ConfigurationManager.getInstance().getEnvironmentPassword();

            final ClientID clientId = new ClientID(ConfigurationManager.getInstance().getEnvironmentUUID());

            final IDTokenValidator idTokenValidator = getIdTokenValidator(providerMetadata, clientId);

            Core.getLogger(getLogNode()).info("Cached identity provider meta data.");

            return new IdentityProviderMetaData(clientId, decryptedClientSecret, providerMetadata,
                    idTokenValidator, new ResponseType(ResponseType.Value.CODE));

        } catch (MalformedURLException | URISyntaxException e) {
            return ExceptionUtils.rethrow(e);
        }

    }

    private static IDTokenValidator getIdTokenValidator(OIDCProviderMetadata providerMetadata, ClientID clientId) throws MalformedURLException {

        final DefaultResourceRetriever resourceRetriever = getDefaultResourceRetriever();
        // set up the validator
        final IDTokenValidator idTokenValidator = new IDTokenValidator(
                providerMetadata.getIssuer(),
                clientId,
                JWSAlgorithm.RS256,
                providerMetadata.getJWKSetURI().toURL(), resourceRetriever);

        idTokenValidator.setMaxClockSkew(ConfigurationManager.getInstance().getTokenValidatorMaxClockSkew().intValue());
        return idTokenValidator;
    }

    private static DefaultResourceRetriever getDefaultResourceRetriever() {

        /**
         *  timeouts for JWK set retrieval, in
         * milliseconds. zero for infinite but not allowed. Must not be negative.
         */
        final int connectTimeout = ConfigurationManager.getInstance().getRemoteJWKSConnectTimeout();
        final int readTimeout = ConfigurationManager.getInstance().getRemoteJWKSReadTimeout();
        final DefaultResourceRetriever resourceRetriever;

        if (connectTimeout <= 0 || readTimeout <= 0) {
            resourceRetriever = new DefaultResourceRetriever();
        } else {
            resourceRetriever = new DefaultResourceRetriever(
                    connectTimeout,
                    readTimeout);
        }
        return resourceRetriever;
    }

    public IdentityProviderMetaData getIdentityProviderMetaData() throws IOException, ParseException {
        if (identityProviderMeta == null || identityProviderMeta.isExpired()) {
            synchronized (this) {
                identityProviderMeta = loadConfiguration();
            }
        }
        return identityProviderMeta;
    }
}
