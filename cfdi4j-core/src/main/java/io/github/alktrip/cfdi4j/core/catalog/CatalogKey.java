package io.github.alktrip.cfdi4j.core.catalog;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A key of a SAT catalog, modeled as an "open enum" (ADR 0003).
 *
 * <p>Each catalog class exposes its known keys as constants and an {@code of(String)} factory that
 * never fails: a key that is not in the catalog produces an instance whose {@link #isKnown()} is
 * {@code false}. An unknown key is a validation problem, not a parsing problem.
 *
 * <p>Two instances are equal when they belong to the same catalog and have the same key.
 * Subclasses are generated from the official catalog file and live in this package only.
 */
public abstract class CatalogKey {

    private final String clave;
    private final String descripcion;
    private final Vigencia vigencia;
    private final boolean known;

    CatalogKey(String clave, String descripcion, Vigencia vigencia, boolean known) {
        this.clave = Objects.requireNonNull(clave, "clave");
        this.descripcion = descripcion;
        this.vigencia = vigencia;
        this.known = known;
    }

    /**
     * Returns the key exactly as it appears in the catalog and in the XML.
     *
     * @return the key
     */
    public final String clave() {
        return clave;
    }

    /**
     * Tells whether the key exists in the catalog version bundled with this library.
     *
     * @return {@code true} for a key of the catalog
     */
    public final boolean isKnown() {
        return known;
    }

    /**
     * Returns the official description of the key.
     *
     * @return the description, empty for an unknown key or a catalog without descriptions
     */
    public final Optional<String> descripcion() {
        return Optional.ofNullable(descripcion);
    }

    /**
     * Returns the validity period of the key as published in the catalog.
     *
     * @return the validity period, empty for an unknown key or for catalogs without validity dates
     */
    public final Optional<Vigencia> vigencia() {
        return Optional.ofNullable(vigencia);
    }

    /**
     * Tells whether the key is known and valid on the given date.
     *
     * @param fecha the date to check
     * @return {@code true} if the key is known and the date falls within its validity period, or
     *     the catalog publishes no validity period
     */
    public final boolean isVigente(LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha");
        return known && (vigencia == null || vigencia.contains(fecha));
    }

    /** Returns the key. */
    @Override
    public final String toString() {
        return clave;
    }
}
