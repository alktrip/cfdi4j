package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.model.Complemento;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.xml.spi.ComplementoXml;
import io.github.alktrip.cfdi4j.xml.spi.XmlElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;

/** Registration and use of complement mappers (ADR 0006). */
class ComplementoXmlTest {

    record Fake(String valor) implements Complemento {
        @Override
        public String namespace() {
            return FakeXml.NS;
        }
    }

    record Other() implements Complemento {
        @Override
        public String namespace() {
            return "http://example.com/other";
        }
    }

    static class FakeXml implements ComplementoXml {
        static final String NS = "http://example.com/fake";

        @Override
        public String namespace() {
            return NS;
        }

        @Override
        public String prefix() {
            return "fake";
        }

        @Override
        public String schemaLocation() {
            return "http://example.com/fake.xsd";
        }

        @Override
        public Class<? extends Complemento> type() {
            return Fake.class;
        }

        @Override
        public Complemento read(XmlElement element) {
            return new Fake(element.required("Valor"));
        }

        @Override
        public void write(Complemento complemento, XMLStreamWriter writer) throws XMLStreamException {
            writer.writeEmptyElement("fake", "Fake", NS);
            writer.writeAttribute("Valor", ((Fake) complemento).valor());
        }
    }

    static class AnotherFakeXml extends FakeXml {}

    static class OtherXml extends FakeXml {
        @Override
        public String namespace() {
            return "http://example.com/a-first";
        }

        @Override
        public Class<? extends Complemento> type() {
            return Other.class;
        }
    }

    @Test
    void registeredMapperReadsAndWritesItsComplement() {
        CfdiXml xml = CfdiXml.builder().register(new FakeXml()).build();
        Comprobante cfdi = Fixtures.base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("1.00").objetoImp(io.github.alktrip.cfdi4j.core.catalog.ObjetoImp.NO_OBJETO)
                        .complementoConcepto(new Fake("en concepto")))
                .complemento(new Fake("42"))
                .build();

        String out = xml.toXml(cfdi);

        assertThat(out).contains("xmlns:fake=\"http://example.com/fake\"")
                .contains("http://example.com/fake http://example.com/fake.xsd")
                .contains("<cfdi:Complemento><fake:Fake Valor=\"42\"/></cfdi:Complemento>");
        assertThat(xml.parse(out)).isEqualTo(cfdi);
    }

    @Test
    void withoutTheMapperTheSameXmlIsKeptAsAnOpaqueComplement() {
        Comprobante cfdi = Fixtures.base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("S")
                        .valorUnitario("1.00").objetoImp(io.github.alktrip.cfdi4j.core.catalog.ObjetoImp.NO_OBJETO))
                .complemento(new Fake("42"))
                .build();
        String out = CfdiXml.builder().register(new FakeXml()).build().toXml(cfdi);

        Comprobante parsed = CfdiXml.defaults().parse(out);

        assertThat(parsed.complemento()).singleElement().satisfies(c -> {
            assertThat(c).isInstanceOf(io.github.alktrip.cfdi4j.core.model.ComplementoNoModelado.class);
            assertThat(c.namespace()).isEqualTo(FakeXml.NS);
        });
        // ...and is written back identically, with its schema location.
        assertThat(CfdiXml.defaults().toXml(parsed)).isEqualTo(out.replace(" xmlns:fake=\"http://example.com/fake\"", "")
                .replace("<fake:Fake Valor=\"42\"/>", "<fake:Fake xmlns:fake=\"http://example.com/fake\" Valor=\"42\"></fake:Fake>"));
    }

    @Test
    void writingAComplementWithoutMapperFailsClearly() {
        Comprobante cfdi = Fixtures.base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("S")
                        .valorUnitario("1.00").objetoImp(io.github.alktrip.cfdi4j.core.catalog.ObjetoImp.NO_OBJETO))
                .complemento(new Fake("42"))
                .build();

        assertThatThrownBy(() -> CfdiXml.defaults().toXml(cfdi))
                .isInstanceOf(Cfdi4jException.class)
                .hasMessageContaining("No XML mapper registered for complement")
                .hasMessageContaining("http://example.com/fake");
    }

    @Test
    void twoMappersForTheSameNamespaceAreAnErrorNamingBoth() {
        assertThatThrownBy(() -> CfdiXml.builder().register(new FakeXml()).register(new AnotherFakeXml()).build())
                .isInstanceOf(Cfdi4jException.class)
                .hasMessageContaining("Two complement mappers for namespace http://example.com/fake")
                .hasMessageContaining(FakeXml.class.getName())
                .hasMessageContaining(AnotherFakeXml.class.getName());
    }

    @Test
    void twoMappersForTheSameTypeAreAnError() {
        FakeXml sameType = new FakeXml() {
            @Override
            public String namespace() {
                return "http://example.com/elsewhere";
            }
        };

        assertThatThrownBy(() -> CfdiXml.builder().register(new FakeXml()).register(sameType).build())
                .hasMessageContaining("Two complement mappers for type");
    }

    @Test
    void mappersAreOrderedByNamespaceWhateverTheRegistrationOrder() {
        CfdiXml xml = CfdiXml.builder().register(new FakeXml()).register(new OtherXml()).build();

        assertThat(xml.complementNamespaces()).containsExactly("http://example.com/a-first", "http://example.com/fake");
        assertThat(CfdiXml.defaults().complementNamespaces()).containsExactly("http://www.sat.gob.mx/TimbreFiscalDigital");
        assertThat(CfdiXml.builder().build().complementNamespaces()).isEmpty();
    }
}
