package io.github.alktrip.cfdi4j.core.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Every violation found by a validation, never just the first one.
 *
 * @param violations the violations, in the order they were found
 */
public record ValidationResult(@SuppressWarnings("serial") List<Violation> violations) implements Serializable {

    private static final long serialVersionUID = 1L;


    private static final ValidationResult EMPTY = new ValidationResult(List.of());

    /** Copies the list. */
    public ValidationResult {
        violations = List.copyOf(violations);
    }

    /**
     * Returns a result without violations.
     *
     * @return the empty result
     */
    public static ValidationResult valid() {
        return EMPTY;
    }

    /**
     * Tells whether the document is valid: no violation has severity {@link Severity#ERROR}.
     * Warnings, including checks that could not run, do not make it invalid.
     *
     * @return {@code true} if there are no errors
     */
    public boolean isValid() {
        return violations.stream().noneMatch(v -> v.severity() == Severity.ERROR);
    }

    /**
     * Returns the violations with severity {@link Severity#ERROR}.
     *
     * @return the errors
     */
    public List<Violation> errors() {
        return violations.stream().filter(v -> v.severity() == Severity.ERROR).toList();
    }

    /**
     * Returns the violations with severity {@link Severity#WARNING}.
     *
     * @return the warnings
     */
    public List<Violation> warnings() {
        return violations.stream().filter(v -> v.severity() == Severity.WARNING).toList();
    }

    /**
     * Tells whether a violation with the given code was found.
     *
     * @param code the code, for example {@code CFDI40108}
     * @return {@code true} if present
     */
    public boolean has(String code) {
        return violations.stream().anyMatch(v -> v.code().equals(code));
    }

    /**
     * Returns a result with the violations of both results, this one's first.
     *
     * @param other the other result
     * @return the combined result
     */
    public ValidationResult and(ValidationResult other) {
        if (other.violations.isEmpty()) {
            return this;
        }
        List<Violation> all = new ArrayList<>(violations);
        all.addAll(other.violations);
        return new ValidationResult(all);
    }
}
