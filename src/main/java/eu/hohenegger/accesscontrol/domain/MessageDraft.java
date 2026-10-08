package eu.hohenegger.accesscontrol.domain;

import java.util.Objects;

/**
 * Composes a message:
 * {@code community.message().from(carol).toOwnersOf(maple).about("Assembly").saying("...")}
 * or {@code community.message().from(carol).to(bob).in(maple).about("Parking").saying("...")}.
 */
public final class MessageDraft {

    private final Community community;
    private User sender;
    private Association association;
    private Audience audience;
    private User recipient;
    private String subject;

    MessageDraft(Community community) {
        this.community = community;
    }

    public MessageDraft from(User sender) {
        this.sender = sender;
        return this;
    }

    public MessageDraft toEveryoneIn(Association association) {
        return addressedTo(Audience.EVERYONE, association, null);
    }

    public MessageDraft toOwnersOf(Association association) {
        return addressedTo(Audience.OWNERS, association, null);
    }

    public MessageDraft toTenantsOf(Association association) {
        return addressedTo(Audience.TENANTS, association, null);
    }

    /** Everyone who lives there, whether they rent or own. */
    public MessageDraft toOccupantsOf(Association association) {
        return addressedTo(Audience.OCCUPANTS, association, null);
    }

    /** A personal message; complete it with {@link #in(Association)}. */
    public MessageDraft to(User recipient) {
        this.audience = Audience.INDIVIDUAL;
        this.recipient = recipient;
        return this;
    }

    public MessageDraft in(Association association) {
        this.association = association;
        return this;
    }

    public MessageDraft addressedTo(Audience audience, Association association, User recipient) {
        this.audience = audience;
        this.association = association;
        this.recipient = audience == Audience.INDIVIDUAL ? Objects.requireNonNull(recipient, "recipient") : null;
        return this;
    }

    public MessageDraft about(String subject) {
        this.subject = subject;
        return this;
    }

    /** Sends the message and returns it. */
    public Message saying(String body) {
        return community.post(new Message(community.nextId(), Objects.requireNonNull(sender, "sender"),
                Objects.requireNonNull(association, "association"), Objects.requireNonNull(audience, "audience"),
                recipient, Objects.requireNonNull(subject, "subject"), body, community.now()));
    }
}
