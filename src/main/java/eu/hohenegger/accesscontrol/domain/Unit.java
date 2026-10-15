package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An apartment or house within an association. Either the owner rents it out to a tenant,
 * {@code maple.unit("Apt 1").ownedBy(alice).rentedTo(bob)}, or lives there,
 * {@code maple.unit("Apt 4").occupiedByOwner(mia)}.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"association_id", "slug"}))
public class Unit {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "association_id")
    private Association association;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Membership> memberships = new ArrayList<>();

    protected Unit() {
    }

    Unit(String name, Association association) {
        this.slug = Community.slug(name);
        this.name = name;
        this.association = association;
    }

    /** An owner who rents the unit out. */
    public Unit ownedBy(User owner) {
        admit(owner, Role.OWNER);
        return this;
    }

    /** An owner who lives in the unit themselves. */
    public Membership occupiedByOwner(User owner) {
        return admit(owner, Role.OWNER).livesThere(true);
    }

    public Membership rentedTo(User tenant) {
        return admit(tenant, Role.TENANT);
    }

    public Membership admit(User person, Role role) {
        Membership membership = new Membership(person, this, role, association.district().now());
        memberships.add(membership);
        return membership;
    }

    public Residents residents() {
        return new Residents(memberships);
    }

    /** Nobody lives here: neither a tenant nor an owner who occupies it. */
    public boolean isVacantAt(Instant instant) {
        return residents().activeAt(instant).stream().noneMatch(Membership::livesThere);
    }

    /** Identifies the unit within its association, e.g. {@code apt-1}. */
    public String id() {
        return slug;
    }

    public String name() {
        return name;
    }

    public Association association() {
        return association;
    }

    List<Membership> memberships() {
        return memberships;
    }

    @Override
    public String toString() {
        return name;
    }
}
