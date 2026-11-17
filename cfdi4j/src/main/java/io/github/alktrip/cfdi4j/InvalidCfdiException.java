package io.github.alktrip.cfdi4j;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.util.stream.Collectors;

/** Thrown when a comprobante with validation errors is about to be sealed. */
public final class InvalidCfdiException extends Cfdi4jException {

    private static final long serialVersionUID = 1L;

    /** The violations that prevented the operation. */
    @SuppressWarnings("serial") // ValidationResult holds an immutable List.copyOf list
    private final ValidationResult result;

    /**
     * Creates the exception.
     *
     * @param result the validation result, with at least one error
     */
    public InvalidCfdiException(ValidationResult result) {
        super("The comprobante is not valid: " + result.errors().stream().map(Violation::toString).collect(Collectors.joining("; ")));
        this.result = result;
    }

    /**
     * Returns every violation found.
     *
     * @return the validation result
     */
    public ValidationResult result() {
        return result;
    }
}
