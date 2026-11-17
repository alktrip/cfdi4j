package io.github.alktrip.cfdi4j.pagos20.internal;

import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.model.Complemento;
import io.github.alktrip.cfdi4j.pagos20.Pagos;
import io.github.alktrip.cfdi4j.xml.spi.ComplementoXml;
import io.github.alktrip.cfdi4j.xml.spi.XmlElement;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/** Maps {@code pago20:Pagos} ({@code Pagos20.xsd}); elements and attributes in XSD order. */
public final class Pagos20Xml implements ComplementoXml {

    private static final String NS = Pagos.NAMESPACE;
    private static final String P = "pago20";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    @Override
    public String namespace() {
        return NS;
    }

    @Override
    public String prefix() {
        return P;
    }

    @Override
    public String schemaLocation() {
        return "http://www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xsd";
    }

    @Override
    public Class<? extends Complemento> type() {
        return Pagos.class;
    }

    // --- reading ---

    @Override
    public Complemento read(XmlElement e) {
        if (!"Pagos".equals(e.localName())) {
            throw e.error("", "expected pago20:Pagos");
        }
        e.checkAttributes(Set.of("Version"), Set.of());
        e.checkChildren(NS, Set.of("Totales", "Pago"));
        String version = e.required("Version");
        if (!Pagos.VERSION.equals(version)) {
            throw e.error("@Version", "'" + version + "' is not supported; only " + Pagos.VERSION);
        }
        XmlElement t = e.child(NS, "Totales").orElseThrow(() -> e.error("pago20:Totales", "required element is missing"));
        t.checkAttributes(Set.of("TotalRetencionesIVA", "TotalRetencionesISR", "TotalRetencionesIEPS", "TotalTrasladosBaseIVA16",
                "TotalTrasladosImpuestoIVA16", "TotalTrasladosBaseIVA8", "TotalTrasladosImpuestoIVA8", "TotalTrasladosBaseIVA0",
                "TotalTrasladosImpuestoIVA0", "TotalTrasladosBaseIVAExento", "MontoTotalPagos"), Set.of());
        t.checkChildren(NS, Set.of());
        Pagos.Totales totales = new Pagos.Totales(t.optionalDecimal("TotalRetencionesIVA"), t.optionalDecimal("TotalRetencionesISR"),
                t.optionalDecimal("TotalRetencionesIEPS"), t.optionalDecimal("TotalTrasladosBaseIVA16"), t.optionalDecimal("TotalTrasladosImpuestoIVA16"),
                t.optionalDecimal("TotalTrasladosBaseIVA8"), t.optionalDecimal("TotalTrasladosImpuestoIVA8"), t.optionalDecimal("TotalTrasladosBaseIVA0"),
                t.optionalDecimal("TotalTrasladosImpuestoIVA0"), t.optionalDecimal("TotalTrasladosBaseIVAExento"), t.requiredDecimal("MontoTotalPagos"));
        return new Pagos(totales, e.children(NS, "Pago").stream().map(Pagos20Xml::pago).toList());
    }

    private static Pagos.Pago pago(XmlElement e) {
        e.checkAttributes(Set.of("FechaPago", "FormaDePagoP", "MonedaP", "TipoCambioP", "Monto", "NumOperacion", "RfcEmisorCtaOrd",
                "NomBancoOrdExt", "CtaOrdenante", "RfcEmisorCtaBen", "CtaBeneficiario", "TipoCadPago", "CertPago", "CadPago", "SelloPago"), Set.of());
        e.checkChildren(NS, Set.of("DoctoRelacionado", "ImpuestosP"));
        return new Pagos.Pago(e.requiredDateTime("FechaPago"), FormaPago.of(e.required("FormaDePagoP")), Moneda.of(e.required("MonedaP")),
                e.optionalDecimal("TipoCambioP"), e.requiredDecimal("Monto"), e.optional("NumOperacion"), e.optional("RfcEmisorCtaOrd"),
                e.optional("NomBancoOrdExt"), e.optional("CtaOrdenante"), e.optional("RfcEmisorCtaBen"), e.optional("CtaBeneficiario"),
                e.optional("TipoCadPago"), e.optional("CertPago"), e.optional("CadPago"), e.optional("SelloPago"),
                e.children(NS, "DoctoRelacionado").stream().map(Pagos20Xml::documento).toList(),
                e.child(NS, "ImpuestosP").map(Pagos20Xml::impuestosP));
    }

    private static Pagos.DoctoRelacionado documento(XmlElement e) {
        e.checkAttributes(Set.of("IdDocumento", "Serie", "Folio", "MonedaDR", "EquivalenciaDR", "NumParcialidad", "ImpSaldoAnt", "ImpPagado",
                "ImpSaldoInsoluto", "ObjetoImpDR"), Set.of());
        e.checkChildren(NS, Set.of("ImpuestosDR"));
        return new Pagos.DoctoRelacionado(e.required("IdDocumento"), e.optional("Serie"), e.optional("Folio"), Moneda.of(e.required("MonedaDR")),
                e.optionalDecimal("EquivalenciaDR"), e.requiredInt("NumParcialidad"), e.requiredDecimal("ImpSaldoAnt"), e.requiredDecimal("ImpPagado"),
                e.requiredDecimal("ImpSaldoInsoluto"), ObjetoImp.of(e.required("ObjetoImpDR")), e.child(NS, "ImpuestosDR").map(Pagos20Xml::impuestosDR));
    }

    private static Pagos.ImpuestosDR impuestosDR(XmlElement e) {
        e.checkAttributes(Set.of(), Set.of());
        e.checkChildren(NS, Set.of("RetencionesDR", "TrasladosDR"));
        List<Pagos.RetencionDR> retenciones = lista(e, "RetencionesDR", "RetencionDR").stream().map(r -> {
            r.checkAttributes(Set.of("BaseDR", "ImpuestoDR", "TipoFactorDR", "TasaOCuotaDR", "ImporteDR"), Set.of());
            r.checkChildren(NS, Set.of());
            return new Pagos.RetencionDR(r.requiredDecimal("BaseDR"), Impuesto.of(r.required("ImpuestoDR")), TipoFactor.of(r.required("TipoFactorDR")),
                    r.requiredDecimal("TasaOCuotaDR"), r.requiredDecimal("ImporteDR"));
        }).toList();
        List<Pagos.TrasladoDR> traslados = lista(e, "TrasladosDR", "TrasladoDR").stream().map(t -> {
            t.checkAttributes(Set.of("BaseDR", "ImpuestoDR", "TipoFactorDR", "TasaOCuotaDR", "ImporteDR"), Set.of());
            t.checkChildren(NS, Set.of());
            return new Pagos.TrasladoDR(t.requiredDecimal("BaseDR"), Impuesto.of(t.required("ImpuestoDR")), TipoFactor.of(t.required("TipoFactorDR")),
                    t.optionalDecimal("TasaOCuotaDR"), t.optionalDecimal("ImporteDR"));
        }).toList();
        return new Pagos.ImpuestosDR(retenciones, traslados);
    }

    private static Pagos.ImpuestosP impuestosP(XmlElement e) {
        e.checkAttributes(Set.of(), Set.of());
        e.checkChildren(NS, Set.of("RetencionesP", "TrasladosP"));
        List<Pagos.RetencionP> retenciones = lista(e, "RetencionesP", "RetencionP").stream().map(r -> {
            r.checkAttributes(Set.of("ImpuestoP", "ImporteP"), Set.of());
            r.checkChildren(NS, Set.of());
            return new Pagos.RetencionP(Impuesto.of(r.required("ImpuestoP")), r.requiredDecimal("ImporteP"));
        }).toList();
        List<Pagos.TrasladoP> traslados = lista(e, "TrasladosP", "TrasladoP").stream().map(t -> {
            t.checkAttributes(Set.of("BaseP", "ImpuestoP", "TipoFactorP", "TasaOCuotaP", "ImporteP"), Set.of());
            t.checkChildren(NS, Set.of());
            return new Pagos.TrasladoP(t.requiredDecimal("BaseP"), Impuesto.of(t.required("ImpuestoP")), TipoFactor.of(t.required("TipoFactorP")),
                    t.optionalDecimal("TasaOCuotaP"), t.optionalDecimal("ImporteP"));
        }).toList();
        return new Pagos.ImpuestosP(retenciones, traslados);
    }

    private static List<XmlElement> lista(XmlElement e, String contenedor, String elemento) {
        return e.child(NS, contenedor).map(c -> {
            c.checkAttributes(Set.of(), Set.of());
            c.checkChildren(NS, Set.of(elemento));
            return c.children(NS, elemento);
        }).orElse(List.of());
    }

    // --- writing ---

    @Override
    public void write(Complemento complemento, XMLStreamWriter w) throws XMLStreamException {
        Pagos pagos = (Pagos) complemento;
        w.writeStartElement(P, "Pagos", NS);
        w.writeAttribute("Version", Pagos.VERSION);
        Pagos.Totales t = pagos.totales();
        w.writeEmptyElement(P, "Totales", NS);
        dec(w, "TotalRetencionesIVA", t.totalRetencionesIVA());
        dec(w, "TotalRetencionesISR", t.totalRetencionesISR());
        dec(w, "TotalRetencionesIEPS", t.totalRetencionesIEPS());
        dec(w, "TotalTrasladosBaseIVA16", t.totalTrasladosBaseIVA16());
        dec(w, "TotalTrasladosImpuestoIVA16", t.totalTrasladosImpuestoIVA16());
        dec(w, "TotalTrasladosBaseIVA8", t.totalTrasladosBaseIVA8());
        dec(w, "TotalTrasladosImpuestoIVA8", t.totalTrasladosImpuestoIVA8());
        dec(w, "TotalTrasladosBaseIVA0", t.totalTrasladosBaseIVA0());
        dec(w, "TotalTrasladosImpuestoIVA0", t.totalTrasladosImpuestoIVA0());
        dec(w, "TotalTrasladosBaseIVAExento", t.totalTrasladosBaseIVAExento());
        w.writeAttribute("MontoTotalPagos", t.montoTotalPagos().toPlainString());
        for (Pagos.Pago p : pagos.pago()) {
            writePago(p, w);
        }
        w.writeEndElement();
    }

    private static void writePago(Pagos.Pago p, XMLStreamWriter w) throws XMLStreamException {
        w.writeStartElement(P, "Pago", NS);
        w.writeAttribute("FechaPago", FECHA.format(p.fechaPago()));
        w.writeAttribute("FormaDePagoP", p.formaDePagoP().clave());
        w.writeAttribute("MonedaP", p.monedaP().clave());
        dec(w, "TipoCambioP", p.tipoCambioP());
        w.writeAttribute("Monto", p.monto().toPlainString());
        attr(w, "NumOperacion", p.numOperacion());
        attr(w, "RfcEmisorCtaOrd", p.rfcEmisorCtaOrd());
        attr(w, "NomBancoOrdExt", p.nomBancoOrdExt());
        attr(w, "CtaOrdenante", p.ctaOrdenante());
        attr(w, "RfcEmisorCtaBen", p.rfcEmisorCtaBen());
        attr(w, "CtaBeneficiario", p.ctaBeneficiario());
        attr(w, "TipoCadPago", p.tipoCadPago());
        attr(w, "CertPago", p.certPago());
        attr(w, "CadPago", p.cadPago());
        attr(w, "SelloPago", p.selloPago());
        for (Pagos.DoctoRelacionado d : p.doctoRelacionado()) {
            w.writeStartElement(P, "DoctoRelacionado", NS);
            w.writeAttribute("IdDocumento", d.idDocumento());
            attr(w, "Serie", d.serie());
            attr(w, "Folio", d.folio());
            w.writeAttribute("MonedaDR", d.monedaDR().clave());
            dec(w, "EquivalenciaDR", d.equivalenciaDR());
            w.writeAttribute("NumParcialidad", Integer.toString(d.numParcialidad()));
            w.writeAttribute("ImpSaldoAnt", d.impSaldoAnt().toPlainString());
            w.writeAttribute("ImpPagado", d.impPagado().toPlainString());
            w.writeAttribute("ImpSaldoInsoluto", d.impSaldoInsoluto().toPlainString());
            w.writeAttribute("ObjetoImpDR", d.objetoImpDR().clave());
            if (d.impuestosDR().isPresent()) {
                Pagos.ImpuestosDR i = d.impuestosDR().get();
                w.writeStartElement(P, "ImpuestosDR", NS);
                if (!i.retencionesDR().isEmpty()) {
                    w.writeStartElement(P, "RetencionesDR", NS);
                    for (Pagos.RetencionDR r : i.retencionesDR()) {
                        w.writeEmptyElement(P, "RetencionDR", NS);
                        w.writeAttribute("BaseDR", r.baseDR().toPlainString());
                        w.writeAttribute("ImpuestoDR", r.impuestoDR().clave());
                        w.writeAttribute("TipoFactorDR", r.tipoFactorDR().clave());
                        w.writeAttribute("TasaOCuotaDR", r.tasaOCuotaDR().toPlainString());
                        w.writeAttribute("ImporteDR", r.importeDR().toPlainString());
                    }
                    w.writeEndElement();
                }
                if (!i.trasladosDR().isEmpty()) {
                    w.writeStartElement(P, "TrasladosDR", NS);
                    for (Pagos.TrasladoDR t : i.trasladosDR()) {
                        w.writeEmptyElement(P, "TrasladoDR", NS);
                        w.writeAttribute("BaseDR", t.baseDR().toPlainString());
                        w.writeAttribute("ImpuestoDR", t.impuestoDR().clave());
                        w.writeAttribute("TipoFactorDR", t.tipoFactorDR().clave());
                        dec(w, "TasaOCuotaDR", t.tasaOCuotaDR());
                        dec(w, "ImporteDR", t.importeDR());
                    }
                    w.writeEndElement();
                }
                w.writeEndElement();
            }
            w.writeEndElement();
        }
        if (p.impuestosP().isPresent()) {
            Pagos.ImpuestosP i = p.impuestosP().get();
            w.writeStartElement(P, "ImpuestosP", NS);
            if (!i.retencionesP().isEmpty()) {
                w.writeStartElement(P, "RetencionesP", NS);
                for (Pagos.RetencionP r : i.retencionesP()) {
                    w.writeEmptyElement(P, "RetencionP", NS);
                    w.writeAttribute("ImpuestoP", r.impuestoP().clave());
                    w.writeAttribute("ImporteP", r.importeP().toPlainString());
                }
                w.writeEndElement();
            }
            if (!i.trasladosP().isEmpty()) {
                w.writeStartElement(P, "TrasladosP", NS);
                for (Pagos.TrasladoP t : i.trasladosP()) {
                    w.writeEmptyElement(P, "TrasladoP", NS);
                    w.writeAttribute("BaseP", t.baseP().toPlainString());
                    w.writeAttribute("ImpuestoP", t.impuestoP().clave());
                    w.writeAttribute("TipoFactorP", t.tipoFactorP().clave());
                    dec(w, "TasaOCuotaP", t.tasaOCuotaP());
                    dec(w, "ImporteP", t.importeP());
                }
                w.writeEndElement();
            }
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    private static void attr(XMLStreamWriter w, String name, Optional<String> value) throws XMLStreamException {
        if (value.isPresent()) {
            w.writeAttribute(name, value.get());
        }
    }

    private static void dec(XMLStreamWriter w, String name, Optional<BigDecimal> value) throws XMLStreamException {
        if (value.isPresent()) {
            w.writeAttribute(name, value.get().toPlainString());
        }
    }
}
