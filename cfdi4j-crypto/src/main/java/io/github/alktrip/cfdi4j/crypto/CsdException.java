package io.github.alktrip.cfdi4j.crypto;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;

/** Thrown when a CSD certificate or private key cannot be read or used. */
public final class CsdException extends Cfdi4jException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message description of the problem
     */
    public CsdException(String message) {
        super(message);
    }

    /**
     * Creates the exception with its cause.
     *
     * @param message description of the problem
     * @param cause underlying cause
     */
    public CsdException(String message, Throwable cause) {
        super(message, cause);
    }
}
