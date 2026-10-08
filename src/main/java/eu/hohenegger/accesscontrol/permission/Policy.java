package eu.hohenegger.accesscontrol.permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Which conditions allow which actions. Anything not explicitly allowed is denied.
 *
 * <pre>{@code
 * Policy.define()
 *     .allow(VIEW_MEMBERS).to(owners(), districtManagers())
 *     .allow(VOTE).to(owners().of(Poll::association))
 *     .build();
 * }</pre>
 */
public final class Policy {

    private final Map<Action<?>, List<Condition<?>>> rules;

    private Policy(Map<Action<?>, List<Condition<?>>> rules) {
        this.rules = rules;
    }

    public static Builder define() {
        return new Builder();
    }

    public <T> Decision decide(Context context, Action<T> action, T target) {
        List<Condition<T>> conditions = conditionsFor(action);
        for (Condition<T> condition : conditions) {
            Optional<String> reason = condition.grantReason(context, target);
            if (reason.isPresent()) {
                return Decision.grant(context.actor(), action, target, reason.get());
            }
        }
        String reason = conditions.isEmpty()
                ? "no rule allows " + action
                : "is not " + conditions.stream().map(Condition::description).collect(Collectors.joining(" or "));
        return Decision.deny(context.actor(), action, target, reason);
    }

    /** Human-readable rules, e.g. {@code VOTE -> [owner]}. */
    public Map<String, List<String>> describe() {
        Map<String, List<String>> description = new LinkedHashMap<>();
        rules.forEach((action, conditions) ->
                description.put(action.name(), conditions.stream().map(Condition::description).toList()));
        return description;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<Condition<T>> conditionsFor(Action<T> action) {
        return (List) rules.getOrDefault(action, List.of());
    }

    public static final class Builder {

        private final Map<Action<?>, List<Condition<?>>> rules = new LinkedHashMap<>();

        private Builder() {
        }

        public <T> Rule<T> allow(Action<T> action) {
            return new Rule<>(action);
        }

        public Policy build() {
            Map<Action<?>, List<Condition<?>>> copy = new LinkedHashMap<>();
            rules.forEach((action, conditions) -> copy.put(action, List.copyOf(conditions)));
            return new Policy(copy);
        }

        public final class Rule<T> {

            private final Action<T> action;

            private Rule(Action<T> action) {
                this.action = action;
            }

            @SafeVarargs
            public final Builder to(Condition<T>... conditions) {
                rules.computeIfAbsent(action, ignored -> new ArrayList<>()).addAll(List.of(conditions));
                return Builder.this;
            }
        }
    }
}
