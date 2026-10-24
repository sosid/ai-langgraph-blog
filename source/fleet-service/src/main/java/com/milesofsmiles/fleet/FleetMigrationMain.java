package com.milesofsmiles.fleet;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;

/** Standalone owner-credential entry point, never invoked by application startup. */
public final class FleetMigrationMain {
    private FleetMigrationMain() {}

    public static void main(String[] args) throws Exception {
        String password = Files.readString(Path.of(required("DB_PASSWORD_FILE"))).stripTrailing();
        migrate(required("DB_URL"), required("DB_USERNAME"), password);
    }

    public static void migrate(String url, String username, String password) {
        Flyway.configure().dataSource(url, username, password)
                .schemas("fleet").defaultSchema("fleet").createSchemas(false)
                .locations("classpath:db/migration").cleanDisabled(true).load().migrate();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing environment variable " + name);
        }
        return value;
    }
}
