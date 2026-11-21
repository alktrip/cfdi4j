package io.github.alktrip.cfdi4j.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SatAuthoritiesTest {

    private static final Path SPECS = Path.of("..", "specs", "sat", "ac");

    private static X509Certificate certificate(Path path) throws IOException {
        return Certificates.parse(Files.readAllBytes(path));
    }

    @Test
    void theTestCsdWasIssuedByTheTestAuthoritiesOnly() throws IOException {
        X509Certificate csd = certificate(Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9", "csd.cer"));

        assertThat(SatAuthorities.test().issued(csd)).isTrue();
        assertThat(SatAuthorities.production().issued(csd)).isFalse();
    }

    /** The OCSP responders of the SAT carry certificates issued by AC5 and AC7: real production cases. */
    @Test
    void recognizesCertificatesIssuedByTheProductionAuthorities() throws IOException {
        for (String file : List.of("OCSP7.crt", "ocsp.ac5_sat.cer")) {
            X509Certificate ocsp = certificate(Path.of("src", "test", "resources", "ocsp", file));

            assertThat(SatAuthorities.production().issued(ocsp)).as(file).isTrue();
            assertThat(SatAuthorities.test().issued(ocsp)).as(file).isFalse();
        }
    }

    @Test
    void rejectsCertificatesOfOtherIssuersAndForgedOnes() throws IOException {
        X509Certificate ec = certificate(Path.of("src", "test", "resources", "ec-prueba.cer"));

        assertThat(SatAuthorities.production().issued(ec)).isFalse();
        // An authority certificate is self-issued only for the roots, never for the AC.
        X509Certificate ac7 = certificate(SPECS.resolve("produccion").resolve("AC7.crt"));
        assertThat(SatAuthorities.of(List.of(ac7)).issued(ac7)).isFalse();
    }

    @Test
    void customAuthoritiesCanBeGiven() throws IOException {
        X509Certificate ac7 = certificate(SPECS.resolve("produccion").resolve("AC7.crt"));
        X509Certificate ocsp7 = certificate(Path.of("src", "test", "resources", "ocsp", "OCSP7.crt"));

        assertThat(SatAuthorities.of(List.of(ac7)).issued(ocsp7)).isTrue();
        assertThat(SatAuthorities.of(List.of()).issued(ocsp7)).isFalse();
    }

    /** Every bundled file is loaded, and duplicates (the same certificate in DER and PEM) count once. */
    @Test
    void loadsEveryBundledCertificateOnce() throws IOException {
        for (String dir : List.of("produccion", "pruebas")) {
            List<X509Certificate> distinct;
            try (Stream<Path> files = Files.list(SPECS.resolve(dir))) {
                distinct = files.map(p -> {
                    try {
                        return certificate(p);
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                }).distinct().toList();
            }
            SatAuthorities authorities = dir.equals("produccion") ? SatAuthorities.production() : SatAuthorities.test();

            assertThat(authorities.certificates()).as(dir).containsExactlyInAnyOrderElementsOf(distinct);
        }
    }
}
