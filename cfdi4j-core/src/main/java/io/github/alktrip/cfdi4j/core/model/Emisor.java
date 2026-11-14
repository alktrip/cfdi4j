package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import java.util.Objects;
import java.util.Optional;

/**
 * Node {@code cfdi:Emisor}.
 *
 * @param rfc attribute {@code Rfc}
 * @param nombre attribute {@code Nombre}
 * @param regimenFiscal attribute {@code RegimenFiscal}
 * @param facAtrAdquirente attribute {@code FacAtrAdquirente}
 */
public record Emisor(String rfc, String nombre, RegimenFiscal regimenFiscal, Optional<String> facAtrAdquirente) {

    /** Checks that no component is null. */
    public Emisor {
        Objects.requireNonNull(rfc, "rfc");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(regimenFiscal, "regimenFiscal");
        Objects.requireNonNull(facAtrAdquirente, "facAtrAdquirente");
    }
}
