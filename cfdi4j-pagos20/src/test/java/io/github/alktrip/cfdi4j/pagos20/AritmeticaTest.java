package io.github.alktrip.cfdi4j.pagos20;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.pagos20.internal.Aritmetica;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AritmeticaTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void limitsOfTheExampleOfTheGuia() {
        // ImpPagado 348,000.00 MXN, EquivalenciaDR 17.7331, payment in USD (Guía de llenado, pages 33 and 77).
        Aritmetica.Limites l = Aritmetica.limitesImpPagado(d("348000.00"), d("17.7331"), 4, 2);

        assertThat(l.inferior()).isEqualTo(d("19624.26"));
        assertThat(l.superior()).isEqualTo(d("19624.37"));
        assertThat(l.contiene(d("19624.32"))).isTrue();
        assertThat(l.contiene(d("19624.25"))).isFalse();
        assertThat(l.contiene(d("19624.38"))).isFalse();
    }

    @Test
    void convertedAmountsAreRoundedToTheCurrencyOfThePayment() {
        BigDecimal base = Aritmetica.aMonedaPago(d("300000.00"), d("17.7331"));

        assertThat(Aritmetica.importe(base, 2)).isEqualTo(d("16917.52"));
        assertThat(Aritmetica.igualConPrecision(d("16917.520000"), base, 2)).isTrue();
        assertThat(Aritmetica.igualConPrecision(d("16917.53"), base, 2)).isFalse();
        assertThat(Aritmetica.igualConPrecision(d("16917.5"), base, 2)).isTrue();
    }

    @Test
    void precisionIsCappedByTheCurrency() {
        assertThat(Aritmetica.precision(d("1.234567"), 2)).isEqualTo(2);
        assertThat(Aritmetica.precision(d("1.2"), 2)).isEqualTo(1);
        assertThat(Aritmetica.mediaUnidad(2)).isEqualTo(d("0.005"));
    }

    @Test
    void totalsAreRoundedHalfUpToCents() {
        assertThat(Aritmetica.totalMxn(d("348000.032961"))).isEqualTo(d("348000.03"));
        assertThat(Aritmetica.totalMxn(d("0.005"))).isEqualTo(d("0.01"));
    }
}
