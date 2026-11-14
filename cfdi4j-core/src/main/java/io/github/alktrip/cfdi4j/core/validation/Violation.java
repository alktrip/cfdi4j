package io.github.alktrip.cfdi4j.core.validation;

import java.util.Objects;

/**
 * A problem found while validating a CFDI.
 *
 * <p>The {@code code} is the stable contract: an official code of the SAT error matrix
 * ({@code CFDI40xxx}) for the rules that implement that matrix entry, or a cfdi4j code
 * ({@code CFDI4J-xxx}) for checks the library adds (ADR 0004). The message is for people and may
 * change between versions.
 *
 * @param code stable error code
 * @param severity how serious the problem is
 * @param status whether the check failed or could not run
 * @param location where the problem is: an XPath-like location, or {@code line N, column M}
 * @param message human-readable description, in the locale the validator was configured with
 */
public record Violation(String code, Severity severity, Status status, String location, String message) {

    /** Checks that no component is null. */
    public Violation {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(message, "message");
    }

    /** Returns {@code code location: message}. */
    @Override
    public String toString() {
        return code + (location.isEmpty() ? "" : " " + location) + ": " + message;
    }
}
