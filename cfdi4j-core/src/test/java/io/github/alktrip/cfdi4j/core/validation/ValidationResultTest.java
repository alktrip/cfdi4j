package io.github.alktrip.cfdi4j.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationResultTest {

    private static final Violation ERROR = new Violation("CFDI40108", Severity.ERROR, Status.FAILED, "/cfdi:Comprobante/@SubTotal", "mal");
    private static final Violation NOT_VERIFIED = new Violation("CFDI4J-002", Severity.WARNING, Status.NOT_VERIFIED, "", "sin verificar");

    @Test
    void emptyResultIsValid() {
        assertThat(ValidationResult.valid().isValid()).isTrue();
        assertThat(ValidationResult.valid().violations()).isEmpty();
    }

    @Test
    void anErrorMakesTheResultInvalidButWarningsDoNot() {
        assertThat(new ValidationResult(List.of(NOT_VERIFIED)).isValid()).isTrue();
        assertThat(new ValidationResult(List.of(NOT_VERIFIED, ERROR)).isValid()).isFalse();
    }

    @Test
    void errorsWarningsAndCodeLookup() {
        ValidationResult result = new ValidationResult(List.of(NOT_VERIFIED, ERROR));

        assertThat(result.errors()).containsExactly(ERROR);
        assertThat(result.warnings()).containsExactly(NOT_VERIFIED);
        assertThat(result.has("CFDI40108")).isTrue();
        assertThat(result.has("CFDI40101")).isFalse();
    }

    @Test
    void combinesResultsKeepingOrder() {
        ValidationResult first = new ValidationResult(List.of(ERROR));

        assertThat(first.and(ValidationResult.valid())).isSameAs(first);
        assertThat(first.and(new ValidationResult(List.of(NOT_VERIFIED))).violations()).containsExactly(ERROR, NOT_VERIFIED);
    }

    @Test
    void violationToStringShowsCodeLocationAndMessage() {
        assertThat(ERROR).hasToString("CFDI40108 /cfdi:Comprobante/@SubTotal: mal");
        assertThat(NOT_VERIFIED).hasToString("CFDI4J-002: sin verificar");
    }
}
