package io.github.alktrip.cfdi4j.core.validation;

/**
 * A validation rule, identified by its error code (ADR 0004). Rules are stateless and
 * thread-safe; they report through the {@link ValidationContext}.
 */
public interface Rule {

    /**
     * Returns the error code the rule reports.
     *
     * @return the code, for example {@code CFDI40108}
     */
    String code();

    /**
     * Returns how completely the code is checked.
     *
     * @return the status
     */
    RuleStatus status();

    /**
     * Returns what is and is not checked, in Spanish, for the coverage report.
     *
     * @return the note, possibly empty
     */
    String nota();

    /**
     * Checks the comprobante of the context and reports every violation found.
     *
     * @param context the context
     */
    void check(ValidationContext context);
}
