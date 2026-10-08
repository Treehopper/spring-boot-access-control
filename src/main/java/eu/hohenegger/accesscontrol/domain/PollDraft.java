package eu.hohenegger.accesscontrol.domain;

import java.util.Objects;

/** Opens a poll: {@code community.poll().in(maple).by(carol).asking("Repaint the facade?")}. */
public final class PollDraft {

    private final Community community;
    private Association association;
    private User askedBy;

    PollDraft(Community community) {
        this.community = community;
    }

    public PollDraft in(Association association) {
        this.association = association;
        return this;
    }

    public PollDraft by(User askedBy) {
        this.askedBy = askedBy;
        return this;
    }

    public Poll asking(String question) {
        return community.open(new Poll(community.nextId(), Objects.requireNonNull(association, "association"),
                Objects.requireNonNull(askedBy, "askedBy"), question, community.now()));
    }
}
