package ar.com.eldars.client1;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
public class ServiceRegistryConfig {
    @Value("${server.base-url}")
    private String baseUrl;
    @Value("${server.endpoint}")
    private String endpoint;

    @Bean
    WebClient configureWebClient(@Value("${server.ssl.trust-store}") String trustStorePath,
                                 @Value("${server.ssl.trust-store-password}") String trustStorePass,
                                 @Value("${server.ssl.key-store}") String keyStorePath,
                                 @Value("${server.ssl.key-store-password}") String keyStorePass,
                                 @Value("${server.ssl.key-alias}") String keyAlias) {
        SslContext sslContext;
        final PrivateKey privateKey;
        final X509Certificate[] certificates;
        try{
            final KeyStore trustStore;
            final KeyStore keyStore;
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(new FileInputStream(ResourceUtils.getFile(trustStorePath)), trustStorePass.toCharArray());
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(ResourceUtils.getFile(keyStorePath)), keyStorePass.toCharArray());

            List<Certificate> certificateList = Collections.list(trustStore.aliases()).stream()
                    .filter( t -> {
                        try {
                            return trustStore.isCertificateEntry(t);
                        } catch (KeyStoreException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(t -> {
                        try {
                            return trustStore.getCertificate(t);
                        } catch (KeyStoreException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            certificates = certificateList.toArray(new X509Certificate[certificateList.size()]);
            privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePass.toCharArray());
            Certificate[] certChain = keyStore.getCertificateChain(keyAlias);
            X509Certificate[] x509CertificateChain = Arrays.stream(certChain)
                    .map(certificate -> (X509Certificate) certificate)
                    .toArray(X509Certificate[]::new);
            X509Certificate certificate = x509CertificateChain[0];
            this.validateCertificate(certificate);
            sslContext = SslContextBuilder.forClient()
                    .keyManager(privateKey, keyStorePass, x509CertificateChain)
                    .trustManager(certificates)
                    .build();
            HttpClient httpClient = HttpClient.create().secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            //return webClientConfiguration(httpClient);
            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (KeyStoreException | UnrecoverableKeyException | CertificateException | IOException |
                 NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean validateCertificate(X509Certificate certificate) {
        var certificateExpirationDate = certificate.getNotAfter().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        var certificateStartDate = certificate.getNotBefore().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        if (LocalDate.now().isBefore(certificateStartDate)){
            throw new RuntimeException("Certificate is not valid yet");
        }
        var subject = Arrays.stream(certificate.getSubjectDN().getName().split(","))
                .map(i -> i.split("="))
                .collect(Collectors.toMap(element -> element[0].trim(), element -> element[1].trim()));
        if (!subject.get("CN").equalsIgnoreCase("Eldar")){
            throw new RuntimeException("Certificate is not valid for Eldar");
        }
        return true;
    }

    private WebClient webClientConfiguration(HttpClient httpClient) {
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        var webClient = WebClient.builder()
                .clientConnector(connector)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        var response = webClient.get().uri(endpoint).retrieve().bodyToMono(String.class).block();
        assert Objects.requireNonNull(response).equals("Server is up and running, TLS is working fine!");
        System.out.println(response);
        return webClient;
    }
}
