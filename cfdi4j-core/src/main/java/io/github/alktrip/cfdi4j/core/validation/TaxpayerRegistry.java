package io.github.alktrip.cfdi4j.core.validation;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Port to the SAT list of registered, not cancelled RFCs (l_RFC / l_LCO), which cfdi4j does not
 * access (ADR 0004). Without an implementation, the rules that need it report "not verified".
 */
@FunctionalInterface
public interface TaxpayerRegistry {

    /**
     * Looks up a taxpayer.
     *
     * @param rfc the RFC
     * @return the registered data, or empty if the RFC is not registered or is cancelled
     */
    Optional<Contribuyente> find(String rfc);

    /**
     * Data of a registered taxpayer.
     *
     * @param rfc the RFC
     * @param nombre the name or business name as registered
     * @param codigoPostal the postal code of the fiscal address
     * @param marcaRetencion the "Retención" mark of the list (used by CFDI140224 and CFDI140225), if any
     */
    record Contribuyente(String rfc, String nombre, String codigoPostal, OptionalInt marcaRetencion) {

        /** Checks that no component is null. */
        public Contribuyente {
            Objects.requireNonNull(rfc, "rfc");
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(codigoPostal, "codigoPostal");
            Objects.requireNonNull(marcaRetencion, "marcaRetencion");
        }
    }
}
