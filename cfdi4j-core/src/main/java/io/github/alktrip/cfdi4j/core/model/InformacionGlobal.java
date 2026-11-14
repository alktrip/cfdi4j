package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.catalog.Meses;
import io.github.alktrip.cfdi4j.core.catalog.Periodicidad;
import java.util.Objects;

/**
 * Node {@code cfdi:InformacionGlobal}.
 *
 * @param periodicidad attribute {@code Periodicidad}
 * @param meses attribute {@code Meses}
 * @param anio attribute {@code Año} (ASCII spelling, see ADR 0002)
 */
public record InformacionGlobal(Periodicidad periodicidad, Meses meses, int anio) {

    /** Checks that no component is null. */
    public InformacionGlobal {
        Objects.requireNonNull(periodicidad, "periodicidad");
        Objects.requireNonNull(meses, "meses");
    }
}
