package io.github.alktrip.cfdi4j.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.IncompleteModelException;
import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Meses;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.Pais;
import io.github.alktrip.cfdi4j.core.catalog.Periodicidad;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.catalog.TipoRelacion;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ComprobanteBuilderTest {

    /** SAT public test taxpayer (Certificados_de_Prueba.zip, persona moral EKU9003173C9). */
    static ComprobanteBuilder base() {
        return Comprobante.builder()
                .serie("A")
                .folio("123")
                .fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0))
                .formaPago(FormaPago.TRANSFERENCIA)
                .metodoPago(MetodoPago.PUE)
                .moneda(Moneda.MXN)
                .tipoDeComprobante(TipoDeComprobante.INGRESO)
                .exportacion(Exportacion.NO_APLICA)
                .lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XAXX010101000").nombre("PUBLICO EN GENERAL").domicilioFiscalReceptor("72000")
                        .regimenFiscalReceptor(RegimenFiscal.SIN_OBLIGACIONES_FISCALES).usoCFDI(UsoCFDI.SIN_EFECTOS_FISCALES));
    }

    @Test
    void derivesEveryAmountOfASimpleInvoice() {
        Comprobante cfdi = base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();

        Concepto concepto = cfdi.conceptos().get(0);
        assertThat(concepto.importe()).hasToString("100.00");
        Concepto.Traslado traslado = concepto.impuestos().orElseThrow().traslados().get(0);
        assertThat(traslado.base()).hasToString("100.00");
        assertThat(traslado.tasaOCuota()).map(BigDecimal::toString).contains("0.160000");
        assertThat(traslado.importe()).map(BigDecimal::toString).contains("16.00");

        assertThat(cfdi.subTotal()).hasToString("100.00");
        assertThat(cfdi.descuento()).isEmpty();
        Impuestos impuestos = cfdi.impuestos().orElseThrow();
        assertThat(impuestos.totalImpuestosTrasladados()).map(BigDecimal::toString).contains("16.00");
        assertThat(impuestos.totalImpuestosRetenidos()).isEmpty();
        assertThat(impuestos.traslados()).singleElement().satisfies(t -> {
            assertThat(t.base()).hasToString("100.00");
            assertThat(t.importe()).map(BigDecimal::toString).contains("16.00");
            assertThat(t.tasaOCuota()).map(BigDecimal::toString).contains("0.160000");
        });
        assertThat(cfdi.total()).hasToString("116.00");
        assertThat(cfdi.sello()).isEmpty();
        assertThat(cfdi.noCertificado()).isEmpty();
        assertThat(cfdi.certificado()).isEmpty();
    }

    @Test
    void reportsEveryMissingRequiredItemAtOnce() {
        ComprobanteBuilder builder = Comprobante.builder()
                .emisor(e -> e.rfc("EKU9003173C9"))
                .concepto(c -> c.claveProdServ("01010101").traslado(t -> t.tasaOCuota("0.160000")).retencion(r -> { }).parte(p -> { }));

        assertThatThrownBy(builder::build)
                .isInstanceOfSatisfying(IncompleteModelException.class, e -> assertThat(e.missing()).containsExactly(
                        "@Fecha", "@Moneda", "@TipoDeComprobante", "@Exportacion", "@LugarExpedicion",
                        "cfdi:Emisor/@Nombre", "cfdi:Emisor/@RegimenFiscal",
                        "cfdi:Receptor",
                        "cfdi:Conceptos/cfdi:Concepto[1]/@Cantidad",
                        "cfdi:Conceptos/cfdi:Concepto[1]/@ClaveUnidad",
                        "cfdi:Conceptos/cfdi:Concepto[1]/@Descripcion",
                        "cfdi:Conceptos/cfdi:Concepto[1]/@ValorUnitario",
                        "cfdi:Conceptos/cfdi:Concepto[1]/@ObjetoImp",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Impuestos/cfdi:Traslados/cfdi:Traslado[1]/@Impuesto",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Impuestos/cfdi:Traslados/cfdi:Traslado[1]/@TipoFactor",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Impuestos/cfdi:Retenciones/cfdi:Retencion[1]/@Impuesto",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Impuestos/cfdi:Retenciones/cfdi:Retencion[1]/@TipoFactor",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Impuestos/cfdi:Retenciones/cfdi:Retencion[1]/@TasaOCuota",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Parte[1]/@ClaveProdServ",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Parte[1]/@Cantidad",
                        "cfdi:Conceptos/cfdi:Concepto[1]/cfdi:Parte[1]/@Descripcion"))
                .hasMessageStartingWith("Missing required data: @Fecha, @Moneda");
    }

    @Test
    void requiresEmisorReceptorAndAtLeastOneConcepto() {
        ComprobanteBuilder builder = Comprobante.builder().fecha(LocalDateTime.of(2026, 1, 1, 0, 0)).moneda(Moneda.MXN)
                .tipoDeComprobante(TipoDeComprobante.INGRESO).exportacion(Exportacion.NO_APLICA).lugarExpedicion("72000")
                .receptor(r -> { });

        assertThatThrownBy(builder::build)
                .isInstanceOfSatisfying(IncompleteModelException.class, e -> assertThat(e.missing()).containsExactly(
                        "cfdi:Emisor", "cfdi:Receptor/@Rfc", "cfdi:Receptor/@Nombre", "cfdi:Receptor/@DomicilioFiscalReceptor",
                        "cfdi:Receptor/@RegimenFiscalReceptor", "cfdi:Receptor/@UsoCFDI", "cfdi:Conceptos/cfdi:Concepto"));
    }

    @Test
    void explicitValuesAreNeverOverwritten() {
        Impuestos explicitImpuestos = new Impuestos(Optional.empty(), Optional.of(new BigDecimal("1.00")), java.util.List.of(),
                java.util.List.of(new Impuestos.Traslado(new BigDecimal("9.00"), Impuesto.IVA, TipoFactor.TASA,
                        Optional.of(new BigDecimal("0.160000")), Optional.of(new BigDecimal("1.00")))));
        Comprobante cfdi = base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("2").claveUnidad("H87").descripcion("Pieza")
                        .valorUnitario("10.00").importe("19.99").objetoImp(ObjetoImp.SI_OBJETO)
                        .traslado(t -> t.impuesto(Impuesto.IVA).tipoFactor(TipoFactor.TASA).tasaOCuota("0.160000").base("5.00").importe("0.01")))
                .subTotal("1.00")
                .descuento("0.50")
                .impuestos(explicitImpuestos)
                .total("3.00")
                .build();

        Concepto concepto = cfdi.conceptos().get(0);
        assertThat(concepto.importe()).hasToString("19.99");
        assertThat(concepto.impuestos().orElseThrow().traslados().get(0).base()).hasToString("5.00");
        assertThat(concepto.impuestos().orElseThrow().traslados().get(0).importe()).map(BigDecimal::toString).contains("0.01");
        assertThat(cfdi.subTotal()).hasToString("1.00");
        assertThat(cfdi.descuento()).map(BigDecimal::toString).contains("0.50");
        assertThat(cfdi.impuestos()).containsSame(explicitImpuestos);
        assertThat(cfdi.total()).hasToString("3.00");
    }

    @Test
    void descuentoReducesTheTaxBaseAndTheTotal() {
        Comprobante cfdi = base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("3.141649").claveUnidad("H87").descripcion("Reparación")
                        .valorUnitario("1230.00").descuento("864.10").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();

        Concepto concepto = cfdi.conceptos().get(0);
        // Guía, Concepto/Descuento example: Importe 3864.22827, Descuento 864.10
        assertThat(concepto.importe()).hasToString("3864.22827");
        Concepto.Traslado traslado = concepto.impuestos().orElseThrow().traslados().get(0);
        assertThat(traslado.base()).hasToString("3000.12827");
        assertThat(traslado.importe()).map(BigDecimal::toString).contains("480.020523");
        assertThat(cfdi.subTotal()).hasToString("3864.23");
        assertThat(cfdi.descuento()).map(BigDecimal::toString).contains("864.10");
        assertThat(cfdi.impuestos().orElseThrow().traslados().get(0).base()).hasToString("3000.13");
        assertThat(cfdi.impuestos().orElseThrow().totalImpuestosTrasladados()).map(BigDecimal::toString).contains("480.02");
        // 3864.23 - 864.10 + 480.02
        assertThat(cfdi.total()).hasToString("3480.15");
    }

    @Test
    void groupsTrasladosByImpuestoTipoFactorAndTasaComparingTasasNumerically() {
        Comprobante cfdi = base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("A")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("B")
                        .valorUnitario("50.005").objetoImp(ObjetoImp.SI_OBJETO).traslado(Impuesto.IVA, TipoFactor.TASA, "0.16"))
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("C")
                        .valorUnitario("10.00").objetoImp(ObjetoImp.SI_OBJETO).traslado(Impuesto.IVA, TipoFactor.TASA, "0.080000"))
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("D")
                        .valorUnitario("20.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoExento(Impuesto.IVA))
                .build();

        Impuestos impuestos = cfdi.impuestos().orElseThrow();
        assertThat(impuestos.traslados()).hasSize(3);
        Impuestos.Traslado iva16 = impuestos.traslados().get(0);
        assertThat(iva16.base()).hasToString("150.01");
        // 16.00 + 8.0008 = 24.0008 -> 24.00
        assertThat(iva16.importe()).map(BigDecimal::toString).contains("24.00");
        assertThat(iva16.tasaOCuota()).map(BigDecimal::toString).contains("0.160000");
        Impuestos.Traslado iva8 = impuestos.traslados().get(1);
        assertThat(iva8.importe()).map(BigDecimal::toString).contains("0.80");
        Impuestos.Traslado exento = impuestos.traslados().get(2);
        assertThat(exento.tipoFactor()).isEqualTo(TipoFactor.EXENTO);
        assertThat(exento.base()).hasToString("20.00");
        assertThat(exento.tasaOCuota()).isEmpty();
        assertThat(exento.importe()).isEmpty();
        assertThat(impuestos.totalImpuestosTrasladados()).map(BigDecimal::toString).contains("24.80");
        // 180.005 -> 180.01 ; 180.01 + 24.80
        assertThat(cfdi.subTotal()).hasToString("180.01");
        assertThat(cfdi.total()).hasToString("204.81");
    }

    @Test
    void onlyExemptTrasladosProduceNoTotalTrasladados() {
        Comprobante cfdi = base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("A")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoExento(Impuesto.IVA))
                .build();

        assertThat(cfdi.impuestos().orElseThrow().totalImpuestosTrasladados()).isEmpty();
        assertThat(cfdi.conceptos().get(0).impuestos().orElseThrow().traslados().get(0).importe()).isEmpty();
        assertThat(cfdi.total()).hasToString("100.00");
    }

    @Test
    void retencionesAreGroupedByImpuestoAndSubtractedFromTheTotal() {
        Comprobante cfdi = base()
                .concepto(c -> c.claveProdServ("84111506").cantidad("1").claveUnidad("E48").descripcion("Honorarios")
                        .valorUnitario("1000.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16()
                        .retencion(Impuesto.ISR, TipoFactor.TASA, "0.100000")
                        .retencion(Impuesto.IVA, TipoFactor.TASA, "0.106666"))
                .concepto(c -> c.claveProdServ("84111506").cantidad("1").claveUnidad("E48").descripcion("Asesoría")
                        .valorUnitario("500.00").objetoImp(ObjetoImp.SI_OBJETO)
                        .retencion(r -> r.impuesto(Impuesto.ISR).tipoFactor(TipoFactor.TASA).tasaOCuota("0.012500").base("400.00")))
                .build();

        Concepto.Retencion isrBaseExplicita = cfdi.conceptos().get(1).impuestos().orElseThrow().retenciones().get(0);
        assertThat(isrBaseExplicita.base()).hasToString("400.00");
        assertThat(isrBaseExplicita.importe()).hasToString("5.00");
        Impuestos impuestos = cfdi.impuestos().orElseThrow();
        assertThat(impuestos.retenciones()).extracting(Impuestos.Retencion::impuesto).containsExactly(Impuesto.ISR, Impuesto.IVA);
        assertThat(impuestos.retenciones().get(0).importe()).hasToString("105.00");
        assertThat(impuestos.retenciones().get(1).importe()).hasToString("106.67");
        assertThat(impuestos.totalImpuestosRetenidos()).map(BigDecimal::toString).contains("211.67");
        // 1500.00 + 160.00 - 211.67
        assertThat(cfdi.total()).hasToString("1448.33");
    }

    @Test
    void trasladoAndPagoHaveZeroSubTotalAndTotal() {
        Comprobante traslado = base().formaPago(null).metodoPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO)
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("H87").descripcion("Mercancía")
                        .valorUnitario("500.00").descuento("1.00").objetoImp(ObjetoImp.NO_OBJETO))
                .build();

        assertThat(traslado.subTotal()).hasToString("0.00");
        assertThat(traslado.total()).hasToString("0.00");
        assertThat(traslado.descuento()).isEmpty();
        assertThat(traslado.formaPago()).isEmpty();

        Comprobante pago = base().moneda(Moneda.XXX).tipoDeComprobante(TipoDeComprobante.PAGO)
                .concepto(c -> c.claveProdServ("84111506").cantidad("1").claveUnidad("ACT").descripcion("Pago")
                        .valorUnitario("0").objetoImp(ObjetoImp.NO_OBJETO))
                .build();

        assertThat(pago.subTotal()).hasToString("0");
        assertThat(pago.total()).hasToString("0");
    }

    @Test
    void currenciesWithOtherDecimalsRoundTotalsToTheirOwnDecimals() {
        Comprobante jpy = base().moneda(Moneda.JPY).tipoCambio("0.130000")
                .concepto(c -> c.claveProdServ("01010101").cantidad("3").claveUnidad("H87").descripcion("Pieza")
                        .valorUnitario("333.5").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();

        assertThat(jpy.conceptos().get(0).importe()).hasToString("1000.5");
        assertThat(jpy.subTotal()).hasToString("1001");
        assertThat(jpy.impuestos().orElseThrow().totalImpuestosTrasladados()).map(BigDecimal::toString).contains("160");
        assertThat(jpy.total()).hasToString("1161");
        assertThat(jpy.tipoCambio()).map(BigDecimal::toString).contains("0.130000");
    }

    @Test
    void unknownCurrencyStopsDerivationWithAClearMessage() {
        ComprobanteBuilder builder = base().moneda(Moneda.of("ABC"))
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("H87").descripcion("Pieza")
                        .valorUnitario("1.00").objetoImp(ObjetoImp.NO_OBJETO));

        assertThatThrownBy(builder::build)
                .isInstanceOf(IncompleteModelException.class)
                .hasMessageContaining("Moneda 'ABC' is not in c_Moneda")
                .satisfies(e -> assertThat(((IncompleteModelException) e).missing()).isEmpty());
    }

    @Test
    void unknownCurrencyIsAcceptedWhenNothingHasToBeDerived() {
        Comprobante cfdi = base().moneda(Moneda.of("ABC"))
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("H87").descripcion("Pieza")
                        .valorUnitario("1.00").importe("1.00").objetoImp(ObjetoImp.NO_OBJETO))
                .subTotal("1.00")
                .total("1.00")
                .build();

        assertThat(cfdi.moneda().isKnown()).isFalse();
        assertThat(cfdi.impuestos()).isEmpty();
    }

    @Test
    void fechaIsTruncatedToSeconds() {
        Comprobante cfdi = base().fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 15, 999_000_000))
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("1.00").objetoImp(ObjetoImp.NO_OBJETO))
                .build();

        assertThat(cfdi.fecha()).isEqualTo(LocalDateTime.of(2026, 11, 14, 10, 30, 15));
        assertThat(base().fecha(null)).isNotNull();
    }

    @Test
    void optionalNodesAndAttributesAreCarriedOver() {
        Concepto.Parte[] parte = new Concepto.Parte[1];
        Comprobante cfdi = base()
                .condicionesDePago("3 meses")
                .confirmacion("ECVH1")
                .informacionGlobal(Periodicidad.MENSUAL, Meses.NOVIEMBRE, 2026)
                .cfdiRelacionados(TipoRelacion.SUSTITUCION, "5FB2822E-396D-4725-8521-CDC4BDD20CCF")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM)
                        .facAtrAdquirente("0123456789"))
                .receptor(r -> r.rfc("XEXX010101000").nombre("CLIENTE EXTRANJERO").domicilioFiscalReceptor("72000")
                        .residenciaFiscal(Pais.USA).numRegIdTrib("123456789")
                        .regimenFiscalReceptor(RegimenFiscal.SIN_OBLIGACIONES_FISCALES).usoCFDI(UsoCFDI.SIN_EFECTOS_FISCALES))
                .concepto(c -> c.claveProdServ("01010101").noIdentificacion("SKU-1").cantidad(new BigDecimal("1"))
                        .claveUnidad("H87").unidad("Pieza").descripcion("Kit").valorUnitario(new BigDecimal("10.00"))
                        .descuento(new BigDecimal("1.00")).objetoImp(ObjetoImp.NO_OBJETO)
                        .aCuentaTerceros("JUFA7608212V6", "ADRIANA JUAREZ FERNANDEZ", RegimenFiscal.ACTIVIDADES_EMPRESARIALES_PF, "29133")
                        .informacionAduanera("21  47  3807  8003832")
                        .cuentaPredial("15956011002")
                        .complementoConcepto(() -> "http://example.com/concepto")
                        .parte(p -> p.claveProdServ("01010101").noIdentificacion("P-1").cantidad("2").unidad("Pieza")
                                .descripcion("Parte").valorUnitario("5.00").importe("10.00").informacionAduanera("21  47  3807  8003832")))
                .complemento(() -> "http://example.com/complemento")
                .build();

        assertThat(cfdi.condicionesDePago()).contains("3 meses");
        assertThat(cfdi.confirmacion()).contains("ECVH1");
        assertThat(cfdi.informacionGlobal()).contains(new InformacionGlobal(Periodicidad.MENSUAL, Meses.NOVIEMBRE, 2026));
        assertThat(cfdi.cfdiRelacionados()).singleElement().satisfies(r -> {
            assertThat(r.tipoRelacion()).isEqualTo(TipoRelacion.SUSTITUCION);
            assertThat(r.cfdiRelacionado()).extracting(CfdiRelacionados.CfdiRelacionado::uuid)
                    .containsExactly("5FB2822E-396D-4725-8521-CDC4BDD20CCF");
        });
        assertThat(cfdi.emisor().facAtrAdquirente()).contains("0123456789");
        assertThat(cfdi.receptor().residenciaFiscal()).contains(Pais.USA);
        assertThat(cfdi.receptor().numRegIdTrib()).contains("123456789");
        Concepto concepto = cfdi.conceptos().get(0);
        assertThat(concepto.noIdentificacion()).contains("SKU-1");
        assertThat(concepto.unidad()).contains("Pieza");
        assertThat(concepto.impuestos()).isEmpty();
        assertThat(concepto.aCuentaTerceros()).hasValueSatisfying(t -> assertThat(t.rfcACuentaTerceros()).isEqualTo("JUFA7608212V6"));
        assertThat(concepto.informacionAduanera()).extracting(Concepto.InformacionAduanera::numeroPedimento)
                .containsExactly("21  47  3807  8003832");
        assertThat(concepto.cuentaPredial()).extracting(Concepto.CuentaPredial::numero).containsExactly("15956011002");
        assertThat(concepto.complementoConcepto()).extracting(Complemento::namespace).containsExactly("http://example.com/concepto");
        parte[0] = concepto.parte().get(0);
        assertThat(parte[0].importe()).map(BigDecimal::toString).contains("10.00");
        assertThat(parte[0].informacionAduanera()).hasSize(1);
        assertThat(cfdi.complemento()).extracting(Complemento::namespace).containsExactly("http://example.com/complemento");
        assertThat(cfdi.descuento()).map(BigDecimal::toString).contains("1.00");
        assertThat(cfdi.total()).hasToString("9.00");
    }

    @Test
    void sealingCopiesSetCertificateAndSealWithoutTouchingTheRest() {
        Comprobante cfdi = base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("1.00").objetoImp(ObjetoImp.NO_OBJETO))
                .build();

        Comprobante conCertificado = cfdi.withCertificado("30001000000500003416", "Y2VydA==");
        Comprobante sellado = conCertificado.withSello("c2VsbG8=");

        assertThat(conCertificado.noCertificado()).contains("30001000000500003416");
        assertThat(conCertificado.certificado()).contains("Y2VydA==");
        assertThat(conCertificado.sello()).isEmpty();
        assertThat(sellado.sello()).contains("c2VsbG8=");
        assertThat(sellado.withCertificado("30001000000500003416", "Y2VydA==").sello()).isEmpty();
        assertThat(sellado.withCertificado("30001000000500003416", "Y2VydA==").withSello("c2VsbG8=")).isEqualTo(sellado);
        assertThat(sellado.conceptos()).isEqualTo(cfdi.conceptos());
        assertThat(sellado.total()).isEqualTo(cfdi.total());
    }
}
