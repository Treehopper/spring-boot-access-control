package eu.hohenegger.accesscontrol.permission;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guards a controller method with the policy, e.g.
 * {@code @RequirePermission(action = "VOTE", on = "pollId")} checks {@link Action#VOTE} on the poll whose id is
 * passed as the method parameter {@code pollId}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@access.check('{action}', #{on}, authentication)")
public @interface RequirePermission {

    /** Name of an {@link Action} constant. */
    String action();

    /** Name of the method parameter holding the target's id. */
    String on();
}
