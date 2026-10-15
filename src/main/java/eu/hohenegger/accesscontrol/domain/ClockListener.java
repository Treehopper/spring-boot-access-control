package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.PostLoad;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;

/**
 * Gives districts loaded from the database the application's {@link Clock}, so that appointing staff or admitting
 * residents to one of its units records the right time. Hibernate creates this listener through Spring.
 */
class ClockListener {

    private final ObjectProvider<Clock> clock;

    ClockListener(ObjectProvider<Clock> clock) {
        this.clock = clock;
    }

    @PostLoad
    void attach(District district) {
        district.attach(clock.getIfAvailable(Clock::systemUTC));
    }
}
