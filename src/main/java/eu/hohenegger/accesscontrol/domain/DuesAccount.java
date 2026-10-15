package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** The dues an owner pays. It belongs to the associations in which its holder currently owns a unit. */
@Entity
public class DuesAccount implements AssociationScoped {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "holder_username", unique = true)
    private User holder;

    @ElementCollection
    @CollectionTable(name = "dues_entry", joinColumns = @JoinColumn(name = "dues_account_id"))
    @OrderColumn(name = "line_no")
    private List<DuesEntry> entries = new ArrayList<>();

    protected DuesAccount() {
    }

    DuesAccount(User holder) {
        this.holder = holder;
    }

    public DuesAccount charge(LocalDate date, String description, long amountCents) {
        entries.add(new DuesEntry(date, description, amountCents));
        return this;
    }

    public DuesAccount paid(LocalDate date, String description, long amountCents) {
        entries.add(new DuesEntry(date, description, -amountCents));
        return this;
    }

    public User holder() {
        return holder;
    }

    public List<DuesEntry> entries() {
        return List.copyOf(entries);
    }

    public long balanceCents() {
        return entries.stream().mapToLong(DuesEntry::amountCents).sum();
    }

    @Override
    public Stream<Association> associationsAt(Instant instant) {
        return holder.memberships().stream()
                .filter(membership -> membership.role() == Role.OWNER && membership.isActiveAt(instant))
                .map(Membership::association)
                .distinct();
    }

    @Override
    public String toString() {
        return "dues account of " + holder;
    }
}
