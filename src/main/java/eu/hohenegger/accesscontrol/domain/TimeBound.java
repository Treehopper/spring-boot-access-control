package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * A relationship that holds from {@link #since()} until (exclusively) {@link #until()}, or indefinitely.
 */
public interface TimeBound {

    Instant since();

    Optional<Instant> until();

    default boolean isActiveAt(Instant instant) {
        return !instant.isBefore(since()) && until().map(instant::isBefore).orElse(true);
    }
}
