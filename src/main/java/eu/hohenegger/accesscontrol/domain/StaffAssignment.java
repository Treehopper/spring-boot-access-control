package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.Optional;

/** Someone working for all associations of a district, e.g. its property manager. */
@Entity
public class StaffAssignment implements TimeBound {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_username")
    private User person;

    @ManyToOne(optional = false)
    @JoinColumn(name = "district_id")
    private District district;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Instant since;

    private Instant until;

    protected StaffAssignment() {
    }

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
