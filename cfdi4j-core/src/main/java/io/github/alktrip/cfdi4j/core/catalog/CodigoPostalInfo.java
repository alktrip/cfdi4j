package io.github.alktrip.cfdi4j.core.catalog;

import java.time.ZoneOffset;
import java.util.Objects;

/**
 * An entry of c_CodigoPostal, with its time zone data.
 *
 * @param codigoPostal column c_CodigoPostal
 * @param estado column c_Estado
 * @param vigencia validity columns
 * @param diferenciaHorariaVerano column Diferencia_Horaria_Verano, as an offset
 * @param diferenciaHorariaInvierno column Diferencia_Horaria_Invierno, as an offset
 */
public record CodigoPostalInfo(
        String codigoPostal, String estado, Vigencia vigencia, ZoneOffset diferenciaHorariaVerano, ZoneOffset diferenciaHorariaInvierno) {

    /** Checks that no component is null. */
    public CodigoPostalInfo {
        Objects.requireNonNull(codigoPostal, "codigoPostal");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(vigencia, "vigencia");
        Objects.requireNonNull(diferenciaHorariaVerano, "diferenciaHorariaVerano");
        Objects.requireNonNull(diferenciaHorariaInvierno, "diferenciaHorariaInvierno");
    }
}
