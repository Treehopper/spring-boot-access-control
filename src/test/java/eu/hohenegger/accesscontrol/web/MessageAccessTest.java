package eu.hohenegger.accesscontrol.web;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageAccessTest extends AccessTestSupport {

    private static final String WATER = "Water shut-off on Tuesday";        // everyone in Maple Court
    private static final String ASSEMBLY = "Annual assembly: agenda";       // owners of Maple Court
    private static final String BICYCLES = "Bicycle storage key";           // everyone living in Maple Court
    private static final String REGISTRATION = "Tenant registration";       // tenants of Maple Court
    private static final String PARKING = "Your parking permit";            // bob personally
    private static final String GARDEN = "Garden clean-up day";             // everyone in Oak Terrace
    private static final String SNOW = "Snow clearing schedule";            // everyone in Birch Hill, sent by ivan

    @TestFactory
    Stream<DynamicTest> readingAMessage() {
        return Stream.of(
                access("read a notice to everyone in Maple Court", () -> get("/api/messages/{id}", messageId(WATER))).grantedOnlyTo("alice", "bob", "carol", "frank", "kim", "mia"),
                access("read a message to the owners of Maple Court", () -> get("/api/messages/{id}", messageId(ASSEMBLY))).grantedOnlyTo("alice", "carol", "frank", "mia"),
                access("read a message to everyone living in Maple Court", () -> get("/api/messages/{id}", messageId(BICYCLES))).grantedOnlyTo("bob", "carol", "kim", "mia"),
                access("read a message to the tenants of Maple Court", () -> get("/api/messages/{id}", messageId(REGISTRATION))).grantedOnlyTo("bob", "carol", "kim"),
                access("read a personal message to Bob", () -> get("/api/messages/{id}", messageId(PARKING))).grantedOnlyTo("bob", "carol"),
                access("read a notice to everyone in Oak Terrace", () -> get("/api/messages/{id}", messageId(GARDEN))).grantedOnlyTo("carol", "grace", "heidi"),
                access("read a notice to everyone in Birch Hill", () -> get("/api/messages/{id}", messageId(SNOW))).grantedOnlyTo("ivan", "judy", "leo"),
                access("read a message that does not exist", () -> get("/api/messages/999")).grantedToNobody()
        ).flatMap(tests -> tests);
    }

    @TestFactory
    Stream<DynamicTest> sendingMessages() {
        String notice = "{\"audience\":\"EVERYONE\",\"subject\":\"Hello\",\"body\":\"Hi all\"}";
        return Stream.of(
                access("message Maple Court", () -> json(post("/api/associations/maple-court/messages"), notice)).grantedOnlyTo("carol"),
                access("message Oak Terrace", () -> json(post("/api/associations/oak-terrace/messages"), notice)).grantedOnlyTo("carol"),
                access("message Birch Hill", () -> json(post("/api/associations/birch-hill/messages"), notice)).grantedOnlyTo("ivan"),
                access("message an association that does not exist", () -> json(post("/api/associations/nowhere/messages"), notice)).grantedToNobody()
        ).flatMap(tests -> tests);
    }

    @TestFactory
    Stream<DynamicTest> theInboxContainsExactlyWhatIsAddressedToYou() {
        Map<String, List<String>> inbox = new LinkedHashMap<>();
        inbox.put("alice", List.of(WATER, ASSEMBLY));
        inbox.put("bob", List.of(WATER, BICYCLES, REGISTRATION, PARKING));
        inbox.put("carol", List.of(WATER, ASSEMBLY, BICYCLES, REGISTRATION, PARKING, GARDEN));
        inbox.put("dave", List.of());
        inbox.put("erin", List.of());
        inbox.put("frank", List.of(WATER, ASSEMBLY));
        inbox.put("grace", List.of(GARDEN));
        inbox.put("heidi", List.of(GARDEN));
        inbox.put("ivan", List.of(SNOW));
        inbox.put("judy", List.of(SNOW));
        inbox.put("kim", List.of(WATER, BICYCLES, REGISTRATION));
        inbox.put("leo", List.of(SNOW));
        inbox.put("mia", List.of(WATER, ASSEMBLY, BICYCLES));
        return inbox.entrySet().stream().map(entry -> DynamicTest.dynamicTest(entry.getKey() + " receives " + entry.getValue(), () ->
                as(entry.getKey(), get("/api/messages"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[*].subject", containsInAnyOrder(entry.getValue().toArray())))));
    }

    @TestFactory
    Stream<DynamicTest> aMessageToOwnersOnlyReachesOwners() {
        return access("read a new message to the owners of Maple Court", () -> {
            sendAsCarol("{\"audience\":\"OWNERS\",\"subject\":\"Owners only\",\"body\":\"Budget draft\"}");
            return get("/api/messages/{id}", messageId("Owners only"));
        }).grantedOnlyTo("alice", "carol", "frank", "mia");
    }

    @TestFactory
    Stream<DynamicTest> aMessageToOccupantsReachesEveryoneWhoLivesThere() {
        return access("read a new message to everyone living in Maple Court", () -> {
            sendAsCarol("{\"audience\":\"OCCUPANTS\",\"subject\":\"Lift maintenance\",\"body\":\"Use the stairs on Friday\"}");
            return get("/api/messages/{id}", messageId("Lift maintenance"));
        }).grantedOnlyTo("bob", "carol", "kim", "mia");
    }

    @TestFactory
    Stream<DynamicTest> aPersonalMessageReachesOnlyItsRecipient() {
        return access("read a new personal message to Frank", () -> {
            sendAsCarol("{\"audience\":\"INDIVIDUAL\",\"recipient\":\"frank\",\"subject\":\"For Frank\",\"body\":\"Your key\"}");
            return get("/api/messages/{id}", messageId("For Frank"));
        }).grantedOnlyTo("carol", "frank");
    }

    @Test
    void personalMessagesGoToCurrentResidentsOfThatAssociationOnly() throws Exception {
        String toGrace = "{\"audience\":\"INDIVIDUAL\",\"recipient\":\"grace\",\"subject\":\"S\",\"body\":\"B\"}";
        String toErin = "{\"audience\":\"INDIVIDUAL\",\"recipient\":\"erin\",\"subject\":\"S\",\"body\":\"B\"}";
        as("carol", json(post("/api/associations/maple-court/messages"), toGrace)).andExpect(status().isBadRequest());
        as("carol", json(post("/api/associations/maple-court/messages"), toErin)).andExpect(status().isBadRequest());
    }

    @Test
    void aMessageNeedsSubjectAndText() throws Exception {
        as("carol", json(post("/api/associations/maple-court/messages"), "{\"audience\":\"EVERYONE\",\"subject\":\"\",\"body\":\"B\"}"))
                .andExpect(status().isBadRequest());
    }

    private void sendAsCarol(String body) {
        try {
            as("carol", json(post("/api/associations/maple-court/messages"), body)).andExpect(status().isCreated());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
