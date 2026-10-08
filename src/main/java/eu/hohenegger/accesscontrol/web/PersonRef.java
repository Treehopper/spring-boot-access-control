package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.User;

public record PersonRef(String username, String displayName) {

    public static PersonRef of(User user) {
        return new PersonRef(user.username(), user.displayName());
    }
}
