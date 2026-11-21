package io.github.alktrip.cfdi4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Messages built from untrusted documents, and errors that cross process boundaries. */
class MessageSafetyTest {

    @Test
    void aValueWithLineBreaksCannotForgeLogLines() {
        String sealed = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).toXml()
                .replace("ClaveUnidad=\"ACT\"", "ClaveUnidad=\"ACT&#10;2026-11-20 INFO pago autorizado&#10;\"");

        ValidationResult result = Cfdi4j.validate(sealed.getBytes(StandardCharsets.UTF_8));

        assertThat(result.errors()).isNotEmpty().allSatisfy(v -> {
            assertThat(v.message()).doesNotContain("\n").doesNotContain("\r");
            assertThat(v.message().length()).isLessThan(1500);
        });
        assertThat(result.errors()).anySatisfy(v -> assertThat(v.message()).contains("ACT\\n2026-11-20 INFO"));
    }

    /** A document read into a String is verified as decoded, whatever encoding its declaration names. */
    @Test
    void verifyingTextIgnoresTheEncodingOfItsDeclaration() throws IOException {
        var comprobante = io.github.alktrip.cfdi4j.xml.CfdiXml.defaults()
                .parse(java.nio.file.Files.readAllBytes(Fixtures.GOLDEN.resolve("espacios-y-caracteres.xml")));
        String utf8 = Cfdi4j.seal(comprobante, Fixtures.CSD).toXml();
        String iso = utf8.replaceFirst("encoding=\"UTF-8\"", "encoding=\"ISO-8859-1\"");
        byte[] isoBytes = iso.getBytes(StandardCharsets.ISO_8859_1);

        assertThat(utf8).contains("ñandú");
        assertThat(Cfdi4j.verify(isoBytes).isValid()).isTrue();
        assertThat(Cfdi4j.verify(new String(isoBytes, StandardCharsets.ISO_8859_1)).isValid()).isTrue();
        assertThat(Cfdi4j.verify(utf8).isValid()).isTrue();
        assertThat(Cfdi4j.verify(utf8.replace("ñandú", "nandu")).failures()).extracting(v -> v.code()).containsExactly(CfdiEngine.SELLO_INVALIDO);
    }

    @Test
    void invalidCfdiExceptionCanBeSerialized() throws IOException, ClassNotFoundException {
        InvalidCfdiException error = new InvalidCfdiException(Cfdi4j.validate(Fixtures.simple()));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(error);
        }

        InvalidCfdiException copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (InvalidCfdiException) in.readObject();
        }

        assertThat(copy.result()).isEqualTo(error.result());
        assertThat(copy.getMessage()).isEqualTo(error.getMessage());
    }
}
