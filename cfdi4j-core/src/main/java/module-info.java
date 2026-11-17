/**
 * CFDI 4.0 model, builders, catalogs and validation rules.
 *
 * <p>This module has no XML and no I/O dependencies and can be used on its own.
 */
// "module" lint: the qualified export below names modules that depend on this one, so they are
// not visible when this module is compiled.
@SuppressWarnings("module")
module io.github.alktrip.cfdi4j.core {
    exports io.github.alktrip.cfdi4j.core;
    exports io.github.alktrip.cfdi4j.core.catalog;
    exports io.github.alktrip.cfdi4j.core.model;
    exports io.github.alktrip.cfdi4j.core.validation;

    uses io.github.alktrip.cfdi4j.core.catalog.CatalogProvider;
    uses io.github.alktrip.cfdi4j.core.validation.RuleSet;

    // Shared helpers for the other cfdi4j modules only; not public API.
    exports io.github.alktrip.cfdi4j.core.internal to
            io.github.alktrip.cfdi4j.xml,
            io.github.alktrip.cfdi4j.crypto,
            io.github.alktrip.cfdi4j.pagos20,
            io.github.alktrip.cfdi4j.catalogs,
            io.github.alktrip.cfdi4j;
}
