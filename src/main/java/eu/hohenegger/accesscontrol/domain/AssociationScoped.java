package eu.hohenegger.accesscontrol.domain;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * Something that belongs to one or more associations. Relationships such as "owners" or "district managers" are
 * checked against these associations, so they work for an association itself as well as for its polls, messages,
 * tenancies or dues accounts.
 */
public interface AssociationScoped {

    Stream<Association> associationsAt(Instant instant);
}
