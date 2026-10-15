package eu.hohenegger.accesscontrol.domain;

import eu.hohenegger.accesscontrol.config.CommunityConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** The demo data lives in the database: who has which role where, and what changes to it. */
@DataJpaTest
@Import(CommunityConfig.class)
class CommunityPersistenceTest {

    @Autowired
    Community community;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcClient jdbc;

    @Test
    void rolesAreStoredPerUnit() {
        List<Map<String, Object>> frank = jdbc.sql("""
                        select u.name as unit, m.role, m.lives_there, m.until
                        from membership m join unit u on u.id = m.unit_id
                        where m.resident_username = 'frank' order by u.name""")
                .query().listOfRows();

        assertThat(frank).extracting(row -> row.get("UNIT"), row -> row.get("ROLE"), row -> row.get("LIVES_THERE"))
                .containsExactly(
                        tuple("Apt 2", "OWNER", false),
                        tuple("Apt 3", "OWNER", false));
        assertThat(jdbc.sql("select title from staff_assignment where person_username = 'carol'").query(String.class).single())
                .isEqualTo("Property manager");
    }

    @Test
    void changesSurviveANewPersistenceContext() {
        User bob = community.findUser("bob").orElseThrow();
        Unit apt1 = community.findAssociation("maple-court").orElseThrow().findUnit("apt-1").orElseThrow();
        apt1.residents().of(bob).orElseThrow().until(community.now());
        community.poll().in(apt1.association()).by(community.findUser("carol").orElseThrow()).asking("New door?")
                .recordVote(community.findUser("alice").orElseThrow(), Choice.NO);
        entityManager.flush();
        entityManager.clear();

        Unit reloaded = community.findAssociation("maple-court").orElseThrow().findUnit("apt-1").orElseThrow();
        assertThat(reloaded.residents().activeAt(community.now()).people()).extracting(User::username).containsExactly("alice");
        assertThat(reloaded.isVacantAt(community.now())).isTrue();
        assertThat(community.polls()).last().satisfies(poll -> assertThat(poll.results()).containsEntry(Choice.NO, 1L));
    }

    @Test
    void districtsLoadedFromTheDatabaseCanStillAppointStaff() {
        entityManager.clear();
        District riverside = community.districts().getFirst();

        StaffAssignment assignment = riverside.appoint(community.findUser("dave").orElseThrow()).as("Caretaker");

        assertThat(assignment.isActiveAt(community.now())).isTrue();
    }
}
