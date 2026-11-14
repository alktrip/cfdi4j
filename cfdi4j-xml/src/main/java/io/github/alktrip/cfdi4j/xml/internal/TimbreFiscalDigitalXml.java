package io.github.alktrip.cfdi4j.xml.internal;

import io.github.alktrip.cfdi4j.core.model.Complemento;
import io.github.alktrip.cfdi4j.core.model.TimbreFiscalDigital;
import io.github.alktrip.cfdi4j.xml.spi.ComplementoXml;
import io.github.alktrip.cfdi4j.xml.spi.XmlElement;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/** Maps the TimbreFiscalDigital 1.1 ({@code TimbreFiscalDigitalv11.xsd}); attributes in XSD order. */
public final class TimbreFiscalDigitalXml implements ComplementoXml {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    @Override
    public String namespace() {
        return TimbreFiscalDigital.NAMESPACE;
    }

    @Override
    public String prefix() {
        return "tfd";
    }

    @Override
    public String schemaLocation() {
        return "http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd";
    }

    @Override
    public Class<? extends Complemento> type() {
        return TimbreFiscalDigital.class;
    }

    @Override
    public Complemento read(XmlElement e) {
        if (!"TimbreFiscalDigital".equals(e.localName())) {
            throw e.error("", "expected tfd:TimbreFiscalDigital");
        }
        e.checkAttributes(Set.of("Version", "UUID", "FechaTimbrado", "RfcProvCertif", "Leyenda", "SelloCFD", "NoCertificadoSAT", "SelloSAT"),
                Set.of("{" + Namespaces.XSI + "}schemaLocation"));
        e.checkChildren(namespace(), Set.of());
        return new TimbreFiscalDigital(e.required("Version"), e.required("UUID"), e.requiredDateTime("FechaTimbrado"),
                e.required("RfcProvCertif"), e.optional("Leyenda"), e.required("SelloCFD"), e.required("NoCertificadoSAT"),
                e.required("SelloSAT"));
    }

    @Override
    public void write(Complemento complemento, XMLStreamWriter w) throws XMLStreamException {
        TimbreFiscalDigital t = (TimbreFiscalDigital) complemento;
        w.writeEmptyElement(prefix(), "TimbreFiscalDigital", namespace());
        attributes(t, w);
    }

    /** Writes the timbre as a standalone document, the input of its cadena original stylesheet. */
    public void writeDocument(TimbreFiscalDigital t, XMLStreamWriter w) throws XMLStreamException {
        w.writeStartDocument("UTF-8", "1.0");
        w.writeEmptyElement(prefix(), "TimbreFiscalDigital", namespace());
        w.writeNamespace(prefix(), namespace());
        attributes(t, w);
        w.writeEndDocument();
    }

    private static void attributes(TimbreFiscalDigital t, XMLStreamWriter w) throws XMLStreamException {
        w.writeAttribute("Version", t.version());
        w.writeAttribute("UUID", t.uuid());
        w.writeAttribute("FechaTimbrado", FECHA.format(t.fechaTimbrado()));
        w.writeAttribute("RfcProvCertif", t.rfcProvCertif());
        if (t.leyenda().isPresent()) {
            w.writeAttribute("Leyenda", t.leyenda().get());
        }
        w.writeAttribute("SelloCFD", t.selloCFD());
        w.writeAttribute("NoCertificadoSAT", t.noCertificadoSAT());
        w.writeAttribute("SelloSAT", t.selloSAT());
    }
}
