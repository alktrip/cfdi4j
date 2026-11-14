package io.github.alktrip.cfdi4j.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Size;

/** Derived amounts always satisfy the Anexo 20 relations they were derived from (ADR 0009). */
class ComprobanteBuilderProperties {

    record Linea(BigDecimal cantidad, BigDecimal valorUnitario, BigDecimal descuentoFraccion, String tasaIva, boolean retieneIsr) {}

    @Property(tries = 300)
    void derivedAmountsAreConsistent(@ForAll("lineas") @Size(min = 1, max = 8) List<Linea> lineas, @ForAll("monedas") Moneda moneda) {
        ComprobanteBuilder builder = ComprobanteBuilderTest.base().moneda(moneda);
        for (Linea linea : lineas) {
            builder.concepto(c -> {
                c.claveProdServ("01010101").cantidad(linea.cantidad()).claveUnidad("H87").descripcion("Pieza")
                        .valorUnitario(linea.valorUnitario()).objetoImp(ObjetoImp.SI_OBJETO);
                BigDecimal importe = Importes.producto(linea.cantidad(), linea.valorUnitario(), moneda.decimales().getAsInt());
                BigDecimal descuento = importe.multiply(linea.descuentoFraccion()).setScale(Importes.decimales(importe), java.math.RoundingMode.DOWN);
                if (descuento.signum() > 0) {
                    c.descuento(descuento);
                }
                c.traslado(Impuesto.IVA, TipoFactor.TASA, linea.tasaIva());
                if (linea.retieneIsr()) {
                    c.retencion(Impuesto.ISR, TipoFactor.TASA, "0.100000");
                }
            });
        }

        Comprobante cfdi = builder.build();
        int d = moneda.decimales().getAsInt();

        BigDecimal sumaImportes = BigDecimal.ZERO;
        BigDecimal sumaDescuentos = BigDecimal.ZERO;
        for (Concepto concepto : cfdi.conceptos()) {
            assertThat(Importes.limitesImporteConcepto(concepto.cantidad(), concepto.valorUnitario(), Importes.decimales(concepto.importe()))
                    .contiene(concepto.importe())).isTrue();
            for (Concepto.Traslado t : concepto.impuestos().orElseThrow().traslados()) {
                BigDecimal importe = t.importe().orElseThrow();
                assertThat(Importes.limitesImporteImpuesto(t.base(), t.tasaOCuota().orElseThrow(), Importes.decimales(importe))
                        .contiene(importe)).isTrue();
            }
            for (Concepto.Retencion r : concepto.impuestos().orElseThrow().retenciones()) {
                assertThat(Importes.limitesImporteImpuesto(r.base(), r.tasaOCuota(), Importes.decimales(r.importe()))
                        .contiene(r.importe())).isTrue();
            }
            sumaImportes = sumaImportes.add(concepto.importe());
            sumaDescuentos = sumaDescuentos.add(concepto.descuento().orElse(BigDecimal.ZERO));
        }

        assertThat(cfdi.subTotal()).isEqualTo(Importes.redondear(sumaImportes, d));
        assertThat(cfdi.descuento().orElse(BigDecimal.ZERO)).isEqualByComparingTo(Importes.redondear(sumaDescuentos, d));
        Impuestos impuestos = cfdi.impuestos().orElseThrow();
        BigDecimal trasladados = impuestos.traslados().stream().map(t -> t.importe().orElseThrow()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retenidos = impuestos.retenciones().stream().map(Impuestos.Retencion::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(impuestos.totalImpuestosTrasladados().orElseThrow()).isEqualByComparingTo(trasladados);
        assertThat(impuestos.totalImpuestosRetenidos().orElse(BigDecimal.ZERO)).isEqualByComparingTo(retenidos);
        assertThat(cfdi.total()).isEqualTo(Importes.redondear(
                cfdi.subTotal().subtract(cfdi.descuento().orElse(BigDecimal.ZERO)).add(trasladados).subtract(retenidos), d));
        for (BigDecimal total : List.of(cfdi.subTotal(), cfdi.total(), impuestos.totalImpuestosTrasladados().orElseThrow())) {
            assertThat(total.scale()).isEqualTo(d);
        }
    }

    @Provide
    Arbitrary<Moneda> monedas() {
        return Arbitraries.of(Moneda.MXN, Moneda.USD, Moneda.JPY, Moneda.OMR, Moneda.CLF);
    }

    @Provide
    Arbitrary<List<Linea>> lineas() {
        Arbitrary<BigDecimal> cantidad = scaled(new BigDecimal("0.000001"), new BigDecimal("10000"));
        Arbitrary<BigDecimal> valorUnitario = scaled(new BigDecimal("0.01"), new BigDecimal("100000"));
        Arbitrary<BigDecimal> descuento = Arbitraries.of(BigDecimal.ZERO, new BigDecimal("0.10"), new BigDecimal("0.5"));
        Arbitrary<String> tasa = Arbitraries.of("0.160000", "0.080000", "0.000000", "0.16");
        return Combinators.combine(cantidad, valorUnitario, descuento, tasa, Arbitraries.of(true, false))
                .as(Linea::new)
                .list();
    }

    private static Arbitrary<BigDecimal> scaled(BigDecimal min, BigDecimal max) {
        return Arbitraries.integers().between(0, 6)
                .flatMap(scale -> Arbitraries.bigDecimals().between(min.max(BigDecimal.ONE.movePointLeft(scale)), max).ofScale(scale));
    }
}
