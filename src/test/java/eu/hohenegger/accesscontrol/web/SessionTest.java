package eu.hohenegger.accesscontrol.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionTest extends AccessTestSupport {

    @Test
    void personasCanBeListedWithoutChoosingOne() throws Exception {
        mvc.perform(get("/api/personas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(PERSONAS.size())))
                .andExpect(jsonPath("$[?(@.username == 'erin')].description").value(hasItem(startsWith("Former tenant · Maple Court"))))
                .andExpect(jsonPath("$[?(@.username == 'dave')].description").value("Accountant for Alice Andersen"));
    }

    @Test
    void personasKnowWhereTheyLiveOrLived() throws Exception {
        mvc.perform(get("/api/personas"))
                .andExpect(jsonPath("$[?(@.username == 'frank')].homes[*].unit", containsInAnyOrder("Apt 2", "Apt 3")))
                .andExpect(jsonPath("$[?(@.username == 'mia')].homes[0].livesThere").value(contains(true)))
                .andExpect(jsonPath("$[?(@.username == 'erin')].homes[0].current").value(contains(false)))
                .andExpect(jsonPath("$[?(@.username == 'carol')].homes[*]").isEmpty());
    }

    @Test
    void choosingAPersonaStartsASessionAndLeavingEndsIt() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/session").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Tenant · Maple Court, Apt 1"));

        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"));

        mvc.perform(delete("/api/session").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownPersonasAreRejected() throws Exception {
        mvc.perform(post("/api/session").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"mallory\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changesRequireACsrfToken() throws Exception {
        mvc.perform(post("/api/session").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"bob\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutPersonaThereIsNoProfile() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void theProfileTellsTheUiWhatToOffer() throws Exception {
        as("carol", get("/api/me"))
                .andExpect(jsonPath("$.associations[*].id", contains("maple-court", "oak-terrace")))
                .andExpect(jsonPath("$.associations[0].relation").value("Property manager"))
                .andExpect(jsonPath("$.associations[0].can.sendMessage").value(true))
                .andExpect(jsonPath("$.associations[0].can.manageResidents").value(true));
        as("alice", get("/api/me"))
                .andExpect(jsonPath("$.associations[0].relation").value("Owner"))
                .andExpect(jsonPath("$.associations[0].can.viewMembers").value(true))
                .andExpect(jsonPath("$.associations[0].can.sendMessage").value(false))
                .andExpect(jsonPath("$.duesAccounts[*].username", contains("alice")));
        as("bob", get("/api/me"))
                .andExpect(jsonPath("$.associations[0].relation").value("Tenant"))
                .andExpect(jsonPath("$.associations[0].can.viewMembers").value(false))
                .andExpect(jsonPath("$.associations[0].can.viewPolls").value(false));
        as("dave", get("/api/me"))
                .andExpect(jsonPath("$.associations", empty()))
                .andExpect(jsonPath("$.duesAccounts[*].username", contains("alice")));
        as("erin", get("/api/me"))
                .andExpect(jsonPath("$.associations", empty()))
                .andExpect(jsonPath("$.duesAccounts", empty()));
    }
}
