package io.github.alktrip.cfdi4j.core.validation;

/** How serious a violation is. */
public enum Severity {
    /** The document is not valid. */
    ERROR,
    /** The document may be valid, but something needs attention or could not be checked. */
    WARNING
}
