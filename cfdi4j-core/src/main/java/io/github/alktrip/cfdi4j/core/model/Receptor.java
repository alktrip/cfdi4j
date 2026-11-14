package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.catalog.Pais;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import java.util.Objects;
import java.util.Optional;

/**
 * Node {@code cfdi:Receptor}.
 *
 * @param rfc attribute {@code Rfc}
 * @param nombre attribute {@code Nombre}
 * @param domicilioFiscalReceptor attribute {@code DomicilioFiscalReceptor}
 * @param residenciaFiscal attribute {@code ResidenciaFiscal}
 * @param numRegIdTrib attribute {@code NumRegIdTrib}
 * @param regimenFiscalReceptor attribute {@code RegimenFiscalReceptor}
 * @param usoCFDI attribute {@code UsoCFDI}
 */
public record Receptor(
        String rfc,
        String nombre,
        String domicilioFiscalReceptor,
        Optional<Pais> residenciaFiscal,
        Optional<String> numRegIdTrib,
        RegimenFiscal regimenFiscalReceptor,
        UsoCFDI usoCFDI) {

    /** Checks that no component is null. */
    public Receptor {
        Objects.requireNonNull(rfc, "rfc");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(domicilioFiscalReceptor, "domicilioFiscalReceptor");
        Objects.requireNonNull(residenciaFiscal, "residenciaFiscal");
        Objects.requireNonNull(numRegIdTrib, "numRegIdTrib");
        Objects.requireNonNull(regimenFiscalReceptor, "regimenFiscalReceptor");
        Objects.requireNonNull(usoCFDI, "usoCFDI");
    }
}
