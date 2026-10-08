package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.Optional;

/** An owner lets someone else see their dues account, e.g. their accountant. */
public final class Delegation implements TimeBound {

    private final User holder;
    private final User delegate;
    private volatile String purpose;
    private final Instant since;
    private volatile Instant until;

    Delegation(User holder, User delegate, String purpose, Instant since) {
        this.holder = holder;
        this.delegate = delegate;
        this.purpose = purpose;
        this.since = since;
    }

    public Delegation until(Instant until) {
        this.until = until;
        return this;
    }

    public Delegation purpose(String purpose) {
        this.purpose = purpose;
        return this;
    }

    public User holder() {
        return holder;
    }

    public User delegate() {
        return delegate;
    }

    public String purpose() {
        return purpose;
    }

    @Override
    public Instant since() {
        return since;
    }

    @Override
    public Optional<Instant> until() {
        return Optional.ofNullable(until);
    }

    @Override
    public String toString() {
        return purpose + " for " + holder.displayName();
    }
}
