package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.model.ComplementoNoModelado;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CfdiXmlEdgeCasesTest {

    private final CfdiXml xml = CfdiXml.defaults();

    private static final String MINIMAL = "<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" Version=\"4.0\" "
            + "Fecha=\"2026-11-14T10:30:00\" SubTotal=\"1\" Moneda=\"MXN\" Total=\"1\" TipoDeComprobante=\"I\" Exportacion=\"01\" "
            + "LugarExpedicion=\"72000\"><!-- note -->"
            + "<cfdi:InformacionGlobal Periodicidad=\"01\" Meses=\"01\" Año=\"2026\"/>"
            + "<cfdi:Emisor Rfc=\"EKU9003173C9\" Nombre=\"E\" RegimenFiscal=\"601\"/>"
            + "<cfdi:Receptor Rfc=\"XAXX010101000\" Nombre=\"R\" DomicilioFiscalReceptor=\"72000\" RegimenFiscalReceptor=\"616\" UsoCFDI=\"S01\"/>"
            + "<cfdi:Conceptos><cfdi:Concepto ClaveProdServ=\"01010101\" Cantidad=\"1\" ClaveUnidad=\"ACT\" Descripcion=\"S\" "
            + "ValorUnitario=\"1\" Importe=\"1\" ObjetoImp=\"01\"/></cfdi:Conceptos></cfdi:Comprobante>";

    @Test
    void schemaLocationAndOptionalNodesMayBeAbsent() {
        Comprobante cfdi = xml.parse(MINIMAL);

        assertThat(cfdi.impuestos()).isEmpty();
        assertThat(cfdi.complemento()).isEmpty();
        assertThat(cfdi.informacionGlobal()).hasValueSatisfying(ig -> assertThat(ig.anio()).isEqualTo(2026));
    }

    @Test
    void missingEmisorOrReceptorIsReported() {
        assertThatThrownBy(() -> xml.parse(MINIMAL.replaceAll("<cfdi:Emisor [^>]*/>", "")))
                .hasMessageContaining("/cfdi:Comprobante/cfdi:Emisor: required element is missing");
        assertThatThrownBy(() -> xml.parse(MINIMAL.replaceAll("<cfdi:Receptor [^>]*/>", "")))
                .hasMessageContaining("/cfdi:Comprobante/cfdi:Receptor: required element is missing");
    }

    @Test
    void wrongRootNameInTheCfdiNamespaceIsRejected() {
        assertThatThrownBy(() -> xml.parse("<cfdi:Retenciones xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\"/>"))
                .hasMessageContaining("expected the element");
    }

    @Test
    void invalidIntegerIsReported() {
        assertThatThrownBy(() -> xml.parse(MINIMAL.replace("Año=\"2026\"", "Año=\"dos mil\"")))
                .isInstanceOfSatisfying(CfdiParseException.class,
                        e -> assertThat(e.location()).isEqualTo("/cfdi:Comprobante/cfdi:InformacionGlobal/@Año"));
    }

    @Test
    void cdataTextInsideAStructuralNodeIsRejected() {
        assertThatThrownBy(() -> xml.parse(MINIMAL.replace("<cfdi:Conceptos>", "<cfdi:Conceptos><![CDATA[x]]>")))
                .hasMessageContaining("unexpected text content");
    }

    @Test
    void conceptWithOnlyRetencionesAndSummaryWithOnlyRetencionesRoundTrip() {
        Comprobante cfdi = Fixtures.base()
                .concepto(c -> c.claveProdServ("84111506").cantidad("1").claveUnidad("E48").descripcion("Honorarios")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).retencion(Impuesto.ISR, TipoFactor.TASA, "0.100000"))
                .build();

        String out = xml.toXml(cfdi);

        assertThat(out).doesNotContain("Traslados").contains("<cfdi:Retenciones>");
        assertThat(xml.parse(out)).isEqualTo(cfdi);
    }

    @Test
    void opaqueComplementsAreWrittenWhenMappersForOtherNamespacesExist() {
        Comprobante cfdi = Fixtures.base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("S")
                        .valorUnitario("1.00").objetoImp(ObjetoImp.NO_OBJETO))
                .complemento(new ComplementoNoModelado(ComplementoXmlTest.FakeXml.NS, "<f:Fake xmlns:f=\"" + ComplementoXmlTest.FakeXml.NS + "\"/>",
                        Optional.empty()))
                .build();

        String out = CfdiXml.builder().register(new ComplementoXmlTest.FakeXml()).build().toXml(cfdi);

        assertThat(out).doesNotContain("xmlns:fake=").contains("<f:Fake xmlns:f=");
    }

    @Test
    void malformedOpaqueFragmentFailsWhenWriting() {
        Comprobante cfdi = Fixtures.base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("S")
                        .valorUnitario("1.00").objetoImp(ObjetoImp.NO_OBJETO))
                .complemento(new ComplementoNoModelado("http://example.com/x", "<x:broken", Optional.empty()))
                .build();

        assertThatThrownBy(() -> xml.toXml(cfdi)).isInstanceOf(Cfdi4jException.class).hasMessageStartingWith("Cannot write CFDI XML");
    }

    @Test
    void parseExceptionMessagesIncludeTheLocationWhenKnown() {
        var cause = new IllegalStateException("x");

        assertThat(new CfdiParseException("", "plain")).hasMessage("plain");
        assertThat(new CfdiParseException("/a/@b", "bad", cause)).hasMessage("/a/@b: bad").hasCause(cause);
        assertThat(new CfdiParseException("/a/@b", "bad", cause).location()).isEqualTo("/a/@b");
    }
}
