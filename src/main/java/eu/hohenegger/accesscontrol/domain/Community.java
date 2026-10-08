package eu.hohenegger.accesscontrol.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The aggregate root and in-memory store of the demo: users, districts with their associations,
 * delegations, dues accounts, polls and messages.
 */
public final class Community {

    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();
    private final List<User> users = new CopyOnWriteArrayList<>();
    private final List<District> districts = new CopyOnWriteArrayList<>();
    private final List<Delegation> delegations = new CopyOnWriteArrayList<>();
    private final List<DuesAccount> duesAccounts = new CopyOnWriteArrayList<>();
    private final List<Poll> polls = new CopyOnWriteArrayList<>();
    private final List<Message> messages = new CopyOnWriteArrayList<>();

    public Community(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // --- building -----------------------------------------------------------------------------------------------

    public User user(String username, String displayName) {
        User user = new User(username, displayName);
        users.add(user);
        return user;
    }

    public District district(String name) {
        District district = new District(this, slug(name), name);
        districts.add(district);
        return district;
    }

    public DelegationDraft grant(User delegate) {
        return new DelegationDraft(this, delegate);
    }

    public synchronized DuesAccount duesOf(User holder) {
        return findDuesAccountOf(holder).orElseGet(() -> {
            DuesAccount account = new DuesAccount(holder);
            duesAccounts.add(account);
            return account;
        });
    }

    public PollDraft poll() {
        return new PollDraft(this);
    }

    public MessageDraft message() {
        return new MessageDraft(this);
    }

    // --- lookups ------------------------------------------------------------------------------------------------

    /** Usernames are matched ignoring case and surrounding blanks. */
    public Optional<User> findUser(String username) {
        String wanted = username == null ? "" : username.strip();
        return users.stream().filter(user -> user.username().equalsIgnoreCase(wanted)).findFirst();
    }

    public Optional<Association> findAssociation(String id) {
        return associations().stream().filter(association -> association.id().equals(id)).findFirst();
    }

    public Optional<Poll> findPoll(String id) {
        return polls.stream().filter(poll -> poll.id().equals(id)).findFirst();
    }

    public Optional<Message> findMessage(String id) {
        return messages.stream().filter(message -> message.id().equals(id)).findFirst();
    }

    public Optional<DuesAccount> findDuesAccountOf(User holder) {
        return duesAccounts.stream().filter(account -> account.holder().equals(holder)).findFirst();
    }

    public List<User> users() {
        return List.copyOf(users);
    }

    public List<District> districts() {
        return List.copyOf(districts);
    }

    public List<Association> associations() {
        return districts.stream().flatMap(district -> district.associations().stream()).toList();
    }

    /** All memberships of a person, current and past. */
    public List<Membership> membershipsOf(User person) {
        return associations().stream().flatMap(association -> association.residents().stream())
                .filter(membership -> membership.resident().equals(person))
                .toList();
    }

    public List<StaffAssignment> staffAssignmentsOf(User person) {
        return districts.stream().flatMap(district -> district.staff().stream())
                .filter(assignment -> assignment.person().equals(person))
                .toList();
    }

    public Delegations delegations() {
        return new Delegations(delegations);
    }

    public List<DuesAccount> duesAccounts() {
        return List.copyOf(duesAccounts);
    }

    public List<Poll> polls() {
        return List.copyOf(polls);
    }

    public List<Message> messages() {
        return List.copyOf(messages);
    }

    /** Forgets everything; used to re-seed the demo. */
    public synchronized void clear() {
        sequence.set(0);
        users.clear();
        districts.clear();
        delegations.clear();
        duesAccounts.clear();
        polls.clear();
        messages.clear();
    }

    // --- internals used by the drafts ---------------------------------------------------------------------------

    String nextId() {
        return Long.toString(sequence.incrementAndGet());
    }

    Message post(Message message) {
        messages.add(message);
        return message;
    }

    Poll open(Poll poll) {
        polls.add(poll);
        return poll;
    }

    Delegation add(Delegation delegation) {
        delegations.add(delegation);
        return delegation;
    }

    static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
