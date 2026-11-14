package io.github.alktrip.cfdi4j.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/** Properties of the Anexo 20 arithmetic. */
class ImportesProperties {

    @Property
    void derivedConceptImporteIsAlwaysWithinTheAnexo20Limits(
            @ForAll("cantidades") BigDecimal cantidad,
            @ForAll("valoresUnitarios") BigDecimal valorUnitario,
            @ForAll @IntRange(min = 0, max = 4) int decimalesMoneda) {
        BigDecimal importe = Importes.producto(cantidad, valorUnitario, decimalesMoneda);

        assertThat(Importes.decimales(importe)).isLessThanOrEqualTo(Importes.MAX_DECIMALES);
        assertThat(Importes.limitesImporteConcepto(cantidad, valorUnitario, Importes.decimales(importe)).contiene(importe))
                .as("importe %s for %s x %s", importe, cantidad, valorUnitario)
                .isTrue();
    }

    @Property
    void derivedTaxImporteIsAlwaysWithinTheAnexo20Limits(
            @ForAll("bases") BigDecimal base,
            @ForAll("tasas") BigDecimal tasa,
            @ForAll @IntRange(min = 0, max = 4) int decimalesMoneda) {
        BigDecimal importe = Importes.producto(base, tasa, decimalesMoneda);

        assertThat(Importes.limitesImporteImpuesto(base, tasa, Importes.decimales(importe)).contiene(importe))
                .as("importe %s for %s x %s", importe, base, tasa)
                .isTrue();
    }

    @Property
    void lowerLimitIsAlwaysBelowTheUpperLimit(
            @ForAll("cantidades") BigDecimal cantidad,
            @ForAll("bases") BigDecimal valorUnitario,
            @ForAll @IntRange(min = 0, max = 6) int decimalesImporte) {
        var limites = Importes.limitesImporteConcepto(cantidad, valorUnitario, decimalesImporte);

        assertThat(limites.inferior()).isLessThan(limites.superior());
    }

    @Property
    void roundingKeepsTheValueWithinHalfAUnitOfTheLastDecimal(
            @ForAll("bases") BigDecimal valor, @ForAll @IntRange(min = 0, max = 4) int decimales) {
        BigDecimal redondeado = Importes.redondear(valor, decimales);

        assertThat(redondeado.scale()).isEqualTo(decimales);
        assertThat(redondeado.subtract(valor).abs()).isLessThanOrEqualTo(new BigDecimal("0.5").movePointLeft(decimales));
    }

    @Provide
    Arbitrary<BigDecimal> cantidades() {
        return scaled(new BigDecimal("0.000001"), new BigDecimal("1000000"));
    }

    @Provide
    Arbitrary<BigDecimal> valoresUnitarios() {
        return scaled(BigDecimal.ZERO, new BigDecimal("10000000"));
    }

    @Provide
    Arbitrary<BigDecimal> bases() {
        return scaled(new BigDecimal("0.000001"), new BigDecimal("100000000"));
    }

    @Provide
    Arbitrary<BigDecimal> tasas() {
        return Arbitraries.oneOf(
                Arbitraries.of(new BigDecimal("0.160000"), new BigDecimal("0.080000"), new BigDecimal("0.265000"),
                        new BigDecimal("1.600000"), new BigDecimal("0.000000"), new BigDecimal("72.160500")),
                Arbitraries.bigDecimals().between(BigDecimal.ZERO, new BigDecimal("0.35")).ofScale(6));
    }

    private static Arbitrary<BigDecimal> scaled(BigDecimal min, BigDecimal max) {
        return Arbitraries.integers().between(0, 6)
                .flatMap(scale -> Arbitraries.bigDecimals().between(min.max(BigDecimal.ONE.movePointLeft(scale)), max).ofScale(scale));
    }
}
