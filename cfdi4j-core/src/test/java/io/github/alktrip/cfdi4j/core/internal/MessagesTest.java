package io.github.alktrip.cfdi4j.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class MessagesTest {

    private static Messages load(Locale locale) {
        return Messages.load(name -> MessagesTest.class.getResourceAsStream(name), "test-messages", locale);
    }

    @Test
    void usesTheRequestedLanguageAndFallsBackToEnglishPerKey() {
        Messages es = load(Locale.forLanguageTag("es-MX"));

        assertThat(es.format("X-001", "7")).isEqualTo("El valor 7 es incorrecto");
        assertThat(es.format("X-002")).isEqualTo("Only in English");
    }

    @Test
    void unknownLanguagesAndTheRootLocaleUseEnglish() {
        assertThat(load(Locale.JAPANESE).format("X-001", "7")).isEqualTo("Value 7 is wrong");
        assertThat(load(Locale.ROOT).format("X-001", "7")).isEqualTo("Value 7 is wrong");
    }

    @Test
    void unknownCodesAreReturnedAsIs() {
        assertThat(load(Locale.ENGLISH).format("NOPE")).isEqualTo("NOPE");
    }

    @Test
    void missingBaseFileAndReadFailuresAreReported() {
        assertThatIllegalStateException().isThrownBy(() -> Messages.load(name -> null, "absent", Locale.ENGLISH));
        assertThatThrownBy(() -> Messages.load(name -> {
            throw new IOException("boom");
        }, "x", Locale.ENGLISH)).isInstanceOf(UncheckedIOException.class);
    }
}
