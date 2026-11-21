package io.github.alktrip.cfdi4j.xml;

import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.xml.internal.CfdiReader;
import io.github.alktrip.cfdi4j.xml.internal.CfdiWriter;
import io.github.alktrip.cfdi4j.xml.internal.ComplementoRegistry;
import io.github.alktrip.cfdi4j.xml.internal.SecureXml;
import io.github.alktrip.cfdi4j.xml.spi.ComplementoXml;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads and writes CFDI 4.0 XML.
 *
 * <p>Instances are immutable and thread-safe. {@link #defaults()} uses the complement mappers found
 * with {@link ServiceLoader}; {@link #builder()} uses only the mappers registered explicitly
 * (ADR 0006).
 *
 * <pre>{@code
 * String xml = CfdiXml.defaults().toXml(comprobante);
 * Comprobante parsed = CfdiXml.defaults().parse(xml);
 * }</pre>
 */
public final class CfdiXml {

    private static final class Defaults {
        private static final CfdiXml INSTANCE =
                new CfdiXml(ServiceLoader.load(ComplementoXml.class).stream()
                        .map(ServiceLoader.Provider::get).toList());
    }

    private final ComplementoRegistry registry;
    private final CfdiWriter writer;
    private final CfdiReader reader;

    private CfdiXml(List<? extends ComplementoXml> mappers) {
        this.registry = new ComplementoRegistry(mappers);
        this.writer = new CfdiWriter(registry);
        this.reader = new CfdiReader(registry);
    }

    /**
     * Returns the shared instance with the complement mappers available through
     * {@link ServiceLoader}, ordered by namespace.
     *
     * @return the default instance
     * @throws io.github.alktrip.cfdi4j.core.Cfdi4jException if two mappers declare the same namespace
     */
    public static CfdiXml defaults() {
        return Defaults.INSTANCE;
    }

    /**
     * Starts configuring an instance with explicitly registered complement mappers only.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the namespaces of the complements this instance can map, in order.
     *
     * @return the namespaces
     */
    public List<String> complementNamespaces() {
        return registry.all().stream().map(ComplementoXml::namespace).toList();
    }

    /**
     * Writes the comprobante as UTF-8 XML.
     *
     * @param comprobante the comprobante
     * @param out the target; not closed
     */
    public void write(Comprobante comprobante, OutputStream out) {
        writer.write(Objects.requireNonNull(comprobante, "comprobante"), Objects.requireNonNull(out, "out"));
    }

    /**
     * Returns the comprobante as UTF-8 XML bytes.
     *
     * @param comprobante the comprobante
     * @return the XML
     */
    public byte[] toBytes(Comprobante comprobante) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(comprobante, out);
        return out.toByteArray();
    }

    /**
     * Returns the comprobante as an XML string.
     *
     * @param comprobante the comprobante
     * @return the XML, with an XML declaration that states UTF-8
     */
    public String toXml(Comprobante comprobante) {
        return new String(toBytes(comprobante), StandardCharsets.UTF_8);
    }

    /**
     * Parses a CFDI 4.0 document.
     *
     * @param xml the XML text
     * @return the comprobante
     * @throws CfdiParseException if the text is not well-formed or is not a CFDI 4.0 comprobante
     */
    public Comprobante parse(String xml) {
        return parse(new InputSource(new StringReader(Objects.requireNonNull(xml, "xml"))));
    }

    /**
     * Parses a CFDI 4.0 document, detecting its encoding from the XML declaration.
     *
     * <p>Amounts are kept as numbers with their scale: a leading zero, which the SAT schema allows
     * ({@code 021.00}), is not kept, so writing the comprobante again may change the cadena
     * original. To check a received document, verify its original bytes.
     *
     * @param xml the XML bytes
     * @return the comprobante
     * @throws CfdiParseException if the bytes are not well-formed XML or not a CFDI 4.0 comprobante
     */
    public Comprobante parse(byte[] xml) {
        return parse(new ByteArrayInputStream(Objects.requireNonNull(xml, "xml")));
    }

    /**
     * Parses a CFDI 4.0 document, detecting its encoding from the XML declaration.
     *
     * @param in the XML; not closed
     * @return the comprobante
     * @throws CfdiParseException if the input is not well-formed XML or not a CFDI 4.0 comprobante
     * @throws UncheckedIOException if reading fails
     */
    public Comprobante parse(InputStream in) {
        return parse(new InputSource(Objects.requireNonNull(in, "in")));
    }

    private Comprobante parse(InputSource source) {
        Document document;
        try {
            var builder = SecureXml.documentBuilder();
            builder.setErrorHandler(new DefaultHandler()); // fatal errors throw; nothing is printed
            document = builder.parse(source);
        } catch (SAXException e) {
            throw new CfdiParseException("", "not well-formed XML: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException | CharConversionException e) {
            // The document declares an unknown encoding or its bytes do not match it.
            throw new CfdiParseException("", "unreadable XML encoding: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return reader.read(document);
    }

    /** Configures a {@link CfdiXml} with explicitly registered complement mappers. */
    public static final class Builder {
        private final List<ComplementoXml> mappers = new ArrayList<>();

        private Builder() {}

        /**
         * Registers a complement mapper.
         *
         * @param mapper the mapper
         * @return this builder
         */
        public Builder register(ComplementoXml mapper) {
            mappers.add(Objects.requireNonNull(mapper, "mapper"));
            return this;
        }

        /**
         * Creates the instance.
         *
         * @return the instance
         * @throws io.github.alktrip.cfdi4j.core.Cfdi4jException if two mappers declare the same
         *     namespace or type
         */
        public CfdiXml build() {
            return new CfdiXml(mappers);
        }
    }
}
