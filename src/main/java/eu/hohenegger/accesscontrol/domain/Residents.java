package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An immutable, chainable view on memberships, e.g. {@code maple.residents().activeAt(now).owners().include(alice)}.
 */
public final class Residents {

    private static final Residents NONE = new Residents(List.of());

    private final List<Membership> memberships;

    Residents(List<Membership> memberships) {
        this.memberships = List.copyOf(memberships);
    }

    public static Residents none() {
        return NONE;
    }

    public Residents activeAt(Instant instant) {
        return filter(membership -> membership.isActiveAt(instant));
    }

    public Residents in(Unit unit) {
        return filter(membership -> membership.unit().equals(unit));
    }

    public Residents owners() {
        return withRole(Role.OWNER);
    }

    public Residents tenants() {
        return withRole(Role.TENANT);
    }

    /** Residents who live in their unit: all tenants, and owners who don't rent theirs out. */
    public Residents occupants() {
        return filter(Membership::livesThere);
    }

    public Residents withRole(Role role) {
        return filter(membership -> membership.role() == role);
    }

    /** The most recent membership of the given person, if any. */
    public Optional<Membership> of(User person) {
        return memberships.stream().filter(membership -> membership.resident().equals(person)).reduce((first, second) -> second);
    }

    public boolean include(User person) {
        return of(person).isPresent();
    }

    public List<User> people() {
        return memberships.stream().map(Membership::resident).distinct().toList();
    }

    public int count() {
        return memberships.size();
    }

    public List<Membership> list() {
        return memberships;
    }

    public Stream<Membership> stream() {
        return memberships.stream();
    }

    private Residents filter(Predicate<Membership> predicate) {
        return new Residents(memberships.stream().filter(predicate).toList());
    }
}
