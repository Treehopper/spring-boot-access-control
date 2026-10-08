package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Choice;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Poll;
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

import java.util.List;
import java.util.Map;

import static eu.hohenegger.accesscontrol.permission.Action.VOTE;
import static eu.hohenegger.accesscontrol.permission.Audit.LOG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
class PollController {

    record PollView(String id, String association, String question, PersonRef askedBy, Map<Choice, Long> results,
                    Choice myVote, boolean canVote) {
    }

    record NewPoll(String question) {
    }

    record Ballot(Choice choice) {
    }

    private final Community community;
    private final Permissions permissions;

    PollController(Community community, Permissions permissions) {
        this.community = community;
        this.permissions = permissions;
    }

    @GetMapping("/associations/{id}/polls")
    @RequirePermission(action = "VIEW_POLLS", on = "id")
    List<PollView> list(@PathVariable String id, User me) {
        Association association = association(id);
        return community.polls().stream()
                .filter(poll -> poll.association().equals(association))
                .map(poll -> view(poll, me))
                .toList();
    }

    @PostMapping("/associations/{id}/polls")
    @RequirePermission(action = "CREATE_POLL", on = "id")
    @ResponseStatus(HttpStatus.CREATED)
    PollView create(@PathVariable String id, @RequestBody NewPoll newPoll, User me) {
        if (newPoll.question() == null || newPoll.question().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "A poll needs a question");
        }
        Poll poll = community.poll().in(association(id)).by(me).asking(newPoll.question().strip());
        LOG.info("POLL    {} opened {}: {}", me, poll, poll.question());
        return view(poll, me);
    }

    @PostMapping("/polls/{pollId}/votes")
    @RequirePermission(action = "VOTE", on = "pollId")
    PollView vote(@PathVariable String pollId, @RequestBody Ballot ballot, User me) {
        if (ballot.choice() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose YES, NO or ABSTAIN");
        }
        Poll poll = community.findPoll(pollId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        poll.recordVote(me, ballot.choice());
        LOG.info("VOTE    {} voted on {}", me, poll);
        return view(poll, me);
    }

    private PollView view(Poll poll, User me) {
        return new PollView(poll.id(), poll.association().id(), poll.question(), PersonRef.of(poll.askedBy()), poll.results(),
                poll.voteOf(me).orElse(null), permissions.check(me).may(VOTE).on(poll).granted());
    }

    private Association association(String id) {
        return community.findAssociation(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
