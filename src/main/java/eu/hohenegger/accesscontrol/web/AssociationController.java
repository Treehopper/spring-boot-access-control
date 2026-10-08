package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Residents;
import eu.hohenegger.accesscontrol.domain.Unit;
import eu.hohenegger.accesscontrol.domain.User;
import eu.hohenegger.accesscontrol.permission.Permissions;
import eu.hohenegger.accesscontrol.permission.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static eu.hohenegger.accesscontrol.permission.Action.VIEW_ASSOCIATION;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_TENANT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/associations")
class AssociationController {

    /** {@code vacantUnits} are units nobody currently lives in. */
    record AssociationSummary(String id, String name, String district, int owners, int tenants, List<String> vacantUnits) {
    }

    record Member(String username, String displayName, String unit, boolean livesThere) {
    }

    /** {@code tenants} lists only the tenants the viewer may know by name; {@code tenantCount} counts all of them. */
    record MemberList(List<Member> owners, List<Member> tenants, int tenantCount) {
    }

    private final Community community;
    private final Permissions permissions;

    AssociationController(Community community, Permissions permissions) {
        this.community = community;
        this.permissions = permissions;
    }

    @GetMapping
    List<AssociationSummary> list(User me) {
        return permissions.check(me).may(VIEW_ASSOCIATION).filter(community.associations()).stream()
                .map(this::summary)
                .toList();
    }

    @GetMapping("/{id}")
    @RequirePermission(action = "VIEW_ASSOCIATION", on = "id")
    AssociationSummary get(@PathVariable String id) {
        return summary(association(id));
    }

    @GetMapping("/{id}/members")
    @RequirePermission(action = "VIEW_MEMBERS", on = "id")
    MemberList members(@PathVariable String id, User me) {
        Association association = association(id);
        Residents residents = association.residents().activeAt(community.now());
        List<Membership> tenancies = residents.tenants().list();
        List<Member> visibleTenants = permissions.check(me).may(VIEW_TENANT).filter(tenancies).stream()
                .map(AssociationController::member)
                .toList();
        return new MemberList(members(residents.owners()), visibleTenants, tenancies.size());
    }

    private AssociationSummary summary(Association association) {
        Residents residents = association.residents().activeAt(community.now());
        return new AssociationSummary(association.id(), association.name(), association.district().name(),
                residents.owners().people().size(), residents.tenants().people().size(),
                association.units().stream().filter(unit -> unit.isVacantAt(community.now())).map(Unit::name).toList());
    }

    private static List<Member> members(Residents residents) {
        return residents.stream().map(AssociationController::member).toList();
    }

    private static Member member(Membership membership) {
        return new Member(membership.resident().username(), membership.resident().displayName(), membership.unit().name(),
                membership.livesThere());
    }

    private Association association(String id) {
        return community.findAssociation(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
