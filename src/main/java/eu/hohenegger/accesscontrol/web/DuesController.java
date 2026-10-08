package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.DuesAccount;
import eu.hohenegger.accesscontrol.domain.DuesEntry;
import eu.hohenegger.accesscontrol.domain.User;
import eu.hohenegger.accesscontrol.permission.Permissions;
import eu.hohenegger.accesscontrol.permission.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static eu.hohenegger.accesscontrol.permission.Action.MANAGE_DELEGATES;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
class DuesController {

    record Delegate(PersonRef delegate, String purpose, Instant since) {
    }

    /** {@code sharedWith} is {@code null} unless the viewer may manage the account's delegates. */
    record DuesView(PersonRef holder, List<DuesEntry> entries, long balanceCents, List<Delegate> sharedWith) {
    }

    private final Community community;
    private final Permissions permissions;

    DuesController(Community community, Permissions permissions) {
        this.community = community;
        this.permissions = permissions;
    }

    @GetMapping("/api/residents/{username}/dues")
    @RequirePermission(action = "VIEW_DUES", on = "username")
    DuesView dues(@PathVariable String username, User me) {
        User holder = community.findUser(username).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        List<DuesEntry> entries = community.findDuesAccountOf(holder).map(DuesAccount::entries).orElse(List.of());
        List<Delegate> sharedWith = permissions.check(me).may(MANAGE_DELEGATES).on(holder).granted()
                ? community.delegations().from(holder).activeAt(community.now()).stream()
                        .map(delegation -> new Delegate(PersonRef.of(delegation.delegate()), delegation.purpose(), delegation.since()))
                        .toList()
                : null;
        return new DuesView(PersonRef.of(holder), entries, entries.stream().mapToLong(DuesEntry::amountCents).sum(), sharedWith);
    }
}
