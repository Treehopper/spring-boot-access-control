package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A resident's place in an association. Ending a membership sets {@link #until()} instead of deleting it,
 * so former residents stay visible as such.
 */
@Entity
public class Membership implements TimeBound, AssociationScoped {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resident_username")
    private User resident;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "lives_there", nullable = false)
    private boolean livesThere;

    @Column(nullable = false)
    private Instant since;

    private Instant until;

    protected Membership() {
    }

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

    @Override
    public Stream<Association> associationsAt(Instant instant) {
        return Stream.of(unit.association());
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
