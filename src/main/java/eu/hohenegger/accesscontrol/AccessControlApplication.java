package eu.hohenegger.accesscontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/** Personas are chosen, not logged in, so Boot's generated default user is not needed. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AccessControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccessControlApplication.class, args);
    }
}
