package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Size;

/** build -> serialize -> parse -> equal model, for arbitrary amounts, scales and texts. */
class CfdiXmlRoundTripProperties {

    record Linea(BigDecimal cantidad, BigDecimal valorUnitario, String descripcion, boolean conIva) {}

    @Property(tries = 200)
    void roundTripPreservesTheModel(@ForAll("lineas") @Size(min = 1, max = 5) List<Linea> lineas) {
        ComprobanteBuilder builder = Fixtures.base();
        for (Linea l : lineas) {
            builder.concepto(c -> {
                c.claveProdServ("01010101").cantidad(l.cantidad()).claveUnidad("H87").descripcion(l.descripcion())
                        .valorUnitario(l.valorUnitario()).objetoImp(l.conIva() ? ObjetoImp.SI_OBJETO : ObjetoImp.NO_OBJETO);
                if (l.conIva()) {
                    c.trasladoIva16();
                }
            });
        }
        Comprobante cfdi = builder.build();

        assertThat(CfdiXml.defaults().parse(CfdiXml.defaults().toXml(cfdi))).isEqualTo(cfdi);
    }

    @Provide
    Arbitrary<List<Linea>> lineas() {
        Arbitrary<BigDecimal> cantidad = Arbitraries.integers().between(0, 6)
                .flatMap(s -> Arbitraries.bigDecimals().between(BigDecimal.ONE, new BigDecimal("100000")).ofScale(s));
        Arbitrary<BigDecimal> valor = Arbitraries.integers().between(0, 6)
                .flatMap(s -> Arbitraries.bigDecimals().between(BigDecimal.ZERO, new BigDecimal("1000000")).ofScale(s));
        // Printable characters, including XML metacharacters and non-ASCII letters; no '|' (forbidden by the XSD pattern).
        Arbitrary<String> texto = Arbitraries.strings().withChars("abcXYZ 0123&<>\"'áéíóúñÑ¿?#-_.,;:()[]{}").ofMinLength(1).ofMaxLength(40)
                .filter(s -> !s.isBlank() && s.equals(s.strip()) && !s.contains("  "));
        return Combinators.combine(cantidad, valor, texto, Arbitraries.of(true, false)).as(Linea::new).list();
    }
}
