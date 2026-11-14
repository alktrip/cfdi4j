package io.github.alktrip.cfdi4j.tools.catalogs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NameTableTest {

    @TempDir
    Path dir;

    private static final Map<String, String> KEYS = keys("01", "Efectivo", "02", "Cheque nominativo");

    @Test
    void curatedNames() throws IOException {
        NameTable table = load("c_X:\n  \"01\": EFECTIVO\n  \"02\": CHEQUE\n");

        assertThat(table.namesFor("c_X", KEYS)).containsExactly(Map.entry("01", "EFECTIVO"), Map.entry("02", "CHEQUE"));
    }

    @Test
    void keyAndKeyUpperModes() throws IOException {
        NameTable table = load("c_A: key\nc_B: key-upper\n");

        assertThat(table.namesFor("c_A", keys("MXN", "Peso"))).containsEntry("MXN", "MXN");
        assertThat(table.namesFor("c_B", keys("Tasa", "Tasa"))).containsEntry("Tasa", "TASA");
        table.checkAllUsed();
    }

    @Test
    void newKeyWithoutNameStopsTheGenerator() throws IOException {
        NameTable table = load("c_X:\n  \"01\": EFECTIVO\n");

        assertThatThrownBy(() -> table.namesFor("c_X", KEYS))
                .isInstanceOf(GeneratorException.class)
                .hasMessageContaining("no name for new key '02' (Cheque nominativo)");
    }

    @Test
    void nameForAKeyThatIsNotInTheCatalogStopsTheGenerator() throws IOException {
        NameTable table = load("c_X:\n  \"01\": EFECTIVO\n  \"02\": CHEQUE\n  \"03\": OTRO\n");

        assertThatThrownBy(() -> table.namesFor("c_X", KEYS)).hasMessageContaining("'03', which is not in the catalog");
    }

    @Test
    void unquotedNumericKeyIsRejected() throws IOException {
        NameTable table = load("c_X:\n  01: EFECTIVO\n  \"02\": CHEQUE\n");

        assertThatThrownBy(() -> table.namesFor("c_X", KEYS)).hasMessageContaining("must be quoted");
    }

    @Test
    void invalidOrDuplicatedNamesAreRejected() throws IOException {
        assertThatThrownBy(() -> load("c_X:\n  \"01\": efectivo\n  \"02\": CHEQUE\n").namesFor("c_X", KEYS))
                .hasMessageContaining("is not a constant name");
        assertThatThrownBy(() -> load("c_X:\n  \"01\": SAME\n  \"02\": SAME\n").namesFor("c_X", KEYS))
                .hasMessageContaining("used twice");
    }

    @Test
    void missingAndUnusedCatalogsAreReported() throws IOException {
        NameTable table = load("c_X: key\n");

        assertThatThrownBy(() -> table.namesFor("c_Y", KEYS)).hasMessageContaining("no entry for c_Y");
        assertThatThrownBy(table::checkAllUsed).hasMessageContaining("c_X");
    }

    private NameTable load(String yaml) throws IOException {
        Path file = dir.resolve("names.yaml");
        Files.writeString(file, yaml);
        return NameTable.load(file);
    }

    private static Map<String, String> keys(String... pairs) {
        Map<String, String> keys = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            keys.put(pairs[i], pairs[i + 1]);
        }
        return keys;
    }
}
