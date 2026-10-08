package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.Optional;

public final class Message {

    private final String id;
    private final User sender;
    private final Association association;
    private final Audience audience;
    private final User recipient;
    private final String subject;
    private final String body;
    private final Instant sentAt;

    Message(String id, User sender, Association association, Audience audience, User recipient,
            String subject, String body, Instant sentAt) {
        this.id = id;
        this.sender = sender;
        this.association = association;
        this.audience = audience;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.sentAt = sentAt;
    }

    public String id() {
        return id;
    }

    public User sender() {
        return sender;
    }

    public Association association() {
        return association;
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
