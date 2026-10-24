package com.milesofsmiles.fleet;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {
    static final String IMAGE = "postgres:17.6-alpine@sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94";
    static PostgreSQLContainer<?> postgres;
    static String url;
    private Path pki;
    static final String OWNER_PASSWORD = UUID.randomUUID().toString();
    static final String APP_PASSWORD = UUID.randomUUID().toString();

    @Override
    public Map<String, String> start() {
        try {
            Files.createDirectories(Path.of("target"));
            pki = Files.createTempDirectory(Path.of("target"), "fleet-pki-").toAbsolutePath();
            Path root = Path.of("../..").toAbsolutePath().normalize();
            var builder = new ProcessBuilder("bash", root.resolve("scripts/pki.sh").toString())
                    .directory(root.toFile()).inheritIO();
            builder.environment().put("PKI_DIR", pki.toString());
            if (builder.start().waitFor() != 0) {
                throw new IllegalStateException("Test PKI generation failed");
            }
            postgres = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("fleet").withUsername("bootstrap")
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
            try (var connection = admin(); var statement = connection.createStatement()) {
                statement.execute("REVOKE CREATE ON SCHEMA public FROM PUBLIC");
                statement.execute("REVOKE ALL ON DATABASE fleet FROM PUBLIC");
                statement.execute("REVOKE ALL ON SCHEMA public FROM PUBLIC");
                statement.execute("CREATE ROLE fleet_migrator LOGIN PASSWORD '" + OWNER_PASSWORD + "'");
                statement.execute("CREATE ROLE fleet_app LOGIN PASSWORD '" + APP_PASSWORD + "'");
                statement.execute("GRANT CONNECT ON DATABASE fleet TO fleet_migrator, fleet_app");
                statement.execute("CREATE SCHEMA fleet AUTHORIZATION fleet_migrator");
                statement.execute("CREATE EXTENSION btree_gist WITH SCHEMA public");
                statement.execute("GRANT USAGE ON SCHEMA public TO fleet_migrator");
            }
            FleetMigrationMain.migrate(url, "fleet_migrator", OWNER_PASSWORD);
            return Map.of("quarkus.datasource.jdbc.url", url,
                    "quarkus.datasource.username", "fleet_app",
                    "quarkus.datasource.password", APP_PASSWORD);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            stop();
            throw new IllegalStateException("Interrupted during fleet bootstrap", failure);
        } catch (Exception failure) {
            stop();
            throw new IllegalStateException("Fleet test bootstrap failed", failure);
        }
    }

    static Connection admin() throws java.sql.SQLException {
        return DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
    }

    static Connection owner() throws java.sql.SQLException {
        return DriverManager.getConnection(url, "fleet_migrator", OWNER_PASSWORD);
    }

    static PGSimpleDataSource runtimeSource() {
        var source = new PGSimpleDataSource();
        source.setURL(url);
        source.setUser("fleet_app");
        source.setPassword(APP_PASSWORD);
        return source;
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
        if (pki != null && Files.exists(pki)) {
            try (var paths = Files.walk(pki)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            } catch (java.io.IOException failure) {
                throw new IllegalStateException("Cannot remove fleet test PKI", failure);
            }
        }
    }
}
