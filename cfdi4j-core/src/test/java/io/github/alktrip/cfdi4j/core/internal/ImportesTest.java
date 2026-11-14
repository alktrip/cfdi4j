package io.github.alktrip.cfdi4j.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Examples taken from the Anexo 20 (DOF 2022-01-13) and the Guía de llenado del CFDI 4.0. */
class ImportesTest {

    @Test
    void conceptImporteExamplesFromTheGuia() {
        // Guía, Concepto/Importe, ejemplo 1: 5 x 1230.00 = 6150.00
        assertThat(Importes.producto(new BigDecimal("5"), new BigDecimal("1230.00"), 2)).hasToString("6150.00");
        // Guía, Concepto/Importe, ejemplo 2: 3.141649 x 1230.00 = 3864.22827
        assertThat(Importes.producto(new BigDecimal("3.141649"), new BigDecimal("1230.00"), 2)).hasToString("3864.22827");
    }

    @Test
    void productWithMoreThanSixDecimalsIsRoundedHalfUpToSix() {
        // 0.333333 x 0.5 = 0.1666665 -> 0.166667
        assertThat(Importes.producto(new BigDecimal("0.333333"), new BigDecimal("0.5"), 2)).hasToString("0.166667");
    }

    @Test
    void productKeepsAtLeastTheCurrencyDecimals() {
        assertThat(Importes.producto(new BigDecimal("100.00"), new BigDecimal("0.160000"), 2)).hasToString("16.00");
        assertThat(Importes.producto(new BigDecimal("600"), new BigDecimal("1"), 2)).hasToString("600.00");
        assertThat(Importes.producto(new BigDecimal("600"), new BigDecimal("1"), 0)).hasToString("600");
    }

    @Test
    void totalsAreRoundedHalfUpAtTheEnd() {
        // Guía, pregunta 37: the four tax amounts add up to 0.7104, which rounds to 0.71
        assertThat(Importes.redondear(new BigDecimal("0.7104"), 2)).hasToString("0.71");
        assertThat(Importes.redondear(new BigDecimal("0.005"), 2)).hasToString("0.01");
        assertThat(Importes.redondear(new BigDecimal("16"), 2)).hasToString("16.00");
    }

    @Test
    void limitsTruncateTheLowerAndRoundUpTheUpper() {
        // Guía, pregunta 41: limits must differ; the upper one is rounded up, not arithmetically.
        var limites = Importes.limitesImporteConcepto(new BigDecimal("3.141649"), new BigDecimal("1230.00"), 5);

        assertThat(limites.inferior()).isLessThan(limites.superior());
        assertThat(limites.contiene(new BigDecimal("3864.22827"))).isTrue();
        // (3.1416485)(1229.995) = 3864.21194675... truncated; (3.1416495-1e-12)(1230.005-1e-12) = 3864.24459324... rounded up
        assertThat(limites.inferior()).hasToString("3864.21194");
        assertThat(limites.superior()).hasToString("3864.24460");
    }

    @Test
    void taxLimitsFollowTheAnexo20Formula() {
        // (100.00 - 0.005) * 0.16 = 15.9992 -> 15.99 ; (100.00 + 0.005 - 1e-12) * 0.16 = 16.0008 -> 16.01
        var limites = Importes.limitesImporteImpuesto(new BigDecimal("100.00"), new BigDecimal("0.160000"), 2);

        assertThat(limites.inferior()).hasToString("15.99");
        assertThat(limites.superior()).hasToString("16.01");
        assertThat(limites.contiene(new BigDecimal("16.00"))).isTrue();
        assertThat(limites.contiene(new BigDecimal("16.02"))).isFalse();
        assertThat(limites.contiene(new BigDecimal("15.98"))).isFalse();
    }

    @Test
    void decimalsAreTheDecimalsAsWritten() {
        assertThat(Importes.decimales(new BigDecimal("1.50"))).isEqualTo(2);
        assertThat(Importes.decimales(new BigDecimal("15"))).isZero();
        assertThat(Importes.decimales(new BigDecimal("1E+2"))).isZero();
    }

    @Test
    void normalizeNeverChangesTheValue() {
        assertThat(Importes.normalizar(new BigDecimal("3864.2282700"), 2)).hasToString("3864.22827");
        assertThat(Importes.normalizar(new BigDecimal("1E+2"), 2)).hasToString("100.00");
    }
}
