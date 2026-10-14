package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.AssociationScoped;
import eu.hohenegger.accesscontrol.domain.DuesAccount;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.Residents;

import java.util.Optional;

import static eu.hohenegger.accesscontrol.permission.Relationship.define;

/**
 * The vocabulary the homeowners' policy is written in. Every relationship counts only while it is active.
 * Relationships to an association apply to everything that belongs to it: its polls, messages, tenancies and the
 * dues accounts of its owners.
 */
public final class Relationships {

    private Relationships() {
    }

    public static final Relationship<AssociationScoped> OWNERS = define("owners", AssociationScoped.class,
            "People who currently own a unit in the association, whether they live there or rent it out.",
            (context, target) -> target.associationsAt(context.now())
                    .flatMap(association -> association.residents().activeAt(context.now()).owners().of(context.actor()).stream())
                    .findFirst().map(Object::toString));

    public static final Relationship<AssociationScoped> TENANTS = define("tenants", AssociationScoped.class,
            "People who currently rent a unit in the association.",
            (context, target) -> target.associationsAt(context.now())
                    .flatMap(association -> association.residents().activeAt(context.now()).tenants().of(context.actor()).stream())
                    .findFirst().map(Object::toString));

    public static final Relationship<Membership> LANDLORDS = define("landlords", Membership.class,
            "The owner of the unit a tenant rents. This only relates an owner to their own tenants.",
            (context, tenancy) -> tenancy.unit().residents().activeAt(context.now()).owners().of(context.actor())
                    .map(ownership -> "landlord of " + tenancy.unit() + " in " + tenancy.association()));

    public static final Relationship<AssociationScoped> DISTRICT_MANAGERS = define("district managers", AssociationScoped.class,
            "Staff currently responsible for the district the association belongs to, such as its property manager.",
            (context, target) -> target.associationsAt(context.now())
                    .flatMap(association -> association.district().activeAssignmentOf(context.actor(), context.now()).stream())
                    .findFirst().map(Object::toString));

    public static final Relationship<Message> SENDERS = define("senders", Message.class,
            "Whoever sent the message.",
            (context, message) -> message.sender().equals(context.actor()) ? Optional.of("sender") : Optional.empty());

    public static final Relationship<Message> ADDRESSEES = define("addressees", Message.class,
            "Residents a message is addressed to, judged by their current role: everyone, owners, people living there, "
                    + "tenants, or one person.",
            (context, message) -> {
                Residents active = message.association().residents().activeAt(context.now());
                Residents addressed = switch (message.audience()) {
                    case EVERYONE -> active;
                    case OWNERS -> active.owners();
                    case TENANTS -> active.tenants();
                    case OCCUPANTS -> active.occupants();
                    case INDIVIDUAL -> message.recipient().filter(context.actor()::equals).isPresent() ? active : Residents.none();
                };
                return addressed.of(context.actor()).map(membership ->
                        "addressed to " + message.audience().name().toLowerCase() + " as " + membership);
            });

    public static final Relationship<DuesAccount> ACCOUNT_HOLDERS = define("account holders", DuesAccount.class,
            "The owner a dues account belongs to.",
            (context, account) -> account.holder().equals(context.actor()) ? Optional.of("own account") : Optional.empty());

    public static final Relationship<DuesAccount> DELEGATES = define("delegates", DuesAccount.class,
            "People the account holder currently shares their dues account with, such as their accountant.",
            (context, account) -> context.community().delegations().from(account.holder()).to(context.actor())
                    .activeAt(context.now()).latest().map(Object::toString));
}
