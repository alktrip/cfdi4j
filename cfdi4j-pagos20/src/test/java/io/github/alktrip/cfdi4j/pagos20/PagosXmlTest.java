package io.github.alktrip.cfdi4j.pagos20;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.xml.CfdiParseException;
import io.github.alktrip.cfdi4j.xml.CfdiSchema;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class PagosXmlTest {

    static Stream<Comprobante> recibos() {
        return Stream.of(PagosFixtures.valido(), PagosFixtures.con(PagosFixtures.ejemploGuia()),
                PagosFixtures.con(Pagos.builder().pago(PagosRulesTest.pagoConRetencion()
                        .andThen(p -> p.numOperacion("1").rfcEmisorCtaOrd("XEXX010101000").nomBancoOrdExt("BANK").ctaOrdenante("1234567890")
                                .rfcEmisorCtaBen("EKU9003173C9").ctaBeneficiario("0123456789")
                                .cadenaPago("01", PagosRulesTest.CERT, "||cadena||", "c2VsbG8=")))
                        .build()));
    }

    @Test
    void complementIsRegisteredWithServiceLoader() {
        assertThat(CfdiXml.defaults().complementNamespaces()).contains(Pagos.NAMESPACE);
    }

    @ParameterizedTest
    @MethodSource("recibos")
    void roundTripsAndPassesTheSatSchema(Comprobante recibo) {
        String xml = CfdiXml.defaults().toXml(recibo);

        assertThat(xml).contains("xmlns:pago20=\"http://www.sat.gob.mx/Pagos20\"")
                .contains("http://www.sat.gob.mx/Pagos20 http://www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xsd");
        assertThat(CfdiXml.defaults().parse(xml)).isEqualTo(recibo);
        assertThat(CfdiSchema.defaults().validateUnsealed(xml.getBytes(StandardCharsets.UTF_8)).violations()).isEmpty();
    }

    @Test
    void writesNodesInSchemaOrder() {
        String xml = CfdiXml.defaults().toXml(PagosFixtures.valido());

        assertThat(xml).containsSubsequence("<pago20:Pagos Version=\"2.0\">", "<pago20:Totales", "<pago20:Pago ", "<pago20:DoctoRelacionado",
                "<pago20:ImpuestosDR>", "<pago20:TrasladosDR>", "<pago20:TrasladoDR", "<pago20:ImpuestosP>", "<pago20:TrasladosP>", "<pago20:TrasladoP");
    }

    @Test
    void schemaRejectsAnInvalidComplement() {
        String xml = CfdiXml.defaults().toXml(PagosFixtures.valido()).replace("MonedaP=\"MXN\"", "MonedaP=\"MX\"");

        assertThat(CfdiSchema.defaults().validateUnsealed(xml.getBytes(StandardCharsets.UTF_8)).errors())
                .anySatisfy(v -> assertThat(v.code()).isEqualTo(CfdiSchema.XSD_INVALIDO));
    }

    @Test
    void rejectsUnknownAttributesAndVersions() {
        String xml = CfdiXml.defaults().toXml(PagosFixtures.valido());

        assertThatThrownBy(() -> CfdiXml.defaults().parse(xml.replace("<pago20:Pagos Version=\"2.0\">", "<pago20:Pagos Version=\"1.0\">")))
                .isInstanceOf(CfdiParseException.class).hasMessageContaining("Version");
        assertThatThrownBy(() -> CfdiXml.defaults().parse(xml.replace("MonedaP=\"MXN\"", "MonedaP=\"MXN\" Extra=\"1\"")))
                .isInstanceOf(CfdiParseException.class).hasMessageContaining("Extra");
    }
}
