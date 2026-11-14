package io.github.alktrip.cfdi4j.xml;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.xml.internal.SatResources;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XmlProcessingError;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Produces the cadena original of a CFDI 4.0 with the official SAT stylesheet
 * {@code cadenaoriginal_4_0.xslt}.
 *
 * <p>The stylesheet is XSLT 2.0, so Saxon-HE runs it; the JDK only implements XSLT 1.0. It and the
 * 34 files it includes are bundled unmodified and resolved offline (ADR 0007). The stylesheet is
 * compiled once; instances are immutable and thread-safe.
 *
 * <p>The cadena original covers {@code NoCertificado} but not {@code Sello} or {@code Certificado},
 * so compute it after {@code NoCertificado} is set. The result is exactly the text the stylesheet
 * produces, starting and ending with {@code ||}.
 */
public final class CadenaOriginal {

    /** Location of the official stylesheet, as published by the SAT. */
    public static final String XSLT_URL = "http://www.sat.gob.mx/sitio_internet/cfd/4/cadenaoriginal_4_0/cadenaoriginal_4_0.xslt";

    private static final class Shared {
        private static final CadenaOriginal INSTANCE = compile();
    }

    private static final System.Logger LOGGER = System.getLogger(CadenaOriginal.class.getName());

    private final Processor processor;
    private final XsltExecutable executable;

    private CadenaOriginal(Processor processor, XsltExecutable executable) {
        this.processor = processor;
        this.executable = executable;
    }

    /**
     * Returns the shared instance.
     *
     * @return the instance
     */
    public static CadenaOriginal defaults() {
        return Shared.INSTANCE;
    }

    /**
     * Returns the cadena original of a comprobante, written with {@link CfdiXml#defaults()}.
     *
     * @param comprobante the comprobante
     * @return the cadena original
     */
    public String of(Comprobante comprobante) {
        return of(CfdiXml.defaults().toBytes(Objects.requireNonNull(comprobante, "comprobante")));
    }

    /**
     * Returns the cadena original of a CFDI document, exactly as given.
     *
     * @param xml the document
     * @return the cadena original
     * @throws CfdiParseException if the document is not well-formed XML or declares a DOCTYPE
     */
    public String of(byte[] xml) {
        Objects.requireNonNull(xml, "xml");
        try {
            XdmNode document = processor.newDocumentBuilder()
                    .build(new SAXSource(secureReader(), new InputSource(new ByteArrayInputStream(xml))));
            Xslt30Transformer transformer = executable.load30();
            transformer.setResourceResolver(CadenaOriginal::rejectAll);
            transformer.setErrorReporter(CadenaOriginal::report);
            StringWriter out = new StringWriter();
            Serializer serializer = processor.newSerializer(out);
            transformer.transform(document.asSource(), serializer);
            return out.toString();
        } catch (SaxonApiException e) {
            throw new CfdiParseException("", "cannot produce the cadena original: " + e.getMessage(), e);
        }
    }

    /**
     * Compiles the bundled stylesheet. Package-private for the test that proves nothing is fetched.
     *
     * @return a new instance
     */
    static CadenaOriginal compile() {
        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        compiler.setResourceResolver(CadenaOriginal::bundled);
        compiler.setErrorReporter(CadenaOriginal::report);
        try (InputStream in = SatResources.open(XSLT_URL)) {
            return new CadenaOriginal(processor, compiler.compile(new StreamSource(in, XSLT_URL)));
        } catch (SaxonApiException e) {
            throw new Cfdi4jException("Cannot compile the bundled cadena original stylesheet", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Resolves {@code xsl:include} and {@code xsl:import} to bundled files only. */
    private static Source bundled(ResourceRequest request) throws XPathException {
        try {
            return new StreamSource(SatResources.open(request.uri), request.uri);
        } catch (Cfdi4jException e) {
            throw new XPathException(e.getMessage());
        }
    }

    /**
     * Sends Saxon warnings to {@link System.Logger} (level DEBUG) instead of standard error. The
     * official stylesheet triggers XTDE0540 on every run: both {@code cadenaoriginal_4_0.xslt} and
     * the included {@code vehiculousado.xslt} match {@code /}. XSLT 2.0 (section 6.4, recoverable
     * error XTRE0540) and XSLT 3.0 let the processor choose the last matching rule in declaration
     * order, which is the one in {@code cadenaoriginal_4_0.xslt}; the golden-file tests check the
     * result. Errors are not affected: they are thrown.
     */
    private static void report(XmlProcessingError error) {
        if (error.isWarning()) {
            LOGGER.log(System.Logger.Level.DEBUG, () -> "Saxon: " + error.getMessage());
        }
    }

    /** The stylesheet needs no resource at run time; any request is refused. */
    private static Source rejectAll(ResourceRequest request) throws XPathException {
        throw new XPathException("cfdi4j does not load resources while producing the cadena original: " + request.uri);
    }

    private static XMLReader secureReader() {
        SAXParserFactory factory = SAXParserFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }
    }
}
