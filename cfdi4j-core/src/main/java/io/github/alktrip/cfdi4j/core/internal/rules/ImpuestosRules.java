package io.github.alktrip.cfdi4j.core.internal.rules;

import static io.github.alktrip.cfdi4j.core.internal.rules.Fiscal.COMPROBANTE;
import static io.github.alktrip.cfdi4j.core.internal.rules.SimpleRule.documented;
import static io.github.alktrip.cfdi4j.core.internal.rules.SimpleRule.rule;

import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import io.github.alktrip.cfdi4j.core.model.Impuestos;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.core.validation.ValidationContext;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Rules of the tax summary cfdi:Impuestos (CFDI40201 to CFDI40221). */
final class ImpuestosRules {

    private static final String IMPUESTOS = COMPROBANTE + "/cfdi:Impuestos";

    private ImpuestosRules() {}

    /** A check of the summary, run only when it exists. */
    @FunctionalInterface
    interface ResumenCheck {
        void check(ValidationContext ctx, Impuestos i);
    }

    private static Rule resumen(String code, ResumenCheck check) {
        return rule(code, ctx -> ctx.comprobante().impuestos().ifPresent(i -> check.check(ctx, i)));
    }

    static List<Rule> rules() {
        return List.of(
                rule("CFDI40201", ctx -> {
                    if (Fiscal.trasladoOPago(ctx.comprobante()) && ctx.comprobante().impuestos().isPresent()) {
                        ctx.fail("CFDI40201", IMPUESTOS, "detalle.omitir", "Impuestos");
                    }
                }),
                resumen("CFDI40202", (ctx, i) -> i.totalImpuestosRetenidos().ifPresent(v -> decimales(ctx, "CFDI40202", IMPUESTOS + "/@TotalImpuestosRetenidos", v))),
                resumen("CFDI40203", (ctx, i) -> i.totalImpuestosRetenidos().ifPresent(v -> {
                    BigDecimal suma = Fiscal.suma(i.retenciones().stream().map(Impuestos.Retencion::importe));
                    if (!Fiscal.iguales(suma, v)) {
                        ctx.fail("CFDI40203", IMPUESTOS + "/@TotalImpuestosRetenidos", "detalle.esperado", suma.toPlainString(), v.toPlainString());
                    }
                })),
                resumen("CFDI40204", (ctx, i) -> i.totalImpuestosTrasladados().ifPresent(v -> decimales(ctx, "CFDI40204", IMPUESTOS + "/@TotalImpuestosTrasladados", v))),
                resumen("CFDI40205", (ctx, i) -> i.totalImpuestosTrasladados().ifPresent(v -> {
                    BigDecimal suma = Fiscal.suma(i.traslados().stream().flatMap(t -> t.importe().stream()));
                    if (!Fiscal.iguales(suma, v)) {
                        ctx.fail("CFDI40205", IMPUESTOS + "/@TotalImpuestosTrasladados", "detalle.esperado", suma.toPlainString(), v.toPlainString());
                    }
                })),
                resumen("CFDI40206", (ctx, i) -> {
                    if (!i.retenciones().isEmpty() && i.totalImpuestosRetenidos().isEmpty()) {
                        ctx.fail("CFDI40206", IMPUESTOS + "/@TotalImpuestosRetenidos", "detalle.requerido", "TotalImpuestosRetenidos");
                    }
                }),
                retencion("CFDI40207", (ctx, r, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), r.impuesto())) {
                        ctx.fail("CFDI40207", p + "/@Impuesto", "detalle.valor", r.impuesto());
                    }
                }),
                resumen("CFDI40208", (ctx, i) -> {
                    Set<Impuesto> vistos = new HashSet<>();
                    for (Impuestos.Retencion r : i.retenciones()) {
                        if (!vistos.add(r.impuesto())) {
                            ctx.fail("CFDI40208", IMPUESTOS + "/cfdi:Retenciones", "detalle.duplicado", r.impuesto());
                        }
                    }
                }),
                documented("CFDI40209", RuleStatus.IMPLEMENTED, "Misma condición que CFDI40206, que es el código que se reporta."),
                retencion("CFDI40210", (ctx, r, p) -> decimales(ctx, "CFDI40210", p + "/@Importe", r.importe())),
                retencion("CFDI40211", (ctx, r, p) -> esperado(ctx, "CFDI40211", p + "/@Importe", r.importe(),
                        conceptoRetenciones(ctx.comprobante()).filter(x -> x.impuesto().equals(r.impuesto())).map(Concepto.Retencion::importe))),
                rule("CFDI40212", ctx -> {
                    Comprobante c = ctx.comprobante();
                    boolean gravados = conceptoTraslados(c).anyMatch(t -> !t.tipoFactor().equals(TipoFactor.EXENTO));
                    boolean total = c.impuestos().flatMap(Impuestos::totalImpuestosTrasladados).isPresent();
                    if (gravados && !total) {
                        ctx.fail("CFDI40212", IMPUESTOS + "/@TotalImpuestosTrasladados", "detalle.requerido", "TotalImpuestosTrasladados");
                    }
                }),
                traslado("CFDI40213", (ctx, t, p) -> {
                    boolean soloExentos = conceptoTraslados(ctx.comprobante()).findAny().isPresent()
                            && conceptoTraslados(ctx.comprobante()).allMatch(x -> x.tipoFactor().equals(TipoFactor.EXENTO));
                    if (soloExentos && (t.tasaOCuota().isPresent() || t.importe().isPresent())) {
                        ctx.fail("CFDI40213", p, "detalle.omitir", "TasaOCuota/Importe");
                    }
                }),
                traslado("CFDI40214", (ctx, t, p) -> decimales(ctx, "CFDI40214", p + "/@Base", t.base())),
                traslado("CFDI40215", (ctx, t, p) -> {
                    if (!t.tipoFactor().equals(TipoFactor.EXENTO)) {
                        esperado(ctx, "CFDI40215", p + "/@Base", t.base(), conceptoTraslados(ctx.comprobante()).filter(mismoGrupo(t)).map(Concepto.Traslado::base));
                    }
                }),
                traslado("CFDI40216", (ctx, t, p) -> {
                    if (t.tipoFactor().equals(TipoFactor.EXENTO)) {
                        esperado(ctx, "CFDI40216", p + "/@Base", t.base(), conceptoTraslados(ctx.comprobante())
                                .filter(x -> x.impuesto().equals(t.impuesto()) && x.tipoFactor().equals(TipoFactor.EXENTO)).map(Concepto.Traslado::base));
                    }
                }),
                traslado("CFDI40217", (ctx, t, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), t.impuesto())) {
                        ctx.fail("CFDI40217", p + "/@Impuesto", "detalle.valor", t.impuesto());
                    }
                }),
                resumen("CFDI40218", (ctx, i) -> {
                    Set<List<Object>> vistos = new HashSet<>();
                    for (Impuestos.Traslado t : i.traslados()) {
                        List<Object> clave = java.util.Arrays.asList(t.impuesto(), t.tipoFactor(), t.tasaOCuota().map(BigDecimal::stripTrailingZeros).orElse(null));
                        if (!vistos.add(clave)) {
                            ctx.fail("CFDI40218", IMPUESTOS + "/cfdi:Traslados", "detalle.duplicado", t.impuesto() + "/" + t.tipoFactor()
                                    + t.tasaOCuota().map(v -> "/" + v.toPlainString()).orElse(""));
                        }
                    }
                }),
                traslado("CFDI40219", (ctx, t, p) -> t.tasaOCuota()
                        .filter(v -> !ConceptoRules.enTasaOCuota(ConceptoRules.fecha(ctx), t.impuesto(), t.tipoFactor(), v, true))
                        .ifPresent(v -> ctx.fail("CFDI40219", p + "/@TasaOCuota", "detalle.tasa", v.toPlainString(), t.impuesto(), t.tipoFactor()))),
                traslado("CFDI40220", (ctx, t, p) -> t.importe().ifPresent(v -> decimales(ctx, "CFDI40220", p + "/@Importe", v))),
                traslado("CFDI40221", (ctx, t, p) -> t.importe().ifPresent(v -> esperado(ctx, "CFDI40221", p + "/@Importe", v,
                        conceptoTraslados(ctx.comprobante()).filter(mismoGrupo(t)).flatMap(x -> x.importe().stream())))));
    }

    @FunctionalInterface
    interface TrasladoCheck {
        void check(ValidationContext ctx, Impuestos.Traslado t, String path);
    }

    @FunctionalInterface
    interface RetencionCheck {
        void check(ValidationContext ctx, Impuestos.Retencion r, String path);
    }

    private static Rule traslado(String code, TrasladoCheck check) {
        return resumen(code, (ctx, i) -> {
            for (int j = 0; j < i.traslados().size(); j++) {
                check.check(ctx, i.traslados().get(j), IMPUESTOS + "/cfdi:Traslados/cfdi:Traslado[" + (j + 1) + "]");
            }
        });
    }

    private static Rule retencion(String code, RetencionCheck check) {
        return resumen(code, (ctx, i) -> {
            for (int j = 0; j < i.retenciones().size(); j++) {
                check.check(ctx, i.retenciones().get(j), IMPUESTOS + "/cfdi:Retenciones/cfdi:Retencion[" + (j + 1) + "]");
            }
        });
    }

    /** Concept traslados of the same Impuesto and TasaOCuota (compared numerically) as the summary row. */
    private static Predicate<Concepto.Traslado> mismoGrupo(Impuestos.Traslado t) {
        return x -> x.impuesto().equals(t.impuesto()) && x.tipoFactor().equals(t.tipoFactor())
                && Objects.equals(x.tasaOCuota().map(BigDecimal::stripTrailingZeros).orElse(null), t.tasaOCuota().map(BigDecimal::stripTrailingZeros).orElse(null));
    }

    private static Stream<Concepto.Traslado> conceptoTraslados(Comprobante c) {
        return c.conceptos().stream().flatMap(x -> Fiscal.traslados(x).stream());
    }

    private static Stream<Concepto.Retencion> conceptoRetenciones(Comprobante c) {
        return c.conceptos().stream().flatMap(x -> Fiscal.retenciones(x).stream());
    }

    private static void decimales(ValidationContext ctx, String code, String location, BigDecimal value) {
        if (Fiscal.excedeDecimales(ctx.comprobante(), value)) {
            ctx.fail(code, location, "detalle.decimales", value.toPlainString(), Fiscal.decimalesMoneda(ctx.comprobante()).getAsInt());
        }
    }

    /** The value must equal the rounded sum of the concept values (unknown currency: not checked; CFDI40113 reports it). */
    private static void esperado(ValidationContext ctx, String code, String location, BigDecimal value, Stream<BigDecimal> valores) {
        OptionalInt d = Fiscal.decimalesMoneda(ctx.comprobante());
        if (d.isPresent()) {
            BigDecimal esperado = Importes.redondear(Fiscal.suma(valores), d.getAsInt());
            if (!Fiscal.iguales(esperado, value)) {
                ctx.fail(code, location, "detalle.esperado", esperado.toPlainString(), value.toPlainString());
            }
        }
    }
}
