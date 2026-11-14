package io.github.alktrip.cfdi4j.core.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class VigenciaTest {

    private static final LocalDate START = LocalDate.of(2022, 1, 1);
    private static final LocalDate END = LocalDate.of(2023, 12, 31);

    @Test
    void openEndedPeriodContainsEveryDayFromItsStart() {
        Vigencia vigencia = Vigencia.desde(START);

        assertThat(vigencia.contains(START.minusDays(1))).isFalse();
        assertThat(vigencia.contains(START)).isTrue();
        assertThat(vigencia.contains(LocalDate.of(2099, 1, 1))).isTrue();
        assertThat(vigencia.fin()).isEmpty();
    }

    @Test
    void closedPeriodIncludesBothEnds() {
        Vigencia vigencia = Vigencia.entre(START, END);

        assertThat(vigencia.contains(START)).isTrue();
        assertThat(vigencia.contains(END)).isTrue();
        assertThat(vigencia.contains(END.plusDays(1))).isFalse();
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> Vigencia.entre(END, START));
    }

    @Test
    void knownKeyOutsideItsPeriodIsNotVigente() {
        assertThat(ObjetoImp.SI_OBJETO_IVA_PODEBI.isVigente(LocalDate.of(2023, 12, 5))).isFalse();
        assertThat(ObjetoImp.SI_OBJETO_IVA_PODEBI.isVigente(LocalDate.of(2023, 12, 6))).isTrue();
    }
}
