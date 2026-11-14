package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.catalog.TipoRelacion;
import java.util.List;
import java.util.Objects;

/**
 * Node {@code cfdi:CfdiRelacionados}.
 *
 * @param tipoRelacion attribute {@code TipoRelacion}
 * @param cfdiRelacionado nodes {@code cfdi:CfdiRelacionado}
 */
public record CfdiRelacionados(TipoRelacion tipoRelacion, List<CfdiRelacionado> cfdiRelacionado) {

    /** Checks that no component is null and copies the list. */
    public CfdiRelacionados {
        Objects.requireNonNull(tipoRelacion, "tipoRelacion");
        cfdiRelacionado = List.copyOf(cfdiRelacionado);
    }

    /**
     * Node {@code cfdi:CfdiRelacionado}.
     *
     * @param uuid attribute {@code UUID}, the folio fiscal of the related CFDI
     */
    public record CfdiRelacionado(String uuid) {

        /** Checks that no component is null. */
        public CfdiRelacionado {
            Objects.requireNonNull(uuid, "uuid");
        }
    }
}
