package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Root node {@code cfdi:Comprobante}, version 4.0.
 *
 * <p>{@code Sello}, {@code NoCertificado} and {@code Certificado} are required by the XSD but are
 * empty until the comprobante is sealed. {@code Version} is always {@value #VERSION}.
 *
 * <p>Create instances with {@link #builder()}, which derives the totals the caller does not set
 * (ADR 0009).
 *
 * @param serie attribute {@code Serie}
 * @param folio attribute {@code Folio}
 * @param fecha attribute {@code Fecha}, local date and time of issue, with second precision
 * @param sello attribute {@code Sello}
 * @param formaPago attribute {@code FormaPago}
 * @param noCertificado attribute {@code NoCertificado}
 * @param certificado attribute {@code Certificado}
 * @param condicionesDePago attribute {@code CondicionesDePago}
 * @param subTotal attribute {@code SubTotal}
 * @param descuento attribute {@code Descuento}
 * @param moneda attribute {@code Moneda}
 * @param tipoCambio attribute {@code TipoCambio}
 * @param total attribute {@code Total}
 * @param tipoDeComprobante attribute {@code TipoDeComprobante}
 * @param exportacion attribute {@code Exportacion}
 * @param metodoPago attribute {@code MetodoPago}
 * @param lugarExpedicion attribute {@code LugarExpedicion}
 * @param confirmacion attribute {@code Confirmacion}
 * @param informacionGlobal node {@code cfdi:InformacionGlobal}
 * @param cfdiRelacionados nodes {@code cfdi:CfdiRelacionados}
 * @param emisor node {@code cfdi:Emisor}
 * @param receptor node {@code cfdi:Receptor}
 * @param conceptos nodes {@code cfdi:Conceptos/cfdi:Concepto}
 * @param impuestos node {@code cfdi:Impuestos}
 * @param complemento children of {@code cfdi:Complemento}; empty when absent
 * @param addenda node {@code cfdi:Addenda}
 */
public record Comprobante(
        Optional<String> serie,
        Optional<String> folio,
        LocalDateTime fecha,
        Optional<String> sello,
        Optional<FormaPago> formaPago,
        Optional<String> noCertificado,
        Optional<String> certificado,
        Optional<String> condicionesDePago,
        BigDecimal subTotal,
        Optional<BigDecimal> descuento,
        Moneda moneda,
        Optional<BigDecimal> tipoCambio,
        BigDecimal total,
        TipoDeComprobante tipoDeComprobante,
        Exportacion exportacion,
        Optional<MetodoPago> metodoPago,
        String lugarExpedicion,
        Optional<String> confirmacion,
        Optional<InformacionGlobal> informacionGlobal,
        List<CfdiRelacionados> cfdiRelacionados,
        Emisor emisor,
        Receptor receptor,
        List<Concepto> conceptos,
        Optional<Impuestos> impuestos,
        List<Complemento> complemento,
        Optional<Addenda> addenda) {

    /** Value of the {@code Version} attribute. */
    public static final String VERSION = "4.0";

    /** Checks that no component is null and copies the lists. */
    public Comprobante {
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(folio, "folio");
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(sello, "sello");
        Objects.requireNonNull(formaPago, "formaPago");
        Objects.requireNonNull(noCertificado, "noCertificado");
        Objects.requireNonNull(certificado, "certificado");
        Objects.requireNonNull(condicionesDePago, "condicionesDePago");
        Objects.requireNonNull(subTotal, "subTotal");
        Objects.requireNonNull(descuento, "descuento");
        Objects.requireNonNull(moneda, "moneda");
        Objects.requireNonNull(tipoCambio, "tipoCambio");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(tipoDeComprobante, "tipoDeComprobante");
        Objects.requireNonNull(exportacion, "exportacion");
        Objects.requireNonNull(metodoPago, "metodoPago");
        Objects.requireNonNull(lugarExpedicion, "lugarExpedicion");
        Objects.requireNonNull(confirmacion, "confirmacion");
        Objects.requireNonNull(informacionGlobal, "informacionGlobal");
        cfdiRelacionados = List.copyOf(cfdiRelacionados);
        Objects.requireNonNull(emisor, "emisor");
        Objects.requireNonNull(receptor, "receptor");
        conceptos = List.copyOf(conceptos);
        Objects.requireNonNull(impuestos, "impuestos");
        complemento = List.copyOf(complemento);
        Objects.requireNonNull(addenda, "addenda");
    }

    /**
     * Starts building a comprobante.
     *
     * @return a new builder
     */
    public static ComprobanteBuilder builder() {
        return new ComprobanteBuilder();
    }
}
