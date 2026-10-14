package eu.hohenegger.accesscontrol.permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which relationships allow which actions. Anything not explicitly allowed is denied.
 *
 * <pre>{@code
 * Policy.define()
 *     .allow(OWNERS).to(VIEW_POLLS, VOTE)
 *     .allow(DISTRICT_MANAGERS).to(VIEW_POLLS, CREATE_POLL)
 *     .build();
 * }</pre>
 *
 * In the application the builder is filled by {@link PolicyCustomizer} beans, see {@link PolicyConfiguration}.
 */
public final class Policy {

    /** One line of the policy: these people may do that. */
    public record Permission(Relationship<?> relationship, Action<?> action) {
    }

    private final List<Permission> permissions;

    private Policy(List<Permission> permissions) {
        this.permissions = List.copyOf(permissions);
    }

    public static Builder define() {
        return new Builder();
    }

    public <T> Decision decide(Context context, Action<T> action, T target) {
        List<Relationship<?>> allowed = relationshipsAllowedTo(action);
        for (Relationship<?> relationship : allowed) {
            Optional<String> reason = reasonFor(relationship, context, target);
            if (reason.isPresent()) {
                return Decision.grant(context.actor(), action, target, reason.get());
            }
        }
        String reason = allowed.isEmpty()
                ? "nobody may " + action
                : "not among " + allowed.stream().map(Relationship::name).collect(Collectors.joining(" or "));
        return Decision.deny(context.actor(), action, target, reason);
    }

    /** All permissions in the order they were added. */
    public List<Permission> permissions() {
        return permissions;
    }

    public List<Relationship<?>> relationshipsAllowedTo(Action<?> action) {
        return permissions.stream().filter(permission -> permission.action() == action).<Relationship<?>>map(Permission::relationship).toList();
    }

    public List<Action<?>> actionsAllowedFor(Relationship<?> relationship) {
        return permissions.stream().filter(permission -> permission.relationship() == relationship).<Action<?>>map(Permission::action).toList();
    }

    /** Human-readable summary, e.g. {@code owners -> [VIEW_POLLS, VOTE]}. */
    public Map<String, List<String>> describe() {
        Map<String, List<String>> description = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            description.computeIfAbsent(permission.relationship().name(), ignored -> new ArrayList<>()).add(permission.action().name());
        }
        return description;
    }

    // Safe: Builder.Allowance.to(...) only accepts actions whose target type the relationship can judge.
    @SuppressWarnings("unchecked")
    private static Optional<String> reasonFor(Relationship<?> relationship, Context context, Object target) {
        return ((Relationship<Object>) relationship).reasonFor(context, target);
    }

    public static final class Builder {

        private final Set<Permission> permissions = new LinkedHashSet<>();

        private Builder() {
        }

        /** Starts a rule: {@code allow(OWNERS).to(VOTE)}. */
        public <T> Allowance<T> allow(Relationship<T> relationship) {
            return new Allowance<>(relationship);
        }

        public Policy build() {
            return new Policy(new ArrayList<>(permissions));
        }

        public final class Allowance<T> {

            private final Relationship<T> relationship;

            private Allowance(Relationship<T> relationship) {
                this.relationship = relationship;
            }

            @SafeVarargs
            public final Builder to(Action<? extends T>... actions) {
                for (Action<? extends T> action : actions) {
                    permissions.add(new Permission(relationship, action));
                }
                return Builder.this;
            }
        }
    }
}
