package com.milesofsmiles.rental;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {
    static final String IMAGE = "postgres:17.6-alpine@sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94";
    static PostgreSQLContainer<?> postgres;
    static Path pki;
    static String url;
    static String appPassword;
    static String migrationPassword;

    @Override
    public Map<String, String> start() {
        try {
            pki = Files.createDirectories(Path.of("target", "rental-test-pki-" + UUID.randomUUID())).toAbsolutePath();
            Path root = Path.of(System.getProperty("baseline.repository.root")).toAbsolutePath().normalize();
            var builder = new ProcessBuilder("bash", root.resolve("scripts/pki.sh").toString())
                    .directory(root.toFile()).inheritIO();
            builder.environment().put("PKI_DIR", pki.toString());
            if (builder.start().waitFor() != 0) {
                throw new IllegalStateException("PKI generation failed");
            }
            postgres = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("rental").withUsername("bootstrap")
                    .withPassword(UUID.randomUUID().toString())
                    .withCreateContainerCmdModifier(command -> command.withUser("70:70"))
                    .withCopyFileToContainer(MountableFile.forHostPath(pki.resolve("postgres/server.crt"), 0644),
                            "/var/lib/postgresql/server.crt")
                    .withCopyFileToContainer(MountableFile.forHostPath(pki.resolve("postgres/server.key"), 0644),
                            "/var/lib/postgresql/source.key")
                    .withCommand("sh", "-c",
                            "cp /var/lib/postgresql/source.key /var/lib/postgresql/server.key && "
                                    + "chmod 600 /var/lib/postgresql/server.key && "
                                    + "exec docker-entrypoint.sh postgres -c ssl=on "
                                    + "-c ssl_cert_file=/var/lib/postgresql/server.crt "
                                    + "-c ssl_key_file=/var/lib/postgresql/server.key");
            postgres.start();
            url = postgres.getJdbcUrl() + "&sslmode=verify-full&sslrootcert=" + pki.resolve("ca.crt");
            appPassword = UUID.randomUUID().toString();
            migrationPassword = UUID.randomUUID().toString();
            try (var connection = DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
                    var statement = connection.createStatement()) {
                statement.execute("REVOKE CREATE ON SCHEMA public FROM PUBLIC");
                statement.execute("CREATE ROLE rental_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD '"
                        + migrationPassword + "'");
                statement.execute("CREATE ROLE rental_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD '"
                        + appPassword + "'");
                statement.execute("CREATE SCHEMA rental AUTHORIZATION rental_migrator");
                statement.execute("REVOKE ALL ON SCHEMA rental FROM PUBLIC");
            }
            flyway().migrate();
            return Map.of("quarkus.datasource.jdbc.url", url,
                    "quarkus.datasource.username", "rental_app",
                    "quarkus.datasource.password", appPassword,
                    "quarkus.http.ssl.certificate.files", pki.resolve("app/server.crt").toString(),
                    "quarkus.http.ssl.certificate.key-files", pki.resolve("app/server.key").toString());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted starting rental test database", failure);
        } catch (Exception failure) {
            if (postgres != null) {
                postgres.stop();
            }
            throw new IllegalStateException("Cannot start rental test database", failure);
        }
    }

    static Flyway flyway() {
        return Flyway.configure().dataSource(url, "rental_migrator", migrationPassword)
                .schemas("rental").defaultSchema("rental").locations("classpath:db/migration")
                .createSchemas(false).cleanDisabled(true).load();
    }

    static PGSimpleDataSource runtimeSource() {
        var source = new PGSimpleDataSource();
        source.setURL(url);
        source.setUser("rental_app");
        source.setPassword(appPassword);
        return source;
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
