package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Layer 3 of ADR 0005: the CFDI 4.0 examples of CryptoSys FirmaSAT, with the cadena original that
 * CryptoSys computed with the SAT stylesheets. The files are all rights reserved, so they are
 * downloaded at test time, checked against the SHA-256 in {@code external/cryptosys-cfdv40.txt}
 * and never committed. Runs only with {@code -Pexternal-vectors}.
 *
 * <p>CryptoSys publishes each XML inside an HTML page ({@code <pre>}); the test takes the text of
 * that element. The examples use fictitious data and are not all valid CFDI; only the cadena
 * original is compared.
 */
@Tag("external")
class CryptoSysVectorsTest {

    private static final String BASE = "https://cryptosys.net/firmasat/";

    record Vector(String name, String htmlSha256, String cadenaSha256) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Vector> vectors() throws IOException {
        try (InputStream in = CryptoSysVectorsTest.class.getResourceAsStream("/external/cryptosys-cfdv40.txt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().filter(l -> !l.startsWith("#") && !l.isBlank())
                    .map(l -> l.split(" ")).map(f -> new Vector(f[0], f[1], f[2])).toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vectors")
    void cadenaOriginalMatchesCryptoSys(Vector v) {
        String html = new String(download(BASE + v.name() + ".xml.html", v.htmlSha256()), StandardCharsets.UTF_8);
        String expected = new String(download(BASE + "docs/cfdv40/" + v.name() + "-cadena.txt", v.cadenaSha256()), StandardCharsets.UTF_8).strip();
        int start = html.indexOf('>', html.indexOf("<pre")) + 1;
        String xml = html.substring(start, html.indexOf("</pre>", start))
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&").strip();

        assertThat(CadenaOriginal.defaults().of(xml.getBytes(StandardCharsets.UTF_8))).isEqualTo(expected);
    }

    private static byte[] download(String url, String sha256) {
        try {
            Path cache = Path.of("target", "external-vectors", "cryptosys", Path.of(URI.create(url).getPath()).getFileName().toString());
            if (!Files.isRegularFile(cache)) {
                Files.createDirectories(cache.getParent());
                try (InputStream in = URI.create(url).toURL().openStream()) {
                    Files.write(cache, in.readAllBytes());
                }
            }
            byte[] content = Files.readAllBytes(cache);
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))).as("SHA-256 of %s", url).isEqualTo(sha256);
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
