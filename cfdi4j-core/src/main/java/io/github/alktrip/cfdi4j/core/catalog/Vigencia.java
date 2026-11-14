package io.github.alktrip.cfdi4j.core.catalog;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Validity period of a catalog entry ("Fecha inicio de vigencia" and "Fecha fin de vigencia").
 *
 * @param inicio first day of validity
 * @param fin last day of validity, empty while the entry is open-ended
 */
public record Vigencia(LocalDate inicio, Optional<LocalDate> fin) {

    /**
     * Validates the components.
     *
     * @throws IllegalArgumentException if {@code fin} is before {@code inicio}
     */
    public Vigencia {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (fin.isPresent() && fin.get().isBefore(inicio)) {
            throw new IllegalArgumentException("fin " + fin.get() + " is before inicio " + inicio);
        }
    }

    /**
     * Creates an open-ended validity period.
     *
     * @param inicio first day of validity
     * @return the period
     */
    public static Vigencia desde(LocalDate inicio) {
        return new Vigencia(inicio, Optional.empty());
    }

    /**
     * Creates a closed validity period.
     *
     * @param inicio first day of validity
     * @param fin last day of validity, inclusive
     * @return the period
     */
    public static Vigencia entre(LocalDate inicio, LocalDate fin) {
        return new Vigencia(inicio, Optional.of(fin));
    }

    /**
     * Tells whether the date falls within the period. Both ends are inclusive.
     *
     * @param fecha the date to check
     * @return {@code true} if {@code inicio <= fecha <= fin}
     */
    public boolean contains(LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha");
        return !fecha.isBefore(inicio) && fin.map(f -> !fecha.isAfter(f)).orElse(true);
    }
}
