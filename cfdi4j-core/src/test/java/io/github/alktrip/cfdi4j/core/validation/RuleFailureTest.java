package io.github.alktrip.cfdi4j.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** A rule that throws is reported as an error and does not stop the other rules. */
class RuleFailureTest {

    private static final Rule ROTA = new Rule() {
        @Override
        public String code() {
            return "PRUEBA-1";
        }

        @Override
        public RuleStatus status() {
            return RuleStatus.IMPLEMENTED;
        }

        @Override
        public String nota() {
            return "";
        }

        @Override
        public void check(ValidationContext context) {
            throw new ArithmeticException("Division by zero");
        }
    };

    private static final Rule SIEMPRE_FALLA = new Rule() {
        @Override
        public String code() {
            return "PRUEBA-2";
        }

        @Override
        public RuleStatus status() {
            return RuleStatus.IMPLEMENTED;
        }

        @Override
        public String nota() {
            return "";
        }

        @Override
        public void check(ValidationContext context) {
            context.fail("PRUEBA-2", "/cfdi:Comprobante", null);
        }
    };

    @Test
    void aRuleThatThrowsIsAnErrorAndTheNextRulesStillRun() {
        List<Rule> rules = new ArrayList<>(List.of(ROTA, SIEMPRE_FALLA));
        Cfdi40Validator validator = Cfdi40Validator.builder().clock(RuleFixtures.CLOCK).rules(rules).ruleSets(List.of()).build();

        ValidationResult result = validator.validate(RuleFixtures.valido().build());

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(Violation::code).containsExactly(Cfdi40Validator.RULE_FAILED, "PRUEBA-2");
        assertThat(result.errors().get(0).message()).contains("PRUEBA-1").contains("ArithmeticException");
    }

    @Test
    void theMessageIsLocalized() {
        Cfdi40Validator validator = Cfdi40Validator.builder().clock(RuleFixtures.CLOCK).rules(List.of(ROTA)).ruleSets(List.of())
                .locale(Locale.ENGLISH).build();

        assertThat(validator.validate(RuleFixtures.valido().build()).errors().get(0).message()).startsWith("Rule PRUEBA-1 could not be evaluated");
    }
}
