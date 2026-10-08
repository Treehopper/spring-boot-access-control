package eu.hohenegger.accesscontrol.domain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Association {

    private final String id;
    private final String name;
    private final District district;
    private final List<Unit> units = new CopyOnWriteArrayList<>();
    private final List<Membership> memberships = new CopyOnWriteArrayList<>();

    Association(String id, String name, District district) {
        this.id = id;
        this.name = name;
        this.district = district;
    }

    /** The unit with this name, created on first use: {@code maple.unit("Apt 1").ownedBy(alice).rentedTo(bob)}. */
    public synchronized Unit unit(String name) {
        return units.stream().filter(unit -> unit.name().equals(name)).findFirst().orElseGet(() -> {
            Unit unit = new Unit(name, this);
            units.add(unit);
            return unit;
        });
    }

    public Optional<Unit> findUnit(String id) {
        return units.stream().filter(unit -> unit.id().equals(id)).findFirst();
    }

    public List<Unit> units() {
        return List.copyOf(units);
    }

    public Residents residents() {
        return new Residents(memberships);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public District district() {
        return district;
    }

    Membership add(Membership membership) {
        memberships.add(membership);
        return membership;
    }

    @Override
    public String toString() {
        return name;
    }
}
