package io.github.alktrip.cfdi4j.xml.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;

/** Drives the filter with SAX events directly and records what reaches the validator. */
class SchemaFilterTest {

    private static final String CFDI = Namespaces.CFDI;
    private static final String TFD = "http://www.sat.gob.mx/TimbreFiscalDigital";
    private static final String OTHER = "http://example.com/other";

    private final List<String> events = new ArrayList<>();
    private final DefaultHandler recorder = new DefaultHandler() {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            StringBuilder sb = new StringBuilder("<" + localName);
            for (int i = 0; i < atts.getLength(); i++) {
                sb.append(' ').append(atts.getLocalName(i)).append('=').append(atts.getValue(i));
            }
            events.add(sb.append('>').toString());
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            events.add("</" + localName + ">");
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            events.add("text:" + new String(ch, start, length));
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            events.add("ws");
        }

        @Override
        public void processingInstruction(String target, String data) {
            events.add("pi:" + target);
        }

        @Override
        public void skippedEntity(String name) {
            events.add("entity:" + name);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            events.add("ns:" + prefix);
        }

        @Override
        public void endPrefixMapping(String prefix) {
            events.add("/ns:" + prefix);
        }
    };

    @Test
    void passesBundledComplementsAndDropsOthersWithTheirEmptyContainer() throws SAXException {
        SchemaFilter filter = new SchemaFilter(recorder, Set.of(CFDI, TFD), false);
        LocatorImpl locator = new LocatorImpl();
        locator.setLineNumber(7);
        filter.setDocumentLocator(locator);

        filter.startDocument();
        start(filter, CFDI, "Comprobante");
        start(filter, CFDI, "Complemento");
        text(filter, "\n");
        start(filter, OTHER, "Otro");
        start(filter, OTHER, "Hijo");
        text(filter, "oculto");
        end(filter, OTHER, "Hijo");
        end(filter, OTHER, "Otro");
        filter.ignorableWhitespace(new char[] {' '}, 0, 1);
        filter.processingInstruction("skipped", "");
        start(filter, TFD, "TimbreFiscalDigital");
        end(filter, TFD, "TimbreFiscalDigital");
        text(filter, "\n");
        filter.ignorableWhitespace(new char[] {' '}, 0, 1);
        filter.processingInstruction("kept", "");
        end(filter, CFDI, "Complemento");
        start(filter, CFDI, "ComplementoConcepto");
        start(filter, OTHER, "Otro");
        end(filter, OTHER, "Otro");
        end(filter, CFDI, "ComplementoConcepto");
        filter.skippedEntity("e");
        end(filter, CFDI, "Comprobante");
        filter.endDocument();

        assertThat(events).containsExactly("<Comprobante>", "<Complemento>", "<TimbreFiscalDigital>", "</TimbreFiscalDigital>",
                "text:\n", "ws", "pi:kept", "</Complemento>", "entity:e", "</Comprobante>");
        assertThat(filter.skippedNamespaces()).containsExactly(java.util.Map.entry(OTHER, 7));
    }

    @Test
    void dropsTheWholeAddendaWithoutReportingIt() throws SAXException {
        SchemaFilter filter = new SchemaFilter(recorder, Set.of(CFDI), false);

        start(filter, CFDI, "Comprobante");
        start(filter, CFDI, "Addenda");
        start(filter, OTHER, "Pedido");
        end(filter, OTHER, "Pedido");
        end(filter, CFDI, "Addenda");
        end(filter, CFDI, "Comprobante");

        assertThat(events).containsExactly("<Comprobante>", "</Comprobante>");
        assertThat(filter.skippedNamespaces()).isEmpty();
    }

    @Test
    void unknownLineIsReportedAsMinusOneWithoutALocator() throws SAXException {
        SchemaFilter filter = new SchemaFilter(recorder, Set.of(CFDI), false);

        start(filter, CFDI, "Comprobante");
        start(filter, CFDI, "Complemento");
        start(filter, OTHER, "Otro");
        end(filter, OTHER, "Otro");
        end(filter, CFDI, "Complemento");
        end(filter, CFDI, "Comprobante");

        assertThat(filter.skippedNamespaces()).containsEntry(OTHER, -1);
    }

    @Test
    void unsealedModeAddsOnlyTheMissingSealAttributesToTheRoot() throws SAXException {
        SchemaFilter filter = new SchemaFilter(recorder, Set.of(CFDI), true);
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "Version", "Version", "CDATA", "4.0");
        atts.addAttribute("", "NoCertificado", "NoCertificado", "CDATA", "30001000000500003416");

        filter.startElement(CFDI, "Comprobante", "cfdi:Comprobante", atts);
        start(filter, CFDI, "Emisor");
        end(filter, CFDI, "Emisor");
        end(filter, CFDI, "Comprobante");

        assertThat(events.get(0)).isEqualTo("<Comprobante Version=4.0 NoCertificado=30001000000500003416 Sello= Certificado=>");
        assertThat(events.get(1)).isEqualTo("<Emisor>");
    }

    @Test
    void unsealedModeLeavesOtherRootsAlone() throws SAXException {
        SchemaFilter filter = new SchemaFilter(recorder, Set.of(CFDI), true);

        start(filter, OTHER, "Comprobante");
        start(filter, CFDI, "Comprobante");

        assertThat(events).containsExactly("<Comprobante>", "<Comprobante>");
    }

    @Test
    void forwardsPrefixMappings() throws SAXException {
        SchemaFilter filter = new SchemaFilter(recorder, Set.of(CFDI), false);

        filter.startPrefixMapping("cfdi", CFDI);
        filter.endPrefixMapping("cfdi");

        assertThat(events).containsExactly("ns:cfdi", "/ns:cfdi");
    }

    private static void start(SchemaFilter filter, String uri, String name) throws SAXException {
        filter.startElement(uri, name, name, new AttributesImpl());
    }

    private static void end(SchemaFilter filter, String uri, String name) throws SAXException {
        filter.endElement(uri, name, name);
    }

    private static void text(SchemaFilter filter, String text) throws SAXException {
        filter.characters(text.toCharArray(), 0, text.length());
    }
}
