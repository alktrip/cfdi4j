package io.github.alktrip.cfdi4j.pagos20;

import static io.github.alktrip.cfdi4j.pagos20.Copias.con;
import static io.github.alktrip.cfdi4j.pagos20.Copias.primerDocto;
import static io.github.alktrip.cfdi4j.pagos20.Copias.primerPago;
import static io.github.alktrip.cfdi4j.pagos20.PagosFixtures.TOTALES_VALIDOS;
import static io.github.alktrip.cfdi4j.pagos20.PagosFixtures.documento;
import static io.github.alktrip.cfdi4j.pagos20.PagosFixtures.documentoSinImpuestos;
import static io.github.alktrip.cfdi4j.pagos20.PagosFixtures.pagoMxn;
import static io.github.alktrip.cfdi4j.pagos20.PagosFixtures.totales;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import io.github.alktrip.cfdi4j.core.validation.Cfdi40Validator;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.Status;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.pagos20.internal.Pagos20RuleSet;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** One invalid case at least for every rule of the CRP matrix that checks something, plus valid variants. */
class PagosRulesTest {

    record Caso(String code, String nombre, Supplier<Comprobante> comprobante, Cfdi40Validator validator) {
        @Override
        public String toString() {
            return code + " " + nombre;
        }

        ValidationResult validar() {
            return validator.validate(comprobante.get());
        }
    }

    // --- case builders ---

    /** A recibo changed at the comprobante level. */
    static Caso recibo(String code, String nombre, Consumer<ComprobanteBuilder> m) {
        return new Caso(code, nombre, () -> {
            ComprobanteBuilder b = PagosFixtures.recibo().complemento(Pagos.builder().pago(pagoMxn()).build());
            m.accept(b);
            return b.build();
        }, PagosFixtures.validator());
    }

    /** A recibo whose concepto is changed. */
    static Caso concepto(String code, String nombre, Consumer<ComprobanteBuilder.ConceptoBuilder> m) {
        return new Caso(code, nombre, () -> PagosFixtures.reciboCon(m).complemento(Pagos.builder().pago(pagoMxn()).build()).build(),
                PagosFixtures.validator());
    }

    /** A complement built with the builder. */
    static Caso pagos(String code, String nombre, Consumer<PagosBuilder> m) {
        return new Caso(code, nombre, () -> {
            PagosBuilder b = Pagos.builder();
            m.accept(b);
            return PagosFixtures.con(b.build());
        }, PagosFixtures.validator());
    }

    /** One Pago in MXN with the standard document, changed by {@code m}. */
    static Caso pago(String code, String nombre, Consumer<PagosBuilder.PagoBuilder> m) {
        return pagos(code, nombre, b -> b.pago(pagoMxn().andThen(m)));
    }

    /** One Pago in MXN with a document without taxes changed by {@code m}. */
    static Caso docto(String code, String nombre, Consumer<PagosBuilder.DoctoRelacionadoBuilder> m) {
        return pagos(code, nombre, b -> b.pago(pagoMxn(documentoSinImpuestos().andThen(m))));
    }

    /** The valid complement changed after it is built, for values the builder does not produce. */
    static Caso post(String code, String nombre, UnaryOperator<Pagos> f) {
        return new Caso(code, nombre, () -> PagosFixtures.con(f.apply(Pagos.builder().pago(pagoMxn()).build())), PagosFixtures.validator());
    }

    /** The standard Pago with these Totales. */
    static Caso conTotales(String code, String nombre, Map<String, String> cambios) {
        Map<String, String> v = new HashMap<>(TOTALES_VALIDOS);
        cambios.forEach((k, x) -> {
            if (x == null) {
                v.remove(k);
            } else {
                v.put(k, x);
            }
        });
        return pagos(code, nombre, b -> b.pago(pagoMxn()).totales(totales(v)));
    }

    static Map<String, String> mapa(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    static Pagos.TrasladoP trasladoP(String base, Impuesto impuesto, TipoFactor factor, String tasa, String importe) {
        return new Pagos.TrasladoP(new BigDecimal(base), impuesto, factor, Optional.ofNullable(tasa).map(BigDecimal::new),
                Optional.ofNullable(importe).map(BigDecimal::new));
    }

    static Pagos.ImpuestosP iva16(String base, String importe) {
        return new Pagos.ImpuestosP(List.of(), List.of(trasladoP(base, Impuesto.IVA, TipoFactor.TASA, "0.160000", importe)));
    }

    /** IVA 16 % plus a retention of ISR 10 % on a base of 100.00. */
    static Consumer<PagosBuilder.DoctoRelacionadoBuilder> conRetencionIsr() {
        return documento().andThen(d -> d.impPagado("106.00").impSaldoAnt("106.00").retencionDR("100.00", Impuesto.ISR, TipoFactor.TASA, "0.100000"));
    }

    static Consumer<PagosBuilder.PagoBuilder> pagoConRetencion() {
        return p -> pagoMxn(conRetencionIsr()).accept(p.monto("106.00"));
    }

    static final String CERT = "MIIBkjCB/AIJAKHBfpegPjMCMA0GCSqGSIb3DQEBCwUAMA0xCzAJBgNVBAYTAk1YMB4XDTI2MTExNDAwMDAwMFoXDTI3MTExNDAwMDAwMFow";

    // --- invalid cases ---

    static Stream<Caso> invalidos() {
        return Stream.of(
                recibo("CRP20101", "tipo I", b -> b.tipoDeComprobante(TipoDeComprobante.INGRESO)),
                recibo("CRP20102", "exportación temporal", b -> b.exportacion(Exportacion.TEMPORAL)),
                recibo("CRP20103", "SubTotal distinto de cero", b -> b.subTotal("1.00")),
                recibo("CRP20104", "moneda MXN", b -> b.moneda(Moneda.MXN)),
                recibo("CRP20105", "con FormaPago", b -> b.formaPago(FormaPago.EFECTIVO)),
                recibo("CRP20106", "con MetodoPago", b -> b.metodoPago(MetodoPago.PUE)),
                recibo("CRP20107", "con CondicionesDePago", b -> b.condicionesDePago("Contado")),
                recibo("CRP20108", "con Descuento", b -> b.descuento("0.00")),
                recibo("CRP20109", "con TipoCambio", b -> b.tipoCambio("1")),
                recibo("CRP20110", "Total distinto de cero", b -> b.total("1.00")),
                recibo("CRP20111", "dos conceptos", b -> b.concepto(PagosFixtures.conceptoPago())),
                concepto("CRP20112", "con CuentaPredial", c -> c.cuentaPredial("12345")),
                concepto("CRP20113", "otra clave", c -> c.claveProdServ("01010101")),
                concepto("CRP20114", "con NoIdentificacion", c -> c.noIdentificacion("X1")),
                concepto("CRP20115", "cantidad 2", c -> c.cantidad("2")),
                concepto("CRP20116", "otra unidad", c -> c.claveUnidad("H87")),
                concepto("CRP20117", "con Unidad", c -> c.unidad("Actividad")),
                concepto("CRP20118", "otra descripción", c -> c.descripcion("Abono")),
                concepto("CRP20119", "ValorUnitario 1", c -> c.valorUnitario("1")),
                concepto("CRP20120", "Importe 1", c -> c.importe("1")),
                concepto("CRP20121", "con Descuento", c -> c.descuento("0")),
                concepto("CRP20122", "objeto de impuesto", c -> c.objetoImp(ObjetoImp.SI_OBJETO)),
                concepto("CRP20123", "con impuestos", c -> c.objetoImp(ObjetoImp.SI_OBJETO).traslado(Impuesto.IVA, TipoFactor.TASA, "0.160000")),

                conTotales("CRP20201", "retenciones IVA sin retenciones", mapa("TotalRetencionesIVA", "1.00")),
                pagos("CRP20202", "retenciones ISR distintas", b -> b.pago(pagoConRetencion())
                        .totales(totales(mapa("TotalRetencionesISR", "11.00", "TotalTrasladosBaseIVA16", "100.00",
                                "TotalTrasladosImpuestoIVA16", "16.00", "MontoTotalPagos", "106.00")))),
                conTotales("CRP20203", "retenciones IEPS sin retenciones", mapa("TotalRetencionesIEPS", "1.00")),
                conTotales("CRP20204", "base IVA 16 distinta", mapa("TotalTrasladosBaseIVA16", "101.00")),
                conTotales("CRP20205", "IVA 16 distinto", mapa("TotalTrasladosImpuestoIVA16", "17.00")),
                conTotales("CRP20206", "base IVA 8 sin traslados", mapa("TotalTrasladosBaseIVA8", "1.00")),
                conTotales("CRP20207", "IVA 8 sin traslados", mapa("TotalTrasladosImpuestoIVA8", "1.00")),
                conTotales("CRP20208", "base IVA 0 sin traslados", mapa("TotalTrasladosBaseIVA0", "1.00")),
                conTotales("CRP20209", "IVA 0 sin traslados", mapa("TotalTrasladosImpuestoIVA0", "1.00")),
                conTotales("CRP20210", "base exenta sin exentos", mapa("TotalTrasladosBaseIVAExento", "1.00")),
                conTotales("CRP20211", "MontoTotalPagos distinto", mapa("MontoTotalPagos", "117.00")),

                pago("CRP20212", "forma 99", p -> p.formaDePagoP(FormaPago.POR_DEFINIR)),
                post("CRP20213", "moneda XXX", x -> primerPago(x, p -> con(p, "monedaP", Moneda.XXX))),
                post("CRP20214", "USD sin TipoCambioP", x -> primerPago(x, p -> con(con(p, "monedaP", Moneda.USD), "tipoCambioP", Optional.empty()))),
                pago("CRP20215", "MXN con TipoCambioP 2", p -> p.tipoCambioP("2")),
                pago("CRP20217", "pagado mayor que Monto", p -> p.monto("100.00")),
                pago("CRP20218", "Monto cero", p -> p.monto("0.00")),
                pago("CRP20219", "Monto con tres decimales", p -> p.monto("116.001")),
                pago("CRP20220", "Monto mayor que el máximo", p -> p.monto("1000000000000000000.00")),
                pago("CRP20221", "RfcEmisorCtaOrd en efectivo", p -> p.formaDePagoP(FormaPago.EFECTIVO).rfcEmisorCtaOrd("XEXX010101000").nomBancoOrdExt("BANCO")),
                new Caso("CRP20222", "RfcEmisorCtaOrd no inscrito", () -> PagosFixtures.con(Pagos.builder()
                        .pago(pagoMxn().andThen(p -> p.rfcEmisorCtaOrd("IIA040805DZ4"))).build()), PagosFixtures.conPadron()),
                pago("CRP20223", "extranjero sin NomBancoOrdExt", p -> p.rfcEmisorCtaOrd("XEXX010101000")),
                pago("CRP20224", "CtaOrdenante en efectivo", p -> p.formaDePagoP(FormaPago.EFECTIVO).ctaOrdenante("1234567890")),
                pago("CRP20225", "CtaOrdenante con letras", p -> p.ctaOrdenante("ABC")),
                pago("CRP20226", "CtaBeneficiario con letras", p -> p.ctaBeneficiario("ABC")),
                pago("CRP20227", "RfcEmisorCtaBen en efectivo", p -> p.formaDePagoP(FormaPago.EFECTIVO).rfcEmisorCtaBen("EKU9003173C9")),
                pago("CRP20228", "CtaBeneficiario en efectivo", p -> p.formaDePagoP(FormaPago.EFECTIVO).ctaBeneficiario("1234567890")),
                pago("CRP20229", "TipoCadPago en efectivo", p -> p.formaDePagoP(FormaPago.EFECTIVO).cadenaPago("01", CERT, "||cadena||", "c2VsbG8=")),
                pago("CRP20230", "TipoCadPago sin CertPago", p -> p.cadenaPago("01", null, "||cadena||", "c2VsbG8=")),
                pago("CRP20231", "CertPago sin TipoCadPago", p -> p.cadenaPago(null, CERT, null, null)),
                pago("CRP20232", "TipoCadPago sin CadPago", p -> p.cadenaPago("01", CERT, null, "c2VsbG8=")),
                pago("CRP20233", "CadPago sin TipoCadPago", p -> p.cadenaPago(null, null, "||cadena||", null)),
                pago("CRP20234", "TipoCadPago sin SelloPago", p -> p.cadenaPago("01", CERT, "||cadena||", null)),
                pago("CRP20235", "SelloPago sin TipoCadPago", p -> p.cadenaPago(null, null, null, "c2VsbG8=")),
                pagos("CRP20276", "Monto menor que el margen", b -> b.pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA)
                        .monedaP(Moneda.USD).tipoCambioP("17.7331").monto("19000.00")
                        .doctoRelacionado(d -> d.idDocumento(PagosFixtures.UUID).monedaDR(Moneda.MXN).equivalenciaDR("17.7331").numParcialidad(1)
                                .impSaldoAnt("348000.00").impPagado("348000.00").objetoImpDR(ObjetoImp.NO_OBJETO)))),

                post("CRP20236", "documento en XXX", x -> primerDocto(x, d -> con(d, "monedaDR", Moneda.XXX))),
                post("CRP20237", "USD sin EquivalenciaDR", x -> primerDocto(x, d -> con(con(d, "monedaDR", Moneda.USD), "equivalenciaDR", Optional.empty()))),
                pagos("CRP20238", "misma moneda con equivalencia 2", b -> b.pago(pagoMxn(documento().andThen(d -> d.equivalenciaDR("2"))))),
                docto("CRP20239", "saldo anterior cero", d -> d.impSaldoAnt("0").impPagado("1.00")),
                docto("CRP20240", "saldo anterior con tres decimales", d -> d.impSaldoAnt("116.001")),
                docto("CRP20241", "pagado cero", d -> d.impPagado("0")),
                docto("CRP20242", "pagado con tres decimales", d -> d.impPagado("115.999")),
                docto("CRP20243", "insoluto con tres decimales", d -> d.impSaldoInsoluto("0.001")),
                docto("CRP20244", "insoluto distinto", d -> d.impSaldoInsoluto("1.00")),
                pagos("CRP20245", "objeto fuera del catálogo", b -> b.pago(pagoMxn(documento().andThen(d -> d.objetoImpDR(ObjetoImp.of("99")))))),
                docto("CRP20246", "objeto 02 sin impuestos", d -> { }),
                pagos("CRP20247", "objeto 01 con impuestos", b -> b.pago(pagoMxn(documento().andThen(d -> d.objetoImpDR(ObjetoImp.NO_OBJETO))))),
                post("CRP20248", "ImpuestosDR vacío", x -> primerDocto(x, d -> con(d, "impuestosDR", Optional.of(new Pagos.ImpuestosDR(List.of(), List.of())))))
        );
    }

    static Stream<Caso> invalidosImpuestos() {
        return Stream.of(
                retencion("CRP20249", "base cero", "0", Impuesto.ISR, TipoFactor.TASA, "0.100000", null),
                retencion("CRP20250", "impuesto fuera del catálogo", "100.00", Impuesto.of("004"), TipoFactor.TASA, "0.100000", null),
                retencion("CRP20251", "factor fuera del catálogo", "100.00", Impuesto.ISR, TipoFactor.of("Otro"), "0.100000", null),
                retencion("CRP20252", "retención exenta", "100.00", Impuesto.ISR, TipoFactor.EXENTO, "0.100000", "10.00"),
                retencion("CRP20253", "tasa fuera de rango", "100.00", Impuesto.ISR, TipoFactor.TASA, "0.500000", null),
                retencion("CRP20254", "importe fuera de límites", "100.00", Impuesto.ISR, TipoFactor.TASA, "0.100000", "1.00"),
                traslado("CRP20255", "base cero", d -> d.trasladoDR("0", Impuesto.IVA, TipoFactor.TASA, "0.160000")),
                traslado("CRP20256", "impuesto fuera del catálogo", d -> d.trasladoDR("100.00", Impuesto.of("004"), TipoFactor.TASA, "0.160000")),
                traslado("CRP20257", "factor fuera del catálogo", d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.of("Otro"), "0.160000")),
                traslado("CRP20258", "exento con tasa", d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.EXENTO, "0.160000")),
                traslado("CRP20259", "tasa sin TasaOCuota", d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, null)),
                traslado("CRP20260", "tasa fuera del catálogo", d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.170000")),
                traslado("CRP20261", "importe fuera de límites", d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.160000", "1.00")),
                pagos("CRP20278", "objeto 05 con IVA", b -> b.pago(pagoMxn(documento().andThen(d -> d.objetoImpDR(ObjetoImp.SI_OBJETO_IVA_NO_TRASLADO))))),
                pagos("CRP20279", "objeto 07 con IVA y sin IEPS", b -> b.pago(pagoMxn(documento().andThen(d -> d.objetoImpDR(ObjetoImp.NO_TRASLADO_IVA_SI_DESGLOSE_IEPS))))),

                retencion("CRP20262", "RetencionP fuera del catálogo", "100.00", Impuesto.of("004"), TipoFactor.TASA, "0.100000", null),
                pago("CRP20263", "RetencionP duplicada", p -> p.impuestosP(new Pagos.ImpuestosP(
                        List.of(new Pagos.RetencionP(Impuesto.ISR, BigDecimal.TEN), new Pagos.RetencionP(Impuesto.ISR, BigDecimal.TEN)),
                        iva16("100.00", "16.00").trasladosP()))),
                pagos("CRP20264", "retenciones sin total", b -> b.pago(pagoConRetencion())
                        .totales(totales(mapa("TotalTrasladosBaseIVA16", "100.00", "TotalTrasladosImpuestoIVA16", "16.00", "MontoTotalPagos", "106.00")))),
                pagos("CRP20265", "RetencionP distinta", b -> b.pago(pagoConRetencion().andThen(p -> p.impuestosP(new Pagos.ImpuestosP(
                        List.of(new Pagos.RetencionP(Impuesto.ISR, new BigDecimal("11.00"))), iva16("100.00", "16.00").trasladosP()))))),
                pagos("CRP20266", "exento con tasa en TrasladoP", b -> b.pago(pagoMxn(documentoSinImpuestos()
                        .andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.EXENTO, null)))
                        .andThen(p -> p.impuestosP(new Pagos.ImpuestosP(List.of(),
                                List.of(trasladoP("100.00", Impuesto.IVA, TipoFactor.EXENTO, "0.160000", "16.00"))))))),
                conTotales("CRP20267", "traslados IVA sin bases", mapa("TotalTrasladosBaseIVA16", null)),
                pago("CRP20268", "BaseP distinta", p -> p.impuestosP(iva16("99.00", "16.00"))),
                pagos("CRP20269", "BaseP exenta distinta", b -> b.pago(pagoMxn(documentoSinImpuestos()
                        .andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.EXENTO, null)))
                        .andThen(p -> p.impuestosP(new Pagos.ImpuestosP(List.of(),
                                List.of(trasladoP("99.00", Impuesto.IVA, TipoFactor.EXENTO, null, null))))))),
                traslado("CRP20270", "TrasladoP fuera del catálogo", d -> d.trasladoDR("100.00", Impuesto.of("004"), TipoFactor.TASA, "0.160000")),
                pago("CRP20271", "TrasladoP duplicado", p -> p.impuestosP(new Pagos.ImpuestosP(List.of(), List.of(
                        trasladoP("100.00", Impuesto.IVA, TipoFactor.TASA, "0.160000", "16.00"),
                        trasladoP("100.00", Impuesto.IVA, TipoFactor.TASA, "0.16", "16.00"))))),
                traslado("CRP20272", "TasaOCuotaP fuera del catálogo", d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.170000")),
                conTotales("CRP20273", "traslados IVA sin importes", mapa("TotalTrasladosImpuestoIVA16", null)),
                pago("CRP20274", "ImporteP distinto", p -> p.impuestosP(iva16("100.00", "17.00"))));
    }

    static Caso retencion(String code, String nombre, String base, Impuesto impuesto, TipoFactor factor, String tasa, String importe) {
        return pagos(code, nombre, b -> b.pago(pagoMxn(documento().andThen(d -> d.retencionDR(base, impuesto, factor, tasa, importe)))));
    }

    static Caso traslado(String code, String nombre, Consumer<PagosBuilder.DoctoRelacionadoBuilder> m) {
        return docto(code, nombre, m);
    }

    static Stream<Caso> todosLosInvalidos() {
        return Stream.concat(invalidos(), invalidosImpuestos());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosLosInvalidos")
    void invalidCaseReportsItsCode(Caso caso) {
        ValidationResult result = caso.validar();

        assertThat(result.errors()).as("errors of %s", caso).anySatisfy(v -> assertThat(v.code()).isEqualTo(caso.code()));
    }

    // --- valid cases ---

    @Test
    void standardReciboIsValid() {
        ValidationResult result = PagosFixtures.validator().validate(PagosFixtures.valido());

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).allSatisfy(v -> assertThat(v.status()).isEqualTo(Status.NOT_VERIFIED));
        assertThat(result.warnings()).extracting(Violation::code).noneMatch(c -> c.startsWith("CRP"));
    }

    @Test
    void exampleOfTheGuiaDeLlenadoIsValid() {
        ValidationResult result = PagosFixtures.validator().validate(PagosFixtures.con(PagosFixtures.ejemploGuia()));

        assertThat(result.errors()).isEmpty();
    }

    static Stream<Caso> validos() {
        return Stream.of(
                pagos("CRP20202", "retención ISR derivada", b -> b.pago(pagoConRetencion())),
                pagos("CRP20211", "dos pagos en monedas distintas", b -> b.pago(pagoMxn())
                        .pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.USD)
                                .tipoCambioP("17.7331").monto("19624.32")
                                .doctoRelacionado(d -> d.idDocumento(PagosFixtures.UUID).monedaDR(Moneda.MXN).equivalenciaDR("17.7331")
                                        .numParcialidad(2).impSaldoAnt("348000.00").impPagado("348000.00").objetoImpDR(ObjetoImp.SI_OBJETO)
                                        .trasladoDR("300000.00", Impuesto.IVA, TipoFactor.TASA, "0.160000")))),
                new Caso("CRP20222", "RfcEmisorCtaOrd inscrito", () -> PagosFixtures.con(Pagos.builder()
                        .pago(pagoMxn().andThen(p -> p.rfcEmisorCtaOrd("XIA190128J61"))).build()), PagosFixtures.conPadron()),
                pago("CRP20223", "extranjero con NomBancoOrdExt", p -> p.rfcEmisorCtaOrd("XEXX010101000").nomBancoOrdExt("BANK OF TESTS")),
                pago("CRP20225", "CLABE de 18 dígitos", p -> p.ctaOrdenante("012345678901234567").ctaBeneficiario("0123456789")),
                pago("CRP20230", "cadena de pago completa", p -> p.cadenaPago("01", CERT, "||cadena||", "c2VsbG8=")),
                pagos("CRP20244", "pago parcial", b -> b.pago(pagoMxn(documento().andThen(d -> d.impPagado("50.00"))).andThen(p -> p.monto("50.00")))),
                pagos("CRP20266", "solo exentos", b -> b.pago(pagoMxn(documentoSinImpuestos()
                        .andThen(d -> d.trasladoDR("100.00", Impuesto.IVA, TipoFactor.EXENTO, null))))),
                pagos("CRP20279", "objeto 07 con IEPS", b -> b.pago(pagoMxn(documentoSinImpuestos().andThen(d -> d.impPagado("108.00").impSaldoAnt("108.00")
                        .objetoImpDR(ObjetoImp.NO_TRASLADO_IVA_SI_DESGLOSE_IEPS).trasladoDR("100.00", Impuesto.IEPS, TipoFactor.TASA, "0.080000")))
                        .andThen(p -> p.monto("108.00")))),
                new Caso("CRP20276", "ejemplo de la Guía", () -> PagosFixtures.con(PagosFixtures.ejemploGuia()), PagosFixtures.validator()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validos")
    void validCaseHasNoErrors(Caso caso) {
        assertThat(caso.validar().errors()).as("errors of %s", caso).isEmpty();
    }

    @Test
    void rulesDoNotApplyWithoutTheComplement() {
        Comprobante sinComplemento = PagosFixtures.recibo().build();

        assertThat(PagosFixtures.validator().validate(sinComplemento).violations()).extracting(Violation::code)
                .noneMatch(c -> c.startsWith("CRP"));
    }

    @Test
    void unknownRfcIsNotVerifiedWithoutRegistry() {
        Comprobante c = PagosFixtures.con(Pagos.builder().pago(pagoMxn().andThen(p -> p.rfcEmisorCtaOrd("IIA040805DZ4"))).build());

        assertThat(PagosFixtures.validator().validate(c).warnings()).anySatisfy(v -> {
            assertThat(v.code()).isEqualTo("CRP20222");
            assertThat(v.status()).isEqualTo(Status.NOT_VERIFIED);
        });
    }

    @Test
    void messagesUseTheOfficialDescription() {
        ValidationResult result = PagosFixtures.validator().validate(PagosFixtures.con(Pagos.builder().pago(pagoMxn().andThen(p -> p.monto("0.00"))).build()));

        assertThat(result.errors()).filteredOn(v -> v.code().equals("CRP20218")).singleElement()
                .satisfies(v -> assertThat(v.message()).startsWith(MatrizErroresPagos20.entradas().stream().filter(e -> e.codigo().equals("CRP20218")).findFirst().orElseThrow().mensaje()));
    }

    // --- coverage of the rules themselves ---

    /** Codes documented without a check, and CRP20275, which cannot fail for a value it computes itself. */
    private static final Set<String> SIN_CASO_INVALIDO = Set.of("CRP20216", "CRP20275", "CRP20277", "CRP20999");

    private static List<Rule> reglas() {
        return new Pagos20RuleSet().rules();
    }

    @Test
    void ruleSetIsDiscoveredByTheValidator() {
        assertThat(PagosFixtures.validator().rules()).containsAll(reglas());
    }

    @Test
    void everyCodeOfTheMatrixHasExactlyOneRule() {
        List<String> codes = reglas().stream().map(Rule::code).toList();

        assertThat(codes).doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(MatrizErroresPagos20.entradas().stream().map(e -> e.codigo()).toList());
    }

    @Test
    void everyRuleThatChecksSomethingHasAnInvalidCase() {
        Set<String> probados = todosLosInvalidos().map(Caso::code).collect(Collectors.toCollection(TreeSet::new));
        Set<String> esperados = reglas().stream().map(Rule::code).filter(c -> !SIN_CASO_INVALIDO.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(probados).containsAll(esperados);
    }
}
