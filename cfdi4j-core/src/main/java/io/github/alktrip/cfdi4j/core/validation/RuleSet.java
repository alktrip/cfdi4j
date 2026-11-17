package io.github.alktrip.cfdi4j.core.validation;

import java.util.List;

/**
 * Rules contributed by another module, for example the complemento de pagos, with the official
 * error matrix their codes come from (ADR 0004, ADR 0006).
 *
 * <p>Implementations are {@link java.util.ServiceLoader} providers
 * ({@code provides io.github.alktrip.cfdi4j.core.validation.RuleSet with ...}); the validator
 * applies them after the CFDI 4.0 rules, in order of {@link #name()}.
 */
public interface RuleSet {

    /**
     * Returns a unique name, used to order rule sets deterministically.
     *
     * @return the name, for example {@code pagos20}
     */
    String name();

    /**
     * Returns the rules.
     *
     * @return the rules
     */
    List<Rule> rules();

    /**
     * Returns the official matrix of the codes these rules report, used for their messages.
     *
     * @return the matrix entries
     */
    List<MatrizErrores.Entrada> matriz();
}
