package eu.hohenegger.accesscontrol.domain;

import java.util.Objects;

public final class User {

    private final String username;
    private final String displayName;

    User(String username, String displayName) {
        this.username = Objects.requireNonNull(username);
        this.displayName = Objects.requireNonNull(displayName);
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User user && user.username.equals(username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return username;
    }
}
