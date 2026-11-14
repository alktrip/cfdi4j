package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CfdiXmlWriteTest {

    private final CfdiXml xml = CfdiXml.defaults();

    @Test
    void writesTheRootWithNamespacesSchemaLocationAndAttributesInXsdOrder() {
        String out = xml.toXml(Fixtures.simple());

        assertThat(out).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><cfdi:Comprobante "
                + "xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd\" "
                + "Version=\"4.0\" Serie=\"A\" Folio=\"123\" Fecha=\"2026-11-14T10:30:00\" FormaPago=\"03\" "
                + "SubTotal=\"100.00\" Moneda=\"MXN\" Total=\"116.00\" TipoDeComprobante=\"I\" Exportacion=\"01\" "
                + "MetodoPago=\"PUE\" LugarExpedicion=\"72000\">");
        assertThat(out).contains("<cfdi:Emisor Rfc=\"EKU9003173C9\" Nombre=\"ESCUELA KEMPER URGATE\" RegimenFiscal=\"601\"/>");
        assertThat(out).contains("<cfdi:Concepto ClaveProdServ=\"01010101\" Cantidad=\"1\" ClaveUnidad=\"ACT\" Descripcion=\"Servicio\" "
                + "ValorUnitario=\"100.00\" Importe=\"100.00\" ObjetoImp=\"02\"><cfdi:Impuestos><cfdi:Traslados>"
                + "<cfdi:Traslado Base=\"100.00\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"0.160000\" Importe=\"16.00\"/>");
        assertThat(out).endsWith("<cfdi:Impuestos TotalImpuestosTrasladados=\"16.00\"><cfdi:Traslados>"
                + "<cfdi:Traslado Base=\"100.00\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"0.160000\" Importe=\"16.00\"/>"
                + "</cfdi:Traslados></cfdi:Impuestos></cfdi:Comprobante>");
    }

    @Test
    void unsealedComprobanteHasNoSelloNoCertificadoOrCertificado() {
        String out = xml.toXml(Fixtures.simple());

        assertThat(out).doesNotContain("Sello=").doesNotContain("NoCertificado=").doesNotContain("Certificado=");
    }

    @Test
    void summaryWritesRetencionesBeforeTrasladosAsTheXsdRequires() {
        String out = xml.toXml(Fixtures.complete());
        String summary = out.substring(out.lastIndexOf("<cfdi:Impuestos "));

        assertThat(summary.indexOf("<cfdi:Retenciones>")).isLessThan(summary.indexOf("<cfdi:Traslados>"));
        String concepto = out.substring(out.indexOf("<cfdi:Concepto "), out.indexOf("</cfdi:Concepto>"));
        assertThat(concepto.indexOf("<cfdi:Traslados>")).isLessThan(concepto.indexOf("<cfdi:Retenciones>"));
    }

    @Test
    void escapesMarkupInAttributes() {
        String out = xml.toXml(Fixtures.complete());

        assertThat(out).contains("CondicionesDePago=\"Contado &amp; &lt;inmediato&gt;\"").contains("Nombre=\"CLIENTE &quot;EXTRANJERO&quot;\"");
    }

    @Test
    void writesOpaqueComplementsAndTheirSchemaLocation() {
        String out = xml.toXml(Fixtures.complete());

        assertThat(out).contains("http://www.sat.gob.mx/TimbreFiscalDigital http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd")
                .contains("http://www.sat.gob.mx/iedu http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xsd")
                .contains("<cfdi:Complemento><tfd:TimbreFiscalDigital xmlns:tfd=\"http://www.sat.gob.mx/TimbreFiscalDigital\"")
                .contains("<cfdi:ComplementoConcepto><iedu:instEducativas xmlns:iedu=\"http://www.sat.gob.mx/iedu\"")
                .contains("<cfdi:Addenda><ext:Pedido xmlns:ext=\"http://example.com/ext\" Numero=\"42\"><ext:Nota>texto</ext:Nota></ext:Pedido>"
                        + "<otra xmlns=\"http://example.com/otra\"></otra></cfdi:Addenda>")
                .contains("<cfdi:InformacionGlobal Periodicidad=\"04\" Meses=\"11\" Año=\"2026\"/>")
                .contains("<cfdi:CfdiRelacionados TipoRelacion=\"04\"><cfdi:CfdiRelacionado UUID=\"5FB2822E-396D-4725-8521-CDC4BDD20CCF\"/>");
    }

    @Test
    void writeToAStreamGivesTheSameBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        xml.write(Fixtures.simple(), out);

        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo(xml.toXml(Fixtures.simple()));
    }
}
