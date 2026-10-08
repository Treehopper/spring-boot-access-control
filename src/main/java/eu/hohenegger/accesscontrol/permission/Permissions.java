package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.User;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static eu.hohenegger.accesscontrol.permission.Audit.LOG;

/**
 * Fluent entry point to the policy:
 * <pre>{@code
 * permissions.check(bob).may(VOTE).on(poll).granted()
 * permissions.check(alice).may(READ_MESSAGE).filter(community.messages())
 * }</pre>
 */
@Component
public class Permissions {

    private final Policy policy;
    private final Community community;

    public Permissions(Policy policy, Community community) {
        this.policy = policy;
        this.community = community;
    }

    public Check check(User actor) {
        return new Check(actor);
    }

    public final class Check {

        private final User actor;

        private Check(User actor) {
            this.actor = actor;
        }

        public <T> Question<T> may(Action<T> action) {
            return new Question<>(actor, action);
        }
    }

    public final class Question<T> {

        private final User actor;
        private final Action<T> action;

        private Question(User actor, Action<T> action) {
            this.actor = actor;
            this.action = action;
        }

        public Decision on(T target) {
            Decision decision = policy.decide(new Context(actor, community.now(), community), action, target);
            LOG.debug("{}", decision);
            return decision;
        }

        /** Keeps the candidates the actor may act on. */
        public List<T> filter(Collection<? extends T> candidates) {
            List<T> granted = candidates.stream().<T>map(candidate -> candidate)
                    .filter(candidate -> on(candidate).granted())
                    .toList();
            LOG.info("FILTERED {} {}: {} of {} granted", actor, action, granted.size(), candidates.size());
            return granted;
        }
    }
}
