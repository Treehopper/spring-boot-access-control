package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.Role;
import eu.hohenegger.accesscontrol.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static eu.hohenegger.accesscontrol.permission.Audit.LOG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** Choosing a persona replaces authentication in this demo. */
@RestController
@RequestMapping("/api")
class SessionController {

    /** {@code homes} are the units a persona owns or rents, now or in the past; the UI groups personas by them. */
    record Persona(String username, String displayName, String description, List<Home> homes) {
    }

    record Home(String associationId, String association, String district, String unitId, String unit, Role role,
                boolean livesThere, boolean current) {
    }

    record ChoosePersona(String username) {
    }

    private final Community community;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    SessionController(Community community, SecurityContextRepository securityContextRepository) {
        this.community = community;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/personas")
    List<Persona> personas() {
        return community.users().stream().map(this::persona).toList();
    }

    @PostMapping("/session")
    Persona choose(@RequestBody ChoosePersona choice, HttpServletRequest request, HttpServletResponse response) {
        User user = community.findUser(choice.username())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unknown persona " + choice.username()));
        String previous = currentName().orElse("visitor");

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user.username(), null, List.of()));
        securityContextHolderStrategy.setContext(context);
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        securityContextRepository.saveContext(context, request, response);

        Persona persona = persona(user);
        LOG.info("SESSION {} now acts as {} ({})", previous, user, persona.description());
        return persona;
    }

    @DeleteMapping("/session")
    ResponseEntity<Void> leave(HttpServletRequest request, HttpServletResponse response) {
        String previous = currentName().orElse("visitor");
        new SecurityContextLogoutHandler().logout(request, response, securityContextHolderStrategy.getContext().getAuthentication());
        LOG.info("SESSION {} left", previous);
        return ResponseEntity.noContent().build();
    }

    private Persona persona(User user) {
        List<Home> homes = community.membershipsOf(user).stream()
                .map(membership -> new Home(membership.association().id(), membership.association().name(),
                        membership.association().district().name(), membership.unit().id(), membership.unit().name(),
                        membership.role(), membership.livesThere(), membership.isActiveAt(community.now())))
                .toList();
        return new Persona(user.username(), user.displayName(), Personas.describe(community, user), homes);
    }

    private Optional<String> currentName() {
        return Optional.ofNullable(securityContextHolderStrategy.getContext().getAuthentication())
                .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName);
    }
}
