package eu.hohenegger.accesscontrol.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class AuditLogTest extends AccessTestSupport {

    @Test
    void deniedRequestsAreLoggedWithTheReason(CapturedOutput output) throws Exception {
        as("bob", json(post("/api/polls/{id}/votes", pollIn("maple-court")), "{\"choice\":\"YES\"}")).andExpect(status().isForbidden());
        assertThat(output).contains("DENIED  bob VOTE on poll #" + pollIn("maple-court") + " in Maple Court (is not owner)");
    }

    @Test
    void grantedRequestsAreLoggedWithTheReason(CapturedOutput output) throws Exception {
        as("dave", get("/api/residents/alice/dues")).andExpect(status().isOk());
        assertThat(output).contains("GRANTED dave VIEW_DUES on alice (Accountant for Alice Andersen)");
    }

    @Test
    void filteredListsAreLogged(CapturedOutput output) throws Exception {
        as("bob", get("/api/messages")).andExpect(status().isOk());
        assertThat(output).contains("FILTERED bob READ_MESSAGE: 4 of 7 granted");
    }

    @Test
    void personaSwitchesAndAdministrativeChangesAreLogged(CapturedOutput output) throws Exception {
        mvc.perform(post("/api/session").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"carol\"}"))
                .andExpect(status().isOk());
        as("carol", delete("/api/admin/associations/maple-court/units/apt-1/residents/bob")).andExpect(status().isNoContent());
        assertThat(output)
                .contains("SESSION visitor now acts as carol (Property manager · Riverside district)")
                .contains("ADMIN   carol ended bob's access to Apt 1 in Maple Court");
    }

    @Test
    void requestsForUnknownTargetsAreLoggedAsDenied(CapturedOutput output) throws Exception {
        as("alice", get("/api/messages/999")).andExpect(status().isForbidden());
        assertThat(output).contains("DENIED  alice READ_MESSAGE on message 999 (no such message)");
    }
}
