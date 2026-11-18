package io.github.alktrip.cfdi4j.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import io.github.alktrip.cfdi4j.Cfdi4j;
import io.github.alktrip.cfdi4j.core.validation.Cfdi40Validator;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Untrusted documents must never make {@code validate} or {@code verify} throw or hang: each
 * attribute of a sealed factura and recibo is replaced by hostile values or removed. Found by
 * mutation fuzzing; the cases that broke are kept here (exponents, a zero EquivalenciaDR, an
 * unknown encoding, a certificate with an EC key).
 */
class HostileInputTest {

    private static final Path CSD_DIR = Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9");
    private static final List<String> VALORES = List.of("", "-1", "0", "1E999999999", "1e-999999999", "abc", "9".repeat(2000));
    private static final Pattern ATRIBUTO = Pattern.compile("([A-Za-z]+)=\"([^\"]*)\"");

    static Stream<String> documentos() {
        return Stream.of("factura", "recibo");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentos")
    void validateAndVerifyNeverThrow(String nombre) throws IOException {
        String xml = nombre.equals("factura") ? QuickStart.run(CSD_DIR) : ReciboDePago.run(CSD_DIR).toXml();
        List<String> variantes = variantes(xml);

        assertTimeoutPreemptively(Duration.ofMinutes(3), () -> {
            for (String variante : variantes) {
                byte[] bytes = variante.getBytes(StandardCharsets.UTF_8);
                assertThatCode(() -> {
                    assertThat(Cfdi4j.validate(bytes).errors()).extracting(Violation::code).doesNotContain(Cfdi40Validator.RULE_FAILED);
                    Cfdi4j.verify(bytes);
                }).as("variant %s", variante.length() > 300 ? variante.substring(0, 300) : variante).doesNotThrowAnyException();
            }
        });
    }

    private static List<String> variantes(String xml) throws IOException {
        String ec = Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of("src", "test", "resources", "ec-prueba.cer")));
        List<String> result = new ArrayList<>();
        Matcher m = ATRIBUTO.matcher(xml);
        while (m.find()) {
            String nombre = m.group(1);
            if (nombre.equals("schemaLocation")) {
                continue;
            }
            String antes = xml.substring(0, m.start(2));
            String despues = xml.substring(m.end(2));
            for (String valor : VALORES) {
                result.add(antes + valor + despues);
            }
            if (nombre.equals("Certificado")) {
                result.add(antes + ec + despues);
            }
            if (nombre.equals("encoding")) {
                result.add(antes + "no-existe" + despues);
            }
            result.add(xml.substring(0, m.start()) + xml.substring(m.end()));
        }
        return result;
    }
}
