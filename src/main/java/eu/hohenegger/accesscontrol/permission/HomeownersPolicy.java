package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.Poll;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static eu.hohenegger.accesscontrol.permission.Action.CREATE_POLL;
import static eu.hohenegger.accesscontrol.permission.Action.MANAGE_DELEGATES;
import static eu.hohenegger.accesscontrol.permission.Action.MANAGE_RESIDENTS;
import static eu.hohenegger.accesscontrol.permission.Action.READ_MESSAGE;
import static eu.hohenegger.accesscontrol.permission.Action.SEND_MESSAGE;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_ASSOCIATION;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_DUES;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_MEMBERS;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_POLLS;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_TENANT;
import static eu.hohenegger.accesscontrol.permission.Action.VOTE;
import static eu.hohenegger.accesscontrol.permission.Conditions.addressees;
import static eu.hohenegger.accesscontrol.permission.Conditions.delegates;
import static eu.hohenegger.accesscontrol.permission.Conditions.districtManagers;
import static eu.hohenegger.accesscontrol.permission.Conditions.landlords;
import static eu.hohenegger.accesscontrol.permission.Conditions.managersOfTheHolder;
import static eu.hohenegger.accesscontrol.permission.Conditions.owners;
import static eu.hohenegger.accesscontrol.permission.Conditions.residents;
import static eu.hohenegger.accesscontrol.permission.Conditions.theAccountHolder;
import static eu.hohenegger.accesscontrol.permission.Conditions.theSender;

/** The whole permission model of the demo in one place. */
@Configuration
public class HomeownersPolicy {

    @Bean
    Policy policy() {
        return Policy.define()
                .allow(VIEW_ASSOCIATION).to(residents(), districtManagers())
                .allow(VIEW_MEMBERS).to(owners(), districtManagers())
                .allow(VIEW_TENANT).to(landlords(), districtManagers().of(Membership::association))
                .allow(VIEW_POLLS).to(owners(), districtManagers())
                .allow(CREATE_POLL).to(districtManagers())
                .allow(VOTE).to(owners().of(Poll::association))
                .allow(SEND_MESSAGE).to(districtManagers())
                .allow(READ_MESSAGE).to(theSender(), addressees(), districtManagers().of(Message::association))
                .allow(MANAGE_RESIDENTS).to(districtManagers())
                .allow(VIEW_DUES).to(theAccountHolder(), delegates(), managersOfTheHolder())
                .allow(MANAGE_DELEGATES).to(theAccountHolder(), managersOfTheHolder())
                .build();
    }
}
