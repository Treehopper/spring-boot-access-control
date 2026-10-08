package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Role;
import eu.hohenegger.accesscontrol.domain.StaffAssignment;
import eu.hohenegger.accesscontrol.domain.User;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Describes what someone currently is in the community, e.g. "Owner · Maple Court, Apt 1". */
final class Personas {

    private Personas() {
    }

    static String describe(Community community, User user) {
        Instant now = community.now();
        List<String> current = new ArrayList<>();
        List<String> past = new ArrayList<>();
        for (StaffAssignment assignment : community.staffAssignmentsOf(user)) {
            (assignment.isActiveAt(now) ? current : past).add(assignment.title() + " · " + assignment.district().name() + " district");
        }
        for (Membership membership : community.membershipsOf(user)) {
            String text = membership.role().label() + " · " + membership.association().name()
                    + ", " + membership.unit().name()
                    + (membership.role() == Role.OWNER && membership.livesThere() ? " (lives there)" : "");
            if (membership.isActiveAt(now)) {
                current.add(text);
            } else {
                past.add("Former " + text.substring(0, 1).toLowerCase() + text.substring(1)
                        + membership.until().map(until -> " (until " + until.atZone(ZoneOffset.UTC).toLocalDate() + ")").orElse(""));
            }
        }
        community.delegations().to(user).activeAt(now).stream()
                .forEach(delegation -> current.add(delegation.purpose() + " for " + delegation.holder().displayName()));
        List<String> parts = current.isEmpty() ? past : current;
        return parts.isEmpty() ? "No role" : String.join("; ", parts);
    }
}
