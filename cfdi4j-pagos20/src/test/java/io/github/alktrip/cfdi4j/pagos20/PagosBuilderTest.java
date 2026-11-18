package io.github.alktrip.cfdi4j.pagos20;

import static io.github.alktrip.cfdi4j.pagos20.PagosFixtures.documento;
import static io.github.alktrip.cfdi4j.pagos20.PagosFixtures.pagoMxn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.IncompleteModelException;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PagosBuilderTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void derivesTheValuesOfAPaymentInPesos() {
        Pagos pagos = Pagos.builder().pago(pagoMxn()).build();
        Pagos.Pago pago = pagos.pago().getFirst();
        Pagos.DoctoRelacionado docto = pago.doctoRelacionado().getFirst();

        assertThat(pago.tipoCambioP()).contains(BigDecimal.ONE);
        assertThat(docto.equivalenciaDR()).contains(BigDecimal.ONE);
        assertThat(docto.impSaldoInsoluto()).isEqualTo(d("0.00"));
        assertThat(docto.impuestosDR().orElseThrow().trasladosDR().getFirst().importeDR()).contains(d("16.00"));
        assertThat(pago.impuestosP().orElseThrow().trasladosP()).containsExactly(new Pagos.TrasladoP(d("100.00"), Impuesto.IVA, TipoFactor.TASA,
                Optional.of(d("0.160000")), Optional.of(d("16.00"))));
        assertThat(pagos.totales()).isEqualTo(PagosFixtures.totales(PagosFixtures.TOTALES_VALIDOS));
    }

    @Test
    void groupsTaxesOfSeveralDocumentsAndRetentions() {
        Pagos pagos = Pagos.builder().pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.MXN)
                .monto("222.00")
                .doctoRelacionado(documento())
                .doctoRelacionado(documento().andThen(d -> d.impSaldoAnt("106.00").impPagado("106.00")
                        .retencionDR("100.00", Impuesto.ISR, TipoFactor.TASA, "0.100000")))).build();
        Pagos.ImpuestosP impuestos = pagos.pago().getFirst().impuestosP().orElseThrow();

        assertThat(impuestos.trasladosP()).singleElement().satisfies(t -> {
            assertThat(t.baseP()).isEqualTo(d("200.00"));
            assertThat(t.importeP()).contains(d("32.00"));
        });
        assertThat(impuestos.retencionesP()).containsExactly(new Pagos.RetencionP(Impuesto.ISR, d("10.00")));
        assertThat(pagos.totales().totalRetencionesISR()).contains(d("10.00"));
        assertThat(pagos.totales().totalRetencionesIVA()).isEmpty();
        assertThat(pagos.totales().montoTotalPagos()).isEqualTo(d("222.00"));
    }

    @Test
    void convertsTaxesOfADocumentInAnotherCurrency() {
        Pagos pagos = Pagos.builder().pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.USD)
                .tipoCambioP("17.7331").monto("19624.32")
                .doctoRelacionado(d -> d.idDocumento(PagosFixtures.UUID).monedaDR(Moneda.MXN).equivalenciaDR("17.7331").numParcialidad(1)
                        .impSaldoAnt("348000.00").impPagado("348000.00").objetoImpDR(ObjetoImp.SI_OBJETO)
                        .trasladoDR("300000.00", Impuesto.IVA, TipoFactor.TASA, "0.160000"))).build();
        Pagos.TrasladoP t = pagos.pago().getFirst().impuestosP().orElseThrow().trasladosP().getFirst();

        // The amounts of the example of the Guía de llenado, page 77.
        assertThat(t.baseP()).isEqualByComparingTo("16917.52");
        assertThat(t.importeP()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("2706.80"));
        assertThat(pagos.totales().totalTrasladosBaseIVA16()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("300000.00"));
        assertThat(pagos.totales().totalTrasladosImpuestoIVA16()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("48000.00"));
        assertThat(pagos.totales().montoTotalPagos()).isEqualByComparingTo("348000.03");
    }

    private static PagosBuilder.PagoBuilder todosLosImpuestos(PagosBuilder.PagoBuilder p) {
        return p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.MXN).monto("2000.00")
                .doctoRelacionado(factura(1, "116.00").andThen(d -> d.retencionDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.106666")
                        .retencionDR("100.00", Impuesto.ISR, TipoFactor.TASA, "0.100000")))
                .doctoRelacionado(factura(2, "108.00").andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.080000")))
                .doctoRelacionado(factura(3, "100.00").andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.000000")))
                .doctoRelacionado(factura(4, "100.00").andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.EXENTO, null)))
                .doctoRelacionado(factura(5, "108.00").andThen(d -> d.trasladoDR("100.00", Impuesto.IEPS, TipoFactor.TASA, "0.080000")
                        .retencionDR("100.00", Impuesto.IEPS, TipoFactor.TASA, "0.080000")));
    }

    private static java.util.function.Consumer<PagosBuilder.DoctoRelacionadoBuilder> factura(int parcialidad, String saldo) {
        return d -> d.idDocumento(PagosFixtures.UUID).monedaDR(Moneda.MXN).numParcialidad(parcialidad).impSaldoAnt(saldo).impPagado(saldo)
                .objetoImpDR(ObjetoImp.SI_OBJETO);
    }

    @Test
    void derivesEveryTotal() {
        Pagos pagos = Pagos.builder().pago(p -> todosLosImpuestos(p).doctoRelacionado(factura(6, "116.00")
                .andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.160000")))).build();
        Pagos.Totales t = pagos.totales();

        assertThat(t.totalRetencionesIVA()).contains(d("10.67"));
        assertThat(t.totalRetencionesISR()).contains(d("10.00"));
        assertThat(t.totalRetencionesIEPS()).contains(d("8.00"));
        assertThat(t.totalTrasladosBaseIVA16()).contains(d("100.00"));
        assertThat(t.totalTrasladosImpuestoIVA16()).contains(d("16.00"));
        assertThat(t.totalTrasladosBaseIVA8()).contains(d("100.00"));
        assertThat(t.totalTrasladosImpuestoIVA8()).contains(d("8.00"));
        assertThat(t.totalTrasladosBaseIVA0()).contains(d("100.00"));
        assertThat(t.totalTrasladosImpuestoIVA0()).contains(d("0.00"));
        assertThat(t.totalTrasladosBaseIVAExento()).contains(d("100.00"));
        assertThat(PagosFixtures.validator().validate(PagosFixtures.con(pagos)).errors()).isEmpty();
    }

    @Test
    void derivesTotalsFromExplicitImpuestosP() {
        Pagos derivado = Pagos.builder().pago(PagosBuilderTest::todosLosImpuestos).build();
        Pagos.ImpuestosP impuestos = derivado.pago().getFirst().impuestosP().orElseThrow();
        Pagos explicito = Pagos.builder().pago(p -> todosLosImpuestos(p).impuestosP(impuestos)).build();

        assertThat(explicito.totales()).isEqualTo(derivado.totales());
        assertThat(PagosFixtures.validator().validate(PagosFixtures.con(explicito)).errors()).isEmpty();
    }

    @Test
    void reportsIncompleteTaxesOfADocument() {
        assertThatThrownBy(() -> Pagos.builder().pago(pagoMxn(documento().andThen(d -> d.trasladoDR(null, Impuesto.IVA, TipoFactor.TASA, "0.160000")))).build())
                .isInstanceOfSatisfying(IncompleteModelException.class, e -> assertThat(e.missing()).contains("pago20:Pago[1]/pago20:DoctoRelacionado[1]/pago20:ImpuestosDR"));
        assertThatThrownBy(() -> Pagos.builder().pago(pagoMxn(documento().andThen(d -> d.retencionDR("100.00", Impuesto.ISR, TipoFactor.TASA, null)))).build())
                .isInstanceOfSatisfying(IncompleteModelException.class, e -> assertThat(e.missing()).contains("pago20:Pago[1]/pago20:DoctoRelacionado[1]/pago20:ImpuestosDR"));
    }

    @Test
    void neverOverwritesExplicitValues() {
        Pagos.Totales totales = PagosFixtures.totales(Map.of("TotalTrasladosBaseIVA16", "100.00", "TotalTrasladosImpuestoIVA16", "16.00"));
        Pagos.ImpuestosP impuestos = new Pagos.ImpuestosP(List.of(), List.of());
        Pagos pagos = Pagos.builder().totales(totales)
                .pago(pagoMxn(documento().andThen(d -> d.impSaldoInsoluto("0.00").equivalenciaDR("1")
                        .trasladoDR("1.00", Impuesto.IEPS, TipoFactor.TASA, "0.080000", "0.09"))).andThen(p -> p.tipoCambioP("1.0").impuestosP(impuestos)))
                .build();
        Pagos.Pago pago = pagos.pago().getFirst();

        assertThat(pagos.totales()).isSameAs(totales);
        assertThat(pago.impuestosP()).containsSame(impuestos);
        assertThat(pago.tipoCambioP()).contains(d("1.0"));
        assertThat(pago.doctoRelacionado().getFirst().impuestosDR().orElseThrow().trasladosDR().get(1).importeDR()).contains(d("0.09"));
    }

    @Test
    void reportsEveryMissingValue() {
        assertThatThrownBy(() -> Pagos.builder().build()).isInstanceOf(IncompleteModelException.class).hasMessageContaining("pago20:Pago");
        assertThatThrownBy(() -> Pagos.builder().pago(p -> p.doctoRelacionado(d -> { })).build())
                .isInstanceOfSatisfying(IncompleteModelException.class, e -> assertThat(e.missing()).contains(
                        "pago20:Pago[1]/@FechaPago", "pago20:Pago[1]/@FormaDePagoP", "pago20:Pago[1]/@MonedaP", "pago20:Pago[1]/@Monto",
                        "pago20:Pago[1]/pago20:DoctoRelacionado[1]/@IdDocumento"));
    }

    @Test
    void requiresTheExchangeRateToDeriveTotals() {
        assertThatThrownBy(() -> Pagos.builder().pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA)
                .monedaP(Moneda.USD).monto("10.00").doctoRelacionado(d -> d.idDocumento(PagosFixtures.UUID).monedaDR(Moneda.USD).numParcialidad(1)
                        .impSaldoAnt("10.00").impPagado("10.00").objetoImpDR(ObjetoImp.NO_OBJETO))).build())
                .isInstanceOf(IncompleteModelException.class).hasMessageContaining("TipoCambioP");
    }

    @Test
    void rejectsANonPositiveEquivalence() {
        assertThatThrownBy(() -> Pagos.builder().pago(pagoMxn(documento().andThen(d -> d.equivalenciaDR("0")))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("EquivalenciaDR must be greater than zero");
    }

    @Test
    void requiresTheEquivalenceBetweenDifferentCurrencies() {
        assertThatThrownBy(() -> Pagos.builder().pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA)
                .monedaP(Moneda.USD).tipoCambioP("17.5").monto("10.00").doctoRelacionado(documento())).build())
                .isInstanceOf(IncompleteModelException.class).hasMessageContaining("EquivalenciaDR");
    }

    @Test
    void presetComprobanteHasTheFixedValuesOfARecibo() {
        Comprobante recibo = PagosFixtures.valido();

        assertThat(recibo.tipoDeComprobante().clave()).isEqualTo("P");
        assertThat(recibo.moneda()).isEqualTo(Moneda.XXX);
        assertThat(recibo.subTotal()).isZero();
        assertThat(recibo.total()).isZero();
        assertThat(recibo.conceptos()).singleElement().satisfies(c -> {
            assertThat(c.claveProdServ()).isEqualTo("84111506");
            assertThat(c.descripcion()).isEqualTo("Pago");
        });
        assertThat(recibo.complemento()).singleElement().isInstanceOf(Pagos.class);
    }

    @Test
    void dropsFractionsOfASecond() {
        Pagos pagos = Pagos.builder().pago(pagoMxn().andThen(p -> p.fechaPago(LocalDateTime.of(2026, 11, 13, 12, 0, 0, 999)))).build();

        assertThat(pagos.pago().getFirst().fechaPago()).isEqualTo(PagosFixtures.FECHA_PAGO);
    }
}
