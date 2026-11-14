package io.github.alktrip.cfdi4j.tools.catalogs;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JavaTextTest {

    @Test
    void escapesJavaStringLiterals() {
        assertThat(JavaText.string("a\"b\\c\n")).isEqualTo("\"a\\\"b\\\\c\\u000a\"");
        assertThat(JavaText.string("Régimen")).isEqualTo("\"Régimen\"");
        assertThat(JavaText.string(null)).isEqualTo("null");
    }

    @Test
    void escapesJavadoc() {
        assertThat(JavaText.javadoc("a<b & c@d */")).isEqualTo("a&lt;b &amp; c&#64;d *&#47;");
    }

    @Test
    void literals() {
        assertThat(JavaText.decimal(new BigDecimal("0.160000"))).isEqualTo("new BigDecimal(\"0.160000\")");
        assertThat(JavaText.vigencia(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 2, 3)))
                .isEqualTo("Vigencia.entre(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 2, 3))");
        assertThat(JavaText.vigencia(null, null)).isEqualTo("null");
        assertThat(JavaText.sentence("Uno.")).isEqualTo("Uno.");
        assertThat(JavaText.sentence("Dos")).isEqualTo("Dos.");
    }
}
