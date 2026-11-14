package io.github.alktrip.cfdi4j;

import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of verifying a CFDI: whether its seal (and, if stamped, its timbre) matches the content.
 *
 * @param result every problem found; errors mean the document was altered or cannot be trusted
 * @param comprobante the parsed comprobante, unless the document could not be read
 * @param cadenaOriginal the cadena original computed from the document, unless it could not be read
 */
public record VerificationResult(ValidationResult result, Optional<Comprobante> comprobante, Optional<String> cadenaOriginal) {

    /** Checks that no component is null. */
    public VerificationResult {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(comprobante, "comprobante");
        Objects.requireNonNull(cadenaOriginal, "cadenaOriginal");
    }

    /**
     * Tells whether the document is intact: no error was found. Checks that could not run (for
     * example the {@code SelloSAT} without the SAT certificate) are warnings.
     *
     * @return {@code true} if intact
     */
    public boolean isValid() {
        return result.isValid();
    }

    /**
     * Returns the errors.
     *
     * @return the errors
     */
    public List<Violation> failures() {
        return result.errors();
    }
}
