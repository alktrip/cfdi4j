package io.github.alktrip.cfdi4j.core.model;

/**
 * A complement of the comprobante ({@code cfdi:Complemento}) or of a concepto
 * ({@code cfdi:ComplementoConcepto}). Each complement is identified by its XML namespace.
 */
public interface Complemento {

    /**
     * Returns the XML namespace of the complement, for example
     * {@code http://www.sat.gob.mx/Pagos20}.
     *
     * @return the namespace URI
     */
    String namespace();
}
