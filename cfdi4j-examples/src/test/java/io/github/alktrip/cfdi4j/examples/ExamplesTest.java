package io.github.alktrip.cfdi4j.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.Cfdi4j;
import io.github.alktrip.cfdi4j.SealedCfdi;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExamplesTest {

    private static final Path CSD_DIR = Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9");
    private static final Path QUICK_START = Path.of("src/main/java/io/github/alktrip/cfdi4j/examples/QuickStart.java");

    @Test
    void quickStartProducesAValidSealedCfdi() {
        String xml = QuickStart.run(CSD_DIR);

        assertThat(Cfdi4j.verify(xml).isValid()).isTrue();
        assertThat(Cfdi4j.validate(xml.getBytes(StandardCharsets.UTF_8)).errors()).isEmpty();
    }

    @Test
    void reciboDePagoIsValid() {
        SealedCfdi recibo = ReciboDePago.run(CSD_DIR);

        assertThat(Cfdi4j.verify(recibo.toBytes()).isValid()).isTrue();
        assertThat(Cfdi4j.validate(recibo.toBytes()).errors()).isEmpty();
        assertThat(recibo.toXml()).contains("TotalTrasladosBaseIVA16=\"300000.00\"");
    }

    @Test
    void validarXmlReportsTampering(@TempDir Path dir) throws IOException {
        Path ok = dir.resolve("ok.xml");
        Path tampered = dir.resolve("tampered.xml");
        String xml = QuickStart.run(CSD_DIR);
        Files.writeString(ok, xml, StandardCharsets.UTF_8);
        Files.writeString(tampered, xml.replace("Descripcion=\"Servicio\"", "Descripcion=\"Servicios\""), StandardCharsets.UTF_8);

        assertThat(ValidarXml.run(ok)).isTrue();
        assertThat(ValidarXml.run(tampered)).isFalse();
        assertThat(Cfdi4j.verify(Files.readAllBytes(tampered)).failures()).extracting(Violation::code).containsExactly("CFDI40102");
    }

    /** The README shows exactly the code that this test runs. */
    @ParameterizedTest
    @ValueSource(strings = {"README.md", "README.es.md"})
    void readmeQuickStartIsTheExampleCode(String readme) throws IOException {
        List<String> source = Files.readAllLines(QUICK_START, StandardCharsets.UTF_8);
        List<String> snippet = source.subList(source.indexOf("        // tag::quickstart[]") + 1, source.indexOf("        // end::quickstart[]"));
        String expected = snippet.stream().map(l -> l.length() >= 8 ? l.substring(8) : l.strip()).collect(Collectors.joining("\n"));

        String text = Files.readString(Path.of("..", readme), StandardCharsets.UTF_8);
        int start = text.indexOf("```java\n", text.indexOf("<!-- quickstart -->")) + "```java\n".length();
        String block = text.substring(start, text.indexOf("\n```", start));

        assertThat(block).isEqualTo(expected);
        assertThat(Arrays.stream(block.split("\n")).filter(l -> !l.isBlank()).count()).isLessThanOrEqualTo(14);
    }
}
