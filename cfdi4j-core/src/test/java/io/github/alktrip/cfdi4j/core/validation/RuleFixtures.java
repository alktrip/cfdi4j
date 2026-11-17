package io.github.alktrip.cfdi4j.core.validation;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.CertificateType;
import io.github.alktrip.cfdi4j.core.catalog.CatalogProvider;
import io.github.alktrip.cfdi4j.core.catalog.ClaveProdServInfo;
import io.github.alktrip.cfdi4j.core.catalog.ClaveUnidadInfo;
import io.github.alktrip.cfdi4j.core.catalog.CodigoPostalInfo;
import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.Requisito;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import io.github.alktrip.cfdi4j.core.catalog.Vigencia;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import io.github.alktrip.cfdi4j.core.validation.TaxpayerRegistry.Contribuyente;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Comprobantes and fake ports for rule tests. RFCs are SAT public test data
 * (Certificados_de_Prueba.zip): emisor EKU9003173C9 (persona moral), receptor XIA190128J61
 * (persona moral) and CACX7605101P8 (persona física).
 */
final class RuleFixtures {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneOffset.UTC);
    static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 14, 10, 30, 0);
    static final Vigencia DESDE_2022 = Vigencia.desde(LocalDate.of(2022, 1, 1));

    /** The SAT test CSD of EKU9003173C9 (values printed by openssl, see specs/SOURCES.md). */
    static final CertificateInfo CSD = new CertificateInfo("30001000000500003416", "EKU9003173C9", "ESCUELA KEMPER URGATE SA DE CV",
            Instant.parse("2023-05-18T11:43:51Z"), Instant.parse("2027-05-18T11:43:51Z"), CertificateType.CSD, "CN=AC UAT");

    /** Name required with RfcACuentaTerceros EXT990101NI1 (error matrix, CFDI40189). */
    static final String TERCERO_EXTRANJERO = "EXPEDICIÓN DE CFDI POR RESIDENTES EN MÉXICO QUE PRESTAN SERVICIOS DE "
            + "INTERMEDIACIÓN ENTRE TERCEROS A OFERENTES DE BIENES Y SERVICIOS RESIDENTES EN EL EXTRANJERO";

    private RuleFixtures() {}

    /** Header, emisor and receptor; no concepto. */
    static ComprobanteBuilder base() {
        return Comprobante.builder().serie("A").folio("1").fecha(FECHA)
                .formaPago(FormaPago.TRANSFERENCIA).metodoPago(MetodoPago.PUE).moneda(Moneda.MXN)
                .tipoDeComprobante(TipoDeComprobante.INGRESO).exportacion(Exportacion.NO_APLICA).lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                        .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.GASTOS_EN_GENERAL));
    }

    /** A standard concepto: 1 x 100.00 with IVA 16 %. */
    static Consumer<ComprobanteBuilder.ConceptoBuilder> servicio() {
        return c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16();
    }

    static ComprobanteBuilder valido() {
        return base().concepto(servicio());
    }

    /** Replaces the conceptos of a comprobante (for shapes the builder cannot produce). */
    static Comprobante conConceptos(Comprobante c, List<Concepto> conceptos) {
        return new Comprobante(c.serie(), c.folio(), c.fecha(), c.sello(), c.formaPago(), c.noCertificado(), c.certificado(),
                c.condicionesDePago(), c.subTotal(), c.descuento(), c.moneda(), c.tipoCambio(), c.total(), c.tipoDeComprobante(),
                c.exportacion(), c.metodoPago(), c.lugarExpedicion(), c.confirmacion(), c.informacionGlobal(), c.cfdiRelacionados(),
                c.emisor(), c.receptor(), conceptos, c.impuestos(), c.complemento(), c.addenda());
    }

    static Comprobante conMoneda(Comprobante c, Moneda moneda) {
        return new Comprobante(c.serie(), c.folio(), c.fecha(), c.sello(), c.formaPago(), c.noCertificado(), c.certificado(),
                c.condicionesDePago(), c.subTotal(), c.descuento(), moneda, c.tipoCambio(), c.total(), c.tipoDeComprobante(),
                c.exportacion(), c.metodoPago(), c.lugarExpedicion(), c.confirmacion(), c.informacionGlobal(), c.cfdiRelacionados(),
                c.emisor(), c.receptor(), c.conceptos(), c.impuestos(), c.complemento(), c.addenda());
    }

    static Comprobante sinDescuento(Comprobante c) {
        return new Comprobante(c.serie(), c.folio(), c.fecha(), c.sello(), c.formaPago(), c.noCertificado(), c.certificado(),
                c.condicionesDePago(), c.subTotal(), Optional.empty(), c.moneda(), c.tipoCambio(), c.total(), c.tipoDeComprobante(),
                c.exportacion(), c.metodoPago(), c.lugarExpedicion(), c.confirmacion(), c.informacionGlobal(), c.cfdiRelacionados(),
                c.emisor(), c.receptor(), c.conceptos(), c.impuestos(), c.complemento(), c.addenda());
    }

    static Concepto conImpuestos(Concepto c, Optional<Concepto.Impuestos> impuestos) {
        return new Concepto(c.claveProdServ(), c.noIdentificacion(), c.cantidad(), c.claveUnidad(), c.unidad(), c.descripcion(),
                c.valorUnitario(), c.importe(), c.descuento(), c.objetoImp(), impuestos, c.aCuentaTerceros(), c.informacionAduanera(),
                c.cuentaPredial(), c.complementoConcepto(), c.parte());
    }

    // --- fake ports ---

    static final CodigoPostalInfo CP_72000 = new CodigoPostalInfo("72000", "PUE", DESDE_2022, ZoneOffset.ofHours(-6), ZoneOffset.ofHours(-6));

    static final CatalogProvider CATALOGOS = new CatalogProvider() {
        private final Map<String, ClaveProdServInfo> prodServ = Map.of(
                "01010101", new ClaveProdServInfo("01010101", "No existe en el catálogo", Requisito.OPCIONAL, Requisito.OPCIONAL, Optional.empty(), DESDE_2022),
                "50202306", new ClaveProdServInfo("50202306", "Refrescos", Requisito.SI, Requisito.SI, Optional.empty(), DESDE_2022),
                "15101505", new ClaveProdServInfo("15101505", "Combustible diesel", Requisito.OPCIONAL, Requisito.OPCIONAL,
                        Optional.of("Complemento Concepto para la facturación de Hidrocarburos y Petrolíferos"), DESDE_2022));

        @Override
        public Optional<ClaveProdServInfo> claveProdServ(String clave) {
            return Optional.ofNullable(prodServ.get(clave));
        }

        @Override
        public Optional<ClaveUnidadInfo> claveUnidad(String clave) {
            return List.of("ACT", "H87", "E48").contains(clave) ? Optional.of(new ClaveUnidadInfo(clave, clave, DESDE_2022)) : Optional.empty();
        }

        @Override
        public Optional<CodigoPostalInfo> codigoPostal(String codigoPostal) {
            return "72000".equals(codigoPostal) ? Optional.of(CP_72000) : Optional.empty();
        }
    };

    static final TaxpayerRegistry PADRON = rfc -> Optional.ofNullable(Map.of(
            "EKU9003173C9", new Contribuyente("EKU9003173C9", "ESCUELA KEMPER URGATE", "72000", OptionalInt.empty()),
            "XIA190128J61", new Contribuyente("XIA190128J61", "XENON INDUSTRIAL ARTICLES", "76343", OptionalInt.empty()),
            "CACX7605101P8", new Contribuyente("CACX7605101P8", "CONTRIBUYENTE DE PRUEBA UNO", "36257", OptionalInt.of(1)),
            "FUNK671228PH6", new Contribuyente("FUNK671228PH6", "CONTRIBUYENTE DE PRUEBA DOS", "01160", OptionalInt.of(2))).get(rfc));

    static final CertificateStatusProvider NO_REVOCADO = (n, at) -> CertificateStatusProvider.Revocation.VALID;

    static Cfdi40Validator sinPuertos() {
        return Cfdi40Validator.builder().clock(CLOCK).build();
    }

    static Cfdi40Validator conPuertos() {
        return Cfdi40Validator.builder().clock(CLOCK).catalogProvider(CATALOGOS).taxpayerRegistry(PADRON)
                .certificateStatusProvider(NO_REVOCADO).build();
    }
}
