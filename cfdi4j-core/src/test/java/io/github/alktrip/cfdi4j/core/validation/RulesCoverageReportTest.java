package io.github.alktrip.cfdi4j.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Generates {@code docs/rules-coverage.md} from {@link Cfdi40Rules} and fails if the committed
 * file is out of date (ADR 0004). Regenerate with {@code -Drules.coverage.write=true}.
 */
class RulesCoverageReportTest {

    private static final Path REPORT = Path.of("..", "docs", "rules-coverage.md");

    /** cfdi4j codes reported by the other modules. */
    private static final List<String[]> OTROS_CODIGOS = List.of(
            new String[] {"CFDI4J-001", "cfdi4j-xml", "El XML no cumple con el esquema XSD del SAT."},
            new String[] {"CFDI4J-002", "cfdi4j-xml", "Complemento sin esquema incluido: no verificado contra XSD (advertencia)."},
            new String[] {"CFDI4J-003", "cfdi4j-xml", "El documento no es XML bien formado o declara DOCTYPE."},
            new String[] {"CFDI4J-101", "cfdi4j", "El documento no se puede leer como CFDI 4.0 (verificación)."},
            new String[] {"CFDI4J-102", "cfdi4j", "Falta Sello, NoCertificado o Certificado (verificación)."},
            new String[] {"CFDI4J-103", "cfdi4j-core, cfdi4j", "NoCertificado no es el número del certificado."},
            new String[] {"CFDI4J-104", "cfdi4j", "SelloCFD del TimbreFiscalDigital no es el Sello (verificación)."},
            new String[] {"CFDI4J-105", "cfdi4j", "SelloSAT no verificado sin el certificado del SAT (advertencia)."},
            new String[] {"CFDI4J-106", "cfdi4j", "SelloSAT no corresponde al timbre (verificación)."},
            new String[] {"CFDI4J-107", "cfdi4j-core", "Una regla no se pudo evaluar con el documento (error; nunca se da por aprobada)."});

    static String report() {
        List<Rule> rules = Cfdi40Rules.all();
        Map<String, Rule> byCode = rules.stream().collect(Collectors.toMap(Rule::code, Function.identity()));
        StringBuilder out = new StringBuilder();
        out.append("# Cobertura de la matriz de errores CFDI 4.0\n\n")
                .append("<!-- Generado por RulesCoverageReportTest a partir de Cfdi40Rules. No editar a mano. -->\n\n")
                .append("Fuente: `").append(MatrizErrores.SOURCE_FILE).append("` (SHA-256 `").append(MatrizErrores.SHA256)
                .append("`), ver `specs/SOURCES.md`.\n\n")
                .append("Las reglas que requieren un catálogo grande, la lista de RFC del SAT o el estado de un certificado se ")
                .append("validan con los puertos `CatalogProvider`, `TaxpayerRegistry` y `CertificateStatusProvider`; sin ellos ")
                .append("se reportan como **no verificadas** (advertencia), nunca como aprobadas (ADR 0004).\n\n")
                .append("| Estado | Códigos |\n|---|---|\n");
        for (RuleStatus status : RuleStatus.values()) {
            out.append("| ").append(status.etiqueta()).append(" | ")
                    .append(MatrizErrores.entradas().stream().filter(e -> byCode.get(e.codigo()).status() == status).count()).append(" |\n");
        }
        out.append("| **total** | ").append(MatrizErrores.entradas().size()).append(" |\n\n")
                .append("## Códigos oficiales\n\n| Código | Elemento | Atributo | Estado | Nota |\n|---|---|---|---|---|\n");
        for (MatrizErrores.Entrada e : MatrizErrores.entradas()) {
            Rule r = byCode.get(e.codigo());
            out.append("| ").append(e.codigo()).append(" | ").append(cell(e.elemento())).append(" | ").append(cell(e.atributo()))
                    .append(" | ").append(r.status().etiqueta()).append(" | ").append(cell(r.nota())).append(" |\n");
        }
        out.append("\n## Códigos de cfdi4j\n\n| Código | Módulo | Descripción |\n|---|---|---|\n");
        for (String[] c : OTROS_CODIGOS) {
            out.append("| ").append(String.join(" | ", Arrays.asList(c))).append(" |\n");
        }
        return out.toString();
    }

    private static String cell(String text) {
        return text.replace("|", "\\|").replace("\n", " ").strip();
    }

    @Test
    void committedReportIsUpToDate() throws IOException {
        String expected = report();
        if (Boolean.getBoolean("rules.coverage.write")) {
            Files.writeString(REPORT, expected, StandardCharsets.UTF_8);
        }
        assertThat(Files.exists(REPORT)).as("docs/rules-coverage.md exists; run with -Drules.coverage.write=true").isTrue();
        assertThat(Files.readString(REPORT, StandardCharsets.UTF_8)).as("docs/rules-coverage.md is up to date").isEqualTo(expected);
    }

    @Test
    void matrixVersionMatchesTheFileRecordedInSources() throws IOException {
        String sources = Files.readString(Path.of("..", "specs", "SOURCES.md"));

        assertThat(sources).contains("`" + MatrizErrores.SOURCE_FILE + "`").contains("`" + MatrizErrores.SHA256 + "`");
    }

    @Test
    void everyCfdi4jCodeIsDocumented() {
        assertThat(OTROS_CODIGOS).extracting(c -> c[0]).contains("CFDI4J-103");
        assertThat(Cfdi40Rules.all()).filteredOn(r -> MatrizErrores.of(r.code()).isEmpty())
                .allSatisfy(r -> assertThat(OTROS_CODIGOS).extracting(c -> c[0]).contains(r.code()));
    }
}
