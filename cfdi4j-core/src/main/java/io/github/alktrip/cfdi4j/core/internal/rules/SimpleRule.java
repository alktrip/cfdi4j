package io.github.alktrip.cfdi4j.core.internal.rules;

import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.core.validation.ValidationContext;
import java.util.Objects;
import java.util.function.Consumer;

/** A rule defined by its code, status, note and check. */
record SimpleRule(String code, RuleStatus status, String nota, Consumer<ValidationContext> checker) implements Rule {

    SimpleRule {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(nota, "nota");
        Objects.requireNonNull(checker, "checker");
    }

    static Rule rule(String code, Consumer<ValidationContext> check) {
        return new SimpleRule(code, RuleStatus.IMPLEMENTED, "", check);
    }

    static Rule rule(String code, RuleStatus status, String nota, Consumer<ValidationContext> check) {
        return new SimpleRule(code, status, nota, check);
    }

    /** A code that is documented but not checked by this library. */
    static Rule documented(String code, RuleStatus status, String nota) {
        return new SimpleRule(code, status, nota, ctx -> { });
    }

    @Override
    public void check(ValidationContext context) {
        checker.accept(context);
    }
}
