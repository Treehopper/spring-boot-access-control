package eu.hohenegger.accesscontrol.permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * How someone relates to the target of an action, e.g. "owners" or "district managers". A relationship that holds
 * explains itself, so that every grant can be logged with its reason. The type parameter limits which targets the
 * relationship can judge: {@code Relationship<Message>} only makes sense for actions on messages.
 * <p>
 * Features may define their own relationships with {@link #define(String, String, BiFunction)}.
 */
public final class Relationship<T> {

    private static final List<Relationship<?>> ALL = new CopyOnWriteArrayList<>();

    private final String name;
    private final String description;
    private final BiFunction<Context, T, Optional<String>> test;

    private Relationship(String name, String description, BiFunction<Context, T, Optional<String>> test) {
        this.name = name;
        this.description = description;
        this.test = test;
    }

    /**
     * Defines a new relationship.
     *
     * @param name        plural name for PERMISSIONS.md and denial reasons, e.g. "owners"
     * @param description plain-language description for PERMISSIONS.md
     * @param test        the reason why the actor has this relationship to the target, or empty if they don't
     */
    public static <T> Relationship<T> define(String name, String description, BiFunction<Context, T, Optional<String>> test) {
        Relationship<T> relationship = new Relationship<>(name, description, test);
        ALL.add(relationship);
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

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
