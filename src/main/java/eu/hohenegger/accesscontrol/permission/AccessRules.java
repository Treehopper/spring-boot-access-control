package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.Poll;
import eu.hohenegger.accesscontrol.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static eu.hohenegger.accesscontrol.permission.Audit.LOG;

/**
 * Bridges {@link RequirePermission} to the policy: resolves the actor and the target by id, asks
 * {@link Permissions} and logs the enforced decision. Unknown targets are denied rather than reported as missing,
 * so that ids of things one may not see are not confirmed.
 */
@Component("access")
public class AccessRules {

    private final Community community;
    private final Permissions permissions;

    public AccessRules(Community community, Permissions permissions) {
        this.community = community;
        this.permissions = permissions;
    }

    public boolean check(String actionName, Object targetId, Authentication authentication) {
        Action<?> action = Action.named(actionName);
        String id = String.valueOf(targetId);
        Optional<User> actor = community.findUser(authentication.getName());
        if (actor.isEmpty()) {
            LOG.warn("DENIED  {} {} on {} (unknown user)", authentication.getName(), action, id);
            return false;
        }
        Optional<?> target = resolve(action.targetType(), id);
        if (target.isEmpty()) {
            LOG.warn("DENIED  {} {} on {} {} (no such {})", actor.get(), action,
                    action.targetType().getSimpleName().toLowerCase(), id, action.targetType().getSimpleName().toLowerCase());
            return false;
        }
        Decision decision = decide(actor.get(), action, target.get());
        if (decision.granted()) {
            LOG.info("{}", decision);
        } else {
            LOG.warn("{}", decision);
        }
        return decision.granted();
    }

    private <T> Decision decide(User actor, Action<T> action, Object target) {
        return permissions.check(actor).may(action).on(action.targetType().cast(target));
    }

    private Optional<?> resolve(Class<?> type, String id) {
        if (type == Association.class) {
            return community.findAssociation(id);
        }
        if (type == Poll.class) {
            return community.findPoll(id);
        }
        if (type == Message.class) {
            return community.findMessage(id);
        }
        if (type == User.class) {
            return community.findUser(id);
        }
        throw new IllegalStateException("No resolver for " + type);
    }
}
