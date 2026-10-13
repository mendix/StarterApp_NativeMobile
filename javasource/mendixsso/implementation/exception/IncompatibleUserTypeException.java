package mendixsso.implementation.exception;


public class IncompatibleUserTypeException extends RuntimeException {

    private final String actualType;

    public IncompatibleUserTypeException(String actualType) {
        super("Incompatible user type: " + actualType);
        this.actualType = actualType;
    }

    public String getActualType() {
        return actualType;
    }
}
