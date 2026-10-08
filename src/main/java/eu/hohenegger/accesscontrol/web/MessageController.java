package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Audience;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.User;
import eu.hohenegger.accesscontrol.permission.Permissions;
import eu.hohenegger.accesscontrol.permission.RequirePermission;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static eu.hohenegger.accesscontrol.permission.Action.READ_MESSAGE;
import static eu.hohenegger.accesscontrol.permission.Audit.LOG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
class MessageController {

    record MessageView(String id, String association, String associationName, PersonRef from, Audience audience,
                       String audienceLabel, String subject, String body, Instant sentAt) {
    }

    /** {@code recipient} is the username for {@link Audience#INDIVIDUAL} messages. */
    record NewMessage(Audience audience, String recipient, String subject, String body) {
    }

    private final Community community;
    private final Permissions permissions;

    MessageController(Community community, Permissions permissions) {
        this.community = community;
        this.permissions = permissions;
    }

    @GetMapping("/messages")
    List<MessageView> inbox(User me) {
        return permissions.check(me).may(READ_MESSAGE).filter(community.messages()).stream()
                .sorted(Comparator.comparing(Message::sentAt).thenComparing(message -> Long.parseLong(message.id())).reversed())
                .map(MessageController::view)
                .toList();
    }

    @GetMapping("/messages/{id}")
    @RequirePermission(action = "READ_MESSAGE", on = "id")
    MessageView read(@PathVariable String id) {
        return view(community.findMessage(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
    }

    @PostMapping("/associations/{id}/messages")
    @RequirePermission(action = "SEND_MESSAGE", on = "id")
    @ResponseStatus(HttpStatus.CREATED)
    MessageView send(@PathVariable String id, @RequestBody NewMessage newMessage, User me) {
        Association association = community.findAssociation(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (newMessage.audience() == null || isBlank(newMessage.subject()) || isBlank(newMessage.body())) {
            throw new ResponseStatusException(BAD_REQUEST, "Audience, subject and text are required");
        }
        User recipient = newMessage.audience() == Audience.INDIVIDUAL ? currentResident(association, newMessage.recipient()) : null;
        Message message = community.message().from(me)
                .addressedTo(newMessage.audience(), association, recipient)
                .about(newMessage.subject().strip())
                .saying(newMessage.body().strip());
        LOG.info("MESSAGE {} sent {} to {}", me, message, audienceLabel(message));
        return view(message);
    }

    private User currentResident(Association association, String username) {
        return community.findUser(username == null ? "" : username)
                .filter(user -> association.residents().activeAt(community.now()).include(user))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST,
                        "'" + username + "' is not a current resident of " + association.name()));
    }

    private static MessageView view(Message message) {
        return new MessageView(message.id(), message.association().id(), message.association().name(),
                PersonRef.of(message.sender()), message.audience(), audienceLabel(message),
                message.subject(), message.body(), message.sentAt());
    }

    private static String audienceLabel(Message message) {
        String association = message.association().name();
        return switch (message.audience()) {
            case EVERYONE -> "Everyone in " + association;
            case OWNERS -> "Owners of " + association;
            case TENANTS -> "Tenants of " + association;
            case OCCUPANTS -> "Everyone living in " + association;
            case INDIVIDUAL -> message.recipient().map(User::displayName).orElse("?") + " (personal)";
        };
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
