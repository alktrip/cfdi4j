package io.github.alktrip.cfdi4j.xml.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Sits between the parser and the schema validator.
 *
 * <p>{@code cfdv40.xsd} declares the content of {@code Complemento}, {@code ComplementoConcepto}
 * and {@code Addenda} as {@code xs:any} with strict processing, so a complement whose schema is not
 * bundled would be reported as invalid even when it is correct. This filter therefore:
 *
 * <ul>
 *   <li>passes complements whose namespace has a bundled schema, which are validated strictly;
 *   <li>removes complements without a bundled schema, and records their namespaces so that the
 *       caller reports them as not verified;
 *   <li>removes the content of {@code Addenda}, which is not fiscal data;
 *   <li>drops a container left empty by the above, since every container is optional.
 * </ul>
 *
 * <p>In unsealed mode it also supplies empty {@code Sello} and {@code Certificado} and a
 * placeholder {@code NoCertificado} when they are absent, so a comprobante can be checked before
 * it is sealed. Values that are present are never changed.
 */
public final class SchemaFilter implements ContentHandler {

    private static final Set<String> CONTAINERS = Set.of("Complemento", "ComplementoConcepto", "Addenda");

    private final ContentHandler next;
    private final Set<String> validatedNamespaces;
    private final boolean unsealed;
    private final Map<String, Integer> skippedNamespaces = new LinkedHashMap<>();
    private Locator locator;

    private int depth;
    private int skipUntilDepth = -1;
    private Pending container;

    /** A container whose start tag is held back until its first validated child. */
    private static final class Pending {
        final String uri;
        final String localName;
        final String qName;
        final Attributes atts;
        final int depth;
        final boolean addenda;
        boolean emitted;

        Pending(String uri, String localName, String qName, Attributes atts, int depth) {
            this.uri = uri;
            this.localName = localName;
            this.qName = qName;
            this.atts = atts;
            this.depth = depth;
            this.addenda = "Addenda".equals(localName);
        }
    }

    public SchemaFilter(ContentHandler next, Set<String> validatedNamespaces, boolean unsealed) {
        this.next = next;
        this.validatedNamespaces = validatedNamespaces;
        this.unsealed = unsealed;
    }

    /** Namespaces of the complements removed because their schema is not bundled, with the line of the first one. */
    public Map<String, Integer> skippedNamespaces() {
        return skippedNamespaces;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        next.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        next.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        next.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        next.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        next.endPrefixMapping(prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        depth++;
        if (skipUntilDepth >= 0) {
            return;
        }
        if (container != null && depth == container.depth + 1) {
            if (container.addenda || !validatedNamespaces.contains(uri)) {
                if (!container.addenda) {
                    skippedNamespaces.putIfAbsent(uri, locator == null ? -1 : locator.getLineNumber());
                }
                skipUntilDepth = depth;
                return;
            }
            emitContainer();
        }
        if (Namespaces.CFDI.equals(uri) && CONTAINERS.contains(localName)) {
            container = new Pending(uri, localName, qName, new AttributesImpl(atts), depth);
            return;
        }
        if (unsealed && depth == 1 && Namespaces.CFDI.equals(uri) && "Comprobante".equals(localName)) {
            next.startElement(uri, localName, qName, withSealPlaceholders(atts));
            return;
        }
        next.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            if (skipUntilDepth >= 0) {
                if (depth == skipUntilDepth) {
                    skipUntilDepth = -1;
                }
                return;
            }
            if (container != null && depth == container.depth) {
                if (container.emitted) {
                    next.endElement(uri, localName, qName);
                }
                container = null;
                return;
            }
            next.endElement(uri, localName, qName);
        } finally {
            depth--;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (passText()) {
            next.characters(ch, start, length);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (passText()) {
            next.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (passText()) {
            next.processingInstruction(target, data);
        }
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        next.skippedEntity(name);
    }

    private boolean passText() {
        return skipUntilDepth < 0 && (container == null || container.emitted);
    }

    private void emitContainer() throws SAXException {
        if (!container.emitted) {
            next.startElement(container.uri, container.localName, container.qName, container.atts);
            container.emitted = true;
        }
    }

    private static Attributes withSealPlaceholders(Attributes atts) {
        AttributesImpl result = new AttributesImpl(atts);
        addIfAbsent(result, "Sello", "");
        addIfAbsent(result, "NoCertificado", "00000000000000000000");
        addIfAbsent(result, "Certificado", "");
        return result;
    }

    private static void addIfAbsent(AttributesImpl atts, String name, String value) {
        if (atts.getIndex("", name) < 0) {
            atts.addAttribute("", name, name, "CDATA", value);
        }
    }
}
