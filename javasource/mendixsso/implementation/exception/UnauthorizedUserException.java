package mendixsso.implementation.exception;

public class UnauthorizedUserException extends RuntimeException {

    private final String userUUID;

    public UnauthorizedUserException(String userUUID) {
        super("Unauthorized user with UUID: " + userUUID);
        this.userUUID = userUUID;
    }

    public String getUserUUID() {
        return userUUID;
    }
}