package io.github.alktrip.cfdi4j.pagos20;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.validation.MatrizErrores;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.pagos20.internal.Pagos20RuleSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Generates {@code docs/rules-coverage-pagos20.md} from the rules of the complement and fails if
 * the committed file is out of date (ADR 0004). Regenerate with {@code -Drules.coverage.write=true}.
 */
class RulesCoverageReportTest {

    private static final Path REPORT = Path.of("..", "docs", "rules-coverage-pagos20.md");

    static String report() {
        Map<String, Rule> byCode = new Pagos20RuleSet().rules().stream().collect(Collectors.toMap(Rule::code, Function.identity()));
        StringBuilder out = new StringBuilder();
        out.append("# Cobertura de la matriz de errores del complemento de pagos 2.0\n\n")
                .append("<!-- Generado por RulesCoverageReportTest (cfdi4j-pagos20). No editar a mano. -->\n\n")
                .append("Fuente: `").append(MatrizErroresPagos20.SOURCE_FILE).append("` (SHA-256 `").append(MatrizErroresPagos20.SHA256)
                .append("`), ver `specs/SOURCES.md`.\n\n")
                .append("Las reglas se aplican solo a comprobantes con el complemento `pago20:Pagos`. Las que requieren un servicio del ")
                .append("SAT se reportan como **no verificadas** (advertencia) sin el puerto correspondiente (ADR 0004).\n\n")
                .append("| Estado | Códigos |\n|---|---|\n");
        for (RuleStatus status : RuleStatus.values()) {
            out.append("| ").append(status.etiqueta()).append(" | ")
                    .append(MatrizErroresPagos20.entradas().stream().filter(e -> byCode.get(e.codigo()).status() == status).count()).append(" |\n");
        }
        out.append("| **total** | ").append(MatrizErroresPagos20.entradas().size()).append(" |\n\n")
                .append("## Códigos oficiales\n\n| Código | Elemento | Atributo | Estado | Nota |\n|---|---|---|---|---|\n");
        for (MatrizErrores.Entrada e : MatrizErroresPagos20.entradas()) {
            Rule r = byCode.get(e.codigo());
            out.append("| ").append(e.codigo()).append(" | ").append(cell(e.elemento())).append(" | ").append(cell(e.atributo()))
                    .append(" | ").append(r.status().etiqueta()).append(" | ").append(cell(r.nota())).append(" |\n");
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
        assertThat(Files.exists(REPORT)).as("docs/rules-coverage-pagos20.md exists; run with -Drules.coverage.write=true").isTrue();
        assertThat(Files.readString(REPORT, StandardCharsets.UTF_8)).as("docs/rules-coverage-pagos20.md is up to date").isEqualTo(expected);
    }

    @Test
    void matrixVersionMatchesTheFileRecordedInSources() throws IOException {
        String sources = Files.readString(Path.of("..", "specs", "SOURCES.md"));

        assertThat(sources).contains("`" + MatrizErroresPagos20.SOURCE_FILE + "`").contains("`" + MatrizErroresPagos20.SHA256 + "`");
    }
}
