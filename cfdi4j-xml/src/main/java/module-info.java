/**
 * CFDI 4.0 XML serialization, parsing, XSD validation and cadena original.
 *
 * <p>Complements are mapped by {@link io.github.alktrip.cfdi4j.xml.spi.ComplementoXml} providers,
 * discovered with {@link java.util.ServiceLoader} (ADR 0006).
 */
// "requires-automatic": Saxon-HE has no module descriptor; its stable automatic name is net.sf.saxon.
@SuppressWarnings("requires-automatic")
module io.github.alktrip.cfdi4j.xml {
    requires transitive io.github.alktrip.cfdi4j.core;
    requires transitive java.xml;
    requires net.sf.saxon;

    exports io.github.alktrip.cfdi4j.xml;
    exports io.github.alktrip.cfdi4j.xml.spi;

    uses io.github.alktrip.cfdi4j.xml.spi.ComplementoXml;
    provides io.github.alktrip.cfdi4j.xml.spi.ComplementoXml with
            io.github.alktrip.cfdi4j.xml.internal.TimbreFiscalDigitalXml;
}
