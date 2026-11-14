package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Layer-1 golden files (ADR 0005): the expected cadenas were produced by the Saxon-HE command line
 * with the stylesheet fetched from the SAT web site, without cfdi4j (tools/golden/generate.sh).
 */
class CadenaOriginalTest {

    private static final Path GOLDEN = Path.of("src/test/resources/golden/own");

    static Stream<String> golden() throws IOException {
        try (Stream<Path> files = Files.list(GOLDEN)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".xml")).map(n -> n.replace(".xml", "")).sorted().toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void cadenaOriginalMatchesTheIndependentPipeline(String name) throws IOException {
        byte[] xml = Files.readAllBytes(GOLDEN.resolve(name + ".xml"));
        String expected = Files.readString(GOLDEN.resolve(name + ".cadena.txt"), StandardCharsets.UTF_8);
        String expectedSha256 = Files.readString(GOLDEN.resolve(name + ".cadena.sha256")).strip();

        String cadena = CadenaOriginal.defaults().of(xml);

        assertThat(cadena).isEqualTo(expected);
        assertThat(sha256(cadena)).isEqualTo(expectedSha256);
        assertThat(cadena).startsWith("||4.0|").endsWith("||");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void parsingAndWritingAgainGivesTheSameCadena(String name) throws IOException {
        byte[] xml = Files.readAllBytes(GOLDEN.resolve(name + ".xml"));

        assertThat(CadenaOriginal.defaults().of(CfdiXml.defaults().parse(xml))).isEqualTo(CadenaOriginal.defaults().of(xml));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void goldenInputsAreValidAgainstTheSchema(String name) throws IOException {
        ValidationResult result = CfdiSchema.defaults().validateUnsealed(Files.readAllBytes(GOLDEN.resolve(name + ".xml")));

        assertThat(result.errors()).isEmpty();
    }

    @Test
    void writesNothingToStandardError() {
        PrintStream original = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            CadenaOriginal.compile().of(Fixtures.simple());
        } finally {
            System.setErr(original);
        }
        assertThat(captured.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void compilesAndRunsWithoutAnyNetworkAccess() {
        List<URI> attempts = new CopyOnWriteArrayList<>();
        ProxySelector original = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                attempts.add(uri);
                throw new IllegalStateException("network access attempted: " + uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                attempts.add(uri);
            }
        });
        try {
            assertThat(CadenaOriginal.compile().of(Fixtures.complete())).startsWith("||4.0|");
        } finally {
            ProxySelector.setDefault(original);
        }
        assertThat(attempts).isEmpty();
    }

    @Test
    void isThreadSafe() {
        byte[] xml = Fixtures.bytes(Fixtures.complete());
        String expected = CadenaOriginal.defaults().of(xml);

        List<String> results = IntStream.range(0, 64).parallel().mapToObj(i -> CadenaOriginal.defaults().of(xml)).toList();

        assertThat(results).allMatch(expected::equals);
    }

    @Test
    void rejectsMalformedInputAndDoctypes() {
        assertThatThrownBy(() -> CadenaOriginal.defaults().of("<cfdi:Comprobante".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(CfdiParseException.class).hasMessageContaining("cannot produce the cadena original");
        assertThatThrownBy(() -> CadenaOriginal.defaults().of(("<?xml version=\"1.0\"?><!DOCTYPE c [<!ENTITY x SYSTEM "
                + "\"http://www.sat.gob.mx/x\">]><c>&x;</c>").getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(CfdiParseException.class).hasMessageContaining("DOCTYPE");
    }

    @Test
    void aDocumentWithoutComprobanteGivesTheEmptyCadena() {
        assertThat(CadenaOriginal.defaults().of("<otro/>".getBytes(StandardCharsets.UTF_8))).isEqualTo("|||");
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
