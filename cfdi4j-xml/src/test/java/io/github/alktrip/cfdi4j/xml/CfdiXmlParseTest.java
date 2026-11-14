package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.model.ComplementoNoModelado;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CfdiXmlParseTest {

    private final CfdiXml xml = CfdiXml.defaults();

    /** Shaped like a stamped CFDI: the TFD prefix is declared on the root, as many PACs do. */
    private static final String STAMPED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante xmlns:cfdi="http://www.sat.gob.mx/cfd/4" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:tfd="http://www.sat.gob.mx/TimbreFiscalDigital"
                xsi:schemaLocation="http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd
                    http://www.sat.gob.mx/TimbreFiscalDigital http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd"
                Version="4.0" Fecha="2026-11-14T10:30:00" Sello="c2VsbG8=" FormaPago="99" NoCertificado="30001000000500003416"
                Certificado="Y2VydA==" SubTotal="1000" Moneda="MXN" Total="1160.000000" TipoDeComprobante="I" Exportacion="01"
                MetodoPago="PPD" LugarExpedicion="72000">
              <cfdi:Emisor Rfc="EKU9003173C9" Nombre="ESCUELA KEMPER URGATE" RegimenFiscal="601"/>
              <cfdi:Receptor Rfc="XAXX010101000" Nombre="PUBLICO EN GENERAL" DomicilioFiscalReceptor="72000"
                  RegimenFiscalReceptor="616" UsoCFDI="S01"/>
              <cfdi:Conceptos>
                <cfdi:Concepto ClaveProdServ="01010101" Cantidad="1.000000" ClaveUnidad="ACT" Descripcion="Servicio"
                    ValorUnitario="1000" Importe="1000" ObjetoImp="02">
                  <cfdi:Impuestos>
                    <cfdi:Traslados>
                      <cfdi:Traslado Base="1000" Impuesto="002" TipoFactor="Tasa" TasaOCuota="0.160000" Importe="160"/>
                    </cfdi:Traslados>
                  </cfdi:Impuestos>
                </cfdi:Concepto>
              </cfdi:Conceptos>
              <cfdi:Impuestos TotalImpuestosTrasladados="160">
                <cfdi:Traslados>
                  <cfdi:Traslado Base="1000" Impuesto="002" TipoFactor="Tasa" TasaOCuota="0.160000" Importe="160"/>
                </cfdi:Traslados>
              </cfdi:Impuestos>
              <cfdi:Complemento>
                <tfd:TimbreFiscalDigital Version="1.1" UUID="5FB2822E-396D-4725-8521-CDC4BDD20CCF"
                    FechaTimbrado="2026-11-14T10:31:00" RfcProvCertif="KIJ0906199R1" SelloCFD="c2VsbG8="
                    NoCertificadoSAT="30001000000500003456" SelloSAT="c2F0"/>
              </cfdi:Complemento>
              <cfdi:Addenda><x:Dato xmlns:x="http://example.com/x">uno</x:Dato></cfdi:Addenda>
            </cfdi:Comprobante>
            """;

    @Test
    void readsAStampedCfdiKeepingValuesAsWritten() {
        Comprobante cfdi = xml.parse(STAMPED);

        assertThat(cfdi.sello()).contains("c2VsbG8=");
        assertThat(cfdi.noCertificado()).contains("30001000000500003416");
        assertThat(cfdi.certificado()).contains("Y2VydA==");
        assertThat(cfdi.formaPago()).contains(FormaPago.POR_DEFINIR);
        assertThat(cfdi.subTotal()).hasToString("1000");
        assertThat(cfdi.total()).hasToString("1160.000000");
        assertThat(cfdi.conceptos().get(0).cantidad()).hasToString("1.000000");
        assertThat(cfdi.timbreFiscalDigital()).hasValueSatisfying(tfd -> {
            assertThat(tfd.uuid()).isEqualTo("5FB2822E-396D-4725-8521-CDC4BDD20CCF");
            assertThat(tfd.fechaTimbrado()).isEqualTo(java.time.LocalDateTime.of(2026, 11, 14, 10, 31, 0));
            assertThat(tfd.rfcProvCertif()).isEqualTo("KIJ0906199R1");
            assertThat(tfd.leyenda()).isEmpty();
            assertThat(tfd.selloCFD()).isEqualTo("c2VsbG8=");
            assertThat(tfd.noCertificadoSAT()).isEqualTo("30001000000500003456");
            assertThat(tfd.selloSAT()).isEqualTo("c2F0");
        });
        // Without the TFD mapper the same element is kept as a self-contained fragment.
        Comprobante opaque = CfdiXml.builder().build().parse(STAMPED);
        assertThat(opaque.complemento()).singleElement().isInstanceOfSatisfying(ComplementoNoModelado.class, tfd -> {
            assertThat(tfd.schemaLocation()).contains("http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd");
            assertThat(tfd.xml()).startsWith("<tfd:TimbreFiscalDigital").contains("xmlns:tfd=\"http://www.sat.gob.mx/TimbreFiscalDigital\"");
        });
        assertThat(cfdi.addenda().orElseThrow().contenido()).isEqualTo("<x:Dato xmlns:x=\"http://example.com/x\">uno</x:Dato>");
    }

    @Test
    void readsFromBytesAndStreamsDetectingTheEncoding() throws IOException {
        byte[] latin1 = STAMPED.replace("encoding=\"UTF-8\"", "encoding=\"ISO-8859-1\"").replace("Servicio", "Reparación")
                .getBytes(StandardCharsets.ISO_8859_1);

        assertThat(xml.parse(latin1).conceptos().get(0).descripcion()).isEqualTo("Reparación");
        try (InputStream in = new ByteArrayInputStream(latin1)) {
            assertThat(xml.parse(in).conceptos().get(0).descripcion()).isEqualTo("Reparación");
        }
    }

    @Test
    void reportsTheLocationOfAMissingRequiredAttribute() {
        String broken = STAMPED.replace(" ClaveUnidad=\"ACT\"", "");

        assertThatThrownBy(() -> xml.parse(broken))
                .isInstanceOfSatisfying(CfdiParseException.class, e -> assertThat(e.location())
                        .isEqualTo("/cfdi:Comprobante/cfdi:Conceptos/cfdi:Concepto[1]/@ClaveUnidad"))
                .hasMessageContaining("required attribute is missing");
    }

    @Test
    void reportsInvalidNumbersAndDates() {
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("SubTotal=\"1000\"", "SubTotal=\"mil\"")))
                .isInstanceOfSatisfying(CfdiParseException.class, e -> assertThat(e.location()).isEqualTo("/cfdi:Comprobante/@SubTotal"));
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("2026-11-14T10:30:00", "14/11/2026")))
                .hasMessageContaining("@Fecha").hasMessageContaining("is not a date-time");
    }

    @Test
    void rejectsAttributesAndElementsTheXsdDoesNotDefine() {
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<cfdi:Emisor ", "<cfdi:Emisor Curp=\"X\" ")))
                .isInstanceOfSatisfying(CfdiParseException.class, e -> assertThat(e.location()).isEqualTo("/cfdi:Comprobante/cfdi:Emisor/@Curp"));
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<cfdi:Conceptos>", "<cfdi:Conceptos><cfdi:Extra/>")))
                .hasMessageContaining("/cfdi:Comprobante/cfdi:Conceptos/cfdi:Extra").hasMessageContaining("unexpected element");
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<cfdi:Emisor ", "<cfdi:Emisor xsi:type=\"x\" ")))
                .hasMessageContaining("@xsi:type");
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<cfdi:Conceptos>", "<cfdi:Conceptos>texto")))
                .hasMessageContaining("unexpected text content");
    }

    @Test
    void rejectsDuplicatedSingleNodes() {
        String twoEmisores = STAMPED.replace("<cfdi:Receptor ", "<cfdi:Emisor Rfc=\"A\" Nombre=\"B\" RegimenFiscal=\"601\"/><cfdi:Receptor ");

        assertThatThrownBy(() -> xml.parse(twoEmisores)).hasMessageContaining("element may appear only once");
    }

    @Test
    void rejectsOtherVersionsAndDocuments() {
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("http://www.sat.gob.mx/cfd/4\"", "http://www.sat.gob.mx/cfd/3\"")))
                .hasMessageContaining("CFDI 3.3 is not supported");
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("Version=\"4.0\"", "Version=\"3.3\"")))
                .hasMessageContaining("/cfdi:Comprobante/@Version").hasMessageContaining("only 4.0");
        assertThatThrownBy(() -> xml.parse("<Comprobante/>")).hasMessageContaining("expected the element {http://www.sat.gob.mx/cfd/4}Comprobante");
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<cfdi:Conceptos>", "").replace("</cfdi:Conceptos>", "")
                .replaceAll("(?s)<cfdi:Concepto .*?</cfdi:Concepto>", "")))
                .hasMessageContaining("cfdi:Conceptos").hasMessageContaining("required element is missing");
        assertThatThrownBy(() -> xml.parse("<cfdi:Comprobante")).isInstanceOf(CfdiParseException.class).hasMessageContaining("not well-formed");
    }

    @Test
    void rejectsDoctypeSoNoEntityCanBeExpandedOrFetched() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE c [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + STAMPED.substring(STAMPED.indexOf("<cfdi:Comprobante")).replace("Servicio", "&x;");

        assertThatThrownBy(() -> xml.parse(xxe)).isInstanceOf(CfdiParseException.class).hasMessageContaining("DOCTYPE");
    }

    @Test
    void complementWithoutNamespaceIsRejected() {
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<tfd:TimbreFiscalDigital ", "<Timbre ").replace("SelloSAT=\"c2F0\"/>", "SelloSAT=\"c2F0\"/>")
                .replaceAll("<Timbre [^>]*/>", "<Timbre/>")))
                .hasMessageContaining("complement element without namespace");
    }

    @Test
    void timbreFieldsAreCheckedLikeAnyOtherNode() {
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<tfd:TimbreFiscalDigital ", "<tfd:TimbreFiscalDigital Otro=\"x\" ")))
                .hasMessageContaining("tfd:TimbreFiscalDigital[1]/@Otro").hasMessageContaining("unexpected attribute");
        assertThatThrownBy(() -> xml.parse(STAMPED.replace("<tfd:TimbreFiscalDigital ", "<tfd:Otro ")))
                .hasMessageContaining("expected tfd:TimbreFiscalDigital");
        assertThat(xml.parse(STAMPED.replace("<tfd:TimbreFiscalDigital ", "<tfd:TimbreFiscalDigital Leyenda=\"Texto\" "))
                .timbreFiscalDigital().orElseThrow().leyenda()).contains("Texto");
    }

    @Test
    void readFailuresAreUnchecked() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("disk on fire");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("disk on fire");
            }
        };

        assertThatThrownBy(() -> xml.parse(failing)).isInstanceOf(UncheckedIOException.class);
    }
}
