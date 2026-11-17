package io.github.alktrip.cfdi4j.core.validation;

import static io.github.alktrip.cfdi4j.core.validation.RuleFixtures.CSD;
import static io.github.alktrip.cfdi4j.core.validation.RuleFixtures.base;
import static io.github.alktrip.cfdi4j.core.validation.RuleFixtures.servicio;
import static io.github.alktrip.cfdi4j.core.validation.RuleFixtures.valido;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.CertificateType;
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
import io.github.alktrip.cfdi4j.core.model.ComplementoNoModelado;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import io.github.alktrip.cfdi4j.core.model.Impuestos;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

/**
 * One invalid case at least for every rule that checks something, and valid cases: the base
 * comprobante (valid for every rule) and specific valid variants of conditional rules.
 */
class RulesTest {

    record Caso(String code, String nombre, Supplier<Comprobante> comprobante, Cfdi40Validator validator, CertificateInfo certificado) {
        @Override
        public String toString() {
            return code + " " + nombre;
        }

        ValidationResult validar() {
            return certificado == null ? validator.validate(comprobante.get()) : validator.validate(comprobante.get(), certificado);
        }
    }

    // --- case builders ---

    static Caso caso(String code, String nombre, Consumer<ComprobanteBuilder> m) {
        return new Caso(code, nombre, () -> {
            ComprobanteBuilder b = base();
            m.accept(b);
            return b.build();
        }, RuleFixtures.sinPuertos(), null);
    }

    static Caso conPuertos(String code, String nombre, Consumer<ComprobanteBuilder> m) {
        Caso c = caso(code, nombre, m);
        return new Caso(code, nombre, c.comprobante(), RuleFixtures.conPuertos(), CSD);
    }

    static Caso post(String code, String nombre, Consumer<ComprobanteBuilder> m, UnaryOperator<Comprobante> f) {
        Caso c = caso(code, nombre, m);
        return new Caso(code, nombre, () -> f.apply(c.comprobante().get()), c.validator(), null);
    }

    static Caso certificado(String code, String nombre, CertificateInfo info, Cfdi40Validator validator) {
        return new Caso(code, nombre, () -> valido().build(), validator, info);
    }

    static Consumer<ComprobanteBuilder> conConcepto(Consumer<ComprobanteBuilder.ConceptoBuilder> extra) {
        return b -> b.concepto(c -> {
            c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio").valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO);
            extra.accept(c);
        });
    }

    static Consumer<ComprobanteBuilder.ConceptoBuilder> sinImpuestos() {
        return c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio").valorUnitario("100.00")
                .objetoImp(ObjetoImp.NO_OBJETO);
    }

    static Consumer<ComprobanteBuilder.ReceptorBuilder> receptor(String rfc, String nombre, String cp, RegimenFiscal regimen, UsoCFDI uso) {
        return r -> r.rfc(rfc).nombre(nombre).domicilioFiscalReceptor(cp).regimenFiscalReceptor(regimen).usoCFDI(uso);
    }

    static Consumer<ComprobanteBuilder.ReceptorBuilder> publicoEnGeneral(String cp) {
        return receptor("XAXX010101000", "PUBLICO EN GENERAL", cp, RegimenFiscal.SIN_OBLIGACIONES_FISCALES, UsoCFDI.SIN_EFECTOS_FISCALES);
    }

    static Consumer<ComprobanteBuilder.ReceptorBuilder> extranjero(Consumer<ComprobanteBuilder.ReceptorBuilder> extra) {
        return r -> {
            receptor("XEXX010101000", "CLIENTE EXTRANJERO", "72000", RegimenFiscal.SIN_OBLIGACIONES_FISCALES, UsoCFDI.SIN_EFECTOS_FISCALES).accept(r);
            extra.accept(r);
        };
    }

    static ComplementoNoModelado comercioExterior() {
        return new ComplementoNoModelado("http://www.sat.gob.mx/ComercioExterior20",
                "<cce20:ComercioExterior xmlns:cce20=\"http://www.sat.gob.mx/ComercioExterior20\" Version=\"2.0\"/>", Optional.empty());
    }

    static Impuestos resumen(String tir, String tit, List<Impuestos.Retencion> retenciones, List<Impuestos.Traslado> traslados) {
        return new Impuestos(Optional.ofNullable(tir).map(BigDecimal::new), Optional.ofNullable(tit).map(BigDecimal::new), retenciones, traslados);
    }

    static Impuestos.Traslado iva(String base, String tasa, String importe) {
        return new Impuestos.Traslado(new BigDecimal(base), Impuesto.IVA, TipoFactor.TASA, Optional.ofNullable(tasa).map(BigDecimal::new),
                Optional.ofNullable(importe).map(BigDecimal::new));
    }

    static Impuestos.Retencion retencion(Impuesto impuesto, String importe) {
        return new Impuestos.Retencion(impuesto, new BigDecimal(importe));
    }

    static final Impuestos IVA_100 = resumen(null, "16.00", List.of(), List.of(iva("100.00", "0.160000", "16.00")));

    // --- invalid cases ---

    static Stream<Caso> invalidos() {
        List<Caso> casos = new ArrayList<>(List.of(
                caso("CFDI40101", "año fuera del patrón", b -> b.fecha(LocalDateTime.of(2009, 1, 1, 0, 0)).concepto(servicio())),
                caso("CFDI40103", "FormaPago en traslado", b -> b.metodoPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO).concepto(sinImpuestos())),
                caso("CFDI40104", "FormaPago fuera del catálogo", b -> b.formaPago(FormaPago.of("07")).concepto(servicio())),
                caso("CFDI40105", "PPD sin 99", b -> b.metodoPago(MetodoPago.PPD).concepto(servicio())),
                caso("CFDI40107", "SubTotal con 3 decimales", b -> b.subTotal("100.001").concepto(servicio())),
                caso("CFDI40108", "SubTotal distinto", b -> b.subTotal("90.00").concepto(servicio())),
                caso("CFDI40109", "SubTotal en traslado", b -> b.formaPago(null).metodoPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO)
                        .subTotal("1.00").concepto(sinImpuestos())),
                caso("CFDI40110", "Descuento mayor que SubTotal", b -> b.descuento("200.00").concepto(servicio())),
                caso("CFDI40111", "Descuento distinto de la suma", b -> b.descuento("5.00").concepto(servicio().andThen(c -> c.descuento("10.00")))),
                caso("CFDI40111", "Descuento sin descuentos en conceptos", b -> b.descuento("5.00").concepto(servicio())),
                post("CFDI40111", "Descuento faltante", b -> b.concepto(servicio().andThen(c -> c.descuento("10.00"))), RuleFixtures::sinDescuento),
                caso("CFDI40112", "Descuento con 3 decimales", b -> b.descuento("1.001").concepto(servicio().andThen(c -> c.descuento("1.00")))),
                post("CFDI40113", "Moneda fuera del catálogo", b -> b.concepto(servicio()), c -> RuleFixtures.conMoneda(c, Moneda.of("ABC"))),
                caso("CFDI40114", "TipoCambio de MXN distinto de 1", b -> b.tipoCambio("2").concepto(servicio())),
                caso("CFDI40115", "USD sin TipoCambio", b -> b.moneda(Moneda.USD).concepto(servicio())),
                caso("CFDI40116", "XXX con TipoCambio", b -> b.moneda(Moneda.XXX).tipoCambio("1").concepto(servicio())),
                caso("CFDI40117", "TipoCambio con 7 decimales", b -> b.moneda(Moneda.USD).tipoCambio("17.1234567").concepto(servicio())),
                caso("CFDI40119", "Total distinto", b -> b.total("120.00").concepto(servicio())),
                caso("CFDI40120", "Total de traslado sobre el máximo", b -> b.formaPago(null).metodoPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO)
                        .total("1.00").concepto(sinImpuestos())),
                caso("CFDI40121", "TipoDeComprobante fuera del catálogo", b -> b.tipoDeComprobante(TipoDeComprobante.of("X")).concepto(servicio())),
                caso("CFDI40123", "Exportacion fuera del catálogo", b -> b.exportacion(Exportacion.of("09")).concepto(servicio())),
                caso("CFDI40124", "MetodoPago fuera del catálogo", b -> b.metodoPago(MetodoPago.of("PIP")).concepto(servicio())),
                caso("CFDI40125", "MetodoPago en traslado", b -> b.formaPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO).concepto(sinImpuestos())),
                conPuertos("CFDI40126", "LugarExpedicion fuera del catálogo", b -> b.lugarExpedicion("99999").concepto(servicio())),
                caso("CFDI40130", "factura global sin InformacionGlobal", b -> b.receptor(publicoEnGeneral("72000")).concepto(servicio())),
                caso("CFDI40131", "Periodicidad fuera del catálogo", b -> b.informacionGlobal(Periodicidad.of("09"), Meses.NOVIEMBRE, 2026).concepto(servicio())),
                caso("CFDI40132", "bimestral fuera del régimen 621", b -> b.informacionGlobal(Periodicidad.BIMESTRAL, Meses.SEPTIEMBRE_OCTUBRE, 2026).concepto(servicio())),
                caso("CFDI40133", "Meses fuera del catálogo", b -> b.informacionGlobal(Periodicidad.MENSUAL, Meses.of("30"), 2026).concepto(servicio())),
                caso("CFDI40134", "mensual con bimestre", b -> b.informacionGlobal(Periodicidad.MENSUAL, Meses.ENERO_FEBRERO, 2026).concepto(servicio())),
                caso("CFDI40135", "bimestral con mes", b -> b.emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE")
                        .regimenFiscal(RegimenFiscal.INCORPORACION_FISCAL)).informacionGlobal(Periodicidad.BIMESTRAL, Meses.NOVIEMBRE, 2026).concepto(servicio())),
                caso("CFDI40136", "Año anterior a 5 ejercicios", b -> b.informacionGlobal(Periodicidad.MENSUAL, Meses.NOVIEMBRE, 2020).concepto(servicio())),
                caso("CFDI40136", "Año futuro", b -> b.informacionGlobal(Periodicidad.MENSUAL, Meses.NOVIEMBRE, 2027).concepto(servicio())),
                caso("CFDI40137", "TipoRelacion fuera del catálogo", b -> b.cfdiRelacionados(TipoRelacion.of("99"), "5FB2822E-396D-4725-8521-CDC4BDD20CCF")
                        .concepto(servicio())),
                conPuertos("CFDI40138", "emisor no inscrito", b -> b.emisor(e -> e.rfc("IIA040805DZ4").nombre("X").regimenFiscal(RegimenFiscal.GENERAL_PM))
                        .concepto(servicio())),
                conPuertos("CFDI40139", "nombre del emisor distinto", b -> b.emisor(e -> e.rfc("EKU9003173C9").nombre("OTRO NOMBRE")
                        .regimenFiscal(RegimenFiscal.GENERAL_PM)).concepto(servicio())),
                caso("CFDI40140", "RegimenFiscal fuera del catálogo", b -> b.emisor(e -> e.rfc("EKU9003173C9").nombre("E").regimenFiscal(RegimenFiscal.of("999")))
                        .concepto(servicio())),
                caso("CFDI40141", "régimen de persona física en persona moral", b -> b.emisor(e -> e.rfc("EKU9003173C9").nombre("E")
                        .regimenFiscal(RegimenFiscal.SUELDOS_SALARIOS)).concepto(servicio())),
                conPuertos("CFDI40143", "receptor no inscrito", b -> b.receptor(receptor("IVD920810GU2", "X", "76343", RegimenFiscal.GENERAL_PM,
                        UsoCFDI.GASTOS_EN_GENERAL)).concepto(servicio())),
                conPuertos("CFDI40144", "receptor no inscrito", b -> b.receptor(receptor("IVD920810GU2", "X", "76343", RegimenFiscal.GENERAL_PM,
                        UsoCFDI.GASTOS_EN_GENERAL)).concepto(servicio())),
                conPuertos("CFDI40145", "nombre del receptor distinto", b -> b.receptor(receptor("XIA190128J61", "OTRO", "76343", RegimenFiscal.GENERAL_PM,
                        UsoCFDI.GASTOS_EN_GENERAL)).concepto(servicio())),
                caso("CFDI40146", "PUBLICO EN GENERAL con otro RFC", b -> b.receptor(receptor("XIA190128J61", "PUBLICO EN GENERAL", "76343",
                        RegimenFiscal.GENERAL_PM, UsoCFDI.GASTOS_EN_GENERAL)).concepto(servicio())),
                conPuertos("CFDI40147", "receptor no inscrito", b -> b.receptor(receptor("IVD920810GU2", "X", "76343", RegimenFiscal.GENERAL_PM,
                        UsoCFDI.GASTOS_EN_GENERAL)).concepto(servicio())),
                conPuertos("CFDI40148", "domicilio del receptor distinto", b -> b.receptor(receptor("XIA190128J61", "XENON INDUSTRIAL ARTICLES", "01000",
                        RegimenFiscal.GENERAL_PM, UsoCFDI.GASTOS_EN_GENERAL)).concepto(servicio())),
                caso("CFDI40149", "genérico con otro domicilio", b -> b.receptor(publicoEnGeneral("01000"))
                        .informacionGlobal(Periodicidad.MENSUAL, Meses.NOVIEMBRE, 2026).concepto(servicio())),
                caso("CFDI40150", "ResidenciaFiscal fuera del catálogo", b -> b.receptor(extranjero(r -> r.residenciaFiscal(Pais.of("ZZX")))).concepto(servicio())),
                caso("CFDI40151", "ResidenciaFiscal con RFC nacional", b -> b.receptor(r -> receptor("XIA190128J61", "XENON INDUSTRIAL ARTICLES", "76343",
                        RegimenFiscal.GENERAL_PM, UsoCFDI.GASTOS_EN_GENERAL).andThen(x -> x.residenciaFiscal(Pais.USA)).accept(r)).concepto(servicio())),
                caso("CFDI40152", "ResidenciaFiscal MEX", b -> b.receptor(extranjero(r -> r.residenciaFiscal(Pais.MEX))).concepto(servicio())),
                caso("CFDI40153", "NumRegIdTrib sin ResidenciaFiscal", b -> b.receptor(extranjero(r -> r.numRegIdTrib("123456789"))).concepto(servicio())),
                caso("CFDI40154", "NumRegIdTrib con RFC nacional", b -> b.receptor(r -> receptor("XIA190128J61", "XENON INDUSTRIAL ARTICLES", "76343",
                        RegimenFiscal.GENERAL_PM, UsoCFDI.GASTOS_EN_GENERAL).andThen(x -> x.numRegIdTrib("1")).accept(r)).concepto(servicio())),
                caso("CFDI40155", "comercio exterior sin NumRegIdTrib", b -> b.receptor(extranjero(r -> r.residenciaFiscal(Pais.USA)))
                        .exportacion(Exportacion.DEFINITIVA_A1).complemento(comercioExterior()).concepto(servicio())),
                caso("CFDI40156", "NumRegIdTrib fuera del patrón", b -> b.receptor(extranjero(r -> r.residenciaFiscal(Pais.USA).numRegIdTrib("ABC")))
                        .concepto(servicio())),
                caso("CFDI40157", "RegimenFiscalReceptor fuera del catálogo", b -> b.receptor(receptor("XIA190128J61", "X", "76343", RegimenFiscal.of("999"),
                        UsoCFDI.GASTOS_EN_GENERAL)).concepto(servicio())),
                caso("CFDI40158", "régimen de persona física en receptor moral", b -> b.receptor(receptor("XIA190128J61", "X", "76343",
                        RegimenFiscal.SUELDOS_SALARIOS, UsoCFDI.NOMINA)).concepto(servicio())),
                caso("CFDI40159", "genérico sin 616", b -> b.receptor(receptor("XAXX010101000", "VENTAS", "72000", RegimenFiscal.GENERAL_PM,
                        UsoCFDI.SIN_EFECTOS_FISCALES)).concepto(servicio())),
                caso("CFDI40160", "UsoCFDI fuera del catálogo", b -> b.receptor(receptor("XIA190128J61", "X", "76343", RegimenFiscal.GENERAL_PM,
                        UsoCFDI.of("Z99"))).concepto(servicio())),
                caso("CFDI40161", "uso de persona física en persona moral", b -> b.receptor(receptor("XIA190128J61", "X", "76343", RegimenFiscal.GENERAL_PM,
                        UsoCFDI.HONORARIOS_MEDICOS)).concepto(servicio())),
                conPuertos("CFDI40162", "ClaveProdServ fuera del catálogo", conConcepto(c -> c.claveProdServ("99999999").trasladoIva16())),
                conPuertos("CFDI40164", "falta el IEPS que exige el catálogo", conConcepto(c -> c.claveProdServ("50202306").trasladoIva16())),
                conPuertos("CFDI40165", "ClaveUnidad fuera del catálogo", conConcepto(c -> c.claveUnidad("ZZZ").trasladoIva16())),
                caso("CFDI40166", "ValorUnitario cero en ingreso", conConcepto(c -> c.valorUnitario("0").objetoImp(ObjetoImp.NO_OBJETO))),
                // With Cantidad="1" the Anexo 20 tolerance is half a unit (10^0 / 2), so 150.00 would still be within limits.
                caso("CFDI40167", "Importe fuera de límites", conConcepto(c -> c.cantidad("1.000000").importe("150.00").trasladoIva16())),
                caso("CFDI40168", "Descuento con más decimales que el Importe", conConcepto(c -> c.descuento("1.001").trasladoIva16())),
                caso("CFDI40169", "Descuento mayor que el Importe", conConcepto(c -> c.descuento("200.00").objetoImp(ObjetoImp.NO_OBJETO))),
                caso("CFDI40170", "ObjetoImp fuera del catálogo", conConcepto(c -> c.objetoImp(ObjetoImp.of("99")))),
                caso("CFDI40171", "objeto de impuesto sin Impuestos", conConcepto(c -> { })),
                caso("CFDI40172", "no objeto con Impuestos", conConcepto(c -> c.objetoImp(ObjetoImp.NO_OBJETO).trasladoIva16())),
                post("CFDI40173", "Impuestos vacío", b -> b.concepto(servicio()), c -> RuleFixtures.conConceptos(c, List.of(
                        RuleFixtures.conImpuestos(c.conceptos().get(0), Optional.of(new Concepto.Impuestos(List.of(), List.of())))))),
                caso("CFDI40174", "Base de traslado cero", conConcepto(c -> c.traslado(t -> t.base("0").impuesto(Impuesto.IVA).tipoFactor(TipoFactor.TASA)
                        .tasaOCuota("0.160000")))),
                caso("CFDI40175", "Impuesto de traslado fuera del catálogo", conConcepto(c -> c.traslado(Impuesto.of("009"), TipoFactor.TASA, "0.160000"))),
                caso("CFDI40176", "TipoFactor de traslado fuera del catálogo", conConcepto(c -> c.traslado(Impuesto.IVA, TipoFactor.of("Otro"), "0.160000"))),
                caso("CFDI40177", "exento con tasa", conConcepto(c -> c.traslado(t -> t.impuesto(Impuesto.IVA).tipoFactor(TipoFactor.EXENTO)
                        .tasaOCuota("0.160000").importe("16.00")))),
                caso("CFDI40178", "tasa sin TasaOCuota", conConcepto(c -> c.traslado(t -> t.impuesto(Impuesto.IVA).tipoFactor(TipoFactor.TASA)))),
                caso("CFDI40179", "TasaOCuota de traslado fuera del catálogo", conConcepto(c -> c.traslado(Impuesto.IVA, TipoFactor.TASA, "0.170000"))),
                caso("CFDI40180", "Importe de traslado fuera de límites", conConcepto(c -> c.traslado(t -> t.impuesto(Impuesto.IVA).tipoFactor(TipoFactor.TASA)
                        .tasaOCuota("0.160000").importe("20.00")))),
                caso("CFDI40181", "Base de retención cero", conConcepto(c -> c.trasladoIva16().retencion(r -> r.base("0").impuesto(Impuesto.ISR)
                        .tipoFactor(TipoFactor.TASA).tasaOCuota("0.100000")))),
                caso("CFDI40182", "Impuesto de retención fuera del catálogo", conConcepto(c -> c.trasladoIva16().retencion(Impuesto.of("009"), TipoFactor.TASA, "0.100000"))),
                caso("CFDI40183", "TipoFactor de retención fuera del catálogo", conConcepto(c -> c.trasladoIva16().retencion(Impuesto.ISR, TipoFactor.of("X"), "0.100000"))),
                caso("CFDI40184", "retención exenta", conConcepto(c -> c.trasladoIva16().retencion(Impuesto.ISR, TipoFactor.EXENTO, "0.100000"))),
                caso("CFDI40185", "TasaOCuota de retención fuera del catálogo", conConcepto(c -> c.trasladoIva16().retencion(Impuesto.ISR, TipoFactor.TASA, "0.400000"))),
                caso("CFDI40186", "Importe de retención fuera de límites", conConcepto(c -> c.trasladoIva16().retencion(r -> r.impuesto(Impuesto.ISR)
                        .tipoFactor(TipoFactor.TASA).tasaOCuota("0.100000").importe("50.00")))),
                conPuertos("CFDI40187", "tercero no inscrito", conConcepto(c -> c.trasladoIva16().aCuentaTerceros("IIA040805DZ4", "X", RegimenFiscal.GENERAL_PM, "01000"))),
                caso("CFDI40188", "tercero igual al emisor", conConcepto(c -> c.trasladoIva16().aCuentaTerceros("EKU9003173C9", "X", RegimenFiscal.GENERAL_PM, "72000"))),
                caso("CFDI40189", "tercero extranjero sin la descripción", conConcepto(c -> c.trasladoIva16()
                        .aCuentaTerceros("EXT990101NI1", "OTRO", RegimenFiscal.SIN_OBLIGACIONES_FISCALES, "72000"))),
                conPuertos("CFDI40189", "tercero no inscrito", conConcepto(c -> c.trasladoIva16().aCuentaTerceros("IIA040805DZ4", "X", RegimenFiscal.GENERAL_PM, "01000"))),
                conPuertos("CFDI40190", "nombre del tercero distinto", conConcepto(c -> c.trasladoIva16()
                        .aCuentaTerceros("CACX7605101P8", "OTRO", RegimenFiscal.ACTIVIDADES_EMPRESARIALES_PF, "36257"))),
                caso("CFDI40191", "régimen del tercero fuera del catálogo", conConcepto(c -> c.trasladoIva16()
                        .aCuentaTerceros("CACX7605101P8", "X", RegimenFiscal.of("999"), "36257"))),
                conPuertos("CFDI40192", "tercero no inscrito", conConcepto(c -> c.trasladoIva16().aCuentaTerceros("IIA040805DZ4", "X", RegimenFiscal.GENERAL_PM, "01000"))),
                caso("CFDI40193", "tercero extranjero con otro domicilio", conConcepto(c -> c.trasladoIva16()
                        .aCuentaTerceros("EXT990101NI1", RuleFixtures.TERCERO_EXTRANJERO, RegimenFiscal.SIN_OBLIGACIONES_FISCALES, "01000"))),
                conPuertos("CFDI40193", "domicilio del tercero distinto", conConcepto(c -> c.trasladoIva16()
                        .aCuentaTerceros("CACX7605101P8", "CONTRIBUYENTE DE PRUEBA UNO", RegimenFiscal.ACTIVIDADES_EMPRESARIALES_PF, "01000"))),
                caso("CFDI140223", "tercero extranjero sin 616", conConcepto(c -> c.trasladoIva16()
                        .aCuentaTerceros("EXT990101NI1", RuleFixtures.TERCERO_EXTRANJERO, RegimenFiscal.GENERAL_PM, "72000"))),
                caso("CFDI40194", "pedimento mal formado", conConcepto(c -> c.trasladoIva16().informacionAduanera("2147380780038322"))),
                caso("CFDI40195", "pedimento con comercio exterior", b -> conConcepto(c -> c.trasladoIva16().informacionAduanera("21  47  3807  8003832"))
                        .accept(b.exportacion(Exportacion.DEFINITIVA_A1).complemento(comercioExterior()))),
                conPuertos("CFDI40196", "ClaveProdServ de Parte fuera del catálogo", conConcepto(c -> c.trasladoIva16()
                        .parte(p -> p.claveProdServ("99999999").cantidad("1").descripcion("Parte")))),
                caso("CFDI40197", "ValorUnitario de Parte cero", conConcepto(c -> c.trasladoIva16()
                        .parte(p -> p.claveProdServ("01010101").cantidad("1").descripcion("Parte").valorUnitario("0")))),
                caso("CFDI40198", "Importe de Parte fuera de límites", conConcepto(c -> c.trasladoIva16()
                        .parte(p -> p.claveProdServ("01010101").cantidad("2").descripcion("Parte").valorUnitario("5.00").importe("50.00")))),
                caso("CFDI40199", "pedimento de Parte mal formado", conConcepto(c -> c.trasladoIva16()
                        .parte(p -> p.claveProdServ("01010101").cantidad("1").descripcion("Parte").informacionAduanera("123")))),
                caso("CFDI40200", "pedimento de Parte con comercio exterior", b -> conConcepto(c -> c.trasladoIva16()
                        .parte(p -> p.claveProdServ("01010101").cantidad("1").descripcion("Parte").informacionAduanera("21  47  3807  8003832")))
                        .accept(b.exportacion(Exportacion.DEFINITIVA_A1).complemento(comercioExterior()))),
                conPuertos("CFDI140224", "RESICO con marca 1 y retención de ISR", b -> b.emisor(e -> e.rfc("CACX7605101P8").nombre("CONTRIBUYENTE DE PRUEBA UNO")
                        .regimenFiscal(RegimenFiscal.RESICO)).concepto(servicio().andThen(c -> c.retencion(Impuesto.ISR, TipoFactor.TASA, "0.012500"))))
                ,
                conPuertos("CFDI140225", "RESICO con marca 2 sin retención de ISR", b -> b.emisor(e -> e.rfc("FUNK671228PH6").nombre("CONTRIBUYENTE DE PRUEBA DOS")
                        .regimenFiscal(RegimenFiscal.RESICO)).concepto(servicio())),
                caso("CFDI140226", "objeto 06 con IVA", conConcepto(c -> c.objetoImp(ObjetoImp.SI_OBJETO_IVA_NO_TRASLADO).trasladoIva16())),
                caso("CFDI140227", "objeto 07 con IVA y sin IEPS", conConcepto(c -> c.objetoImp(ObjetoImp.NO_TRASLADO_IVA_SI_DESGLOSE_IEPS).trasladoIva16())),
                caso("CFDI140228", "hidrocarburo sin complemento", conConcepto(c -> c.claveProdServ("15101505").trasladoIva16())),
                caso("CFDI40201", "Impuestos en traslado", b -> b.formaPago(null).metodoPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO)
                        .impuestos(IVA_100).concepto(sinImpuestos())),
                caso("CFDI40202", "TotalImpuestosRetenidos con 3 decimales", b -> b.impuestos(resumen("1.001", "16.00",
                        List.of(retencion(Impuesto.ISR, "1.001")), IVA_100.traslados())).concepto(servicio())),
                caso("CFDI40203", "TotalImpuestosRetenidos distinto de la suma", b -> b.impuestos(resumen("5.00", "16.00",
                        List.of(retencion(Impuesto.ISR, "10.00")), IVA_100.traslados())).concepto(servicio())),
                caso("CFDI40204", "TotalImpuestosTrasladados con 3 decimales", b -> b.impuestos(resumen(null, "16.001", List.of(), IVA_100.traslados()))
                        .concepto(servicio())),
                caso("CFDI40205", "TotalImpuestosTrasladados distinto de la suma", b -> b.impuestos(resumen(null, "15.00", List.of(), IVA_100.traslados()))
                        .concepto(servicio())),
                caso("CFDI40206", "Retenciones sin total", b -> b.impuestos(resumen(null, "16.00", List.of(retencion(Impuesto.ISR, "10.00")), IVA_100.traslados()))
                        .concepto(servicio())),
                caso("CFDI40207", "Impuesto retenido fuera del catálogo", b -> b.impuestos(resumen("10.00", "16.00",
                        List.of(retencion(Impuesto.of("009"), "10.00")), IVA_100.traslados())).concepto(servicio())),
                caso("CFDI40208", "retención repetida", b -> b.impuestos(resumen("20.00", "16.00",
                        List.of(retencion(Impuesto.ISR, "10.00"), retencion(Impuesto.ISR, "10.00")), IVA_100.traslados())).concepto(servicio())),
                caso("CFDI40210", "Importe retenido con 3 decimales", b -> b.impuestos(resumen("1.001", "16.00",
                        List.of(retencion(Impuesto.ISR, "1.001")), IVA_100.traslados())).concepto(servicio())),
                caso("CFDI40211", "Importe retenido distinto de los conceptos", b -> b.impuestos(resumen("9.00", "16.00",
                        List.of(retencion(Impuesto.ISR, "9.00")), IVA_100.traslados()))
                        .concepto(servicio().andThen(c -> c.retencion(Impuesto.ISR, TipoFactor.TASA, "0.100000")))),
                caso("CFDI40212", "traslados sin total", b -> b.impuestos(resumen(null, null, List.of(), IVA_100.traslados())).concepto(servicio())),
                caso("CFDI40213", "solo exentos con tasa en el resumen", b -> b.impuestos(resumen(null, null, List.of(), List.of(
                        new Impuestos.Traslado(new BigDecimal("100.00"), Impuesto.IVA, TipoFactor.EXENTO, Optional.of(new BigDecimal("0.160000")),
                                Optional.of(new BigDecimal("16.00"))))))
                        .concepto(sinImpuestos().andThen(c -> c.objetoImp(ObjetoImp.SI_OBJETO).trasladoExento(Impuesto.IVA)))),
                caso("CFDI40214", "Base del resumen con 3 decimales", b -> b.impuestos(resumen(null, "16.00", List.of(), List.of(iva("100.001", "0.160000", "16.00"))))
                        .concepto(servicio())),
                caso("CFDI40215", "Base del resumen distinta", b -> b.impuestos(resumen(null, "16.00", List.of(), List.of(iva("90.00", "0.160000", "16.00"))))
                        .concepto(servicio())),
                caso("CFDI40216", "Base exenta distinta", b -> b.impuestos(resumen(null, null, List.of(), List.of(
                        new Impuestos.Traslado(new BigDecimal("90.00"), Impuesto.IVA, TipoFactor.EXENTO, Optional.empty(), Optional.empty()))))
                        .concepto(sinImpuestos().andThen(c -> c.objetoImp(ObjetoImp.SI_OBJETO).trasladoExento(Impuesto.IVA)))),
                caso("CFDI40217", "Impuesto trasladado fuera del catálogo", b -> b.impuestos(resumen(null, "16.00", List.of(), List.of(
                        new Impuestos.Traslado(new BigDecimal("100.00"), Impuesto.of("009"), TipoFactor.TASA, Optional.of(new BigDecimal("0.160000")),
                                Optional.of(new BigDecimal("16.00")))))).concepto(servicio())),
                caso("CFDI40218", "traslado repetido", b -> b.impuestos(resumen(null, "32.00", List.of(), List.of(iva("100.00", "0.160000", "16.00"),
                        iva("100.00", "0.16", "16.00")))).concepto(servicio())),
                caso("CFDI40219", "TasaOCuota del resumen fuera del catálogo", b -> b.impuestos(resumen(null, "16.00", List.of(), List.of(iva("100.00", "0.170000", "16.00"))))
                        .concepto(servicio())),
                caso("CFDI40220", "Importe del resumen con 3 decimales", b -> b.impuestos(resumen(null, "16.001", List.of(), List.of(iva("100.00", "0.160000", "16.001"))))
                        .concepto(servicio())),
                caso("CFDI40221", "Importe del resumen distinto", b -> b.impuestos(resumen(null, "15.00", List.of(), List.of(iva("100.00", "0.160000", "15.00"))))
                        .concepto(servicio())),
                caso("CFDI40222", "comercio exterior con Exportacion 01", b -> b.receptor(extranjero(r -> r.residenciaFiscal(Pais.USA).numRegIdTrib("123456789")))
                        .complemento(comercioExterior()).concepto(servicio())),
                certificado("CFDI40106", "certificado de otro RFC", new CertificateInfo(CSD.noCertificado(), "XIA190128J61", "X", CSD.validFrom(),
                        CSD.validUntil(), CertificateType.CSD, CSD.issuer()), RuleFixtures.conPuertos()),
                certificado("CFDI40106", "e.firma", new CertificateInfo(CSD.noCertificado(), CSD.rfc(), CSD.nombre(), CSD.validFrom(),
                        CSD.validUntil(), CertificateType.FIEL, CSD.issuer()), RuleFixtures.conPuertos()),
                certificado("CFDI40106", "certificado vencido", new CertificateInfo(CSD.noCertificado(), CSD.rfc(), CSD.nombre(), CSD.validFrom(),
                        Instant.parse("2026-01-01T00:00:00Z"), CertificateType.CSD, CSD.issuer()), RuleFixtures.conPuertos()),
                certificado("CFDI40106", "certificado revocado", CSD, Cfdi40Validator.builder().clock(RuleFixtures.CLOCK)
                        .certificateStatusProvider((n, at) -> CertificateStatusProvider.Revocation.REVOKED).build()),
                new Caso("CFDI4J-103", "NoCertificado distinto", () -> valido().build().withCertificado("30001000000500003415", "Y2VydA=="),
                        RuleFixtures.conPuertos(), CSD)));
        return casos.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void invalidCaseReportsItsCode(Caso caso) {
        ValidationResult result = caso.validar();

        assertThat(result.errors()).as("errors of %s", caso).anySatisfy(v -> assertThat(v.code()).isEqualTo(caso.code()));
        assertThat(result.isValid()).isFalse();
    }

    // --- valid cases ---

    @Test
    void baseComprobanteIsValidAndFullyVerifiedWithEveryPort() {
        ValidationResult result = RuleFixtures.conPuertos().validate(valido().build(), CSD);

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void baseComprobanteHasNoErrorsWithoutPortsOnlyNotVerifiedWarnings() {
        ValidationResult result = RuleFixtures.sinPuertos().validate(valido().build());

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).allSatisfy(v -> assertThat(v.status()).isEqualTo(Status.NOT_VERIFIED));
        assertThat(result.warnings()).extracting(Violation::code).containsExactlyInAnyOrder(
                "CFDI40126", "CFDI40138", "CFDI40139", "CFDI40143", "CFDI40144", "CFDI40145", "CFDI40147", "CFDI40148",
                "CFDI40162", "CFDI40163", "CFDI40164", "CFDI40165");
    }

    static Stream<Caso> validos() {
        return Stream.of(
                caso("CFDI40105", "PPD con 99", b -> b.metodoPago(MetodoPago.PPD).formaPago(FormaPago.POR_DEFINIR).concepto(servicio())),
                caso("CFDI40111", "descuento igual a la suma", b -> b.concepto(servicio().andThen(c -> c.descuento("10.00")))),
                caso("CFDI40115", "USD con TipoCambio", b -> b.moneda(Moneda.USD).tipoCambio("17.123456").concepto(servicio())),
                caso("CFDI40109", "traslado en cero", b -> b.formaPago(null).metodoPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO).concepto(sinImpuestos())),
                caso("CFDI40130", "factura global con InformacionGlobal", b -> b.receptor(publicoEnGeneral("72000"))
                        .informacionGlobal(Periodicidad.MENSUAL, Meses.OCTUBRE, 2026).concepto(servicio())),
                caso("CFDI40132", "bimestral en el régimen 621", b -> b.emisor(e -> e.rfc("EKU9003173C9").nombre("E").regimenFiscal(RegimenFiscal.INCORPORACION_FISCAL))
                        .informacionGlobal(Periodicidad.BIMESTRAL, Meses.SEPTIEMBRE_OCTUBRE, 2026).concepto(servicio())),
                caso("CFDI40153", "extranjero con ResidenciaFiscal y NumRegIdTrib", b -> b.receptor(extranjero(r -> r.residenciaFiscal(Pais.USA).numRegIdTrib("123456789")))
                        .concepto(servicio())),
                caso("CFDI40189", "tercero extranjero con la descripción", conConcepto(c -> c.trasladoIva16()
                        .aCuentaTerceros("EXT990101NI1", RuleFixtures.TERCERO_EXTRANJERO, RegimenFiscal.SIN_OBLIGACIONES_FISCALES, "72000"))),
                caso("CFDI140227", "objeto 07 con IEPS", conConcepto(c -> c.objetoImp(ObjetoImp.NO_TRASLADO_IVA_SI_DESGLOSE_IEPS)
                        .traslado(Impuesto.IEPS, TipoFactor.TASA, "0.080000"))),
                caso("CFDI40194", "pedimento bien formado", conConcepto(c -> c.trasladoIva16().informacionAduanera("21  47  3807  8003832"))),
                caso("CFDI40213", "solo exentos sin tasa", conConcepto(c -> c.trasladoExento(Impuesto.IVA))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validos")
    void validCaseDoesNotReportItsCode(Caso caso) {
        assertThat(caso.validar().errors()).as("errors of %s", caso).noneSatisfy(v -> assertThat(v.code()).isEqualTo(caso.code()));
    }

    // --- coverage of the rules themselves ---

    /** Codes documented without a check (verification, PAC-only or external without port). */
    private static final Set<String> SIN_REVISION = Set.of("CFDI40102", "CFDI40118", "CFDI40122", "CFDI40127", "CFDI40128", "CFDI40129",
            "CFDI40209", "CFDI40999");
    /** Codes that can only be "not verified" with the ports of these tests. */
    private static final Set<String> SOLO_NO_VERIFICADO = Set.of("CFDI40142", "CFDI40163");

    @Test
    void everyCodeOfTheMatrixHasExactlyOneRule() {
        List<String> codes = Cfdi40Rules.all().stream().map(Rule::code).toList();

        assertThat(codes).doesNotHaveDuplicates();
        assertThat(codes).containsAll(MatrizErrores.entradas().stream().map(MatrizErrores.Entrada::codigo).toList());
        assertThat(codes).filteredOn(c -> MatrizErrores.of(c).isEmpty()).containsExactly("CFDI4J-103");
    }

    @Test
    void everyRuleThatChecksSomethingHasAnInvalidCase() {
        Set<String> probados = invalidos().map(Caso::code).collect(Collectors.toCollection(TreeSet::new));
        Set<String> conRevision = Cfdi40Rules.all().stream().map(Rule::code)
                .filter(c -> !SIN_REVISION.contains(c) && !SOLO_NO_VERIFICADO.contains(c)).collect(Collectors.toCollection(TreeSet::new));

        assertThat(probados).containsAll(conRevision);
    }

    @Test
    void notVerifiedOnlyRulesReportTheirCode() {
        Comprobante adquirente = base().emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM)
                .facAtrAdquirente("0123456789")).concepto(servicio()).build();
        Comprobante hidrocarburo = base().concepto(conConceptoServicio("15101505")).build();

        assertThat(RuleFixtures.conPuertos().validate(adquirente).warnings()).extracting(Violation::code).contains("CFDI40142");
        assertThat(RuleFixtures.conPuertos().validate(hidrocarburo).warnings())
                .anySatisfy(v -> {
                    assertThat(v.code()).isEqualTo("CFDI40163");
                    assertThat(v.message()).contains("Hidrocarburos");
                });
    }

    private static Consumer<ComprobanteBuilder.ConceptoBuilder> conConceptoServicio(String clave) {
        return servicio().andThen(c -> c.claveProdServ(clave));
    }
}
