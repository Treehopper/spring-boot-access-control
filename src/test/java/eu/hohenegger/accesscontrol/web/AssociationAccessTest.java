package eu.hohenegger.accesscontrol.web;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssociationAccessTest extends AccessTestSupport {

    @TestFactory
    Stream<DynamicTest> viewingAnAssociation() {
        return Stream.of(
                access("view Maple Court", () -> get("/api/associations/maple-court")).grantedOnlyTo("alice", "bob", "carol", "frank", "kim", "mia"),
                access("view Oak Terrace", () -> get("/api/associations/oak-terrace")).grantedOnlyTo("carol", "grace", "heidi"),
                access("view Birch Hill", () -> get("/api/associations/birch-hill")).grantedOnlyTo("ivan", "judy", "leo"),
                access("view an association that does not exist", () -> get("/api/associations/nowhere")).grantedToNobody()
        ).flatMap(tests -> tests);
    }

    @TestFactory
    Stream<DynamicTest> viewingTheMemberList() {
        return Stream.of(
                access("see the member list of Maple Court", () -> get("/api/associations/maple-court/members")).grantedOnlyTo("alice", "carol", "frank", "mia"),
                access("see the member list of Oak Terrace", () -> get("/api/associations/oak-terrace/members")).grantedOnlyTo("carol", "grace"),
                access("see the member list of Birch Hill", () -> get("/api/associations/birch-hill/members")).grantedOnlyTo("ivan", "judy")
        ).flatMap(tests -> tests);
    }

    @TestFactory
    Stream<DynamicTest> listingAssociationsShowsOnlyOwnOnes() {
        Map<String, List<String>> visible = new LinkedHashMap<>();
        visible.put("alice", List.of("maple-court"));
        visible.put("bob", List.of("maple-court"));
        visible.put("carol", List.of("maple-court", "oak-terrace"));
        visible.put("dave", List.of());
        visible.put("erin", List.of());
        visible.put("frank", List.of("maple-court"));
        visible.put("grace", List.of("oak-terrace"));
        visible.put("heidi", List.of("oak-terrace"));
        visible.put("ivan", List.of("birch-hill"));
        visible.put("judy", List.of("birch-hill"));
        visible.put("kim", List.of("maple-court"));
        visible.put("leo", List.of("birch-hill"));
        visible.put("mia", List.of("maple-court"));
        return visible.entrySet().stream().map(entry -> DynamicTest.dynamicTest(entry.getKey() + " sees " + entry.getValue(), () ->
                as(entry.getKey(), get("/api/associations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[*].id", containsInAnyOrder(entry.getValue().toArray())))));
    }

    @TestFactory
    Stream<DynamicTest> ownersSeeTheTenantsOfTheirOwnFlatsByName() {
        Map<String, List<String>> visibleTenants = new LinkedHashMap<>();
        visibleTenants.put("alice", List.of("bob"));
        visibleTenants.put("frank", List.of("kim"));
        visibleTenants.put("mia", List.of());
        visibleTenants.put("carol", List.of("bob", "kim"));
        return visibleTenants.entrySet().stream().map(entry -> DynamicTest.dynamicTest(entry.getKey() + " sees tenants " + entry.getValue(), () ->
                as(entry.getKey(), get("/api/associations/maple-court/members"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.tenants[*].username", containsInAnyOrder(entry.getValue().toArray())))
                        .andExpect(jsonPath("$.tenantCount").value(2))));
    }

    @Test
    void ownersSeeOtherOwnersButOnlyTheNumberOfOtherTenants() throws Exception {
        as("alice", get("/api/associations/maple-court/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners[*].unit", containsInAnyOrder("Apt 1", "Apt 2", "Apt 3", "Apt 4")))
                .andExpect(jsonPath("$.owners[*].username", containsInAnyOrder("alice", "frank", "frank", "mia")))
                .andExpect(jsonPath("$.tenants[*].username", containsInAnyOrder("bob")))
                .andExpect(jsonPath("$.tenantCount").value(2));
    }

    @Test
    void propertyManagersSeeTenantNames() throws Exception {
        as("carol", get("/api/associations/maple-court/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants[*].username", containsInAnyOrder("bob", "kim")))
                .andExpect(jsonPath("$.tenantCount").value(2));
    }

    @Test
    void ownerAndTenantOfTheSameUnitHaveDifferentRights() throws Exception {
        as("alice", get("/api/associations/maple-court/members"))
                .andExpect(jsonPath("$.owners[?(@.username == 'alice')].unit").value(contains("Apt 1")));
        as("carol", get("/api/associations/maple-court/members"))
                .andExpect(jsonPath("$.tenants[?(@.username == 'bob')].unit").value(contains("Apt 1")));
        as("bob", get("/api/associations/maple-court/members")).andExpect(status().isForbidden());
        as("bob", get("/api/residents/alice/dues")).andExpect(status().isForbidden());
        as("alice", get("/api/messages/{id}", community.messages().stream()
                .filter(message -> message.subject().equals("Tenant registration")).findFirst().orElseThrow().id()))
                .andExpect(status().isForbidden());
    }

    @Test
    void erinsFormerApartmentIsTheOnlyVacantUnit() throws Exception {
        as("carol", get("/api/associations/maple-court"))
                .andExpect(jsonPath("$.vacantUnits", contains("Apt 3")))
                .andExpect(jsonPath("$.owners").value(3))
                .andExpect(jsonPath("$.tenants").value(2));
        as("ivan", get("/api/associations/birch-hill")).andExpect(jsonPath("$.vacantUnits", empty()));
    }

    @Test
    void anOwnerWhoLivesInHerFlatHasOwnerRightsAndTheFlatIsNotVacant() throws Exception {
        as("mia", get("/api/associations/maple-court/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners[?(@.username == 'mia')].livesThere").value(contains(true)))
                .andExpect(jsonPath("$.owners[?(@.username == 'alice')].livesThere").value(contains(false)));
        as("carol", get("/api/associations/maple-court")).andExpect(jsonPath("$.vacantUnits", contains("Apt 3")));
    }

    @Test
    void formerResidentsAreNotListed() throws Exception {
        as("carol", get("/api/associations/maple-court/members"))
                .andExpect(jsonPath("$.tenants[?(@.username == 'erin')]").isEmpty());
    }

    @Test
    void withoutPersonaTheApiAsksForOne() throws Exception {
        mvc.perform(get("/api/associations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/associations/maple-court")).andExpect(status().isUnauthorized());
    }
}
