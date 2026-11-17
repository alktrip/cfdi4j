/**
 * Complemento para recepción de pagos 2.0: model, builder, XML mapping and the rules of its SAT
 * error matrix.
 *
 * <p>The module name keeps the complement version ("pagos20"), as the SAT namespace and the
 * artifact do, so a future Pagos version can live in its own module.
 */
@SuppressWarnings("module") // javac lint: "module name component pagos20 should avoid terminal digits"
module io.github.alktrip.cfdi4j.pagos20 {
    requires transitive io.github.alktrip.cfdi4j.core;
    requires io.github.alktrip.cfdi4j.xml;

    exports io.github.alktrip.cfdi4j.pagos20;

    provides io.github.alktrip.cfdi4j.xml.spi.ComplementoXml with io.github.alktrip.cfdi4j.pagos20.internal.Pagos20Xml;
    provides io.github.alktrip.cfdi4j.core.validation.RuleSet with io.github.alktrip.cfdi4j.pagos20.internal.Pagos20RuleSet;
}
