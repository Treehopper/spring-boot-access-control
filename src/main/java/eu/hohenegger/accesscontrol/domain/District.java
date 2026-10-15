package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Transient;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A group of associations looked after by the same staff. */
@Entity
@EntityListeners(ClockListener.class)
public class District {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false)
    private String name;

    /** Position among the districts, to keep them in the order they were created. */
    @Column(nullable = false)
    private int ordinal;

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL)
    @OrderBy("ordinal")
    private List<Association> associations = new ArrayList<>();

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<StaffAssignment> staff = new ArrayList<>();

    @Transient
    private Clock clock;

    protected District() {
    }

    District(Clock clock, String id, String name, int ordinal) {
        this.clock = clock;
        this.id = id;
        this.name = name;
        this.ordinal = ordinal;
    }

    public Association association(String name) {
        Association association = new Association(Community.slug(name), name, this, associations.size());
        associations.add(association);
        return association;
    }

    /** Appoints staff: {@code riverside.appoint(carol).as("Property manager")}. */
    public Appointment appoint(User person) {
        return new Appointment(person);
    }

    public Optional<StaffAssignment> activeAssignmentOf(User person, Instant instant) {
        return staff.stream()
                .filter(assignment -> assignment.person().equals(person))
                .filter(assignment -> assignment.isActiveAt(instant))
                .findFirst();
    }

    public List<StaffAssignment> staff() {
        return List.copyOf(staff);
    }

    public List<Association> associations() {
        return List.copyOf(associations);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    Instant now() {
        return clock.instant();
    }

    void attach(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String toString() {
        return name;
    }

    public final class Appointment {

        private final User person;

        private Appointment(User person) {
            this.person = person;
        }

        public StaffAssignment as(String title) {
            StaffAssignment assignment = new StaffAssignment(person, District.this, title, now());
            staff.add(assignment);
            return assignment;
        }
    }
}
