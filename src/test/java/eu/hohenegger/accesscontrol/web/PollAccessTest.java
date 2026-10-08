package eu.hohenegger.accesscontrol.web;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PollAccessTest extends AccessTestSupport {

    @TestFactory
    Stream<DynamicTest> viewingPolls() {
        return Stream.of(
                access("see the polls of Maple Court", () -> get("/api/associations/maple-court/polls")).grantedOnlyTo("alice", "carol", "frank", "mia"),
                access("see the polls of Oak Terrace", () -> get("/api/associations/oak-terrace/polls")).grantedOnlyTo("carol", "grace"),
                access("see the polls of Birch Hill", () -> get("/api/associations/birch-hill/polls")).grantedOnlyTo("ivan", "judy")
        ).flatMap(tests -> tests);
    }

    @TestFactory
    Stream<DynamicTest> openingPolls() {
        return Stream.of(
                access("open a poll in Maple Court", () -> json(post("/api/associations/maple-court/polls"), "{\"question\":\"New door?\"}")).grantedOnlyTo("carol"),
                access("open a poll in Oak Terrace", () -> json(post("/api/associations/oak-terrace/polls"), "{\"question\":\"New door?\"}")).grantedOnlyTo("carol"),
                access("open a poll in Birch Hill", () -> json(post("/api/associations/birch-hill/polls"), "{\"question\":\"New door?\"}")).grantedOnlyTo("ivan")
        ).flatMap(tests -> tests);
    }

    @TestFactory
    Stream<DynamicTest> voting() {
        return Stream.of(
                access("vote in Maple Court", () -> json(post("/api/polls/{id}/votes", pollIn("maple-court")), "{\"choice\":\"NO\"}")).grantedOnlyTo("alice", "frank", "mia"),
                access("vote in Birch Hill", () -> json(post("/api/polls/{id}/votes", pollIn("birch-hill")), "{\"choice\":\"NO\"}")).grantedOnlyTo("judy"),
                access("vote in a poll that does not exist", () -> json(post("/api/polls/999/votes"), "{\"choice\":\"NO\"}")).grantedToNobody()
        ).flatMap(tests -> tests);
    }

    @Test
    void aVoteIsCountedOnceAndCanBeChanged() throws Exception {
        String poll = pollIn("maple-court");
        as("alice", json(post("/api/polls/{id}/votes", poll), "{\"choice\":\"NO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myVote").value("NO"))
                .andExpect(jsonPath("$.results.YES").value(1))
                .andExpect(jsonPath("$.results.NO").value(1));
        as("alice", json(post("/api/polls/{id}/votes", poll), "{\"choice\":\"YES\"}"))
                .andExpect(jsonPath("$.results.YES").value(2))
                .andExpect(jsonPath("$.results.NO").value(0));
    }

    @Test
    void managersFollowResultsButCannotVote() throws Exception {
        as("carol", get("/api/associations/maple-court/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].canVote").value(false))
                .andExpect(jsonPath("$[0].results.YES").value(1));
        as("alice", get("/api/associations/maple-court/polls"))
                .andExpect(jsonPath("$[0].canVote").value(true));
    }

    @Test
    void aBallotNeedsAChoiceAndAPollAQuestion() throws Exception {
        as("alice", json(post("/api/polls/{id}/votes", pollIn("maple-court")), "{}")).andExpect(status().isBadRequest());
        as("carol", json(post("/api/associations/maple-court/polls"), "{\"question\":\" \"}")).andExpect(status().isBadRequest());
    }

    @Test
    void anOpenedPollIsVisibleToOwners() throws Exception {
        as("carol", json(post("/api/associations/maple-court/polls"), "{\"question\":\"New door?\"}"))
                .andExpect(status().isCreated());
        as("frank", get("/api/associations/maple-court/polls"))
                .andExpect(jsonPath("$[1].question").value("New door?"));
    }
}
