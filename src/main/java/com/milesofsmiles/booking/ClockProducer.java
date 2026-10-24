package com.milesofsmiles.booking;

import jakarta.enterprise.inject.Produces;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ClockProducer {
    @Produces
    Clock clock(@ConfigProperty(name = "baseline.clock.fixed-instant") Optional<String> fixedInstant) {
        return fixedInstant.map(value -> Clock.fixed(Instant.parse(value), ZoneOffset.UTC))
                .orElseGet(Clock::systemUTC);
    }
}
