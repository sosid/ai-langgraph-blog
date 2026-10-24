package com.milesofsmiles.booking;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Clock;

/** Initializes the fixture at startup; storage failure aborts startup rather than fabricating data. */
@ApplicationScoped
public class FixtureInitializer {
    private final BookingRepository repository;
    private final Clock clock;

    @Inject
    public FixtureInitializer(BookingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    void start(@Observes StartupEvent event) {
        repository.initialize(clock.instant());
    }
}
