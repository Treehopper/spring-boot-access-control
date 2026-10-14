package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.DuesAccount;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.Poll;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Something a user may want to do, typed by what it is done to: {@code Action<Poll>} can only be checked against a
 * {@link Poll}. The description is what domain experts read in PERMISSIONS.md.
 * <p>
 * Features may define their own actions with {@link #define(String, Class, String)}.
 */
public final class Action<T> {

    private static final Map<String, Action<?>> BY_NAME = new ConcurrentHashMap<>();
    private static final List<Action<?>> ALL = new CopyOnWriteArrayList<>();

    public static final Action<Association> VIEW_ASSOCIATION =
            define("VIEW_ASSOCIATION", Association.class, "see the association and its notices");
    public static final Action<Association> VIEW_OWNERS =
            define("VIEW_OWNERS", Association.class, "see who owns which unit");
    public static final Action<Association> COUNT_TENANTS =
            define("COUNT_TENANTS", Association.class, "see how many tenants live in the association");
    /** The target is one tenancy, so this can be granted for some tenants and not for others. */
    public static final Action<Membership> VIEW_TENANT =
            define("VIEW_TENANT", Membership.class, "see who a tenant is (name and unit)");
    public static final Action<Association> VIEW_VACANCIES =
            define("VIEW_VACANCIES", Association.class, "see which units nobody lives in");
    public static final Action<Association> VIEW_POLLS =
            define("VIEW_POLLS", Association.class, "see polls and their results");
    public static final Action<Association> CREATE_POLL =
            define("CREATE_POLL", Association.class, "open a poll");
    public static final Action<Poll> VOTE =
            define("VOTE", Poll.class, "vote in a poll");
    public static final Action<Association> SEND_MESSAGE =
            define("SEND_MESSAGE", Association.class, "send messages to residents");
    public static final Action<Message> READ_MESSAGE =
            define("READ_MESSAGE", Message.class, "read a message");
    public static final Action<Association> MANAGE_RESIDENTS =
            define("MANAGE_RESIDENTS", Association.class, "admit residents, change their role and end their access");
    public static final Action<DuesAccount> VIEW_DUES =
            define("VIEW_DUES", DuesAccount.class, "see a dues account");
    public static final Action<DuesAccount> MANAGE_DELEGATES =
            define("MANAGE_DELEGATES", DuesAccount.class, "decide who else may see a dues account");

    private final String name;
    private final Class<T> targetType;
    private final String description;

    private Action(String name, Class<T> targetType, String description) {
        this.name = name;
        this.targetType = targetType;
        this.description = description;
    }

    /**
     * Defines a new action.
     *
     * @param name        unique name, used in {@link RequirePermission#action()} and in the audit log
     * @param targetType  what the action is done to
     * @param description plain-language description for PERMISSIONS.md, e.g. "vote in a poll"
     */
    public static <T> Action<T> define(String name, Class<T> targetType, String description) {
        Action<T> action = new Action<>(name, targetType, description);
        if (BY_NAME.putIfAbsent(name, action) != null) {
            throw new IllegalArgumentException("Action " + name + " is already defined");
        }
        ALL.add(action);
        return action;
    }

    public static Action<?> named(String name) {
        Action<?> action = BY_NAME.get(name);
        if (action == null) {
            throw new IllegalArgumentException("Unknown action '" + name + "', expected one of " + BY_NAME.keySet());
        }
        return action;
    }

    /** All actions in the order they were defined. */
    public static Collection<Action<?>> all() {
        return List.copyOf(ALL);
    }

    public String name() {
        return name;
    }

    public Class<T> targetType() {
        return targetType;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
