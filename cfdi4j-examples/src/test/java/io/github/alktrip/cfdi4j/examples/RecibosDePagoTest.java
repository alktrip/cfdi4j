package io.github.alktrip.cfdi4j.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.Cfdi4j;
import io.github.alktrip.cfdi4j.CfdiEngine;
import io.github.alktrip.cfdi4j.SealedCfdi;
import io.github.alktrip.cfdi4j.VerificationResult;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.crypto.Csd;
import io.github.alktrip.cfdi4j.pagos20.Pagos;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** End to end with the facade: sealing and verifying recibos de pago with the SAT test CSD of EKU9003173C9. */
class RecibosDePagoTest {

    private static final Path CSD_DIR = Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9");
    private static final Path GOLDEN = Path.of("..", "cfdi4j-pagos20", "src", "test", "resources", "golden");
    private static final Csd CSD = Csd.load(CSD_DIR.resolve("csd.cer"), CSD_DIR.resolve("csd.key"), "12345678a".toCharArray());
    private static final CfdiEngine ENGINE = Cfdi4j.configure().clock(Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneOffset.UTC)).build();

    static Stream<String> golden() throws IOException {
        try (Stream<Path> files = Files.list(GOLDEN)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".xml")).map(n -> n.replace(".xml", "")).sorted().toList().stream();
        }
    }

    /** The facade seals each recibo exactly as OpenSSL signed the independently produced cadena. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void sealOfEachReciboIsTheOpenSslSeal(String name) throws IOException {
        Comprobante recibo = CfdiXml.defaults().parse(Files.readAllBytes(GOLDEN.resolve(name + ".xml")));
        String expectedSello = Files.readString(GOLDEN.resolve(name + ".sello"), StandardCharsets.US_ASCII).strip();

        SealedCfdi sealed = ENGINE.seal(recibo, CSD);

        assertThat(sealed.cadenaOriginal()).isEqualTo(Files.readString(GOLDEN.resolve(name + ".cadena.txt"), StandardCharsets.UTF_8));
        assertThat(sealed.sello()).isEqualTo(expectedSello);
        VerificationResult verified = ENGINE.verify(sealed.toBytes());
        assertThat(verified.isValid()).isTrue();
        assertThat(verified.comprobante().orElseThrow().complemento()).singleElement().isInstanceOf(Pagos.class);
    }

    @Test
    void validationOfTheXmlAppliesThePagosRules() throws IOException {
        String xml = Files.readString(GOLDEN.resolve("pago-mxn.xml"), StandardCharsets.UTF_8);
        String sealed = ENGINE.seal(CfdiXml.defaults().parse(xml), CSD).toXml().replace("Monto=\"106.00\"", "Monto=\"105.00\"");

        assertThat(ENGINE.validate(sealed.getBytes(StandardCharsets.UTF_8)).errors()).extracting(v -> v.code()).contains("CRP20217", "CRP20211");
    }
}
