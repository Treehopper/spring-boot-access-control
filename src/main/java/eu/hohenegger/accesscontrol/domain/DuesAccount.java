package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/** The dues an owner pays. It belongs to the associations in which its holder currently owns a unit. */
public final class DuesAccount implements AssociationScoped {

    private final User holder;
    private final Community community;
    private final List<DuesEntry> entries = new CopyOnWriteArrayList<>();

    DuesAccount(User holder, Community community) {
        this.holder = holder;
        this.community = community;
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
        return community.associations().stream()
                .filter(association -> association.residents().activeAt(instant).owners().include(holder));
    }

    @Override
    public String toString() {
        return "dues account of " + holder;
    }
}
