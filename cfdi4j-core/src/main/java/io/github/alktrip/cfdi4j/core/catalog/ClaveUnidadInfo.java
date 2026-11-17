package io.github.alktrip.cfdi4j.core.catalog;

import java.util.Objects;

/**
 * An entry of c_ClaveUnidad.
 *
 * @param clave column c_ClaveUnidad
 * @param nombre column Nombre
 * @param vigencia columns "Fecha de inicio de vigencia" and "Fecha de fin de vigencia"
 */
public record ClaveUnidadInfo(String clave, String nombre, Vigencia vigencia) {

    /** Checks that no component is null. */
    public ClaveUnidadInfo {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(vigencia, "vigencia");
    }
}
