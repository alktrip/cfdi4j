package io.github.alktrip.cfdi4j.xml.internal;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

/**
 * JDK XML factories configured to reject DTDs and external entities, and never to fetch anything.
 *
 * <p>The JDK default implementations are used explicitly ({@code newDefaultInstance}) so that other
 * providers on the class path, such as Saxon, never replace them.
 */
public final class SecureXml {

    private static final DocumentBuilderFactory DOCUMENTS = documentBuilderFactory();
    private static final TransformerFactory TRANSFORMERS = transformerFactory();
    private static final XMLInputFactory INPUTS = newInputFactory();
    private static final XMLOutputFactory OUTPUTS = XMLOutputFactory.newDefaultFactory();

    private SecureXml() {}

    /** Returns a new namespace-aware document builder that rejects DOCTYPE declarations. */
    public static DocumentBuilder documentBuilder() {
        synchronized (DOCUMENTS) {
            try {
                return DOCUMENTS.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Returns a new identity transformer. */
    public static Transformer identityTransformer() {
        synchronized (TRANSFORMERS) {
            try {
                return TRANSFORMERS.newTransformer();
            } catch (TransformerConfigurationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Returns the StAX input factory (thread-safe once configured). */
    public static XMLInputFactory inputFactory() {
        return INPUTS;
    }

    /** Returns the StAX output factory (thread-safe once configured). */
    public static XMLOutputFactory outputFactory() {
        return OUTPUTS;
    }

    private static DocumentBuilderFactory documentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static TransformerFactory transformerFactory() {
        TransformerFactory factory = TransformerFactory.newDefaultInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }

    private static XMLInputFactory newInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
        return factory;
    }
}
