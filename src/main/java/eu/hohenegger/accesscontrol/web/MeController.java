package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.DuesAccount;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.StaffAssignment;
import eu.hohenegger.accesscontrol.domain.User;
import eu.hohenegger.accesscontrol.permission.Action;
import eu.hohenegger.accesscontrol.permission.Permissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static eu.hohenegger.accesscontrol.permission.Action.CREATE_POLL;
import static eu.hohenegger.accesscontrol.permission.Action.MANAGE_RESIDENTS;
import static eu.hohenegger.accesscontrol.permission.Action.SEND_MESSAGE;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_ASSOCIATION;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_DUES;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_MEMBERS;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_POLLS;

/**
 * Who the current persona is and what the UI should offer them. The UI only hides what is not allowed;
 * every endpoint enforces the same policy on its own.
 */
@RestController
class MeController {

    record Capabilities(boolean viewMembers, boolean viewPolls, boolean createPoll,
                        boolean sendMessage, boolean manageResidents) {
    }

    record AssociationView(String id, String name, String district, String relation, Capabilities can) {
    }

    record Me(String username, String displayName, String description, List<AssociationView> associations,
              List<PersonRef> duesAccounts) {
    }

    private final Community community;
    private final Permissions permissions;

    MeController(Community community, Permissions permissions) {
        this.community = community;
        this.permissions = permissions;
    }

    @GetMapping("/api/me")
    Me me(User me) {
        List<AssociationView> associations = permissions.check(me).may(VIEW_ASSOCIATION).filter(community.associations())
                .stream()
                .map(association -> new AssociationView(association.id(), association.name(), association.district().name(),
                        relation(me, association), capabilities(me, association)))
                .toList();
        List<PersonRef> duesAccounts = permissions.check(me).may(VIEW_DUES)
                .filter(community.duesAccounts().stream().map(DuesAccount::holder).toList())
                .stream().map(PersonRef::of).toList();
        return new Me(me.username(), me.displayName(), Personas.describe(community, me), associations, duesAccounts);
    }

    private Capabilities capabilities(User me, Association association) {
        return new Capabilities(
                may(me, VIEW_MEMBERS, association),
                may(me, VIEW_POLLS, association),
                may(me, CREATE_POLL, association),
                may(me, SEND_MESSAGE, association),
                may(me, MANAGE_RESIDENTS, association));
    }

    private boolean may(User me, Action<Association> action, Association association) {
        return permissions.check(me).may(action).on(association).granted();
    }

    private String relation(User me, Association association) {
        return association.residents().activeAt(community.now()).of(me)
                .map(Membership::role).map(role -> role.label())
                .or(() -> association.district().activeAssignmentOf(me, community.now()).map(StaffAssignment::title))
                .orElse("");
    }
}
