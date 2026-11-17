package io.github.alktrip.cfdi4j.pagos20.internal;

import io.github.alktrip.cfdi4j.core.validation.MatrizErrores;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleSet;
import io.github.alktrip.cfdi4j.pagos20.MatrizErroresPagos20;
import java.util.List;

/** The rules of the complemento de pagos 2.0, discovered by the validator (ADR 0006). */
public final class Pagos20RuleSet implements RuleSet {

    private static final List<Rule> RULES = Pagos20Rules.rules();

    @Override
    public String name() {
        return "pagos20";
    }

    @Override
    public List<Rule> rules() {
        return RULES;
    }

    @Override
    public List<MatrizErrores.Entrada> matriz() {
        return MatrizErroresPagos20.entradas();
    }
}
