package io.github.alktrip.cfdi4j.xml.spi;

import io.github.alktrip.cfdi4j.core.model.Complemento;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Maps one complement between its model and its XML (ADR 0006).
 *
 * <p>Implementations are registered as {@link java.util.ServiceLoader} providers
 * ({@code provides io.github.alktrip.cfdi4j.xml.spi.ComplementoXml with ...}) or explicitly with
 * {@code CfdiXml.builder()}. They must be stateless and thread-safe.
 */
public interface ComplementoXml {

    /**
     * Returns the XML namespace of the complement element. At most one provider may exist per
     * namespace.
     *
     * @return the namespace URI
     */
    String namespace();

    /**
     * Returns the prefix used when writing, for example {@code pago20}.
     *
     * @return the prefix
     */
    String prefix();

    /**
     * Returns the location of the complement's XSD, written in {@code xsi:schemaLocation}.
     *
     * @return the schema location URL
     */
    String schemaLocation();

    /**
     * Returns the model type this provider writes.
     *
     * @return the complement type
     */
    Class<? extends Complemento> type();

    /**
     * Reads the complement element.
     *
     * @param element the element, in this provider's namespace
     * @return the complement
     * @throws io.github.alktrip.cfdi4j.xml.CfdiParseException if the element is not valid
     */
    Complemento read(XmlElement element);

    /**
     * Writes the complement element. The prefix is already declared on the root element.
     *
     * @param complemento the complement, an instance of {@link #type()}
     * @param writer the writer, positioned where the element starts
     * @throws XMLStreamException if writing fails
     */
    void write(Complemento complemento, XMLStreamWriter writer) throws XMLStreamException;
}
