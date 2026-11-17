/**
 * Optional {@link io.github.alktrip.cfdi4j.core.catalog.CatalogProvider} with the large SAT
 * catalogs: c_ClaveProdServ, c_ClaveUnidad and c_CodigoPostal.
 */
module io.github.alktrip.cfdi4j.catalogs {
    requires transitive io.github.alktrip.cfdi4j.core;

    exports io.github.alktrip.cfdi4j.catalogs;

    provides io.github.alktrip.cfdi4j.core.catalog.CatalogProvider with io.github.alktrip.cfdi4j.catalogs.SatCatalogs;
}
