package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.model.ComplementoNoModelado;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CfdiXmlRoundTripTest {

    private final CfdiXml xml = CfdiXml.defaults();

    @Test
    void simpleInvoiceRoundTripsToAnEqualModel() {
        Comprobante cfdi = Fixtures.simple();

        assertThat(xml.parse(xml.toXml(cfdi))).isEqualTo(cfdi);
        assertThat(xml.parse(xml.toBytes(cfdi))).isEqualTo(cfdi);
    }

    @Test
    void everyNodeAndAttributeRoundTrips() {
        Comprobante cfdi = Fixtures.complete();

        Comprobante parsed = xml.parse(xml.toXml(cfdi));

        // Opaque fragments may be re-serialized with another attribute order; everything else is equal.
        assertThat(withoutFragments(parsed)).isEqualTo(withoutFragments(cfdi));
        assertThat(parsed.complemento()).containsExactly(Fixtures.timbre());
        assertThat(parsed.timbreFiscalDigital()).contains(Fixtures.timbre());
        assertThat(parsed.conceptos().get(0).complementoConcepto()).singleElement().isInstanceOfSatisfying(ComplementoNoModelado.class, c -> {
            assertThat(c.namespace()).isEqualTo("http://www.sat.gob.mx/iedu");
            assertThat(c.schemaLocation()).contains("http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xsd");
        });
        assertThat(parsed.addenda().orElseThrow().contenido()).contains("<ext:Nota>texto</ext:Nota>").contains("http://example.com/otra");
        // Writing the parsed model again is stable.
        assertThat(xml.parse(xml.toXml(parsed))).isEqualTo(parsed);
        assertThat(xml.toXml(xml.parse(xml.toXml(parsed)))).isEqualTo(xml.toXml(parsed));
    }

    @Test
    void scaleOfEveryAmountIsKept() {
        Comprobante parsed = xml.parse(xml.toXml(Fixtures.complete()));

        assertThat(parsed.conceptos().get(0).cantidad()).hasToString("1.500000");
        assertThat(parsed.conceptos().get(0).valorUnitario()).hasToString("1000.000000");
        assertThat(parsed.conceptos().get(1).importe()).hasToString("0.999999");
        assertThat(parsed.tipoCambio()).map(Object::toString).contains("17.123456");
    }

    @Test
    void outputIsUtf8() {
        String text = new String(xml.toBytes(Fixtures.base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Reparación de lavadora ñ")
                        .valorUnitario("1.00").objetoImp(io.github.alktrip.cfdi4j.core.catalog.ObjetoImp.NO_OBJETO))
                .build()), StandardCharsets.UTF_8);

        assertThat(text).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").contains("Reparación de lavadora ñ");
    }

    private static Comprobante withoutFragments(Comprobante c) {
        return new Comprobante(c.serie(), c.folio(), c.fecha(), c.sello(), c.formaPago(), c.noCertificado(), c.certificado(),
                c.condicionesDePago(), c.subTotal(), c.descuento(), c.moneda(), c.tipoCambio(), c.total(), c.tipoDeComprobante(),
                c.exportacion(), c.metodoPago(), c.lugarExpedicion(), c.confirmacion(), c.informacionGlobal(), c.cfdiRelacionados(),
                c.emisor(), c.receptor(),
                c.conceptos().stream().map(x -> new io.github.alktrip.cfdi4j.core.model.Concepto(x.claveProdServ(), x.noIdentificacion(),
                        x.cantidad(), x.claveUnidad(), x.unidad(), x.descripcion(), x.valorUnitario(), x.importe(), x.descuento(),
                        x.objetoImp(), x.impuestos(), x.aCuentaTerceros(), x.informacionAduanera(), x.cuentaPredial(),
                        java.util.List.of(), x.parte())).toList(),
                c.impuestos(), c.complemento(), java.util.Optional.empty());
    }
}
