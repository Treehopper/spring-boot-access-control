package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Membership;
import eu.hohenegger.accesscontrol.domain.Message;
import eu.hohenegger.accesscontrol.domain.Residents;
import eu.hohenegger.accesscontrol.domain.User;

import java.util.Optional;

import static eu.hohenegger.accesscontrol.permission.Condition.named;

/** The vocabulary the homeowners' policy is written in. All relationships must be active right now. */
public final class Conditions {

    private Conditions() {
    }

    /** Owners and tenants of the association. */
    public static Condition<Association> residents() {
        return named("resident", (context, association) ->
                association.residents().activeAt(context.now()).of(context.actor()).map(Object::toString));
    }

    public static Condition<Association> owners() {
        return named("owner", (context, association) ->
                association.residents().activeAt(context.now()).owners().of(context.actor()).map(Object::toString));
    }

    public static Condition<Association> tenants() {
        return named("tenant", (context, association) ->
                association.residents().activeAt(context.now()).tenants().of(context.actor()).map(Object::toString));
    }

    /** Staff of the district the association belongs to. */
    public static Condition<Association> districtManagers() {
        return named("district manager", (context, association) ->
                association.district().activeAssignmentOf(context.actor(), context.now()).map(Object::toString));
    }

    /** Owners of the unit a tenancy is for. */
    public static Condition<Membership> landlords() {
        return named("landlord", (context, tenancy) ->
                tenancy.unit().residents().activeAt(context.now()).owners().of(context.actor())
                        .map(ownership -> "landlord of " + tenancy.unit() + " in " + tenancy.association()));
    }

    public static Condition<Message> theSender() {
        return named("sender", (context, message) ->
                message.sender().equals(context.actor()) ? Optional.of("sender") : Optional.empty());
    }

    /** Residents the message is addressed to, judged by their current role. */
    public static Condition<Message> addressees() {
        return named("addressee", (context, message) -> {
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
    }

    public static Condition<User> theAccountHolder() {
        return named("account holder", (context, holder) ->
                holder.equals(context.actor()) ? Optional.of("own account") : Optional.empty());
    }

    public static Condition<User> delegates() {
        return named("delegate", (context, holder) ->
                context.community().delegations().from(holder).to(context.actor()).activeAt(context.now())
                        .latest().map(Object::toString));
    }

    /** Staff of a district in which the account holder currently owns property. */
    public static Condition<User> managersOfTheHolder() {
        return named("manager of the holder's association", (context, holder) ->
                context.community().associations().stream()
                        .flatMap(association -> association.residents().activeAt(context.now()).owners().of(holder).stream())
                        .flatMap(ownership -> ownership.association().district()
                                .activeAssignmentOf(context.actor(), context.now())
                                .map(assignment -> assignment + ", where " + holder + " is " + ownership)
                                .stream())
                        .findFirst());
    }
}
