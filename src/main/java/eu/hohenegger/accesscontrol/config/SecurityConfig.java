package eu.hohenegger.accesscontrol.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * There is no login: choosing a persona ({@code POST /api/session}) puts it into the HTTP session. All
 * fine-grained decisions are made per method through {@code @RequirePermission}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Enables placeholders such as {@code {action}} in meta-annotations like {@code @RequirePermission}. */
    @Bean
    static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * The H2 console ({@code spring.h2.console.enabled}) to look at the stored policy. It runs in frames and posts
     * forms without the CSRF token of the portal, so it gets its own chain. Only for the demo: the console can run
     * any SQL and is therefore only reachable from localhost (H2's default).
     */
    @Bean
    @Order(1)
    SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http, @Value("${spring.h2.console.path:/h2-console}") String path) throws Exception {
        return http
                .securityMatcher(path + "/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/personas", "/api/session").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .csrf(csrf -> csrf.spa())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
