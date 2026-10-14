package eu.hohenegger.accesscontrol.permission;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * How someone relates to the target of an action, e.g. "owners" or "district managers". A relationship that holds
 * explains itself, so that every grant can be logged with its reason. The type parameter limits which targets the
 * relationship can judge: {@code Relationship<Message>} only makes sense for actions on messages.
 * <p>
 * Features may define their own relationships with {@link #define(String, Class, String, BiFunction)}.
 */
public final class Relationship<T> {

    private static final Map<String, Relationship<?>> BY_NAME = new ConcurrentHashMap<>();
    private static final List<Relationship<?>> ALL = new CopyOnWriteArrayList<>();

    private final String name;
    private final Class<T> targetType;
    private final String description;
    private final BiFunction<Context, T, Optional<String>> test;

    private Relationship(String name, Class<T> targetType, String description, BiFunction<Context, T, Optional<String>> test) {
        this.name = name;
        this.targetType = targetType;
        this.description = description;
        this.test = test;
    }

    /**
     * Defines a new relationship.
     *
     * @param name        unique plural name for PERMISSIONS.md and denial reasons, e.g. "owners"
     * @param targetType  what the relationship relates someone to
     * @param description plain-language description for PERMISSIONS.md
     * @param test        the reason why the actor has this relationship to the target, or empty if they don't
     */
    public static <T> Relationship<T> define(String name, Class<T> targetType, String description,
                                             BiFunction<Context, T, Optional<String>> test) {
        Relationship<T> relationship = new Relationship<>(name, targetType, description, test);
        if (BY_NAME.putIfAbsent(name, relationship) != null) {
            throw new IllegalArgumentException("Relationship " + name + " is already defined");
        }
        ALL.add(relationship);
        return relationship;
    }

    public static Relationship<?> named(String name) {
        Relationship<?> relationship = BY_NAME.get(name);
        if (relationship == null) {
            throw new IllegalArgumentException("Unknown relationship '" + name + "', expected one of " + BY_NAME.keySet());
        }
        return relationship;
    }

    /** All relationships in the order they were defined. */
    public static Collection<Relationship<?>> all() {
        return List.copyOf(ALL);
    }

    public Optional<String> reasonFor(Context context, T target) {
        return test.apply(context, target);
    }

    public String name() {
        return name;
    }

    public Class<T> targetType() {
        return targetType;
    }

    /** Whether this relationship can judge targets of the given action. */
    public boolean fits(Action<?> action) {
        return targetType.isAssignableFrom(action.targetType());
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
