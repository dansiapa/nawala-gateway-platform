package id.nawala.platform.model;

public enum Role {
    USER,
    DEVELOPER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
    
    public boolean isAtLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }
}
