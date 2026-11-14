package io.github.alktrip.cfdi4j.xml.internal;

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
import io.github.alktrip.cfdi4j.core.model.Addenda;
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
import io.github.alktrip.cfdi4j.xml.spi.XmlElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.w3c.dom.Document;

/**
 * Reads a CFDI 4.0 document into the model.
 *
 * <p>The reader is strict about the CFDI namespace: an attribute or element that
 * {@code cfdv40.xsd} does not define is an error, so no data is silently dropped. Values are kept
 * as written; checking them is the job of XSD and business-rule validation. Complements without a
 * registered mapper are kept as {@link ComplementoNoModelado}.
 */
public final class CfdiReader {

    private static final String CFDI = Namespaces.CFDI;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final ComplementoRegistry registry;

    public CfdiReader(ComplementoRegistry registry) {
        this.registry = registry;
    }

    public Comprobante read(Document document) {
        XmlElement root = XmlElement.root(document.getDocumentElement());
        if (Namespaces.CFDI_33.equals(root.namespace())) {
            throw root.error("", "CFDI 3.3 is not supported; only CFDI 4.0 (namespace " + CFDI + ")");
        }
        if (!CFDI.equals(root.namespace()) || !"Comprobante".equals(root.localName())) {
            throw root.error("", "expected the element {" + CFDI + "}Comprobante");
        }
        root.checkAttributes(
                Set.of("Version", "Serie", "Folio", "Fecha", "Sello", "FormaPago", "NoCertificado", "Certificado",
                        "CondicionesDePago", "SubTotal", "Descuento", "Moneda", "TipoCambio", "Total", "TipoDeComprobante",
                        "Exportacion", "MetodoPago", "LugarExpedicion", "Confirmacion"),
                Set.of("{" + Namespaces.XSI + "}schemaLocation"));
        root.checkChildren(CFDI, Set.of("InformacionGlobal", "CfdiRelacionados", "Emisor", "Receptor", "Conceptos",
                "Impuestos", "Complemento", "Addenda"));
        String version = root.required("Version");
        if (!Comprobante.VERSION.equals(version)) {
            throw root.error("@Version", "'" + version + "' is not supported; only " + Comprobante.VERSION);
        }
        Map<String, String> schemaLocations = schemaLocations(root);

        XmlElement conceptos = root.child(CFDI, "Conceptos").orElseThrow(() -> root.error("cfdi:Conceptos", "required element is missing"));
        conceptos.checkAttributes(Set.of(), Set.of());
        conceptos.checkChildren(CFDI, Set.of("Concepto"));

        return new Comprobante(
                root.optional("Serie"),
                root.optional("Folio"),
                root.requiredDateTime("Fecha"),
                root.optional("Sello"),
                root.optional("FormaPago").map(FormaPago::of),
                root.optional("NoCertificado"),
                root.optional("Certificado"),
                root.optional("CondicionesDePago"),
                root.requiredDecimal("SubTotal"),
                root.optionalDecimal("Descuento"),
                Moneda.of(root.required("Moneda")),
                root.optionalDecimal("TipoCambio"),
                root.requiredDecimal("Total"),
                TipoDeComprobante.of(root.required("TipoDeComprobante")),
                Exportacion.of(root.required("Exportacion")),
                root.optional("MetodoPago").map(MetodoPago::of),
                root.required("LugarExpedicion"),
                root.optional("Confirmacion"),
                root.child(CFDI, "InformacionGlobal").map(CfdiReader::informacionGlobal),
                root.children(CFDI, "CfdiRelacionados").stream().map(CfdiReader::cfdiRelacionados).toList(),
                emisor(root.child(CFDI, "Emisor").orElseThrow(() -> root.error("cfdi:Emisor", "required element is missing"))),
                receptor(root.child(CFDI, "Receptor").orElseThrow(() -> root.error("cfdi:Receptor", "required element is missing"))),
                conceptos.children(CFDI, "Concepto").stream().map(c -> concepto(c, schemaLocations)).toList(),
                root.child(CFDI, "Impuestos").map(CfdiReader::impuestos),
                root.child(CFDI, "Complemento").map(c -> complementos(c, schemaLocations)).orElse(List.of()),
                root.child(CFDI, "Addenda").map(CfdiReader::addenda));
    }

    private static Map<String, String> schemaLocations(XmlElement root) {
        Map<String, String> locations = new HashMap<>();
        Optional<String> value = root.optional(Namespaces.XSI, "schemaLocation");
        if (value.isPresent()) {
            List<String> tokens = WHITESPACE.splitAsStream(value.get().strip()).toList();
            for (int i = 0; i + 1 < tokens.size(); i += 2) {
                locations.putIfAbsent(tokens.get(i), tokens.get(i + 1));
            }
        }
        return locations;
    }

    private static InformacionGlobal informacionGlobal(XmlElement e) {
        e.checkAttributes(Set.of("Periodicidad", "Meses", "Año"), Set.of());
        e.checkChildren(CFDI, Set.of());
        return new InformacionGlobal(Periodicidad.of(e.required("Periodicidad")), Meses.of(e.required("Meses")), e.requiredInt("Año"));
    }

    private static CfdiRelacionados cfdiRelacionados(XmlElement e) {
        e.checkAttributes(Set.of("TipoRelacion"), Set.of());
        e.checkChildren(CFDI, Set.of("CfdiRelacionado"));
        return new CfdiRelacionados(TipoRelacion.of(e.required("TipoRelacion")), e.children(CFDI, "CfdiRelacionado").stream().map(r -> {
            r.checkAttributes(Set.of("UUID"), Set.of());
            r.checkChildren(CFDI, Set.of());
            return new CfdiRelacionados.CfdiRelacionado(r.required("UUID"));
        }).toList());
    }

    private static Emisor emisor(XmlElement e) {
        e.checkAttributes(Set.of("Rfc", "Nombre", "RegimenFiscal", "FacAtrAdquirente"), Set.of());
        e.checkChildren(CFDI, Set.of());
        return new Emisor(e.required("Rfc"), e.required("Nombre"), RegimenFiscal.of(e.required("RegimenFiscal")), e.optional("FacAtrAdquirente"));
    }

    private static Receptor receptor(XmlElement e) {
        e.checkAttributes(Set.of("Rfc", "Nombre", "DomicilioFiscalReceptor", "ResidenciaFiscal", "NumRegIdTrib",
                "RegimenFiscalReceptor", "UsoCFDI"), Set.of());
        e.checkChildren(CFDI, Set.of());
        return new Receptor(e.required("Rfc"), e.required("Nombre"), e.required("DomicilioFiscalReceptor"),
                e.optional("ResidenciaFiscal").map(Pais::of), e.optional("NumRegIdTrib"),
                RegimenFiscal.of(e.required("RegimenFiscalReceptor")), UsoCFDI.of(e.required("UsoCFDI")));
    }

    private Concepto concepto(XmlElement e, Map<String, String> schemaLocations) {
        e.checkAttributes(Set.of("ClaveProdServ", "NoIdentificacion", "Cantidad", "ClaveUnidad", "Unidad", "Descripcion",
                "ValorUnitario", "Importe", "Descuento", "ObjetoImp"), Set.of());
        e.checkChildren(CFDI, Set.of("Impuestos", "ACuentaTerceros", "InformacionAduanera", "CuentaPredial", "ComplementoConcepto", "Parte"));
        return new Concepto(
                e.required("ClaveProdServ"),
                e.optional("NoIdentificacion"),
                e.requiredDecimal("Cantidad"),
                e.required("ClaveUnidad"),
                e.optional("Unidad"),
                e.required("Descripcion"),
                e.requiredDecimal("ValorUnitario"),
                e.requiredDecimal("Importe"),
                e.optionalDecimal("Descuento"),
                ObjetoImp.of(e.required("ObjetoImp")),
                e.child(CFDI, "Impuestos").map(CfdiReader::impuestosConcepto),
                e.child(CFDI, "ACuentaTerceros").map(CfdiReader::aCuentaTerceros),
                informacionAduanera(e),
                e.children(CFDI, "CuentaPredial").stream().map(cp -> {
                    cp.checkAttributes(Set.of("Numero"), Set.of());
                    cp.checkChildren(CFDI, Set.of());
                    return new Concepto.CuentaPredial(cp.required("Numero"));
                }).toList(),
                e.child(CFDI, "ComplementoConcepto").map(c -> complementos(c, schemaLocations)).orElse(List.of()),
                e.children(CFDI, "Parte").stream().map(CfdiReader::parte).toList());
    }

    private static Concepto.Impuestos impuestosConcepto(XmlElement e) {
        e.checkAttributes(Set.of(), Set.of());
        e.checkChildren(CFDI, Set.of("Traslados", "Retenciones"));
        List<Concepto.Traslado> traslados = e.child(CFDI, "Traslados").map(ts -> {
            ts.checkAttributes(Set.of(), Set.of());
            ts.checkChildren(CFDI, Set.of("Traslado"));
            return ts.children(CFDI, "Traslado").stream().map(t -> {
                t.checkAttributes(Set.of("Base", "Impuesto", "TipoFactor", "TasaOCuota", "Importe"), Set.of());
                t.checkChildren(CFDI, Set.of());
                return new Concepto.Traslado(t.requiredDecimal("Base"), Impuesto.of(t.required("Impuesto")),
                        TipoFactor.of(t.required("TipoFactor")), t.optionalDecimal("TasaOCuota"), t.optionalDecimal("Importe"));
            }).toList();
        }).orElse(List.of());
        List<Concepto.Retencion> retenciones = e.child(CFDI, "Retenciones").map(rs -> {
            rs.checkAttributes(Set.of(), Set.of());
            rs.checkChildren(CFDI, Set.of("Retencion"));
            return rs.children(CFDI, "Retencion").stream().map(r -> {
                r.checkAttributes(Set.of("Base", "Impuesto", "TipoFactor", "TasaOCuota", "Importe"), Set.of());
                r.checkChildren(CFDI, Set.of());
                return new Concepto.Retencion(r.requiredDecimal("Base"), Impuesto.of(r.required("Impuesto")),
                        TipoFactor.of(r.required("TipoFactor")), r.requiredDecimal("TasaOCuota"), r.requiredDecimal("Importe"));
            }).toList();
        }).orElse(List.of());
        return new Concepto.Impuestos(traslados, retenciones);
    }

    private static Concepto.ACuentaTerceros aCuentaTerceros(XmlElement e) {
        e.checkAttributes(Set.of("RfcACuentaTerceros", "NombreACuentaTerceros", "RegimenFiscalACuentaTerceros",
                "DomicilioFiscalACuentaTerceros"), Set.of());
        e.checkChildren(CFDI, Set.of());
        return new Concepto.ACuentaTerceros(e.required("RfcACuentaTerceros"), e.required("NombreACuentaTerceros"),
                RegimenFiscal.of(e.required("RegimenFiscalACuentaTerceros")), e.required("DomicilioFiscalACuentaTerceros"));
    }

    private static List<Concepto.InformacionAduanera> informacionAduanera(XmlElement e) {
        return e.children(CFDI, "InformacionAduanera").stream().map(ia -> {
            ia.checkAttributes(Set.of("NumeroPedimento"), Set.of());
            ia.checkChildren(CFDI, Set.of());
            return new Concepto.InformacionAduanera(ia.required("NumeroPedimento"));
        }).toList();
    }

    private static Concepto.Parte parte(XmlElement e) {
        e.checkAttributes(Set.of("ClaveProdServ", "NoIdentificacion", "Cantidad", "Unidad", "Descripcion", "ValorUnitario", "Importe"), Set.of());
        e.checkChildren(CFDI, Set.of("InformacionAduanera"));
        return new Concepto.Parte(e.required("ClaveProdServ"), e.optional("NoIdentificacion"), e.requiredDecimal("Cantidad"),
                e.optional("Unidad"), e.required("Descripcion"), e.optionalDecimal("ValorUnitario"), e.optionalDecimal("Importe"),
                informacionAduanera(e));
    }

    private static Impuestos impuestos(XmlElement e) {
        e.checkAttributes(Set.of("TotalImpuestosRetenidos", "TotalImpuestosTrasladados"), Set.of());
        e.checkChildren(CFDI, Set.of("Retenciones", "Traslados"));
        List<Impuestos.Retencion> retenciones = e.child(CFDI, "Retenciones").map(rs -> {
            rs.checkAttributes(Set.of(), Set.of());
            rs.checkChildren(CFDI, Set.of("Retencion"));
            return rs.children(CFDI, "Retencion").stream().map(r -> {
                r.checkAttributes(Set.of("Impuesto", "Importe"), Set.of());
                r.checkChildren(CFDI, Set.of());
                return new Impuestos.Retencion(Impuesto.of(r.required("Impuesto")), r.requiredDecimal("Importe"));
            }).toList();
        }).orElse(List.of());
        List<Impuestos.Traslado> traslados = e.child(CFDI, "Traslados").map(ts -> {
            ts.checkAttributes(Set.of(), Set.of());
            ts.checkChildren(CFDI, Set.of("Traslado"));
            return ts.children(CFDI, "Traslado").stream().map(t -> {
                t.checkAttributes(Set.of("Base", "Impuesto", "TipoFactor", "TasaOCuota", "Importe"), Set.of());
                t.checkChildren(CFDI, Set.of());
                return new Impuestos.Traslado(t.requiredDecimal("Base"), Impuesto.of(t.required("Impuesto")),
                        TipoFactor.of(t.required("TipoFactor")), t.optionalDecimal("TasaOCuota"), t.optionalDecimal("Importe"));
            }).toList();
        }).orElse(List.of());
        return new Impuestos(e.optionalDecimal("TotalImpuestosRetenidos"), e.optionalDecimal("TotalImpuestosTrasladados"),
                retenciones, traslados);
    }

    private List<Complemento> complementos(XmlElement container, Map<String, String> schemaLocations) {
        container.checkAttributes(Set.of(), Set.of());
        List<Complemento> result = new ArrayList<>();
        for (XmlElement child : container.children()) {
            Optional<ComplementoXml> mapper = registry.forNamespace(child.namespace());
            if (mapper.isPresent()) {
                result.add(mapper.get().read(child));
            } else if (child.namespace().isEmpty()) {
                throw child.error("", "complement element without namespace");
            } else {
                result.add(new ComplementoNoModelado(child.namespace(), Fragments.toXml(child.element()),
                        Optional.ofNullable(schemaLocations.get(child.namespace()))));
            }
        }
        return result;
    }

    private static Addenda addenda(XmlElement e) {
        e.checkAttributes(Set.of(), Set.of());
        StringBuilder content = new StringBuilder();
        for (XmlElement child : e.children()) {
            content.append(Fragments.toXml(child.element()));
        }
        return new Addenda(content.toString());
    }
}
