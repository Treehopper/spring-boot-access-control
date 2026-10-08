package eu.hohenegger.accesscontrol.domain;

import java.util.Objects;

/** Shares a dues account: {@code community.grant(dave).accessToDuesOf(alice).as("Accountant")}. */
public final class DelegationDraft {

    private final Community community;
    private final User delegate;
    private User holder;

    DelegationDraft(Community community, User delegate) {
        this.community = community;
        this.delegate = delegate;
    }

    public DelegationDraft accessToDuesOf(User holder) {
        this.holder = holder;
        return this;
    }

    public Delegation as(String purpose) {
        return community.add(new Delegation(Objects.requireNonNull(holder, "holder"), delegate, purpose, community.now()));
    }
}
