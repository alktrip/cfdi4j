package io.github.alktrip.cfdi4j.xml.spi;

import io.github.alktrip.cfdi4j.core.internal.SafeText;
import io.github.alktrip.cfdi4j.xml.CfdiParseException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Read-only view of an XML element that reports problems with their location
 * ({@code /cfdi:Comprobante/cfdi:Conceptos/cfdi:Concepto[2]/@Importe}).
 *
 * <p>Attribute values are returned exactly as written. Decimal, integer and date values are parsed
 * after removing surrounding whitespace, as the XSD types collapse whitespace.
 */
public final class XmlElement {

    private final Element element;
    private final String path;

    /** Lexical space of {@code xs:decimal}: optional sign, digits and an optional fraction, no exponent. */
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)");
    /** {@code yyyy-MM-ddTHH:mm:ss}, the only date-time form of the SAT schemas. */
    private static final Pattern DATE_TIME = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}");
    private static final int MAX_QUOTED = 60;

    private XmlElement(Element element, String path) {
        this.element = element;
        this.path = path;
    }

    /**
     * Wraps a root element.
     *
     * @param element the element
     * @return the view
     */
    public static XmlElement root(Element element) {
        return new XmlElement(Objects.requireNonNull(element, "element"), "/" + element.getNodeName());
    }

    /**
     * Returns the location of this element.
     *
     * @return the XPath-like location
     */
    public String path() {
        return path;
    }

    /**
     * Returns the namespace URI of this element.
     *
     * @return the namespace, empty string if none
     */
    public String namespace() {
        return element.getNamespaceURI() == null ? "" : element.getNamespaceURI();
    }

    /**
     * Returns the local name of this element.
     *
     * @return the local name
     */
    public String localName() {
        return element.getLocalName();
    }

    /**
     * Returns the underlying DOM element.
     *
     * @return the element
     */
    public Element element() {
        return element;
    }

    /**
     * Returns an attribute without namespace.
     *
     * @param name attribute name
     * @return the value as written, or empty if absent
     */
    public Optional<String> optional(String name) {
        Attr attr = element.getAttributeNodeNS(null, name);
        return attr == null ? Optional.empty() : Optional.of(attr.getValue());
    }

    /**
     * Returns a required attribute without namespace.
     *
     * @param name attribute name
     * @return the value as written
     * @throws CfdiParseException if the attribute is absent
     */
    public String required(String name) {
        return optional(name).orElseThrow(() -> error("@" + name, "required attribute is missing"));
    }

    /**
     * Returns an optional decimal attribute, keeping its scale.
     *
     * @param name attribute name
     * @return the value, or empty if absent
     * @throws CfdiParseException if the value is not a decimal number
     */
    public Optional<BigDecimal> optionalDecimal(String name) {
        return optional(name).map(v -> {
            String text = v.strip();
            // Only the lexical form of xs:decimal: an exponent such as 1E999999999 would make
            // later rounding build a number with a billion digits.
            if (!DECIMAL.matcher(text).matches()) {
                throw error("@" + name, "'" + abbreviate(v) + "' is not a decimal number");
            }
            return new BigDecimal(text);
        });
    }

    /**
     * Returns a required decimal attribute, keeping its scale.
     *
     * @param name attribute name
     * @return the value
     * @throws CfdiParseException if the attribute is absent or not a decimal number
     */
    public BigDecimal requiredDecimal(String name) {
        required(name);
        return optionalDecimal(name).orElseThrow();
    }

    /**
     * Returns a required integer attribute.
     *
     * @param name attribute name
     * @return the value
     * @throws CfdiParseException if the attribute is absent or not an integer
     */
    public int requiredInt(String name) {
        String value = required(name);
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw error("@" + name, "'" + abbreviate(value) + "' is not an integer");
        }
    }

    /**
     * Returns a required date-time attribute in the format {@code yyyy-MM-ddTHH:mm:ss}.
     *
     * @param name attribute name
     * @return the value
     * @throws CfdiParseException if the attribute is absent or not a date-time
     */
    public LocalDateTime requiredDateTime(String name) {
        String value = required(name);
        String text = value.strip();
        try {
            if (!DATE_TIME.matcher(text).matches()) {
                throw new DateTimeParseException("not yyyy-MM-ddTHH:mm:ss", text, 0);
            }
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            throw error("@" + name, "'" + abbreviate(value) + "' is not a date-time (yyyy-MM-ddTHH:mm:ss)");
        }
    }

    /**
     * Returns the attribute in a namespace, for example {@code xsi:schemaLocation}.
     *
     * @param namespace namespace URI
     * @param localName local name
     * @return the value, or empty if absent
     */
    public Optional<String> optional(String namespace, String localName) {
        Attr attr = element.getAttributeNodeNS(namespace, localName);
        return attr == null ? Optional.empty() : Optional.of(attr.getValue());
    }

    /**
     * Returns the child elements with the given name, in document order.
     *
     * @param namespace namespace URI
     * @param localName local name
     * @return the children; each one's location carries its 1-based index
     */
    public List<XmlElement> children(String namespace, String localName) {
        List<Element> matches = new ArrayList<>();
        for (Element child : elementChildren()) {
            if (namespace.equals(child.getNamespaceURI()) && localName.equals(child.getLocalName())) {
                matches.add(child);
            }
        }
        List<XmlElement> result = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            Element child = matches.get(i);
            result.add(new XmlElement(child, path + "/" + child.getNodeName() + "[" + (i + 1) + "]"));
        }
        return result;
    }

    /**
     * Returns the only child element with the given name.
     *
     * @param namespace namespace URI
     * @param localName local name
     * @return the child, or empty if absent
     * @throws CfdiParseException if there is more than one
     */
    public Optional<XmlElement> child(String namespace, String localName) {
        List<XmlElement> matches = children(namespace, localName);
        if (matches.size() > 1) {
            throw error(matches.get(1).element.getNodeName(), "element may appear only once");
        }
        return matches.isEmpty()
                ? Optional.empty()
                : Optional.of(new XmlElement(matches.get(0).element, path + "/" + matches.get(0).element.getNodeName()));
    }

    /**
     * Returns every child element, in document order.
     *
     * @return the children
     */
    public List<XmlElement> children() {
        List<Element> children = elementChildren();
        List<XmlElement> result = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            result.add(new XmlElement(children.get(i), path + "/" + children.get(i).getNodeName() + "[" + (i + 1) + "]"));
        }
        return result;
    }

    /**
     * Fails if the element has an attribute that is not expected. Namespace declarations are
     * always allowed.
     *
     * @param allowed names of the attributes without namespace that may appear
     * @param allowedForeign {@code {namespace}localName} of namespaced attributes that may appear
     * @throws CfdiParseException on the first unexpected attribute
     */
    public void checkAttributes(Set<String> allowed, Set<String> allowedForeign) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            String ns = attr.getNamespaceURI();
            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(ns)) {
                continue;
            }
            boolean ok = ns == null ? allowed.contains(attr.getLocalName()) : allowedForeign.contains("{" + ns + "}" + attr.getLocalName());
            if (!ok) {
                throw error("@" + attr.getName(), "unexpected attribute");
            }
        }
    }

    /**
     * Fails if the element has a child element that is not expected, or text content.
     *
     * @param namespace namespace the expected children belong to
     * @param allowed local names of the children that may appear
     * @throws CfdiParseException on the first unexpected child
     */
    public void checkChildren(String namespace, Set<String> allowed) {
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (!namespace.equals(node.getNamespaceURI()) || !allowed.contains(node.getLocalName())) {
                    throw error(node.getNodeName(), "unexpected element");
                }
            } else if ((node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE)
                    && !node.getNodeValue().isBlank()) {
                throw error("text()", "unexpected text content");
            }
        }
    }

    /**
     * Creates a parse exception located at a child of this element.
     *
     * @param relative relative location, for example {@code @Importe}
     * @param message description of the problem
     * @return the exception
     */
    public CfdiParseException error(String relative, String message) {
        return new CfdiParseException(relative.isEmpty() ? path : path + "/" + relative, message);
    }

    private List<Element> elementChildren() {
        List<Element> result = new ArrayList<>();
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) node);
            }
        }
        return result;
    }

    /** Shortens a value quoted in an error message, so a huge attribute does not end up in logs. */
    private static String abbreviate(String value) {
        return SafeText.of(value, MAX_QUOTED);
    }
}
