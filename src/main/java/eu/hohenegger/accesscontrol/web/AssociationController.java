package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Residents;
import eu.hohenegger.accesscontrol.domain.Unit;
import eu.hohenegger.accesscontrol.domain.User;
import eu.hohenegger.accesscontrol.permission.Action;
import eu.hohenegger.accesscontrol.permission.Permissions;
import eu.hohenegger.accesscontrol.permission.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static eu.hohenegger.accesscontrol.permission.Action.COUNT_TENANTS;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_ASSOCIATION;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_OWNERS;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_TENANT;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_VACANCIES;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/associations")
class AssociationController {

    /**
     * Each detail is {@code null} unless the viewer may see it: {@code owners} with VIEW_OWNERS, {@code tenants}
     * with COUNT_TENANTS and {@code vacantUnits} (units nobody lives in) with VIEW_VACANCIES.
     */
    record AssociationSummary(String id, String name, String district, Integer owners, Integer tenants,
                              List<String> vacantUnits) {
    }

    record Member(String username, String displayName, String unit, boolean livesThere) {
    }

    /**
     * {@code tenants} lists only the tenants the viewer may see (VIEW_TENANT, checked per tenancy);
     * {@code tenantCount} counts all of them and is {@code null} without COUNT_TENANTS.
     */
    record MemberList(List<Member> owners, List<Member> tenants, Integer tenantCount) {
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
                .map(association -> summary(association, me))
                .toList();
    }

    @GetMapping("/{id}")
    @RequirePermission(action = "VIEW_ASSOCIATION", on = "id")
    AssociationSummary get(@PathVariable String id, User me) {
        return summary(association(id), me);
    }

    @GetMapping("/{id}/members")
    @RequirePermission(action = "VIEW_OWNERS", on = "id")
    MemberList members(@PathVariable String id, User me) {
        Association association = association(id);
        Residents residents = association.residents().activeAt(community.now());
        List<Membership> tenancies = residents.tenants().list();
        List<Member> visibleTenants = permissions.check(me).may(VIEW_TENANT).filter(tenancies).stream()
                .map(AssociationController::member)
                .toList();
        return new MemberList(members(residents.owners()), visibleTenants,
                may(me, COUNT_TENANTS, association) ? tenancies.size() : null);
    }

    private AssociationSummary summary(Association association, User me) {
        Residents residents = association.residents().activeAt(community.now());
        return new AssociationSummary(association.id(), association.name(), association.district().name(),
                may(me, VIEW_OWNERS, association) ? residents.owners().people().size() : null,
                may(me, COUNT_TENANTS, association) ? residents.tenants().people().size() : null,
                may(me, VIEW_VACANCIES, association)
                        ? association.units().stream().filter(unit -> unit.isVacantAt(community.now())).map(Unit::name).toList()
                        : null);
    }

    private boolean may(User me, Action<Association> action, Association association) {
        return permissions.check(me).may(action).on(association).granted();
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
