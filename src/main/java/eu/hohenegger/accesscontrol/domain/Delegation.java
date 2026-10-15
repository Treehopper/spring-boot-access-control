package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.Optional;

/** An owner lets someone else see their dues account, e.g. their accountant. */
@Entity
public class Delegation implements TimeBound {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "holder_username")
    private User holder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "delegate_username")
    private User delegate;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private Instant since;

    private Instant until;

    protected Delegation() {
    }

    Delegation(User holder, User delegate, String purpose, Instant since) {
        this.holder = holder;
        this.delegate = delegate;
        this.purpose = purpose;
        this.since = since;
    }

    public Delegation until(Instant until) {
        this.until = until;
        return this;
    }

    public Delegation purpose(String purpose) {
        this.purpose = purpose;
        return this;
    }

    public User holder() {
        return holder;
    }

    public User delegate() {
        return delegate;
    }

    public String purpose() {
        return purpose;
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
        return purpose + " for " + holder.displayName();
    }
}
