package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * A resident's place in an association. Ending a membership sets {@link #until()} instead of deleting it,
 * so former residents stay visible as such.
 */
public final class Membership implements TimeBound {

    private final User resident;
    private final Unit unit;
    private volatile Role role;
    private volatile boolean livesThere;
    private volatile Instant since;
    private volatile Instant until;

    Membership(User resident, Unit unit, Role role, Instant since) {
        this.resident = resident;
        this.unit = unit;
        this.role = role;
        this.livesThere = role == Role.TENANT;
        this.since = since;
    }

    /** Whether the resident occupies the unit. Tenants always do; owners only if they don't rent it out. */
    public Membership livesThere(boolean livesThere) {
        this.livesThere = livesThere || role == Role.TENANT;
        return this;
    }

    public Membership since(Instant since) {
        this.since = since;
        return this;
    }

    public Membership until(Instant until) {
        this.until = until;
        return this;
    }

    /** Changes the role; a tenant who becomes owner of their unit keeps living there. */
    public Membership changeRoleTo(Role role) {
        this.role = role;
        return livesThere(livesThere);
    }

    public User resident() {
        return resident;
    }

    public Association association() {
        return unit.association();
    }

    public Role role() {
        return role;
    }

    public boolean livesThere() {
        return livesThere;
    }

    public Unit unit() {
        return unit;
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
        return role.label().toLowerCase() + " in " + association().name() + ", " + unit
                + (role == Role.OWNER && livesThere ? " (lives there)" : "");
    }
}
