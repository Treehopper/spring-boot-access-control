package eu.hohenegger.accesscontrol.permission;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * One reason why an actor may do something to a target, e.g. "owner of the association".
 * A satisfied condition explains itself so that grants can be logged with their reason.
 */
public interface Condition<T> {

    /** Short name used in denial reasons, e.g. {@code "owner"}. */
    String description();

    /** The reason for granting if the condition holds, empty otherwise. */
    Optional<String> grantReason(Context context, T target);

    static <T> Condition<T> named(String description, BiFunction<Context, T, Optional<String>> test) {
        return new Condition<>() {
            @Override
            public String description() {
                return description;
            }

            @Override
            public Optional<String> grantReason(Context context, T target) {
                return test.apply(context, target);
            }
        };
    }

    /**
     * Applies this condition to something derived from the actual target,
     * e.g. {@code owners().of(Poll::association)}.
     */
    default <S> Condition<S> of(Function<S, T> via) {
        return named(description(), (context, target) -> grantReason(context, via.apply(target)));
    }
}
