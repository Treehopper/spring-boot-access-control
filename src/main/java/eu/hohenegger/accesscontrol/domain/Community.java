package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.EntityManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The aggregate root and store of the demo: users, districts with their associations, delegations, dues accounts,
 * polls and messages, kept in the database through JPA.
 * <p>
 * Creating things ({@link #user}, {@link #district}, the drafts) needs a transaction; reading doesn't.
 */
public final class Community {

    /** All domain tables, children first, so they can be emptied in this order. */
    private static final List<String> TABLES = List.of("poll_vote", "poll", "message", "dues_entry", "dues_account",
            "delegation", "membership", "staff_assignment", "unit", "association", "district", "app_user");

    private final Clock clock;
    private final EntityManager entityManager;

    public Community(Clock clock, EntityManager entityManager) {
        this.clock = clock;
        this.entityManager = entityManager;
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // --- building -----------------------------------------------------------------------------------------------

    public User user(String username, String displayName) {
        return persist(new User(username, displayName));
    }

    public District district(String name) {
        return persist(new District(clock, slug(name), name, districts().size()));
    }

    public DelegationDraft grant(User delegate) {
        return new DelegationDraft(this, delegate);
    }

    public DuesAccount duesOf(User holder) {
        return findDuesAccountOf(holder).orElseGet(() -> persist(new DuesAccount(holder)));
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
        return entityManager.createQuery("select u from User u where lower(u.username) = lower(:username)", User.class)
                .setParameter("username", wanted)
                .getResultStream().findFirst();
    }

    public Optional<Association> findAssociation(String id) {
        return Optional.ofNullable(id).map(key -> entityManager.find(Association.class, key));
    }

    public Optional<Poll> findPoll(String id) {
        return numeric(id).map(key -> entityManager.find(Poll.class, key));
    }

    public Optional<Message> findMessage(String id) {
        return numeric(id).map(key -> entityManager.find(Message.class, key));
    }

    public Optional<DuesAccount> findDuesAccountOf(User holder) {
        return entityManager.createQuery("select d from DuesAccount d where d.holder = :holder", DuesAccount.class)
                .setParameter("holder", holder)
                .getResultStream().findFirst();
    }

    public List<User> users() {
        return entityManager.createQuery("select u from User u order by u.username", User.class).getResultList();
    }

    public List<District> districts() {
        return entityManager.createQuery("select d from District d order by d.ordinal", District.class).getResultList();
    }

    public List<Association> associations() {
        return districts().stream().flatMap(district -> district.associations().stream()).toList();
    }

    /** All memberships of a person, current and past. */
    public List<Membership> membershipsOf(User person) {
        return entityManager.createQuery("select m from Membership m where m.resident = :person order by m.id", Membership.class)
                .setParameter("person", person)
                .getResultList();
    }

    public List<StaffAssignment> staffAssignmentsOf(User person) {
        return entityManager.createQuery("select s from StaffAssignment s where s.person = :person order by s.id", StaffAssignment.class)
                .setParameter("person", person)
                .getResultList();
    }

    public Delegations delegations() {
        return new Delegations(entityManager.createQuery("select d from Delegation d order by d.id", Delegation.class).getResultList());
    }

    public List<DuesAccount> duesAccounts() {
        return entityManager.createQuery("select d from DuesAccount d order by d.id", DuesAccount.class).getResultList();
    }

    public List<Poll> polls() {
        return entityManager.createQuery("select p from Poll p order by p.id", Poll.class).getResultList();
    }

    public List<Message> messages() {
        return entityManager.createQuery("select m from Message m order by m.id", Message.class).getResultList();
    }

    /** Forgets everything except the permission policy; used to re-seed the demo. */
    public void clear() {
        entityManager.flush();
        TABLES.forEach(table -> entityManager.createNativeQuery("delete from " + table).executeUpdate());
        entityManager.clear();
    }

    // --- internals used by the drafts ---------------------------------------------------------------------------

    Message post(Message message) {
        return persist(message);
    }

    Poll open(Poll poll) {
        return persist(poll);
    }

    Delegation add(Delegation delegation) {
        return persist(delegation);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private static Optional<Long> numeric(String id) {
        try {
            return Optional.of(Long.parseLong(id));
        } catch (NumberFormatException notAnId) {
            return Optional.empty();
        }
    }

    static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
