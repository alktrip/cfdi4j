package io.github.alktrip.cfdi4j.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Layer-1 golden files (ADR 0005): the expected sellos were produced by OpenSSL
 * ({@code openssl dgst -sha256 -sign}) with the SAT test key decrypted by OpenSSL
 * (tools/golden/generate.sh). They live with the cadenas in cfdi4j-xml.
 */
class SealsTest {

    private static final Path GOLDEN = Path.of("..", "cfdi4j-xml", "src", "test", "resources", "golden", "own");
    private static final Csd CSD = CsdTest.load();

    static Stream<String> golden() throws IOException {
        try (Stream<Path> files = Files.list(GOLDEN)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".sello")).map(n -> n.replace(".sello", "")).sorted().toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void selloIsIdenticalToOpenSsl(String name) throws IOException {
        String cadena = Files.readString(GOLDEN.resolve(name + ".cadena.txt"), StandardCharsets.UTF_8);
        String expected = Files.readString(GOLDEN.resolve(name + ".sello"), StandardCharsets.US_ASCII).strip();

        assertThat(CSD.sign(cadena)).isEqualTo(expected);
        assertThat(Seals.verify(cadena, expected, CSD.certificate())).isTrue();
    }

    @Test
    void anyChangeToTheCadenaBreaksTheSeal() throws IOException {
        String cadena = Files.readString(GOLDEN.resolve("ingreso-basico.cadena.txt"), StandardCharsets.UTF_8);
        String sello = CSD.sign(cadena);

        assertThat(Seals.verify(cadena.replace("|116.00|", "|117.00|"), sello, CSD.certificate())).isFalse();
        assertThat(Seals.verify(cadena + " ", sello, CSD.certificate())).isFalse();
    }

    @Test
    void sealOfAnotherCertificateDoesNotVerify() throws IOException {
        var fiel = Certificates.parse(Files.readAllBytes(CsdTest.DIR.resolve("fiel.cer")));

        assertThat(Seals.verify("||4.0||", CSD.sign("||4.0||"), fiel)).isFalse();
    }

    @Test
    void malformedSealsDoNotVerify() {
        assertThat(Seals.verify("||4.0||", "no es base64!", CSD.certificate())).isFalse();
        assertThat(Seals.verify("||4.0||", "AAAA", CSD.certificate())).isFalse();
        assertThat(Seals.verify("||4.0||", "", CSD.certificate())).isFalse();
    }

    @Test
    void verifiesMimeWrappedSeals() {
        String sello = CSD.sign("||4.0||");
        String wrapped = sello.substring(0, 76) + "\r\n" + sello.substring(76);

        assertThat(Seals.verify("||4.0||", wrapped, CSD.certificate())).isTrue();
    }
}
