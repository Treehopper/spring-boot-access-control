package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.User;
import org.springframework.security.access.AccessDeniedException;

/** The outcome of a permission check, including why. */
public record Decision(User actor, Action<?> action, Object target, boolean granted, String reason) {

    static Decision grant(User actor, Action<?> action, Object target, String reason) {
        return new Decision(actor, action, target, true, reason);
    }

    static Decision deny(User actor, Action<?> action, Object target, String reason) {
        return new Decision(actor, action, target, false, reason);
    }

    public Decision orElseThrow() {
        if (!granted) {
            throw new AccessDeniedException(toString());
        }
        return this;
    }

    @Override
    public String toString() {
        return (granted ? "GRANTED " : "DENIED  ") + actor + " " + action + " on " + target + " (" + reason + ")";
    }
}
