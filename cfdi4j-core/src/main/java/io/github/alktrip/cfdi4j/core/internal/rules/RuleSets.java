package io.github.alktrip.cfdi4j.core.internal.rules;

import io.github.alktrip.cfdi4j.core.validation.MatrizErrores;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Assembles every rule, sorted in matrix order; cfdi4j codes go last. */
public final class RuleSets {

    /** Every rule. */
    public static final List<Rule> ALL = assemble();

    private RuleSets() {}

    private static List<Rule> assemble() {
        List<Rule> all = new ArrayList<>(Stream.of(ComprobanteRules.rules(), PartesRules.rules(), ConceptoRules.rules(),
                ImpuestosRules.rules(), CertificadoRules.rules()).flatMap(List::stream).toList());
        all.add(SimpleRule.documented("CFDI40999", RuleStatus.NOT_APPLICABLE, "Código del PAC para errores no clasificados."));
        List<String> order = MatrizErrores.entradas().stream().map(MatrizErrores.Entrada::codigo).toList();
        all.sort(Comparator.comparingInt((Rule r) -> order.contains(r.code()) ? order.indexOf(r.code()) : Integer.MAX_VALUE)
                .thenComparing(Rule::code));
        return List.copyOf(all);
    }
}
