package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Entity
public class Poll implements AssociationScoped {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "association_id")
    private Association association;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asked_by_username")
    private User askedBy;

    @Column(nullable = false)
    private String question;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** One vote per owner; voting again replaces the earlier vote. */
    @ElementCollection
    @CollectionTable(name = "poll_vote", joinColumns = @JoinColumn(name = "poll_id"))
    @MapKeyJoinColumn(name = "voter_username")
    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, length = 20)
    private Map<User, Choice> votes = new HashMap<>();

    protected Poll() {
    }

    Poll(Association association, User askedBy, String question, Instant createdAt) {
        this.association = association;
        this.askedBy = askedBy;
        this.question = question;
        this.createdAt = createdAt;
    }

    /** Records a vote; voting again replaces the earlier vote. */
    public Poll recordVote(User voter, Choice choice) {
        votes.put(voter, choice);
        return this;
    }

    public Optional<Choice> voteOf(User voter) {
        return Optional.ofNullable(votes.get(voter));
    }

    public Map<Choice, Long> results() {
        Map<Choice, Long> results = new EnumMap<>(Choice.class);
        for (Choice choice : Choice.values()) {
            results.put(choice, votes.values().stream().filter(choice::equals).count());
        }
        return results;
    }

    /** Assigned when the poll is opened. */
    public String id() {
        return String.valueOf(id);
    }

    public Association association() {
        return association;
    }

    @Override
    public Stream<Association> associationsAt(Instant instant) {
        return Stream.of(association);
    }

    public User askedBy() {
        return askedBy;
    }

    public String question() {
        return question;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "poll #" + id + " in " + association.name();
    }
}
