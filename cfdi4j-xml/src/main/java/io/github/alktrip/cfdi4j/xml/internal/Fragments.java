package io.github.alktrip.cfdi4j.xml.internal;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Converts XML elements that have no model (unknown complements, Addenda content) to
 * self-contained strings, and writes them back.
 */
public final class Fragments {

    private Fragments() {}

    /**
     * Serializes an element so that it declares every namespace it uses, including those declared
     * only on its ancestors in the original document.
     *
     * @param element the element
     * @return the XML of the element, without XML declaration
     */
    public static String toXml(Element element) {
        Document document = SecureXml.documentBuilder().newDocument();
        Element copy = (Element) document.importNode(element, true);
        document.appendChild(copy);
        declareMissingNamespaces(copy, copy);
        try {
            Transformer transformer = SecureXml.identityTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter out = new StringWriter();
            transformer.transform(new DOMSource(copy), new StreamResult(out));
            return out.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("Cannot serialize element " + element.getNodeName(), e);
        }
    }

    private static void declareMissingNamespaces(Element root, Node node) {
        declare(root, node);
        if (node instanceof Element element) {
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr) attributes.item(i);
                if (attr.getNamespaceURI() != null && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) {
                    declare(root, attr);
                }
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                declareMissingNamespaces(root, child);
            }
        }
    }

    private static void declare(Element root, Node node) {
        String uri = node.getNamespaceURI();
        if (uri == null || XMLConstants.XML_NS_URI.equals(uri)) {
            return;
        }
        String prefix = node.getPrefix();
        if (Objects.equals(lookup(node, prefix), uri)) {
            return;
        }
        String qname = prefix == null ? XMLConstants.XMLNS_ATTRIBUTE : XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix;
        if (!root.hasAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix == null ? XMLConstants.XMLNS_ATTRIBUTE : prefix)) {
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, qname, uri);
        }
    }

    /** Looks up a prefix through the xmlns attributes present in the (detached) tree. */
    private static String lookup(Node node, String prefix) {
        Node current = node instanceof Attr attr ? attr.getOwnerElement() : node;
        String name = prefix == null ? XMLConstants.XMLNS_ATTRIBUTE : prefix;
        while (current instanceof Element element) {
            Attr decl = element.getAttributeNodeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, name);
            if (decl != null) {
                return decl.getValue();
            }
            current = element.getParentNode();
        }
        return null;
    }

    /**
     * Writes fragments as produced by {@link #toXml(Element)}: one complement element, or the
     * concatenated child elements of an Addenda.
     *
     * @param xml the fragments
     * @param writer the target, not in namespace-repairing mode
     * @throws XMLStreamException if the content is not well-formed or writing fails
     */
    public static void write(String xml, XMLStreamWriter writer) throws XMLStreamException {
        writeChildrenOf("<r>" + xml + "</r>", writer);
    }

    private static void writeChildrenOf(String wrapped, XMLStreamWriter writer) throws XMLStreamException {
        XMLStreamReader reader = SecureXml.inputFactory().createXMLStreamReader(new StringReader(wrapped));
        try {
            int depth = 0;
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        depth++;
                        if (depth > 1) {
                            startElement(reader, writer);
                        }
                    }
                    case XMLStreamConstants.END_ELEMENT -> {
                        if (depth > 1) {
                            writer.writeEndElement();
                        }
                        depth--;
                    }
                    // CDATA sections are reported as characters and written escaped: same content.
                    case XMLStreamConstants.CHARACTERS, XMLStreamConstants.SPACE, XMLStreamConstants.CDATA -> {
                        if (depth > 1) {
                            writer.writeCharacters(reader.getText());
                        }
                    }
                    case XMLStreamConstants.COMMENT -> writer.writeComment(reader.getText());
                    case XMLStreamConstants.PROCESSING_INSTRUCTION -> writer.writeProcessingInstruction(reader.getPITarget(), reader.getPIData());
                    default -> { }
                }
            }
        } finally {
            reader.close();
        }
    }

    private static void startElement(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        String prefix = reader.getPrefix() == null ? "" : reader.getPrefix();
        String uri = reader.getNamespaceURI() == null ? "" : reader.getNamespaceURI();
        writer.writeStartElement(prefix, reader.getLocalName(), uri);
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String p = reader.getNamespacePrefix(i);
            if (p == null || p.isEmpty()) {
                writer.writeDefaultNamespace(reader.getNamespaceURI(i));
            } else {
                writer.writeNamespace(p, reader.getNamespaceURI(i));
            }
        }
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String ns = reader.getAttributeNamespace(i);
            if (ns == null || ns.isEmpty()) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(reader.getAttributePrefix(i), ns, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        }
    }
}
