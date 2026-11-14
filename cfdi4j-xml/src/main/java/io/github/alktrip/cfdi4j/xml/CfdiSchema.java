package io.github.alktrip.cfdi4j.xml;

import io.github.alktrip.cfdi4j.core.internal.Messages;
import io.github.alktrip.cfdi4j.core.validation.Severity;
import io.github.alktrip.cfdi4j.core.validation.Status;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.xml.internal.Namespaces;
import io.github.alktrip.cfdi4j.xml.internal.SatResourceResolver;
import io.github.alktrip.cfdi4j.xml.internal.SatResources;
import io.github.alktrip.cfdi4j.xml.internal.SchemaFilter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Validates CFDI 4.0 XML against the official SAT schemas bundled in this module (ADR 0007):
 * {@code cfdv40.xsd} with {@code catCFDI.xsd} and {@code tdCFDI.xsd}, the TimbreFiscalDigital 1.1
 * and the complemento de Pagos 2.0 (with {@code catPagos.xsd}).
 *
 * <p>The schema is compiled once and shared; instances are immutable and thread-safe. Nothing is
 * downloaded: imports resolve to bundled files and any other location is an error. Every problem
 * is returned, not only the first one:
 *
 * <ul>
 *   <li>{@code CFDI4J-001} (error): the XML does not conform to the schema;
 *   <li>{@code CFDI4J-002} (warning, not verified): a complement whose schema is not bundled;
 *   <li>{@code CFDI4J-003} (error): the document is not well-formed XML, or declares a DOCTYPE.
 * </ul>
 *
 * <p>Note that {@code catCFDI.xsd} enumerates the keys of every catalog, including
 * c_CodigoPostal, c_ClaveProdServ and c_ClaveUnidad, so this validation also checks that keys exist
 * in the catalog version of that schema. The content of {@code Addenda} is not validated.
 */
public final class CfdiSchema {

    /** Code: the XML does not conform to the SAT XSD schema. */
    public static final String XSD_INVALIDO = "CFDI4J-001";
    /** Code: a complement was not validated because its schema is not bundled. */
    public static final String COMPLEMENTO_NO_VALIDADO = "CFDI4J-002";
    /** Code: the document is not well-formed XML. */
    public static final String XML_MAL_FORMADO = "CFDI4J-003";

    private static final Locale SPANISH = Locale.forLanguageTag("es-MX");
    private static final String TFD = "http://www.sat.gob.mx/TimbreFiscalDigital";
    private static final String PAGOS20 = "http://www.sat.gob.mx/Pagos20";

    /** Schemas compiled into the validator, by target namespace. */
    private static final Map<String, String> SCHEMAS = Map.of(
            Namespaces.CFDI, Namespaces.CFDI_SCHEMA_LOCATION,
            TFD, "http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd",
            PAGOS20, "http://www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xsd");

    private static final class Shared {
        private static final Schema SCHEMA = compile();
        private static final CfdiSchema DEFAULT = new CfdiSchema(SCHEMA, SPANISH);
    }

    private final Schema schema;
    private final Messages messages;

    CfdiSchema(Schema schema, Locale locale) {
        this.schema = schema;
        this.messages = Messages.load(name -> CfdiSchema.class.getResourceAsStream(name), "messages", locale);
    }

    /**
     * Returns the shared validator, with messages in Spanish (es-MX).
     *
     * @return the validator
     */
    public static CfdiSchema defaults() {
        return Shared.DEFAULT;
    }

    /**
     * Returns a validator that writes messages in another language (Spanish and English are
     * included; other languages fall back to English). The compiled schema is shared.
     *
     * <p>The schema detail embedded in {@code CFDI4J-001} and {@code CFDI4J-003} messages (for
     * example {@code cvc-complex-type.4: ...}) is produced by the JDK in the JVM default language;
     * the JDK ignores a per-validator locale.
     *
     * @param locale the language
     * @return the validator
     */
    public CfdiSchema withLocale(Locale locale) {
        return new CfdiSchema(schema, Objects.requireNonNull(locale, "locale"));
    }

    /**
     * Returns the namespaces validated strictly, in no particular order.
     *
     * @return the namespaces
     */
    public static Set<String> validatedNamespaces() {
        return SCHEMAS.keySet();
    }

    /**
     * Validates a sealed (or stamped) CFDI as it is.
     *
     * @param xml the document
     * @return every problem found
     */
    public ValidationResult validate(byte[] xml) {
        return validate(xml, false);
    }

    /**
     * Validates a CFDI that is not sealed yet: missing {@code Sello}, {@code NoCertificado} and
     * {@code Certificado} are not reported. Values that are present are validated normally.
     *
     * @param xml the document
     * @return every problem found
     */
    public ValidationResult validateUnsealed(byte[] xml) {
        return validate(xml, true);
    }

    private ValidationResult validate(byte[] xml, boolean unsealed) {
        Objects.requireNonNull(xml, "xml");
        List<Violation> violations = new ArrayList<>();
        ValidatorHandler validator = schema.newValidatorHandler();
        validator.setResourceResolver(new SatResourceResolver());
        try {
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        validator.setErrorHandler(new Collector(violations));
        SchemaFilter filter = new SchemaFilter(validator, SCHEMAS.keySet(), unsealed);
        try {
            XMLReader reader = newReader();
            reader.setContentHandler(filter);
            reader.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    // Parser warnings carry no validity information.
                }

                @Override
                public void error(SAXParseException e) throws SAXParseException {
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXParseException {
                    throw e;
                }
            });
            reader.parse(new InputSource(new ByteArrayInputStream(xml)));
        } catch (MalformedException e) {
            violations.add(violation(XML_MAL_FORMADO, e.getCause()));
            return new ValidationResult(violations);
        } catch (SAXParseException e) {
            violations.add(violation(XML_MAL_FORMADO, e));
            return new ValidationResult(violations);
        } catch (SAXException e) {
            throw new IllegalStateException("Unexpected validation failure", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        filter.skippedNamespaces().forEach((namespace, line) -> violations.add(new Violation(COMPLEMENTO_NO_VALIDADO,
                Severity.WARNING, Status.NOT_VERIFIED, "line " + line, messages.format(COMPLEMENTO_NO_VALIDADO, namespace))));
        return new ValidationResult(violations);
    }

    private Violation violation(String code, SAXParseException e) {
        String location = e.getLineNumber() < 0 ? "" : "line " + e.getLineNumber() + ", column " + e.getColumnNumber();
        return new Violation(code, Severity.ERROR, Status.FAILED, location, messages.format(code, e.getMessage()));
    }

    /** Collects schema errors and keeps going, so every problem is reported. */
    private final class Collector implements ErrorHandler {
        private final List<Violation> violations;

        Collector(List<Violation> violations) {
            this.violations = violations;
        }

        @Override
        public void warning(SAXParseException e) {
            // Schema warnings carry no validity information.
        }

        @Override
        public void error(SAXParseException e) {
            violations.add(violation(XSD_INVALIDO, e));
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw new MalformedException(e);
        }
    }

    /** A fatal error reported through the validator, unwound as malformed input. */
    private static final class MalformedException extends SAXException {
        private static final long serialVersionUID = 1L;

        MalformedException(SAXParseException cause) {
            super(cause);
        }

        @Override
        public synchronized SAXParseException getCause() {
            return (SAXParseException) super.getCause();
        }
    }

    private static XMLReader newReader() throws SAXException {
        SAXParserFactory factory = SAXParserFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Compiles the bundled schemas. Package-private for the test that proves nothing is fetched.
     *
     * @return the schema
     */
    static Schema compile() {
        SchemaFactory factory = SchemaFactory.newDefaultInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        factory.setResourceResolver(new SatResourceResolver());
        List<InputStream> streams = new ArrayList<>();
        try {
            Source[] sources = SCHEMAS.values().stream().sorted().map(url -> {
                InputStream in = SatResources.open(url);
                streams.add(in);
                StreamSource source = new StreamSource(in, url);
                return (Source) source;
            }).toArray(Source[]::new);
            return factory.newSchema(sources);
        } catch (SAXException e) {
            throw new IllegalStateException("Cannot compile the bundled SAT schemas", e);
        } finally {
            for (InputStream in : streams) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Closing a bundled resource cannot fail in a way that matters.
                }
            }
        }
    }
}
