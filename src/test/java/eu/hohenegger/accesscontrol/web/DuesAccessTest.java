package eu.hohenegger.accesscontrol.web;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DuesAccessTest extends AccessTestSupport {

    @TestFactory
    Stream<DynamicTest> viewingDuesAccounts() {
        return Stream.of(
                access("see Alice's dues", () -> get("/api/residents/alice/dues")).grantedOnlyTo("alice", "carol", "dave"),
                access("see Frank's dues", () -> get("/api/residents/frank/dues")).grantedOnlyTo("carol", "frank"),
                access("see Grace's dues", () -> get("/api/residents/grace/dues")).grantedOnlyTo("carol", "grace"),
                access("see Mia's dues", () -> get("/api/residents/mia/dues")).grantedOnlyTo("carol", "mia"),
                access("see Judy's dues", () -> get("/api/residents/judy/dues")).grantedOnlyTo("ivan", "judy"),
                access("see the dues of someone who does not exist", () -> get("/api/residents/nobody/dues")).grantedToNobody()
        ).flatMap(tests -> tests);
    }

    @Test
    void theAccountShowsEntriesAndBalance() throws Exception {
        as("dave", get("/api/residents/alice/dues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(5))
                .andExpect(jsonPath("$.balanceCents").value(1_200_00));
    }

    @Test
    void holderAndManagerSeeWhoElseHasAccessButTheDelegateDoesNot() throws Exception {
        as("alice", get("/api/residents/alice/dues")).andExpect(jsonPath("$.sharedWith[*].delegate.username", containsInAnyOrder("dave")));
        as("carol", get("/api/residents/alice/dues")).andExpect(jsonPath("$.sharedWith[*].delegate.username", containsInAnyOrder("dave")));
        as("dave", get("/api/residents/alice/dues")).andExpect(jsonPath("$.sharedWith").doesNotExist());
    }
}
