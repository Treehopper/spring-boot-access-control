package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Chainable view on delegations, e.g. {@code community.delegations().from(alice).to(dave).activeAt(now)}. */
public final class Delegations {

    private final List<Delegation> delegations;

    Delegations(List<Delegation> delegations) {
        this.delegations = List.copyOf(delegations);
    }

    public Delegations from(User holder) {
        return filter(delegation -> delegation.holder().equals(holder));
    }

    public Delegations to(User delegate) {
        return filter(delegation -> delegation.delegate().equals(delegate));
    }

    public Delegations activeAt(Instant instant) {
        return filter(delegation -> delegation.isActiveAt(instant));
    }

    public Optional<Delegation> latest() {
        return delegations.stream().reduce((first, second) -> second);
    }

    public List<Delegation> list() {
        return delegations;
    }

    public Stream<Delegation> stream() {
        return delegations.stream();
    }

    private Delegations filter(Predicate<Delegation> predicate) {
        return new Delegations(delegations.stream().filter(predicate).toList());
    }
}
