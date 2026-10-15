package eu.hohenegger.accesscontrol.config;

import eu.hohenegger.accesscontrol.demo.DemoData;
import eu.hohenegger.accesscontrol.domain.Community;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Configuration
public class CommunityConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** The community, stored in the database and seeded with the demo data at startup. */
    @Bean
    Community community(Clock clock, EntityManager entityManager, PlatformTransactionManager transactionManager) {
        Community community = new Community(clock, entityManager);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            community.clear();
            DemoData.populate(community);
        });
        return community;
    }
}
