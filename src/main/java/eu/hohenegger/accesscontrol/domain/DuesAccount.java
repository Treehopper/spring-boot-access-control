package eu.hohenegger.accesscontrol.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DuesAccount {

    private final User holder;
    private final List<DuesEntry> entries = new CopyOnWriteArrayList<>();

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
}
