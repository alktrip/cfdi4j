package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Golden file produced by the Saxon command line with the SAT stylesheet (tools/golden/generate.sh). */
class CadenaOriginalTimbreTest {

    private static final Path GOLDEN = Path.of("src/test/resources/golden/tfd");

    @Test
    void cadenaOfTheParsedTimbreMatchesTheIndependentPipeline() throws IOException {
        byte[] tfd = Files.readAllBytes(GOLDEN.resolve("timbre.xml"));
        String expected = Files.readString(GOLDEN.resolve("timbre.cadena.txt"), StandardCharsets.UTF_8);
        String stamped = "<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\" Version=\"4.0\" Fecha=\"2026-11-14T10:30:00\" "
                + "SubTotal=\"1\" Moneda=\"MXN\" Total=\"1\" TipoDeComprobante=\"I\" Exportacion=\"01\" LugarExpedicion=\"72000\">"
                + "<cfdi:Emisor Rfc=\"EKU9003173C9\" Nombre=\"E\" RegimenFiscal=\"601\"/>"
                + "<cfdi:Receptor Rfc=\"XAXX010101000\" Nombre=\"R\" DomicilioFiscalReceptor=\"72000\" RegimenFiscalReceptor=\"616\" UsoCFDI=\"S01\"/>"
                + "<cfdi:Conceptos><cfdi:Concepto ClaveProdServ=\"01010101\" Cantidad=\"1\" ClaveUnidad=\"ACT\" Descripcion=\"S\" "
                + "ValorUnitario=\"1\" Importe=\"1\" ObjetoImp=\"01\"/></cfdi:Conceptos><cfdi:Complemento>"
                + new String(tfd, StandardCharsets.UTF_8).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "")
                + "</cfdi:Complemento></cfdi:Comprobante>";

        var timbre = CfdiXml.defaults().parse(stamped).timbreFiscalDigital().orElseThrow();

        assertThat(CadenaOriginalTimbre.defaults().of(timbre)).isEqualTo(expected);
        assertThat(expected).isEqualTo("||1.1|5FB2822E-396D-4725-8521-CDC4BDD20CCF|2026-11-14T10:31:00|KIJ0906199R1|Leyenda de prueba|abc|30001000000500003456||");
    }

    @Test
    void leyendaIsOptional() {
        assertThat(CadenaOriginalTimbre.defaults().of(Fixtures.timbre()))
                .isEqualTo("||1.1|5FB2822E-396D-4725-8521-CDC4BDD20CCF|2026-11-14T10:31:00|KIJ0906199R1|abc|30001000000500003456||");
    }
}
