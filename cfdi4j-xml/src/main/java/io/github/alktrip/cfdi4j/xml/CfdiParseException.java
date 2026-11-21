package io.github.alktrip.cfdi4j.xml;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.internal.SafeText;

/** Thrown when an XML document cannot be read as a CFDI 4.0 comprobante. */
public final class CfdiParseException extends Cfdi4jException {

    private static final long serialVersionUID = 1L;

    /** XPath-like location of the problem; empty if unknown. */
    private final String location;

    /**
     * Creates the exception.
     *
     * @param location XPath-like location of the problem, for example
     *     {@code /cfdi:Comprobante/cfdi:Conceptos/cfdi:Concepto[2]/@Importe}; empty if unknown
     * @param message description of the problem
     */
    public CfdiParseException(String location, String message) {
        super(safe(location, message));
        this.location = location;
    }

    /**
     * Creates the exception with a cause.
     *
     * @param location location of the problem; empty if unknown
     * @param message description of the problem
     * @param cause underlying cause
     */
    public CfdiParseException(String location, String message, Throwable cause) {
        super(safe(location, message), cause);
        this.location = location;
    }

    /**
     * Returns the location of the problem.
     *
     * @return the location, empty if unknown
     */
    public String location() {
        return location;
    }

    /** The message quotes values of the document: escape control characters and bound its length. */
    private static String safe(String location, String message) {
        return SafeText.of(location.isEmpty() ? message : location + ": " + message, 2000);
    }
}
