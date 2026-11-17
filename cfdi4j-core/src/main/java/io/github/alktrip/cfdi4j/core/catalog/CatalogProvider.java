package io.github.alktrip.cfdi4j.core.catalog;

import java.util.Optional;

/**
 * Port to the large SAT catalogs that {@code cfdi4j-core} does not include (ADR 0003):
 * c_ClaveProdServ, c_ClaveUnidad and c_CodigoPostal. The optional module {@code cfdi4j-catalogs}
 * provides an implementation. Without a provider, the rules that need these catalogs report
 * "not verified"; they never pass silently.
 */
public interface CatalogProvider {

    /**
     * Looks up a product or service key.
     *
     * @param clave the key, for example {@code 01010101}
     * @return the entry, or empty if the key is not in the catalog
     */
    Optional<ClaveProdServInfo> claveProdServ(String clave);

    /**
     * Looks up a unit key.
     *
     * @param clave the key, for example {@code H87}
     * @return the entry, or empty if the key is not in the catalog
     */
    Optional<ClaveUnidadInfo> claveUnidad(String clave);

    /**
     * Looks up a postal code.
     *
     * @param codigoPostal the postal code
     * @return the entry, or empty if the code is not in the catalog
     */
    Optional<CodigoPostalInfo> codigoPostal(String codigoPostal);
}
