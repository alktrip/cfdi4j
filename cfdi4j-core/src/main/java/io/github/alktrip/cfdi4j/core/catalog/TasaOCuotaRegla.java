package io.github.alktrip.cfdi4j.core.catalog;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One row of the catálogo {@code c_TasaOCuota}.
 *
 * @param rango {@code true} for a range ("Rango"), {@code false} for a fixed value ("Fijo")
 * @param minimo lowest allowed value; equal to {@code maximo} for a fixed value
 * @param maximo highest allowed value, or the fixed value
 * @param impuesto tax the row applies to
 * @param impuestoPublicado tax label exactly as published, for example "IVA Crédito aplicado del 50%"
 * @param tipoFactor factor the row applies to
 * @param traslado whether the value can be used in a Traslado
 * @param retencion whether the value can be used in a Retencion
 * @param vigencia validity period of the row
 */
public record TasaOCuotaRegla(
        boolean rango,
        BigDecimal minimo,
        BigDecimal maximo,
        Impuesto impuesto,
        String impuestoPublicado,
        TipoFactor tipoFactor,
        boolean traslado,
        boolean retencion,
        Vigencia vigencia) {

    /** Checks that no component is null and that the range is well formed. */
    public TasaOCuotaRegla {
        Objects.requireNonNull(minimo, "minimo");
        Objects.requireNonNull(maximo, "maximo");
        Objects.requireNonNull(impuesto, "impuesto");
        Objects.requireNonNull(impuestoPublicado, "impuestoPublicado");
        Objects.requireNonNull(tipoFactor, "tipoFactor");
        Objects.requireNonNull(vigencia, "vigencia");
        if (minimo.compareTo(maximo) > 0) {
            throw new IllegalArgumentException("minimo " + minimo + " is greater than maximo " + maximo);
        }
    }

    /**
     * Tells whether a value is allowed by this row: equal to the fixed value, or within the range
     * (both ends inclusive). Values are compared numerically, so {@code 0.16} matches
     * {@code 0.160000}.
     *
     * @param valor the TasaOCuota value
     * @return {@code true} if the value matches this row
     */
    public boolean admite(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor");
        return valor.compareTo(minimo) >= 0 && valor.compareTo(maximo) <= 0;
    }
}
