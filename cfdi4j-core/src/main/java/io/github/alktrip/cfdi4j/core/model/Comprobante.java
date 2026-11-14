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
     * Returns a copy with {@code NoCertificado} and {@code Certificado} set, the first step of
     * sealing: the cadena original covers {@code NoCertificado}. Any previous {@code Sello} is
     * removed, because it no longer matches.
     *
     * @param noCertificado the certificate number (20 digits)
     * @param certificado the certificate, DER encoded in Base64
     * @return the copy
     */
    public Comprobante withCertificado(String noCertificado, String certificado) {
        return new Comprobante(serie, folio, fecha, Optional.empty(), formaPago, Optional.of(noCertificado),
                Optional.of(certificado), condicionesDePago, subTotal, descuento, moneda, tipoCambio, total, tipoDeComprobante,
                exportacion, metodoPago, lugarExpedicion, confirmacion, informacionGlobal, cfdiRelacionados, emisor, receptor,
                conceptos, impuestos, complemento, addenda);
    }

    /**
     * Returns a copy with {@code Sello} set.
     *
     * @param sello the seal, Base64
     * @return the copy
     */
    public Comprobante withSello(String sello) {
        return new Comprobante(serie, folio, fecha, Optional.of(sello), formaPago, noCertificado, certificado,
                condicionesDePago, subTotal, descuento, moneda, tipoCambio, total, tipoDeComprobante, exportacion, metodoPago,
                lugarExpedicion, confirmacion, informacionGlobal, cfdiRelacionados, emisor, receptor, conceptos, impuestos,
                complemento, addenda);
    }

    /**
     * Returns a copy with one more complement, for example the TimbreFiscalDigital added when the
     * comprobante is stamped. The seal is kept: complements such as the timbre are not part of the
     * cadena original of the comprobante, and verification reports it if one that is changes it.
     *
     * @param complemento the complement to append
     * @return the copy
     */
    public Comprobante withComplemento(Complemento complemento) {
        List<Complemento> all = new java.util.ArrayList<>(this.complemento);
        all.add(Objects.requireNonNull(complemento, "complemento"));
        return new Comprobante(serie, folio, fecha, sello, formaPago, noCertificado, certificado, condicionesDePago, subTotal,
                descuento, moneda, tipoCambio, total, tipoDeComprobante, exportacion, metodoPago, lugarExpedicion, confirmacion,
                informacionGlobal, cfdiRelacionados, emisor, receptor, conceptos, impuestos, all, addenda);
    }

    /**
     * Returns the TimbreFiscalDigital, if the comprobante is stamped and the timbre was read with
     * its mapper.
     *
     * @return the timbre
     */
    public Optional<TimbreFiscalDigital> timbreFiscalDigital() {
        return complemento.stream().filter(TimbreFiscalDigital.class::isInstance).map(TimbreFiscalDigital.class::cast).findFirst();
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
