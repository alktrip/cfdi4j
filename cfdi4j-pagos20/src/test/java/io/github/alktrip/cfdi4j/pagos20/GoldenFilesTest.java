package io.github.alktrip.cfdi4j.pagos20;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.xml.CadenaOriginal;
import io.github.alktrip.cfdi4j.xml.CfdiSchema;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Layer-1 golden files of recibos de pago (ADR 0005): the expected cadena was produced without
 * cfdi4j, by the Saxon-HE command line with the stylesheet fetched from the SAT web site
 * (tools/golden/generate.sh).
 */
class GoldenFilesTest {

    static final Path GOLDEN = Path.of("src/test/resources/golden");

    static Stream<String> golden() throws IOException {
        try (Stream<Path> files = Files.list(GOLDEN)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".xml")).map(n -> n.replace(".xml", "")).sorted().toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void cadenaOriginalMatchesTheIndependentPipeline(String name) throws IOException, NoSuchAlgorithmException {
        byte[] xml = Files.readAllBytes(GOLDEN.resolve(name + ".xml"));
        String expected = Files.readString(GOLDEN.resolve(name + ".cadena.txt"), StandardCharsets.UTF_8);
        String sha256 = Files.readString(GOLDEN.resolve(name + ".cadena.sha256")).strip();

        String cadena = CadenaOriginal.defaults().of(xml);

        assertThat(cadena).isEqualTo(expected).contains("|2.0|");
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cadena.getBytes(StandardCharsets.UTF_8)))).isEqualTo(sha256);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void parsesIntoTheModelAndWritesTheSameBytes(String name) throws IOException {
        byte[] xml = Files.readAllBytes(GOLDEN.resolve(name + ".xml"));

        Comprobante c = CfdiXml.defaults().parse(xml);

        assertThat(c.complemento()).singleElement().isInstanceOf(Pagos.class);
        assertThat(CfdiXml.defaults().toBytes(c)).isEqualTo(xml);
        assertThat(CadenaOriginal.defaults().of(c)).isEqualTo(CadenaOriginal.defaults().of(xml));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void goldenInputsPassTheSchemaAndTheRules(String name) throws IOException {
        byte[] xml = Files.readAllBytes(GOLDEN.resolve(name + ".xml"));

        assertThat(CfdiSchema.defaults().validateUnsealed(xml).errors()).isEmpty();
        assertThat(PagosFixtures.validator().validate(CfdiXml.defaults().parse(xml)).errors()).isEmpty();
    }
}
