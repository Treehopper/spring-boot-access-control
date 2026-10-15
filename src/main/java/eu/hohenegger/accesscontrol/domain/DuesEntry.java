package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/** A charge (positive amount) or payment (negative amount) on a dues account. */
@Embeddable
public record DuesEntry(@Column(name = "entry_date", nullable = false) LocalDate date,
                        @Column(nullable = false) String description,
                        @Column(name = "amount_cents", nullable = false) long amountCents) {
}
