package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.Optional;

/** Someone working for all associations of a district, e.g. its property manager. */
public final class StaffAssignment implements TimeBound {

    private final User person;
    private final District district;
    private final String title;
    private final Instant since;
    private volatile Instant until;

    StaffAssignment(User person, District district, String title, Instant since) {
        this.person = person;
        this.district = district;
        this.title = title;
        this.since = since;
    }

    public StaffAssignment until(Instant until) {
        this.until = until;
        return this;
    }

    public User person() {
        return person;
    }

    public District district() {
        return district;
    }

    public String title() {
        return title;
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
        return title + " of " + district.name();
    }
}
