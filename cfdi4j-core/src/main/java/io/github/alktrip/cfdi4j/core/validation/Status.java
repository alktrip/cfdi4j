package io.github.alktrip.cfdi4j.core.validation;

/** Outcome of the check that produced a violation (ADR 0004). */
public enum Status {
    /** The check ran and the document does not satisfy it. */
    FAILED,
    /**
     * The check could not run, for example because it needs a catalog provider or a SAT service
     * that is not configured, or a schema that is not bundled. Never treated as passed.
     */
    NOT_VERIFIED
}
