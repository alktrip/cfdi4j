package io.github.alktrip.cfdi4j.core;

/**
 * Root of every exception thrown by cfdi4j.
 *
 * <p>All cfdi4j exceptions are unchecked. Rule violations are not exceptions: they are reported
 * in a validation result.
 */
public class Cfdi4jException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message description of the problem
     */
    public Cfdi4jException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and its cause.
     *
     * @param message description of the problem
     * @param cause underlying cause
     */
    public Cfdi4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
