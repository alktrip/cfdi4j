package io.github.alktrip.cfdi4j.core.validation;

/** How completely cfdi4j implements a code of the error matrix ({@code docs/rules-coverage.md}). */
public enum RuleStatus {
    /** Checked completely. */
    IMPLEMENTED("implementada"),
    /** Checked in part; the note says what is not. */
    PARTIAL("parcial"),
    /** Needs data or a service of the SAT; checked through a port when one is configured. */
    EXTERNAL_SERVICE("requiere servicio externo"),
    /** Does not apply to an issuer-side library, for example checks only the PAC can make. */
    NOT_APPLICABLE("no aplica");

    private final String etiqueta;

    RuleStatus(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Returns the Spanish label used in the coverage report.
     *
     * @return the label
     */
    public String etiqueta() {
        return etiqueta;
    }
}
