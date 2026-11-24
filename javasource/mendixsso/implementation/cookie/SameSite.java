package mendixsso.implementation.cookie;

public enum SameSite {
    Strict,
    Lax,
    None;

    public String attributeValue() {
        return name();
    }

    public static SameSite valueOfIgnoreCase(String name) {
        for (SameSite sameSite : values()) {
            if (sameSite.name().equalsIgnoreCase(name)) {
                return sameSite;
            }
        }
        throw new IllegalArgumentException(
                "No enum constant " + SameSite.class.getCanonicalName() + "." + name
        );
    }
}
