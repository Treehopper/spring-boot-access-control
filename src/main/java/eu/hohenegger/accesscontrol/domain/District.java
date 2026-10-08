package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** A group of associations looked after by the same staff. */
public final class District {

    private final Community community;
    private final String id;
    private final String name;
    private final List<Association> associations = new CopyOnWriteArrayList<>();
    private final List<StaffAssignment> staff = new CopyOnWriteArrayList<>();

    District(Community community, String id, String name) {
        this.community = community;
        this.id = id;
        this.name = name;
    }

    public Association association(String name) {
        Association association = new Association(Community.slug(name), name, this);
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

    Community community() {
        return community;
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
            StaffAssignment assignment = new StaffAssignment(person, District.this, title, community.now());
            staff.add(assignment);
            return assignment;
        }
    }
}
