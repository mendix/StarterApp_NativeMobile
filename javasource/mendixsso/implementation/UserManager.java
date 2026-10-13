package mendixsso.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import mendixsso.implementation.exception.IncompatibleUserTypeException;
import mendixsso.implementation.exception.UnauthorizedUserException;
import mendixsso.implementation.utils.ForeignIdentityUtils;
import mendixsso.implementation.utils.OpenIDUtils;
import mendixsso.proxies.ForeignIdentity;
import mendixsso.proxies.UserProfile;
import mendixsso.proxies.microflows.Microflows;
import system.proxies.Language;
import system.proxies.User;

import java.util.function.Consumer;

import static mendixsso.proxies.constants.Constants.getConsentToDeleteIncompatibleUsers;
import static mendixsso.proxies.constants.Constants.getLogNode;

public class UserManager {

    private UserManager() {
    }

    private static final ILogNode LOG = Core.getLogger(getLogNode());

    public static void authorizeUser(User user, String uuid) throws CoreException {
        final IContext c = Core.createSystemContext();
        c.startTransaction();
        try {
            LOG.debug("Re-authorizing user for existing session.");
            final boolean hasAccess = Microflows.retrieveUserRoles(c, user, uuid);
            if (!hasAccess) {
                throw new UnauthorizedUserException(uuid);
            }
            user.commit();
            c.endTransaction();
        } catch (Exception e) {
            LOG.warn(
                    String.format(
                            "Authorizing the user with UUID '%s' has failed. Triggering rollback.",
                            uuid));
            c.rollbackTransaction();
            throw e;
        }
    }

    public static User findOrCreateUser(UserProfile userProfile, final String userUUID, final String emailAddress) throws CoreException {
        final IContext c = Core.createSystemContext();
        c.startTransaction();
        try {

            ForeignIdentity foreignIdentity = ForeignIdentityUtils.retrieveForeignIdentity(c, userUUID);
            final User user;

            // Existing Foreign Identity
            if (foreignIdentity != null) {
                user = updateUser(c, userProfile, foreignIdentity, emailAddress);
                LOG.debug(
                        String.format(
                                "User associated to the foreign identity with UUID %s has been updated.",
                                userUUID));
            }

            // New Foreign Identity
            else {
                // Create a new user wih an associated foreign identity
                user = createUserWithForeignIdentity(c, userProfile, userUUID, emailAddress);
                LOG.debug(
                        String.format("New foreign identity with UUID %s has been created.", userUUID));
            }

            c.endTransaction();
            return user;
        } catch (Exception e) {
            LOG.warn(
                    String.format(
                            "Find or create user for UUID '%s' caught exception. Triggering rollback.",
                            userUUID));
            c.rollbackTransaction();
            throw e;
        }
    }

    private static User createUserWithForeignIdentity(
            IContext context, UserProfile userProfile, String uuid, String emailAddress) throws CoreException {
        final IMendixObject mxNewUser =
                UserMapper.getInstance().createUser(context, userProfile, uuid, emailAddress);
        final User user = User.initialize(context, mxNewUser);

        final boolean hasAccess = Microflows.retrieveUserRoles(context, user, uuid);
        if (!hasAccess) {
            throw new UnauthorizedUserException(uuid);
        }

        user.setPassword(OpenIDUtils.randomStrongPassword(48, 48, 7, 9, 6));
        updateUserLanguage(context, userProfile.getLocale(), user::setUser_Language);

        user.commit();
        ForeignIdentityUtils.createForeignIdentity(context, user, uuid);

        return user;
    }

    private static User updateUser(
            IContext context, UserProfile userProfile, ForeignIdentity foreignIdentity, String emailAddress)
            throws CoreException {
        final User user = foreignIdentity.getForeignIdentity_User();

        // because of a change to a new user entity type, it can happen that an old user of an
        // incompatible type is provided
        if (!UserMapper.getInstance().isCompatibleUserType(user)) {
            if (!getConsentToDeleteIncompatibleUsers()) {
                // we do not have consent, in which case we throw an exception that is rendered to
                // the
                // end-user
                throw new IncompatibleUserTypeException(user.getMendixObject().getType());
            } else {
                // we have consent, so delete the old user...
                user.delete();
                // note: the foreign identity is deleted because of delete behavior

                // and create a fresh new user instead
                return createUserWithForeignIdentity(context, userProfile, foreignIdentity.getUUID(), emailAddress);
            }
        }

        UserMapper.getInstance().updateUser(context, user, userProfile, foreignIdentity.getUUID(), emailAddress);

        final boolean hasAccess = Microflows.retrieveUserRoles(context, user,  foreignIdentity.getUUID());
        if (!hasAccess) {
            throw new UnauthorizedUserException( foreignIdentity.getUUID());
        }
        updateUserLanguage(context, userProfile.getLocale(), user::setUser_Language);
        user.commit();
        return user;
    }

    private static void updateUserLanguage(IContext context, String languageCode, Consumer<Language> languageConsumer) {
        if (ConfigurationManager.getInstance().getLocalizationEnabled()) {
            languageConsumer.accept(Microflows.retrieveLanguage(context, languageCode));
        } else {
            languageConsumer.accept(null);
        }
    }

}
