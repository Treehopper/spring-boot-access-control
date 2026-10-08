package eu.hohenegger.accesscontrol.domain;

import java.time.LocalDate;

/** A charge (positive amount) or payment (negative amount) on a dues account. */
public record DuesEntry(LocalDate date, String description, long amountCents) {
}
