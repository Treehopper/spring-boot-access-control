package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.demo.DemoData;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.Poll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demo data (see {@link DemoData}):
 * <pre>
 * Riverside (carol, property manager)
 *   Maple Court   Apt 1    owner alice   tenant bob
 *                 Apt 2    owner frank   tenant kim
 *                 Apt 3    owner frank   (vacant, erin's lease ended)
 *                 Apt 4    owner mia, who lives there herself
 *   Oak Terrace   No. 7    owner grace   tenant heidi
 * Hillside (ivan, property manager)
 *   Birch Hill    House 2  owner judy    tenant leo
 * dave: Alice's accountant
 * </pre>
 */
@AccessControlWebMvcTest
abstract class AccessTestSupport {

    static final List<String> PERSONAS = List.of("alice", "bob", "carol", "dave", "erin", "frank", "grace", "heidi", "ivan", "judy", "kim", "leo", "mia");

    @Autowired
    MockMvc mvc;

    @Autowired
    Community community;

    @BeforeEach
    void resetDemoData() {
        community.clear();
        DemoData.populate(community);
    }

    ResultActions as(String persona, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(user(persona)).with(csrf()));
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    String messageId(String subject) {
        return community.messages().stream().filter(message -> message.subject().equals(subject))
                .map(Message::id).findFirst().orElseThrow();
    }

    String pollIn(String associationId) {
        return community.polls().stream().filter(poll -> poll.association().id().equals(associationId))
                .map(Poll::id).findFirst().orElseThrow();
    }

    /**
     * Expects a request to succeed for exactly the given personas and to be forbidden for all others:
     * {@code access("vote in Maple Court", () -> post(...)).grantedOnlyTo("alice", "frank")}.
     */
    AccessExpectation access(String description, Supplier<MockHttpServletRequestBuilder> request) {
        return new AccessExpectation(description, request);
    }

    final class AccessExpectation {

        private final String description;
        private final Supplier<MockHttpServletRequestBuilder> request;

        private AccessExpectation(String description, Supplier<MockHttpServletRequestBuilder> request) {
            this.description = description;
            this.request = request;
        }

        Stream<DynamicTest> grantedOnlyTo(String... personas) {
            Set<String> granted = Set.of(personas);
            if (!PERSONAS.containsAll(granted)) {
                throw new IllegalArgumentException("Unknown persona in " + granted);
            }
            return PERSONAS.stream().map(persona -> {
                boolean expected = granted.contains(persona);
                return DynamicTest.dynamicTest(persona + (expected ? " may " : " may not ") + description, () -> {
                    resetDemoData();
                    as(persona, request.get())
                            .andExpect(expected ? status().is2xxSuccessful() : status().isForbidden());
                });
            });
        }

        Stream<DynamicTest> grantedToNobody() {
            return grantedOnlyTo();
        }
    }
}
