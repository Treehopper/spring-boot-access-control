package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Poll {

    private final String id;
    private final Association association;
    private final User askedBy;
    private final String question;
    private final Instant createdAt;
    private final Map<User, Choice> votes = new ConcurrentHashMap<>();

    Poll(String id, Association association, User askedBy, String question, Instant createdAt) {
        this.id = id;
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

    public String id() {
        return id;
    }

    public Association association() {
        return association;
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
