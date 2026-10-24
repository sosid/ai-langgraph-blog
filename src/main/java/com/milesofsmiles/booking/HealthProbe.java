package com.milesofsmiles.booking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public final class HealthProbe {
    private HealthProbe() {
    }

    public static SSLContext trust(Path certificate) throws Exception {
        var store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null);
        try (var input = Files.newInputStream(certificate)) {
            store.setCertificateEntry("development-ca", CertificateFactory.getInstance("X.509")
                    .generateCertificate(input));
        }
        var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        managers.init(store);
        var context = SSLContext.getInstance("TLS");
        context.init(null, managers.getTrustManagers(), null);
        return context;
    }

    public static void main(String[] args) throws Exception {
        var client = HttpClient.newBuilder().sslContext(trust(Path.of(args[1])))
                .connectTimeout(Duration.ofSeconds(2)).build();
        var request = HttpRequest.newBuilder(URI.create(args[0])).timeout(Duration.ofSeconds(4)).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Health endpoint returned " + response.statusCode());
        }
    }
}
