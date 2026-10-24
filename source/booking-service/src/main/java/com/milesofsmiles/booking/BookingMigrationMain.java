package com.milesofsmiles.booking;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;

/** Separate owner-credential process; never run inside a runtime service. */
public final class BookingMigrationMain {
    private BookingMigrationMain() {
    }

    public static void main(String[] args) throws Exception {
        String password = Files.readString(Path.of(required("DB_PASSWORD_FILE"))).strip();
        Flyway.configure().dataSource(required("DB_URL"), required("DB_USERNAME"), password)
                .schemas("booking").defaultSchema("booking").createSchemas(false)
                .locations("classpath:db/migration").cleanDisabled(true)
                .validateMigrationNaming(true).load().migrate();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + name);
        }
        return value;
    }
}
