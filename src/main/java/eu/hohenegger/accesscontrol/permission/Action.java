package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.Poll;
import eu.hohenegger.accesscontrol.domain.User;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Something a user may want to do, typed by what it is done to. {@code Action<Poll>} can only be checked
 * against a {@link Poll}.
 */
public final class Action<T> {

    private static final Map<String, Action<?>> BY_NAME = new LinkedHashMap<>();

    public static final Action<Association> VIEW_ASSOCIATION = define("VIEW_ASSOCIATION", Association.class);
    public static final Action<Association> VIEW_MEMBERS = define("VIEW_MEMBERS", Association.class);
    /** The target is a tenancy; granting it reveals who the tenant is. */
    public static final Action<Membership> VIEW_TENANT = define("VIEW_TENANT", Membership.class);
    public static final Action<Association> VIEW_POLLS = define("VIEW_POLLS", Association.class);
    public static final Action<Association> CREATE_POLL = define("CREATE_POLL", Association.class);
    public static final Action<Association> SEND_MESSAGE = define("SEND_MESSAGE", Association.class);
    public static final Action<Association> MANAGE_RESIDENTS = define("MANAGE_RESIDENTS", Association.class);
    public static final Action<Poll> VOTE = define("VOTE", Poll.class);
    public static final Action<Message> READ_MESSAGE = define("READ_MESSAGE", Message.class);
    /** The target is the holder of the dues account. */
    public static final Action<User> VIEW_DUES = define("VIEW_DUES", User.class);
    /** The target is the holder of the dues account whose delegates are managed. */
    public static final Action<User> MANAGE_DELEGATES = define("MANAGE_DELEGATES", User.class);

    private final String name;
    private final Class<T> targetType;

    private Action(String name, Class<T> targetType) {
        this.name = name;
        this.targetType = targetType;
    }

    private static <T> Action<T> define(String name, Class<T> targetType) {
        Action<T> action = new Action<>(name, targetType);
        BY_NAME.put(name, action);
        return action;
    }

    public static Action<?> named(String name) {
        Action<?> action = BY_NAME.get(name);
        if (action == null) {
            throw new IllegalArgumentException("Unknown action '" + name + "', expected one of " + BY_NAME.keySet());
        }
        return action;
    }

    public static Collection<Action<?>> all() {
        return List.copyOf(BY_NAME.values());
    }

    public String name() {
        return name;
    }

    public Class<T> targetType() {
        return targetType;
    }

    @Override
    public String toString() {
        return name;
    }
}
