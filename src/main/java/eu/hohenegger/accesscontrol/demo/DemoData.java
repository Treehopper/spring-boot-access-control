package eu.hohenegger.accesscontrol.demo;

import eu.hohenegger.accesscontrol.domain.Association;
import eu.hohenegger.accesscontrol.domain.Choice;
import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.District;
import eu.hohenegger.accesscontrol.domain.User;

import java.time.Instant;
import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Two districts with three associations. Most units are rented out by their owner to a tenant; mia lives in her own
 * flat, and Apt 3 has been vacant since erin moved out:
 * <pre>
 * Riverside (carol, property manager)
 *   Maple Court   Apt 1    owner alice   tenant bob
 *                 Apt 2    owner frank   tenant kim
 *                 Apt 3    owner frank   (vacant, erin's lease ended 30 days ago)
 *                 Apt 4    owner mia, who lives there herself
 *   Oak Terrace   No. 7    owner grace   tenant heidi
 * Hillside (ivan, property manager)
 *   Birch Hill    House 2  owner judy    tenant leo
 * dave is Alice's accountant and may see her dues account.
 * </pre>
 */
public final class DemoData {

    private DemoData() {
    }

    public static void populate(Community community) {
        Instant now = community.now();
        LocalDate today = community.today();

        User alice = community.user("alice", "Alice Andersen");
        User bob = community.user("bob", "Bob Brown");
        User carol = community.user("carol", "Carol Chen");
        User dave = community.user("dave", "Dave Dupont");
        User erin = community.user("erin", "Erin Evans");
        User frank = community.user("frank", "Frank Fischer");
        User grace = community.user("grace", "Grace Garcia");
        User heidi = community.user("heidi", "Heidi Huber");
        User ivan = community.user("ivan", "Ivan Ivanov");
        User judy = community.user("judy", "Judy Jensen");
        User kim = community.user("kim", "Kim Kowalski");
        User leo = community.user("leo", "Leo Lambert");
        User mia = community.user("mia", "Mia Moreau");

        District riverside = community.district("Riverside");
        riverside.appoint(carol).as("Property manager");

        Association maple = riverside.association("Maple Court");
        maple.unit("Apt 1").ownedBy(alice).rentedTo(bob);
        maple.unit("Apt 2").ownedBy(frank).rentedTo(kim);
        maple.unit("Apt 3").ownedBy(frank).rentedTo(erin).since(now.minus(400, DAYS)).until(now.minus(30, DAYS));
        maple.unit("Apt 4").occupiedByOwner(mia);

        Association oak = riverside.association("Oak Terrace");
        oak.unit("No. 7").ownedBy(grace).rentedTo(heidi);

        District hillside = community.district("Hillside");
        hillside.appoint(ivan).as("Property manager");

        Association birch = hillside.association("Birch Hill");
        birch.unit("House 2").ownedBy(judy).rentedTo(leo);

        community.grant(dave).accessToDuesOf(alice).as("Accountant");

        community.duesOf(alice)
                .charge(today.minusMonths(5), "Service charge Q2", 450_00)
                .paid(today.minusMonths(5).plusDays(9), "Bank transfer", 450_00)
                .charge(today.minusMonths(2), "Service charge Q3", 450_00)
                .charge(today.minusMonths(1), "Roof repair levy", 1_200_00)
                .paid(today.minusDays(20), "Bank transfer", 450_00);
        community.duesOf(frank)
                .charge(today.minusMonths(2), "Service charge Q3", 520_00)
                .paid(today.minusMonths(2).plusDays(3), "Direct debit", 520_00);
        community.duesOf(mia)
                .charge(today.minusMonths(2), "Service charge Q3", 390_00)
                .paid(today.minusMonths(2).plusDays(5), "Bank transfer", 390_00);
        community.duesOf(grace)
                .charge(today.minusMonths(2), "Service charge Q3", 380_00);
        community.duesOf(judy)
                .charge(today.minusMonths(2), "Service charge Q3", 610_00)
                .paid(today.minusMonths(1), "Bank transfer", 610_00);

        community.poll().in(maple).by(carol).asking("Repaint the facade in spring?")
                .recordVote(frank, Choice.YES);
        community.poll().in(birch).by(ivan).asking("Install EV chargers in the garage?");

        community.message().from(carol).toEveryoneIn(maple).about("Water shut-off on Tuesday")
                .saying("The water supply will be off on Tuesday from 9:00 to 12:00 for pipe maintenance.");
        community.message().from(carol).toOwnersOf(maple).about("Annual assembly: agenda")
                .saying("The agenda for the annual assembly is attached. Proposals are due in two weeks.");
        community.message().from(carol).toOccupantsOf(maple).about("Bicycle storage key")
                .saying("Keys for the new bicycle storage can be picked up at the management office.");
        community.message().from(carol).toTenantsOf(maple).about("Tenant registration")
                .saying("Please return the tenant registration form so we can reach you in an emergency.");
        community.message().from(carol).to(bob).in(maple).about("Your parking permit")
                .saying("Your parking permit for space 12 has been renewed until the end of next year.");
        community.message().from(carol).toEveryoneIn(oak).about("Garden clean-up day")
                .saying("Join us on Saturday at 10:00 to get the shared garden ready for winter.");
        community.message().from(ivan).toEveryoneIn(birch).about("Snow clearing schedule")
                .saying("Snow will be cleared from paths and the driveway by 7:00 on working days.");
    }
}
