package com.milesofsmiles.booking;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {
    static final String IMAGE = "postgres:17.6-alpine@sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94";
    static PostgreSQLContainer<?> postgres;
    static Path pki;

    @Override
    public Map<String, String> start() {
        try {
            Path directory = Files.createTempDirectory(Path.of("target"), "test-pki-");
            pki = directory.toAbsolutePath();
            var builder = new ProcessBuilder("bash", "scripts/pki.sh").inheritIO();
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
        String ca = pki.resolve("ca.crt").toString();
        return Map.of(
                "quarkus.datasource.jdbc.url", postgres.getJdbcUrl() + "&sslmode=verify-full&sslrootcert=" + ca,
                "quarkus.datasource.username", postgres.getUsername(),
                "quarkus.datasource.password", postgres.getPassword(),
                "quarkus.http.ssl.certificate.files", pki.resolve("app/server.crt").toString(),
                "quarkus.http.ssl.certificate.key-files", pki.resolve("app/server.key").toString());
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
