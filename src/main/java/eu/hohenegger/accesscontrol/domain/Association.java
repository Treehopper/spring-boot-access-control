package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Entity
public class Association implements AssociationScoped {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "district_id")
    private District district;

    /** Position within the district, to keep the associations in the order they were created. */
    @Column(nullable = false)
    private int ordinal;

    @OneToMany(mappedBy = "association", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Unit> units = new ArrayList<>();

    protected Association() {
    }

    Association(String id, String name, District district, int ordinal) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.ordinal = ordinal;
    }

    /** The unit with this name, created on first use: {@code maple.unit("Apt 1").ownedBy(alice).rentedTo(bob)}. */
    public Unit unit(String name) {
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

    /** The memberships of all units, unit by unit. */
    public Residents residents() {
        return new Residents(units.stream().flatMap(unit -> unit.memberships().stream()).toList());
    }

    @Override
    public Stream<Association> associationsAt(Instant instant) {
        return Stream.of(this);
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

    @Override
    public String toString() {
        return name;
    }
}
