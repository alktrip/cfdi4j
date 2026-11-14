package io.github.alktrip.cfdi4j.xml.internal;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.catalog.CatalogKey;
import io.github.alktrip.cfdi4j.core.model.CfdiRelacionados;
import io.github.alktrip.cfdi4j.core.model.Complemento;
import io.github.alktrip.cfdi4j.core.model.ComplementoNoModelado;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import io.github.alktrip.cfdi4j.core.model.Emisor;
import io.github.alktrip.cfdi4j.core.model.Impuestos;
import io.github.alktrip.cfdi4j.core.model.InformacionGlobal;
import io.github.alktrip.cfdi4j.core.model.Receptor;
import io.github.alktrip.cfdi4j.xml.spi.ComplementoXml;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Writes a {@link Comprobante} as CFDI 4.0 XML. Elements and attributes are written in the order
 * of {@code cfdv40.xsd}; amounts are written with their scale ({@link BigDecimal#toPlainString()}).
 *
 * <p>{@code Sello}, {@code NoCertificado} and {@code Certificado} are written only when present:
 * an unsealed comprobante is not a valid CFDI yet.
 */
public final class CfdiWriter {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    private static final String CFDI = Namespaces.CFDI;

    private final ComplementoRegistry registry;

    public CfdiWriter(ComplementoRegistry registry) {
        this.registry = registry;
    }

    public void write(Comprobante c, OutputStream out) {
        try {
            XMLStreamWriter w = SecureXml.outputFactory().createXMLStreamWriter(out, "UTF-8");
            w.writeStartDocument("UTF-8", "1.0");
            writeComprobante(c, w);
            w.writeEndDocument();
            w.flush();
            w.close();
        } catch (XMLStreamException e) {
            throw new Cfdi4jException("Cannot write CFDI XML: " + e.getMessage(), e);
        }
    }

    private void writeComprobante(Comprobante c, XMLStreamWriter w) throws XMLStreamException {
        Map<String, ComplementoXml> mapped = mappedComplements(c);
        w.writeStartElement("cfdi", "Comprobante", CFDI);
        w.writeNamespace("cfdi", CFDI);
        w.writeNamespace("xsi", Namespaces.XSI);
        for (ComplementoXml mapper : mapped.values()) {
            w.writeNamespace(mapper.prefix(), mapper.namespace());
        }
        w.writeAttribute("xsi", Namespaces.XSI, "schemaLocation", schemaLocation(c, mapped));

        w.writeAttribute("Version", Comprobante.VERSION);
        attr(w, "Serie", c.serie());
        attr(w, "Folio", c.folio());
        w.writeAttribute("Fecha", FECHA.format(c.fecha()));
        attr(w, "Sello", c.sello());
        key(w, "FormaPago", c.formaPago());
        attr(w, "NoCertificado", c.noCertificado());
        attr(w, "Certificado", c.certificado());
        attr(w, "CondicionesDePago", c.condicionesDePago());
        dec(w, "SubTotal", c.subTotal());
        dec(w, "Descuento", c.descuento());
        w.writeAttribute("Moneda", c.moneda().clave());
        dec(w, "TipoCambio", c.tipoCambio());
        dec(w, "Total", c.total());
        w.writeAttribute("TipoDeComprobante", c.tipoDeComprobante().clave());
        w.writeAttribute("Exportacion", c.exportacion().clave());
        key(w, "MetodoPago", c.metodoPago());
        w.writeAttribute("LugarExpedicion", c.lugarExpedicion());
        attr(w, "Confirmacion", c.confirmacion());

        if (c.informacionGlobal().isPresent()) {
            InformacionGlobal ig = c.informacionGlobal().get();
            w.writeEmptyElement("cfdi", "InformacionGlobal", CFDI);
            w.writeAttribute("Periodicidad", ig.periodicidad().clave());
            w.writeAttribute("Meses", ig.meses().clave());
            w.writeAttribute("Año", Integer.toString(ig.anio()));
        }
        for (CfdiRelacionados relacionados : c.cfdiRelacionados()) {
            w.writeStartElement("cfdi", "CfdiRelacionados", CFDI);
            w.writeAttribute("TipoRelacion", relacionados.tipoRelacion().clave());
            for (CfdiRelacionados.CfdiRelacionado r : relacionados.cfdiRelacionado()) {
                w.writeEmptyElement("cfdi", "CfdiRelacionado", CFDI);
                w.writeAttribute("UUID", r.uuid());
            }
            w.writeEndElement();
        }
        writeEmisor(c.emisor(), w);
        writeReceptor(c.receptor(), w);
        w.writeStartElement("cfdi", "Conceptos", CFDI);
        for (Concepto concepto : c.conceptos()) {
            writeConcepto(concepto, w);
        }
        w.writeEndElement();
        if (c.impuestos().isPresent()) {
            writeImpuestos(c.impuestos().get(), w);
        }
        if (!c.complemento().isEmpty()) {
            w.writeStartElement("cfdi", "Complemento", CFDI);
            for (Complemento complemento : c.complemento()) {
                writeComplemento(complemento, w);
            }
            w.writeEndElement();
        }
        if (c.addenda().isPresent()) {
            w.writeStartElement("cfdi", "Addenda", CFDI);
            Fragments.write(c.addenda().get().contenido(), w);
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    private static void writeEmisor(Emisor e, XMLStreamWriter w) throws XMLStreamException {
        w.writeEmptyElement("cfdi", "Emisor", CFDI);
        w.writeAttribute("Rfc", e.rfc());
        w.writeAttribute("Nombre", e.nombre());
        w.writeAttribute("RegimenFiscal", e.regimenFiscal().clave());
        attr(w, "FacAtrAdquirente", e.facAtrAdquirente());
    }

    private static void writeReceptor(Receptor r, XMLStreamWriter w) throws XMLStreamException {
        w.writeEmptyElement("cfdi", "Receptor", CFDI);
        w.writeAttribute("Rfc", r.rfc());
        w.writeAttribute("Nombre", r.nombre());
        w.writeAttribute("DomicilioFiscalReceptor", r.domicilioFiscalReceptor());
        key(w, "ResidenciaFiscal", r.residenciaFiscal());
        attr(w, "NumRegIdTrib", r.numRegIdTrib());
        w.writeAttribute("RegimenFiscalReceptor", r.regimenFiscalReceptor().clave());
        w.writeAttribute("UsoCFDI", r.usoCFDI().clave());
    }

    private void writeConcepto(Concepto c, XMLStreamWriter w) throws XMLStreamException {
        w.writeStartElement("cfdi", "Concepto", CFDI);
        w.writeAttribute("ClaveProdServ", c.claveProdServ());
        attr(w, "NoIdentificacion", c.noIdentificacion());
        dec(w, "Cantidad", c.cantidad());
        w.writeAttribute("ClaveUnidad", c.claveUnidad());
        attr(w, "Unidad", c.unidad());
        w.writeAttribute("Descripcion", c.descripcion());
        dec(w, "ValorUnitario", c.valorUnitario());
        dec(w, "Importe", c.importe());
        dec(w, "Descuento", c.descuento());
        w.writeAttribute("ObjetoImp", c.objetoImp().clave());

        if (c.impuestos().isPresent()) {
            Concepto.Impuestos impuestos = c.impuestos().get();
            w.writeStartElement("cfdi", "Impuestos", CFDI);
            if (!impuestos.traslados().isEmpty()) {
                w.writeStartElement("cfdi", "Traslados", CFDI);
                for (Concepto.Traslado t : impuestos.traslados()) {
                    w.writeEmptyElement("cfdi", "Traslado", CFDI);
                    dec(w, "Base", t.base());
                    w.writeAttribute("Impuesto", t.impuesto().clave());
                    w.writeAttribute("TipoFactor", t.tipoFactor().clave());
                    dec(w, "TasaOCuota", t.tasaOCuota());
                    dec(w, "Importe", t.importe());
                }
                w.writeEndElement();
            }
            if (!impuestos.retenciones().isEmpty()) {
                w.writeStartElement("cfdi", "Retenciones", CFDI);
                for (Concepto.Retencion r : impuestos.retenciones()) {
                    w.writeEmptyElement("cfdi", "Retencion", CFDI);
                    dec(w, "Base", r.base());
                    w.writeAttribute("Impuesto", r.impuesto().clave());
                    w.writeAttribute("TipoFactor", r.tipoFactor().clave());
                    dec(w, "TasaOCuota", r.tasaOCuota());
                    dec(w, "Importe", r.importe());
                }
                w.writeEndElement();
            }
            w.writeEndElement();
        }
        if (c.aCuentaTerceros().isPresent()) {
            Concepto.ACuentaTerceros t = c.aCuentaTerceros().get();
            w.writeEmptyElement("cfdi", "ACuentaTerceros", CFDI);
            w.writeAttribute("RfcACuentaTerceros", t.rfcACuentaTerceros());
            w.writeAttribute("NombreACuentaTerceros", t.nombreACuentaTerceros());
            w.writeAttribute("RegimenFiscalACuentaTerceros", t.regimenFiscalACuentaTerceros().clave());
            w.writeAttribute("DomicilioFiscalACuentaTerceros", t.domicilioFiscalACuentaTerceros());
        }
        writeInformacionAduanera(c.informacionAduanera(), w);
        for (Concepto.CuentaPredial cp : c.cuentaPredial()) {
            w.writeEmptyElement("cfdi", "CuentaPredial", CFDI);
            w.writeAttribute("Numero", cp.numero());
        }
        if (!c.complementoConcepto().isEmpty()) {
            w.writeStartElement("cfdi", "ComplementoConcepto", CFDI);
            for (Complemento complemento : c.complementoConcepto()) {
                writeComplemento(complemento, w);
            }
            w.writeEndElement();
        }
        for (Concepto.Parte p : c.parte()) {
            w.writeStartElement("cfdi", "Parte", CFDI);
            w.writeAttribute("ClaveProdServ", p.claveProdServ());
            attr(w, "NoIdentificacion", p.noIdentificacion());
            dec(w, "Cantidad", p.cantidad());
            attr(w, "Unidad", p.unidad());
            w.writeAttribute("Descripcion", p.descripcion());
            dec(w, "ValorUnitario", p.valorUnitario());
            dec(w, "Importe", p.importe());
            writeInformacionAduanera(p.informacionAduanera(), w);
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    private static void writeInformacionAduanera(List<Concepto.InformacionAduanera> list, XMLStreamWriter w) throws XMLStreamException {
        for (Concepto.InformacionAduanera ia : list) {
            w.writeEmptyElement("cfdi", "InformacionAduanera", CFDI);
            w.writeAttribute("NumeroPedimento", ia.numeroPedimento());
        }
    }

    /** The XSD order of the summary is Retenciones, then Traslados (unlike the concept level). */
    private static void writeImpuestos(Impuestos i, XMLStreamWriter w) throws XMLStreamException {
        w.writeStartElement("cfdi", "Impuestos", CFDI);
        dec(w, "TotalImpuestosRetenidos", i.totalImpuestosRetenidos());
        dec(w, "TotalImpuestosTrasladados", i.totalImpuestosTrasladados());
        if (!i.retenciones().isEmpty()) {
            w.writeStartElement("cfdi", "Retenciones", CFDI);
            for (Impuestos.Retencion r : i.retenciones()) {
                w.writeEmptyElement("cfdi", "Retencion", CFDI);
                w.writeAttribute("Impuesto", r.impuesto().clave());
                dec(w, "Importe", r.importe());
            }
            w.writeEndElement();
        }
        if (!i.traslados().isEmpty()) {
            w.writeStartElement("cfdi", "Traslados", CFDI);
            for (Impuestos.Traslado t : i.traslados()) {
                w.writeEmptyElement("cfdi", "Traslado", CFDI);
                dec(w, "Base", t.base());
                w.writeAttribute("Impuesto", t.impuesto().clave());
                w.writeAttribute("TipoFactor", t.tipoFactor().clave());
                dec(w, "TasaOCuota", t.tasaOCuota());
                dec(w, "Importe", t.importe());
            }
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    private void writeComplemento(Complemento complemento, XMLStreamWriter w) throws XMLStreamException {
        if (complemento instanceof ComplementoNoModelado opaque) {
            Fragments.write(opaque.xml(), w);
            return;
        }
        ComplementoXml mapper = registry.forType(complemento.getClass()).orElseThrow(() -> new Cfdi4jException(
                "No XML mapper registered for complement " + complemento.getClass().getName()
                        + " (namespace " + complemento.namespace() + ")"));
        mapper.write(complemento, w);
    }

    /** Mappers of the modeled complements used in the document, sorted by namespace. */
    private Map<String, ComplementoXml> mappedComplements(Comprobante c) {
        Map<String, ComplementoXml> used = new LinkedHashMap<>();
        for (ComplementoXml mapper : registry.all()) {
            boolean present = allComplements(c).anyMatch(x -> !(x instanceof ComplementoNoModelado) && mapper.type().isInstance(x));
            if (present) {
                used.put(mapper.namespace(), mapper);
            }
        }
        return used;
    }

    private static Stream<Complemento> allComplements(Comprobante c) {
        return Stream.concat(c.complemento().stream(), c.conceptos().stream().flatMap(x -> x.complementoConcepto().stream()));
    }

    private static String schemaLocation(Comprobante c, Map<String, ComplementoXml> mapped) {
        List<String> pairs = new ArrayList<>();
        pairs.add(CFDI + " " + Namespaces.CFDI_SCHEMA_LOCATION);
        for (ComplementoXml mapper : mapped.values()) {
            pairs.add(mapper.namespace() + " " + mapper.schemaLocation());
        }
        allComplements(c)
                .filter(ComplementoNoModelado.class::isInstance)
                .map(ComplementoNoModelado.class::cast)
                .filter(x -> x.schemaLocation().isPresent())
                .map(x -> x.namespace() + " " + x.schemaLocation().get())
                .distinct()
                .forEach(pairs::add);
        return String.join(" ", pairs);
    }

    private static void attr(XMLStreamWriter w, String name, Optional<String> value) throws XMLStreamException {
        if (value.isPresent()) {
            w.writeAttribute(name, value.get());
        }
    }

    private static void key(XMLStreamWriter w, String name, Optional<? extends CatalogKey> value) throws XMLStreamException {
        if (value.isPresent()) {
            w.writeAttribute(name, value.get().clave());
        }
    }

    private static void dec(XMLStreamWriter w, String name, BigDecimal value) throws XMLStreamException {
        w.writeAttribute(name, value.toPlainString());
    }

    private static void dec(XMLStreamWriter w, String name, Optional<BigDecimal> value) throws XMLStreamException {
        if (value.isPresent()) {
            dec(w, name, value.get());
        }
    }
}
