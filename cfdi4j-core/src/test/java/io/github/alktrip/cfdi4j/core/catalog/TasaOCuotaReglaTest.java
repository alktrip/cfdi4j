package io.github.alktrip.cfdi4j.core.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TasaOCuotaReglaTest {

    private static final Vigencia VIGENCIA = Vigencia.desde(LocalDate.of(2022, 1, 1));

    @Test
    void fixedValueMatchesNumericallyEqualValuesOnly() {
        var regla = new TasaOCuotaRegla(false, new BigDecimal("0.160000"), new BigDecimal("0.160000"),
                Impuesto.IVA, "IVA", TipoFactor.TASA, true, false, VIGENCIA);

        assertThat(regla.admite(new BigDecimal("0.16"))).isTrue();
        assertThat(regla.admite(new BigDecimal("0.160000"))).isTrue();
        assertThat(regla.admite(new BigDecimal("0.159999"))).isFalse();
    }

    @Test
    void rangeIncludesBothEnds() {
        var regla = new TasaOCuotaRegla(true, new BigDecimal("0.000000"), new BigDecimal("0.350000"),
                Impuesto.ISR, "ISR", TipoFactor.TASA, false, true, VIGENCIA);

        assertThat(regla.admite(BigDecimal.ZERO)).isTrue();
        assertThat(regla.admite(new BigDecimal("0.35"))).isTrue();
        assertThat(regla.admite(new BigDecimal("0.350001"))).isFalse();
        assertThat(regla.admite(new BigDecimal("-0.000001"))).isFalse();
    }

    @Test
    void minimumAboveMaximumIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TasaOCuotaRegla(true, BigDecimal.ONE, BigDecimal.ZERO,
                Impuesto.ISR, "ISR", TipoFactor.TASA, false, true, VIGENCIA));
    }
}
