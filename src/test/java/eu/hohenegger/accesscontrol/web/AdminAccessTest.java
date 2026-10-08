package eu.hohenegger.accesscontrol.web;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAccessTest extends AccessTestSupport {

    @TestFactory
    Stream<DynamicTest> managingResidents() {
        return Stream.of(
                access("list the residents of Maple Court", () -> get("/api/admin/associations/maple-court/residents")).grantedOnlyTo("carol"),
                access("list the residents of Oak Terrace", () -> get("/api/admin/associations/oak-terrace/residents")).grantedOnlyTo("carol"),
                access("list the residents of Birch Hill", () -> get("/api/admin/associations/birch-hill/residents")).grantedOnlyTo("ivan"),
                access("make Bob an owner", () -> json(put("/api/admin/associations/maple-court/units/apt-1/residents/bob"), "{\"role\":\"OWNER\"}")).grantedOnlyTo("carol"),
                access("reinstate Erin", () -> json(put("/api/admin/associations/maple-court/units/apt-3/residents/erin"), "{\"role\":\"TENANT\"}")).grantedOnlyTo("carol"),
                access("admit Judy to Birch Hill again", () -> json(put("/api/admin/associations/birch-hill/units/house-2/residents/judy"), "{\"role\":\"OWNER\"}")).grantedOnlyTo("ivan"),
                access("end Bob's residency", () -> delete("/api/admin/associations/maple-court/units/apt-1/residents/bob")).grantedOnlyTo("carol")
        ).flatMap(tests -> tests);
    }

    @TestFactory
    Stream<DynamicTest> managingDelegates() {
        return Stream.of(
                access("let Frank see Alice's dues", () -> json(put("/api/admin/residents/alice/delegates/frank"), "{\"purpose\":\"Neighbour\"}")).grantedOnlyTo("alice", "carol"),
                access("revoke Dave's access to Alice's dues", () -> delete("/api/admin/residents/alice/delegates/dave")).grantedOnlyTo("alice", "carol"),
                access("let Grace's partner see Judy's dues", () -> json(put("/api/admin/residents/judy/delegates/grace"), "{\"purpose\":\"Partner\"}")).grantedOnlyTo("ivan", "judy")
        ).flatMap(tests -> tests);
    }

    @Test
    void aTenantMadeOwnerMayVoteAndSeeTheMemberList() throws Exception {
        as("bob", json(post("/api/polls/{id}/votes", pollIn("maple-court")), "{\"choice\":\"YES\"}")).andExpect(status().isForbidden());
        as("carol", json(put("/api/admin/associations/maple-court/units/apt-1/residents/bob"), "{\"role\":\"OWNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
        as("bob", json(post("/api/polls/{id}/votes", pollIn("maple-court")), "{\"choice\":\"YES\"}")).andExpect(status().isOk());
        as("bob", get("/api/associations/maple-court/members")).andExpect(status().isOk());
    }

    @Test
    void endingAResidencyRevokesAccessImmediatelyAndKeepsHistory() throws Exception {
        as("carol", delete("/api/admin/associations/maple-court/units/apt-1/residents/alice")).andExpect(status().isNoContent());
        as("alice", get("/api/associations/maple-court")).andExpect(status().isForbidden());
        as("alice", get("/api/messages/{id}", messageId("Water shut-off on Tuesday"))).andExpect(status().isForbidden());
        as("carol", get("/api/admin/associations/maple-court/residents"))
                .andExpect(jsonPath("$[?(@.person.username == 'alice')].active").value(false));
    }

    @Test
    void aReinstatedFormerTenantSeesNoticesAgain() throws Exception {
        as("erin", get("/api/associations/maple-court")).andExpect(status().isForbidden());
        as("carol", json(put("/api/admin/associations/maple-court/units/apt-3/residents/erin"), "{\"role\":\"TENANT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.unit").value("Apt 3"));
        as("carol", get("/api/associations/maple-court")).andExpect(jsonPath("$.vacantUnits").isEmpty());
        as("erin", get("/api/associations/maple-court")).andExpect(status().isOk());
        as("erin", get("/api/messages/{id}", messageId("Water shut-off on Tuesday"))).andExpect(status().isOk());
        as("erin", get("/api/associations/maple-court/members")).andExpect(status().isForbidden());
    }

    @Test
    void delegationsCanBeGrantedAndRevoked() throws Exception {
        as("frank", get("/api/residents/alice/dues")).andExpect(status().isForbidden());
        as("alice", json(put("/api/admin/residents/alice/delegates/frank"), "{\"purpose\":\"Neighbour\"}")).andExpect(status().isOk());
        as("frank", get("/api/residents/alice/dues")).andExpect(status().isOk());

        as("alice", delete("/api/admin/residents/alice/delegates/dave")).andExpect(status().isNoContent());
        as("dave", get("/api/residents/alice/dues")).andExpect(status().isForbidden());
    }

    @Test
    void anOwnerOfTwoFlatsIsManagedPerFlat() throws Exception {
        as("carol", delete("/api/admin/associations/maple-court/units/apt-2/residents/frank")).andExpect(status().isNoContent());
        as("carol", get("/api/admin/associations/maple-court/residents"))
                .andExpect(jsonPath("$[?(@.person.username == 'frank' && @.unitId == 'apt-2')].active").value(contains(false)))
                .andExpect(jsonPath("$[?(@.person.username == 'frank' && @.unitId == 'apt-3')].active").value(contains(true)));
        as("frank", json(post("/api/polls/{id}/votes", pollIn("maple-court")), "{\"choice\":\"NO\"}")).andExpect(status().isOk());
    }

    @Test
    void anOwnerCanMoveIntoTheirOwnFlat() throws Exception {
        as("carol", json(put("/api/admin/associations/maple-court/units/apt-3/residents/frank"), "{\"role\":\"OWNER\",\"livesThere\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livesThere").value(true));
        as("carol", get("/api/associations/maple-court")).andExpect(jsonPath("$.vacantUnits").isEmpty());
        as("carol", get("/api/admin/associations/maple-court/residents"))
                .andExpect(jsonPath("$[?(@.person.username == 'frank' && @.unitId == 'apt-2')].livesThere").value(contains(false)));
    }

    @Test
    void usernamesAreMatchedIgnoringCase() throws Exception {
        as("alice", json(put("/api/admin/residents/alice/delegates/Dave"), "{\"purpose\":\"Accountant\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delegate.username").value("dave"));
    }

    @Test
    void errorsExplainWhatWentWrong() throws Exception {
        as("alice", json(put("/api/admin/residents/alice/delegates/davey"), "{\"purpose\":\"Accountant\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No user davey"));
    }

    @Test
    void reinstatingAnOwnerWhoLivedInHerFlatRestoresThat() throws Exception {
        as("carol", delete("/api/admin/associations/maple-court/units/apt-4/residents/mia")).andExpect(status().isNoContent());
        as("carol", get("/api/associations/maple-court")).andExpect(jsonPath("$.vacantUnits", contains("Apt 3", "Apt 4")));

        as("carol", json(put("/api/admin/associations/maple-court/units/apt-4/residents/mia"), "{\"role\":\"OWNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livesThere").value(true));
        as("carol", get("/api/associations/maple-court")).andExpect(jsonPath("$.vacantUnits", contains("Apt 3")));
        as("mia", get("/api/messages/{id}", messageId("Bicycle storage key"))).andExpect(status().isOk());
    }

    @Test
    void invalidChangesAreRejected() throws Exception {
        as("carol", json(put("/api/admin/associations/maple-court/units/apt-1/residents/bob"), "{}")).andExpect(status().isBadRequest());
        as("carol", json(put("/api/admin/associations/maple-court/units/apt-1/residents/nobody"), "{\"role\":\"OWNER\"}")).andExpect(status().isNotFound());
        as("carol", json(put("/api/admin/associations/maple-court/units/apt-99/residents/grace"), "{\"role\":\"OWNER\"}")).andExpect(status().isNotFound());
        as("carol", delete("/api/admin/associations/maple-court/units/apt-3/residents/erin")).andExpect(status().isNotFound());
        as("carol", delete("/api/admin/associations/maple-court/units/apt-1/residents/frank")).andExpect(status().isNotFound());
        as("alice", json(put("/api/admin/residents/alice/delegates/alice"), "{}")).andExpect(status().isBadRequest());
        as("alice", delete("/api/admin/residents/alice/delegates/frank")).andExpect(status().isNotFound());
    }
}
