package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.User;

import java.time.Instant;

/** Everything a {@link Condition} may look at besides the target: who asks, when, and the community. */
public record Context(User actor, Instant now, Community community) {
}
