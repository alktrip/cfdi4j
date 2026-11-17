package io.github.alktrip.cfdi4j.core.catalog;

import java.util.Objects;
import java.util.Optional;

/**
 * An entry of c_ClaveProdServ.
 *
 * @param clave column c_ClaveProdServ
 * @param descripcion column Descripción
 * @param incluirIvaTrasladado column "Incluir IVA trasladado"
 * @param incluirIepsTrasladado column "Incluir IEPS trasladado"
 * @param complementoQueDebeIncluir column "Complemento que debe incluir"
 * @param vigencia columns FechaInicioVigencia and FechaFinVigencia
 */
public record ClaveProdServInfo(
        String clave,
        String descripcion,
        Requisito incluirIvaTrasladado,
        Requisito incluirIepsTrasladado,
        Optional<String> complementoQueDebeIncluir,
        Vigencia vigencia) {

    /** Checks that no component is null. */
    public ClaveProdServInfo {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(descripcion, "descripcion");
        Objects.requireNonNull(incluirIvaTrasladado, "incluirIvaTrasladado");
        Objects.requireNonNull(incluirIepsTrasladado, "incluirIepsTrasladado");
        Objects.requireNonNull(complementoQueDebeIncluir, "complementoQueDebeIncluir");
        Objects.requireNonNull(vigencia, "vigencia");
    }
}
