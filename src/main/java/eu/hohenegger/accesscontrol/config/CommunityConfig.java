package eu.hohenegger.accesscontrol.config;

import eu.hohenegger.accesscontrol.demo.DemoData;
import eu.hohenegger.accesscontrol.domain.Community;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CommunityConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Community community(Clock clock) {
        Community community = new Community(clock);
        DemoData.populate(community);
        return community;
    }
}
