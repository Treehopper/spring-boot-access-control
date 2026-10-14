package eu.hohenegger.accesscontrol.permission.rules;

import eu.hohenegger.accesscontrol.permission.PolicyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static eu.hohenegger.accesscontrol.permission.Action.READ_MESSAGE;
import static eu.hohenegger.accesscontrol.permission.Action.SEND_MESSAGE;
import static eu.hohenegger.accesscontrol.permission.Relationships.ADDRESSEES;
import static eu.hohenegger.accesscontrol.permission.Relationships.DISTRICT_MANAGERS;
import static eu.hohenegger.accesscontrol.permission.Relationships.SENDERS;

/** Messages from management to residents. */
@Configuration
public class MessageRules {

    @Bean
    PolicyCustomizer messagePermissions() {
        return policy -> policy
                .allow(DISTRICT_MANAGERS).to(SEND_MESSAGE, READ_MESSAGE)
                .allow(SENDERS).to(READ_MESSAGE)
                .allow(ADDRESSEES).to(READ_MESSAGE);
    }
}
