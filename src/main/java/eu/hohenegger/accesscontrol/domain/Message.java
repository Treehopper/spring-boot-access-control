package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

@Entity
public class Message implements AssociationScoped {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_username")
    private User sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "association_id")
    private Association association;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Audience audience;

    /** Only for {@link Audience#INDIVIDUAL} messages. */
    @ManyToOne
    @JoinColumn(name = "recipient_username")
    private User recipient;

    @Column(nullable = false)
    private String subject;

    @Column(length = 4000)
    private String body;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected Message() {
    }

    Message(User sender, Association association, Audience audience, User recipient,
            String subject, String body, Instant sentAt) {
        this.sender = sender;
        this.association = association;
        this.audience = audience;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.sentAt = sentAt;
    }

    /** Assigned when the message is sent. */
    public String id() {
        return String.valueOf(id);
    }

    public User sender() {
        return sender;
    }

    public Association association() {
        return association;
    }

    @Override
    public Stream<Association> associationsAt(Instant instant) {
        return Stream.of(association);
    }

    public Audience audience() {
        return audience;
    }

    /** Present only for {@link Audience#INDIVIDUAL} messages. */
    public Optional<User> recipient() {
        return Optional.ofNullable(recipient);
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public Instant sentAt() {
        return sentAt;
    }

    @Override
    public String toString() {
        return "message #" + id + " in " + association.name();
    }
}
