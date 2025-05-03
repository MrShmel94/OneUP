package OneUP.main.security;

public enum Role {
    USER,
    VIEWER,
    MODERATOR,
    ADMIN;

    public boolean hasAtLeast(Role required) {
        return this.ordinal() >= required.ordinal();
    }
}
