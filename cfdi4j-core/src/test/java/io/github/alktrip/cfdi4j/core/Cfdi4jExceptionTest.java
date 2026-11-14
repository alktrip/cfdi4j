package io.github.alktrip.cfdi4j.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Cfdi4jExceptionTest {

    @Test
    void isUnchecked() {
        assertThat(new Cfdi4jException("x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void keepsMessageAndCause() {
        var cause = new IllegalStateException("root");

        var exception = new Cfdi4jException("failed", cause);

        assertThat(exception).hasMessage("failed").hasCause(cause);
    }
}
