package com.milesofsmiles.rental;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;

/** Separate owner-credential process; the application never invokes migrations. */
public final class RentalMigrationMain {
    private RentalMigrationMain() {
    }

    public static void main(String[] args) throws Exception {
        String password = Files.readString(Path.of(required("DB_PASSWORD_FILE")), StandardCharsets.UTF_8)
                .stripTrailing();
        Flyway.configure().dataSource(required("DB_URL"), required("DB_USERNAME"), password)
                .schemas("rental").defaultSchema("rental").locations("classpath:db/migration")
                .createSchemas(false).cleanDisabled(true).load().migrate();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required environment variable missing: " + name);
        }
        return value;
    }
}
