package mendixsso.implementation.handlers;

import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.ISession;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import mendixsso.implementation.SessionManager;
import mendixsso.implementation.UserManager;
import mendixsso.implementation.UserMapper;
import mendixsso.implementation.error.ErrorPageContent;
import mendixsso.implementation.error.ErrorPageHandler;
import mendixsso.implementation.exception.IncompatibleUserTypeException;
import mendixsso.implementation.exception.UnauthorizedUserException;
import mendixsso.implementation.utils.MendixSSOLogger;
import mendixsso.implementation.utils.OpenIDUtils;
import mendixsso.proxies.UserProfile;
import system.proxies.User;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class DefaultLoginHandler implements ILoginHandler {

    @Override
    public void onCompleteLogin(IContext context, UserProfile userProfile, String userUUID, String emailAddress, OIDCTokenResponse oidcTokenResponse, String continuation, IMxRuntimeRequest req, IMxRuntimeResponse resp) {
        final User user;
        try {
            user = UserManager.findOrCreateUser(userProfile, userUUID, emailAddress);
        } catch (UnauthorizedUserException e) {
            MendixSSOLogger.debug("Since the user with uuid %s does not have a role, the user is not authorized to use this application.", e.getUserUUID());
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.UNAUTHORIZED);
            return;
        } catch (IncompatibleUserTypeException e) {
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.INCOMPATIBLE_USER_TYPE_ERROR, e.getActualType(), UserMapper.getInstance().getUserEntityType(), UserMapper.getInstance().getUpdateUserMicroflowName());
            return;
        } catch (Exception e) {
            MendixSSOLogger.error(e, "Failed to register user: %s", e.getMessage());
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.FAILED_TO_REGISTER_YOUR_ACCOUNT);
            return;
        }

        try {
            SessionManager.createSessionForUser(context, resp, user, oidcTokenResponse);
            if (continuation != null) {
                continuation = URLDecoder.decode(continuation, StandardCharsets.UTF_8);
            }
            OpenIDUtils.redirectToIndex(req, resp, continuation);
        } catch (ParseException e) {
            MendixSSOLogger.error(e, "Unexpected IdP server response: %s", e.getMessage());
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.UNEXPECTED_IDP_RESPONSE);
        } catch (Exception e) {
            MendixSSOLogger.error(e, "Failed to initialize session: %s", e.getMessage());
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.FAILED_TO_INITIALIZE_SESSION);
        }
    }

    @Override
    public void onAlreadyHasSession(IContext context, User user, ISession session, String uuid, String continuation, IMxRuntimeRequest req, IMxRuntimeResponse resp) {
        try {
            UserManager.authorizeUser(user, uuid);
        } catch (UnauthorizedUserException e) {
            MendixSSOLogger.debug("Since the user with uuid %s does not have a role, the user is not authorized to use this application.", e.getUserUUID());
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.UNAUTHORIZED);
            return;
        } catch (Exception e) {
            MendixSSOLogger.error(e, "Failed to register user: %s", e.getMessage());
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.FAILED_TO_REGISTER_YOUR_ACCOUNT);
            return;
        }

        try {
            SessionManager.writeSessionCookies(session, resp);
            if (continuation != null) {
                continuation = URLDecoder.decode(continuation, StandardCharsets.UTF_8);
            }
            OpenIDUtils.redirectToIndex(req, resp, continuation);
        } catch (Exception e) {
            MendixSSOLogger.error(e, "Failed to initialize session: %s", e.getMessage());
            ErrorPageHandler.serveErrorPage(req, resp, ErrorPageContent.FAILED_TO_INITIALIZE_SESSION);
        }
    }

}
