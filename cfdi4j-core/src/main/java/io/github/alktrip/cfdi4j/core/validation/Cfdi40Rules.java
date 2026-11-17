package io.github.alktrip.cfdi4j.core.validation;

import io.github.alktrip.cfdi4j.core.internal.rules.RuleSets;
import java.util.List;

/**
 * The CFDI 4.0 rules of cfdi4j: one rule per code of the SAT error matrix (see
 * {@link MatrizErrores}), plus the cfdi4j certificate rule {@code CFDI4J-103}. Each rule states how
 * completely it checks its code ({@link RuleStatus}); {@code docs/rules-coverage.md} is generated
 * from this list.
 */
public final class Cfdi40Rules {

    private Cfdi40Rules() {}

    /**
     * Returns every rule, in matrix order.
     *
     * @return the rules
     */
    public static List<Rule> all() {
        return RuleSets.ALL;
    }
}
