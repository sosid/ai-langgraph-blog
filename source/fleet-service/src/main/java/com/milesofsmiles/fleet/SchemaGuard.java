package com.milesofsmiles.fleet;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class SchemaGuard {
    private final FleetRepository repository;

    @Inject
    public SchemaGuard(FleetRepository repository) {
        this.repository = repository;
    }

    void onStart(@Observes StartupEvent event) {
        repository.verifySchema();
    }
}
