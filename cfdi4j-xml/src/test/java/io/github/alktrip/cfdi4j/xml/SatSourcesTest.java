package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checks that the bundled official SAT files are exactly the ones recorded in
 * {@code specs/SOURCES.md} (ADR 0007).
 */
class SatSourcesTest {

    private static final Path SPECS = Path.of("..", "specs");

    /** Matches the "Bundled" table rows: {@code | [`sat/...`](url) | ... | `sha256` |}. */
    private static final Pattern BUNDLED_ROW =
            Pattern.compile("^\\| \\[?`(sat/[^`]+)`.*\\| `([0-9a-f]{64})` \\|$");

    @Test
    void everyBundledFileIsListedInSourcesWithItsHash() throws IOException {
        Map<String, String> listed = listedHashes();

        Map<String, String> actual;
        try (Stream<Path> files = Files.walk(SPECS.resolve("sat"))) {
            actual = files.filter(Files::isRegularFile)
                    .collect(Collectors.toMap(
                            f -> SPECS.relativize(f).toString().replace('\\', '/'),
                            SatSourcesTest::sha256OfFile,
                            (a, b) -> a,
                            TreeMap::new));
        }

        assertThat(actual).isEqualTo(listed);
    }

    /**
     * Reads through the module, as the production resolvers will: directories such as
     * {@code .../cfd/CartaPorte/} form valid package names, so in a named module those resources
     * are encapsulated and invisible to {@code ClassLoader.getResourceAsStream}.
     */
    @Test
    void bundledResourcesInTheJarMatchTheRecordedHashes() throws IOException {
        Map<String, String> xmlEntries = new TreeMap<>(listedHashes());
        xmlEntries.keySet().removeIf(path -> !path.startsWith("sat/xml/"));

        assertThat(xmlEntries).hasSize(41);
        for (Map.Entry<String, String> entry : xmlEntries.entrySet()) {
            try (InputStream in = SatSourcesTest.class.getModule().getResourceAsStream(entry.getKey())) {
                assertThat(in).as("resource %s", entry.getKey()).isNotNull();
                assertThat(sha256(in.readAllBytes())).as("hash of %s", entry.getKey()).isEqualTo(entry.getValue());
            }
        }
    }

    private static Map<String, String> listedHashes() throws IOException {
        Map<String, String> listed = new TreeMap<>();
        for (String line : Files.readAllLines(SPECS.resolve("SOURCES.md"))) {
            var matcher = BUNDLED_ROW.matcher(line);
            if (matcher.matches()) {
                listed.put(matcher.group(1), matcher.group(2));
            }
        }
        return listed;
    }

    private static String sha256OfFile(Path file) {
        try {
            return sha256(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
