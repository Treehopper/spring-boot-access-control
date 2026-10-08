package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;

/**
 * An apartment or house within an association. Either the owner rents it out to a tenant,
 * {@code maple.unit("Apt 1").ownedBy(alice).rentedTo(bob)}, or lives there,
 * {@code maple.unit("Apt 4").occupiedByOwner(mia)}.
 */
public final class Unit {

    private final String name;
    private final Association association;

    Unit(String name, Association association) {
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
        return association.add(new Membership(person, this, role, association.district().community().now()));
    }

    public Residents residents() {
        return association.residents().in(this);
    }

    /** Nobody lives here: neither a tenant nor an owner who occupies it. */
    public boolean isVacantAt(Instant instant) {
        return residents().activeAt(instant).stream().noneMatch(Membership::livesThere);
    }

    public String id() {
        return Community.slug(name);
    }

    public String name() {
        return name;
    }

    public Association association() {
        return association;
    }

    @Override
    public String toString() {
        return name;
    }
}
