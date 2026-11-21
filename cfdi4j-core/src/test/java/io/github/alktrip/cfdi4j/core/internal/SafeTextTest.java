package io.github.alktrip.cfdi4j.core.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafeTextTest {

    @Test
    void escapesEverythingThatCouldStartANewLogLine() {
        assertThat(SafeText.of("ACT\n2026 INFO falso\r\tx\u0000 y", 100)).isEqualTo("ACT\\n2026 INFO falso\\r\\tx\\u0000\\u2028y");
    }

    @Test
    void keepsOrdinaryTextIncludingAccents() {
        assertThat(SafeText.of("Servicio de facturación ñ €", 100)).isEqualTo("Servicio de facturación ñ €");
    }

    @Test
    void shortensLongValues() {
        assertThat(SafeText.of("9".repeat(1000), 10)).isEqualTo("9999999999… (1000 characters)");
        assertThat(SafeText.of("12345", 5)).isEqualTo("12345");
    }
}
