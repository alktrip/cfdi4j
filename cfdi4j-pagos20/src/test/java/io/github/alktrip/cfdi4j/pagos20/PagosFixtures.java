package io.github.alktrip.cfdi4j.pagos20;

import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import io.github.alktrip.cfdi4j.core.validation.Cfdi40Validator;
import io.github.alktrip.cfdi4j.core.validation.TaxpayerRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/** Recibos de pago for tests; RFCs are SAT public test data (Certificados_de_Prueba.zip). */
final class PagosFixtures {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneOffset.UTC);
    static final String UUID = "5FB2822E-396D-4725-8521-CDC4BDD20CCF";
    static final LocalDateTime FECHA_PAGO = LocalDateTime.of(2026, 11, 13, 12, 0, 0);

    /** Totales of {@link #valido()}. */
    static final Map<String, String> TOTALES_VALIDOS = Map.of("TotalTrasladosBaseIVA16", "100.00", "TotalTrasladosImpuestoIVA16", "16.00",
            "MontoTotalPagos", "116.00");

    private PagosFixtures() {}

    static Cfdi40Validator validator() {
        return Cfdi40Validator.builder().clock(CLOCK).build();
    }

    /** A validator whose taxpayer registry knows only the SAT test RFCs used here. */
    static Cfdi40Validator conPadron() {
        Map<String, TaxpayerRegistry.Contribuyente> inscritos = Map.of(
                "EKU9003173C9", new TaxpayerRegistry.Contribuyente("EKU9003173C9", "ESCUELA KEMPER URGATE", "72000", OptionalInt.empty()),
                "XIA190128J61", new TaxpayerRegistry.Contribuyente("XIA190128J61", "XENON INDUSTRIAL ARTICLES", "76343", OptionalInt.empty()));
        TaxpayerRegistry padron = rfc -> Optional.ofNullable(inscritos.get(rfc));
        return Cfdi40Validator.builder().clock(CLOCK).taxpayerRegistry(padron).build();
    }

    static ComprobanteBuilder recibo() {
        return partes(Pagos20.comprobante());
    }

    /** A recibo whose only concepto is the standard one changed by {@code concepto}. */
    static ComprobanteBuilder reciboCon(Consumer<ComprobanteBuilder.ConceptoBuilder> concepto) {
        return partes(Comprobante.builder().tipoDeComprobante(TipoDeComprobante.PAGO).exportacion(Exportacion.NO_APLICA).moneda(Moneda.XXX))
                .concepto(conceptoPago().andThen(concepto));
    }

    static Consumer<ComprobanteBuilder.ConceptoBuilder> conceptoPago() {
        return c -> c.claveProdServ("84111506").cantidad("1").claveUnidad("ACT").descripcion("Pago").valorUnitario("0")
                .objetoImp(ObjetoImp.NO_OBJETO);
    }

    private static ComprobanteBuilder partes(ComprobanteBuilder b) {
        return b.serie("P").folio("1").fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0)).lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                        .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.PAGOS));
    }

    /** A payment in MXN of an invoice of 100.00 + IVA 16 %. */
    static Consumer<PagosBuilder.PagoBuilder> pagoMxn() {
        return pagoMxn(documento());
    }

    /** A payment in MXN of 116.00 with the given document. */
    static Consumer<PagosBuilder.PagoBuilder> pagoMxn(Consumer<PagosBuilder.DoctoRelacionadoBuilder> documento) {
        return p -> p.fechaPago(FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.MXN).monto("116.00")
                .doctoRelacionado(documento);
    }

    /** A document in MXN of 116.00, paid in one exhibition, without taxes. */
    static Consumer<PagosBuilder.DoctoRelacionadoBuilder> documentoSinImpuestos() {
        return d -> d.idDocumento(UUID).monedaDR(Moneda.MXN).numParcialidad(1).impSaldoAnt("116.00").impPagado("116.00")
                .objetoImpDR(ObjetoImp.SI_OBJETO);
    }

    /** {@link #documentoSinImpuestos()} with IVA 16 % on a base of 100.00. */
    static Consumer<PagosBuilder.DoctoRelacionadoBuilder> documento() {
        return documentoSinImpuestos().andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.160000"));
    }

    static Comprobante con(Pagos pagos) {
        return recibo().complemento(pagos).build();
    }

    static Comprobante valido() {
        return con(Pagos.builder().pago(pagoMxn()).build());
    }

    /** Totales with the given attributes (by XML name); MontoTotalPagos defaults to 116.00. */
    static Pagos.Totales totales(Map<String, String> v) {
        return new Pagos.Totales(o(v, "TotalRetencionesIVA"), o(v, "TotalRetencionesISR"), o(v, "TotalRetencionesIEPS"),
                o(v, "TotalTrasladosBaseIVA16"), o(v, "TotalTrasladosImpuestoIVA16"), o(v, "TotalTrasladosBaseIVA8"),
                o(v, "TotalTrasladosImpuestoIVA8"), o(v, "TotalTrasladosBaseIVA0"), o(v, "TotalTrasladosImpuestoIVA0"),
                o(v, "TotalTrasladosBaseIVAExento"), new BigDecimal(v.getOrDefault("MontoTotalPagos", "116.00")));
    }

    private static Optional<BigDecimal> o(Map<String, String> v, String name) {
        return Optional.ofNullable(v.get(name)).map(BigDecimal::new);
    }

    /**
     * The amounts of the example of the Guía de llenado del complemento de pagos 2.0 (pages 76 and
     * 77): a payment in USD of an invoice in MXN. Names, RFCs and UUIDs are not taken from the guide.
     */
    static Pagos ejemploGuia() {
        return Pagos.builder()
                .totales(totales(Map.of("TotalTrasladosBaseIVA16", "300000.00", "TotalTrasladosImpuestoIVA16", "48000.00",
                        "MontoTotalPagos", "348000.00")))
                .pago(p -> p.fechaPago(FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.USD).tipoCambioP("17.7331")
                        .monto("19624.32")
                        .doctoRelacionado(d -> d.idDocumento(UUID).monedaDR(Moneda.MXN).equivalenciaDR("17.7331").numParcialidad(1)
                                .impSaldoAnt("348000.00").impPagado("348000.00").impSaldoInsoluto("0.00").objetoImpDR(ObjetoImp.SI_OBJETO)
                                .trasladoDR("300000.000000", Impuesto.IVA, TipoFactor.TASA, "0.160000", "48000.000000"))
                        .impuestosP(new Pagos.ImpuestosP(List.of(), List.of(new Pagos.TrasladoP(new BigDecimal("16917.520000"), Impuesto.IVA,
                                TipoFactor.TASA, Optional.of(new BigDecimal("0.160000")), Optional.of(new BigDecimal("2706.800000")))))))
                .build();
    }
}
