package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.permission.model.ActionEntity;
import eu.hohenegger.accesscontrol.permission.model.PermissionEntity;
import eu.hohenegger.accesscontrol.permission.model.RelationshipEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Which relationships allow which actions. Anything not explicitly allowed is denied.
 * <p>
 * The fluent builder produces the JPA model of the policy, ready to be stored; a {@code Policy} is created from the
 * stored model:
 *
 * <pre>{@code
 * List<PermissionEntity> model = Policy.define()
 *     .allow(OWNERS).to(VIEW_POLLS, VOTE)
 *     .allow(DISTRICT_MANAGERS).to(VIEW_POLLS, CREATE_POLL)
 *     .build();
 * Policy policy = Policy.of(policyStore.replaceWith(model));
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

    /**
     * The policy described by the stored model. Relationships and actions are looked up by name among those defined
     * in code, because only the code knows how to tell whether a relationship holds.
     *
     * @throws IllegalStateException if the model names an unknown relationship or action, or pairs a relationship
     *                               with an action on a target it cannot judge
     */
    public static Policy of(Collection<PermissionEntity> model) {
        List<Permission> permissions = new ArrayList<>();
        for (PermissionEntity entity : model) {
            try {
                Relationship<?> relationship = Relationship.named(entity.relationship().name());
                Action<?> action = Action.named(entity.action().name());
                if (!relationship.fits(action)) {
                    throw new IllegalStateException("Stored permission " + entity + " is invalid: " + relationship
                            + " relate to " + relationship.targetType().getSimpleName() + ", but " + action + " is done to "
                            + action.targetType().getSimpleName());
                }
                permissions.add(new Permission(relationship, action));
            } catch (IllegalArgumentException unknown) {
                throw new IllegalStateException("Stored permission " + entity + " is invalid: " + unknown.getMessage(), unknown);
            }
        }
        return new Policy(permissions);
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

    // Safe: Builder.Allowance.to(...) and of(...) only accept actions whose target type the relationship can judge.
    @SuppressWarnings("unchecked")
    private static Optional<String> reasonFor(Relationship<?> relationship, Context context, Object target) {
        return ((Relationship<Object>) relationship).reasonFor(context, target);
    }

    /** Collects the rules as a JPA model; equal rules from several customizers are stored once. */
    public static final class Builder {

        private final Map<String, RelationshipEntity> relationships = new LinkedHashMap<>();
        private final Map<String, ActionEntity> actions = new LinkedHashMap<>();
        private final Map<List<String>, PermissionEntity> permissions = new LinkedHashMap<>();
        private String definedBy;

        private Builder() {
        }

        /** Starts a rule: {@code allow(OWNERS).to(VOTE)}. */
        public <T> Allowance<T> allow(Relationship<T> relationship) {
            return new Allowance<>(relationship);
        }

        /** Records which bean contributes the following rules. */
        Builder definedBy(String source) {
            this.definedBy = source;
            return this;
        }

        /** The policy as unsaved JPA entities, in the order the rules were added. */
        public List<PermissionEntity> build() {
            return List.copyOf(permissions.values());
        }

        public final class Allowance<T> {

            private final Relationship<T> relationship;

            private Allowance(Relationship<T> relationship) {
                this.relationship = relationship;
            }

            @SafeVarargs
            public final Builder to(Action<? extends T>... actions) {
                for (Action<? extends T> action : actions) {
                    add(relationship, action);
                }
                return Builder.this;
            }
        }

        private void add(Relationship<?> relationship, Action<?> action) {
            RelationshipEntity relationshipEntity = relationships.computeIfAbsent(relationship.name(), name ->
                    new RelationshipEntity(name, relationship.targetType().getSimpleName(), relationship.description()));
            ActionEntity actionEntity = actions.computeIfAbsent(action.name(), name ->
                    new ActionEntity(name, action.targetType().getSimpleName(), action.description()));
            permissions.computeIfAbsent(List.of(relationship.name(), action.name()), key ->
                    new PermissionEntity(relationshipEntity, actionEntity, permissions.size(), definedBy));
        }
    }
}
