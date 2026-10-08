package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Delegation;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Role;
import eu.hohenegger.accesscontrol.domain.Unit;
import eu.hohenegger.accesscontrol.domain.User;
import eu.hohenegger.accesscontrol.permission.RequirePermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static eu.hohenegger.accesscontrol.permission.Audit.LOG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Managing who belongs where. Nothing is deleted: ending access sets an end date, so the history remains.
 */
@RestController
@RequestMapping("/api/admin")
class AdminController {

    record Resident(PersonRef person, Role role, String unitId, String unit, boolean livesThere,
                    Instant since, Instant until, boolean active) {
    }

    /**
     * {@code livesThere} only matters for owners; tenants always live in their unit. When omitted, an existing
     * membership keeps its value and a re-admitted resident gets the value of their previous membership.
     */
    record ResidentChange(Role role, Boolean livesThere) {
    }

    record DelegateChange(String purpose) {
    }

    record Delegate(PersonRef holder, PersonRef delegate, String purpose, Instant since) {
    }

    private final Community community;

    AdminController(Community community) {
        this.community = community;
    }

    @GetMapping("/associations/{id}/residents")
    @RequirePermission(action = "MANAGE_RESIDENTS", on = "id")
    List<Resident> residents(@PathVariable String id) {
        Instant now = community.now();
        return association(id).residents().stream()
                .sorted(Comparator.comparing((Membership membership) -> membership.unit().name())
                        .thenComparing(membership -> !membership.isActiveAt(now))
                        .thenComparing(Membership::role))
                .map(this::resident)
                .toList();
    }

    /**
     * Admits someone to a unit, or changes the role of their current membership in that unit. Memberships in other
     * units are not affected, so an owner of two flats is managed per flat.
     */
    @PutMapping("/associations/{id}/units/{unitId}/residents/{username}")
    @RequirePermission(action = "MANAGE_RESIDENTS", on = "id")
    Resident putResident(@PathVariable String id, @PathVariable String unitId, @PathVariable String username,
                         @RequestBody ResidentChange change, User me) {
        if (change.role() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Role OWNER or TENANT is required");
        }
        Unit unit = unit(id, unitId);
        User person = user(username);
        Optional<Membership> current = unit.residents().activeAt(community.now()).of(person);
        if (current.isPresent()) {
            Membership membership = current.get();
            Role previous = membership.role();
            membership.changeRoleTo(change.role());
            if (change.livesThere() != null) {
                membership.livesThere(change.livesThere());
            }
            LOG.info("ADMIN   {} changed {} in {} {}: {} -> {}", me, person, unit.association(), unit, previous, membership.role());
            return resident(membership);
        }
        // Reinstating someone restores whether they lived there, unless the request says otherwise.
        boolean livesThere = change.livesThere() != null
                ? change.livesThere()
                : unit.residents().of(person).map(Membership::livesThere).orElse(false);
        Membership membership = unit.admit(person, change.role()).livesThere(livesThere);
        LOG.info("ADMIN   {} admitted {} to {} {} as {}{}", me, person, unit.association(), unit, change.role(),
                change.role() == Role.OWNER && membership.livesThere() ? " who lives there" : "");
        return resident(membership);
    }

    @DeleteMapping("/associations/{id}/units/{unitId}/residents/{username}")
    @RequirePermission(action = "MANAGE_RESIDENTS", on = "id")
    ResponseEntity<Void> endResidency(@PathVariable String id, @PathVariable String unitId, @PathVariable String username, User me) {
        Unit unit = unit(id, unitId);
        User person = user(username);
        Membership membership = unit.residents().activeAt(community.now()).of(person)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, person.displayName() + " is not a current resident of " + unit));
        membership.until(community.now());
        LOG.info("ADMIN   {} ended {}'s access to {} in {}", me, person, unit, unit.association());
        return ResponseEntity.noContent().build();
    }

    /** Lets {@code delegate} see the dues account of {@code username}, or changes the stated purpose. */
    @PutMapping("/residents/{username}/delegates/{delegate}")
    @RequirePermission(action = "MANAGE_DELEGATES", on = "username")
    Delegate putDelegate(@PathVariable String username, @PathVariable String delegate, @RequestBody DelegateChange change, User me) {
        User holder = user(username);
        User delegateUser = user(delegate);
        if (holder.equals(delegateUser)) {
            throw new ResponseStatusException(BAD_REQUEST, "Nobody needs to be their own delegate");
        }
        String purpose = change.purpose() == null || change.purpose().isBlank() ? "Delegate" : change.purpose().strip();
        Delegation delegation = activeDelegation(holder, delegateUser)
                .map(existing -> existing.purpose(purpose))
                .orElseGet(() -> community.grant(delegateUser).accessToDuesOf(holder).as(purpose));
        LOG.info("ADMIN   {} let {} see the dues of {} as {}", me, delegateUser, holder, purpose);
        return new Delegate(PersonRef.of(holder), PersonRef.of(delegateUser), delegation.purpose(), delegation.since());
    }

    @DeleteMapping("/residents/{username}/delegates/{delegate}")
    @RequirePermission(action = "MANAGE_DELEGATES", on = "username")
    ResponseEntity<Void> endDelegation(@PathVariable String username, @PathVariable String delegate, User me) {
        User holder = user(username);
        User delegateUser = user(delegate);
        Delegation delegation = activeDelegation(holder, delegateUser)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, delegateUser.displayName() + " is no delegate"));
        delegation.until(community.now());
        LOG.info("ADMIN   {} revoked {}'s access to the dues of {}", me, delegateUser, holder);
        return ResponseEntity.noContent().build();
    }

    private Optional<Delegation> activeDelegation(User holder, User delegate) {
        return community.delegations().from(holder).to(delegate).activeAt(community.now()).latest();
    }

    private Resident resident(Membership membership) {
        return new Resident(PersonRef.of(membership.resident()), membership.role(), membership.unit().id(),
                membership.unit().name(), membership.livesThere(), membership.since(), membership.until().orElse(null),
                membership.isActiveAt(community.now()));
    }

    private Association association(String id) {
        return community.findAssociation(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    private Unit unit(String associationId, String unitId) {
        Association association = association(associationId);
        return association.findUnit(unitId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No unit " + unitId + " in " + association));
    }

    private User user(String username) {
        return community.findUser(username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No user " + username));
    }
}
