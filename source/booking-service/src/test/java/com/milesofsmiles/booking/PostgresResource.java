package com.milesofsmiles.booking;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** Starts pinned PostgreSQL with ephemeral module-local certificates for integration tests. */
public class PostgresResource implements QuarkusTestResourceLifecycleManager {
    static final String IMAGE = "postgres:17.6-alpine@sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94";
    static PostgreSQLContainer<?> postgres;
    static Path pki;
    static final String ROLE_PASSWORD = java.util.UUID.randomUUID().toString();

    static String url() {
        return postgres.getJdbcUrl() + "&sslmode=verify-full&sslrootcert=" + pki.resolve("ca.crt");
    }

    @Override
    public Map<String, String> start() {
        try {
            Path directory = Files.createTempDirectory(Path.of("target"), "test-pki-");
            pki = directory.toAbsolutePath();
            Path root = Path.of(System.getProperty("baseline.repository.root")).toAbsolutePath().normalize();
            var builder = new ProcessBuilder("bash", root.resolve("scripts/pki.sh").toString())
                    .directory(root.toFile()).inheritIO();
            builder.environment().put("PKI_DIR", pki.toString());
            int exit = builder.start().waitFor();
            if (exit != 0) {
                throw new IllegalStateException("PKI generation failed: " + exit);
            }
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot run PKI bootstrap", failure);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during PKI bootstrap", failure);
        }
        postgres = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("booking")
                .withUsername("booking")
                .withPassword(java.util.UUID.randomUUID().toString())
                .withCreateContainerCmdModifier(command -> command.withUser("70:70"))
                .withCopyFileToContainer(MountableFile.forHostPath(pki.resolve("postgres/server.crt"), 0644),
                        "/tmp/server.crt")
                .withCopyFileToContainer(MountableFile.forHostPath(pki.resolve("postgres/server.key"), 0644),
                        "/tmp/source.key")
                .withCommand("sh", "-c",
                        "cp /tmp/source.key /tmp/server.key && chmod 600 /tmp/server.key && "
                                + "exec docker-entrypoint.sh postgres -c ssl=on "
                                + "-c ssl_cert_file=/tmp/server.crt -c ssl_key_file=/tmp/server.key");
        postgres.start();
        try (var connection = DriverManager.getConnection(url(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("REVOKE ALL ON SCHEMA public FROM PUBLIC");
            statement.execute("REVOKE CREATE, TEMPORARY ON DATABASE booking FROM PUBLIC");
            for (String owner : new String[]{"booking", "rental", "fleet"}) {
                statement.execute("CREATE ROLE " + owner + "_migrator LOGIN PASSWORD '" + ROLE_PASSWORD + "'");
                statement.execute("CREATE ROLE " + owner + "_app LOGIN PASSWORD '" + ROLE_PASSWORD + "'");
                statement.execute("CREATE SCHEMA " + owner + " AUTHORIZATION " + owner + "_migrator");
            }
            statement.execute("GRANT USAGE ON SCHEMA public TO booking_migrator");
        } catch (java.sql.SQLException failure) {
            throw new IllegalStateException("Cannot initialize owner roles", failure);
        }
        migrate();
        String ca = pki.resolve("ca.crt").toString();
        return Map.of(
                "quarkus.datasource.jdbc.url", postgres.getJdbcUrl() + "&sslmode=verify-full&sslrootcert=" + ca,
                "quarkus.datasource.username", "booking_app",
                "quarkus.datasource.password", ROLE_PASSWORD,
                "quarkus.http.ssl.certificate.files", pki.resolve("app/server.crt").toString(),
                "quarkus.http.ssl.certificate.key-files", pki.resolve("app/server.key").toString());
    }

    static Flyway flyway() {
        return Flyway.configure().dataSource(url(), "booking_migrator", ROLE_PASSWORD)
                .schemas("booking").defaultSchema("booking").createSchemas(false)
                .locations("classpath:db/migration").cleanDisabled(true).load();
    }

    static void migrate() {
        flyway().migrate();
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
