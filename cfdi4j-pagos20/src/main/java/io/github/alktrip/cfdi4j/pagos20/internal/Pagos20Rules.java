package io.github.alktrip.cfdi4j.pagos20.internal;

import io.github.alktrip.cfdi4j.core.catalog.CatalogKey;
import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TasaOCuota;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.core.validation.ValidationContext;
import io.github.alktrip.cfdi4j.pagos20.Pagos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Rules of the error matrix of the complemento para recepción de pagos 2.0, revisión B
 * ({@code MatrizDeErrores_CRP_V20_RevB.xls}). They apply only to comprobantes that include the
 * complement.
 */
final class Pagos20Rules {

    private static final String COMPROBANTE = "/cfdi:Comprobante";
    private static final String PAGOS = COMPROBANTE + "/cfdi:Complemento/pago20:Pagos";
    private static final String CONCEPTO = COMPROBANTE + "/cfdi:Conceptos/cfdi:Concepto[1]";
    private static final Set<String> FORMAS_ORDENANTE = Set.of("02", "03", "04", "05", "06", "28", "29");
    private static final Set<String> FORMAS_BENEFICIARIO = Set.of("02", "03", "04", "05", "28", "29");
    private static final BigDecimal IVA16 = new BigDecimal("0.16");
    private static final BigDecimal IVA8 = new BigDecimal("0.08");

    private Pagos20Rules() {}

    /** A rule with its code, status and note. */
    private record PagosRule(String code, RuleStatus status, String nota, BiConsumer<ValidationContext, Pagos> checker) implements Rule {
        @Override
        public void check(ValidationContext context) {
            context.comprobante().complemento().stream().filter(Pagos.class::isInstance).map(Pagos.class::cast).findFirst()
                    .ifPresent(p -> checker.accept(context, p));
        }
    }

    private static Rule rule(String code, BiConsumer<ValidationContext, Pagos> check) {
        return new PagosRule(code, RuleStatus.IMPLEMENTED, "", check);
    }

    private static Rule rule(String code, RuleStatus status, String nota, BiConsumer<ValidationContext, Pagos> check) {
        return new PagosRule(code, status, nota, check);
    }

    private static Rule documented(String code, RuleStatus status, String nota) {
        return new PagosRule(code, status, nota, (ctx, p) -> { });
    }

    /** A check of one Pago. */
    @FunctionalInterface
    interface PagoCheck {
        void check(ValidationContext ctx, Pagos.Pago pago, String path);
    }

    /** A check of one DoctoRelacionado. */
    @FunctionalInterface
    interface DoctoCheck {
        void check(ValidationContext ctx, Pagos.Pago pago, Pagos.DoctoRelacionado d, String path);
    }

    private static Rule porPago(String code, PagoCheck check) {
        return porPago(code, RuleStatus.IMPLEMENTED, "", check);
    }

    private static Rule porPago(String code, RuleStatus status, String nota, PagoCheck check) {
        return rule(code, status, nota, (ctx, pagos) -> {
            for (int i = 0; i < pagos.pago().size(); i++) {
                check.check(ctx, pagos.pago().get(i), PAGOS + "/pago20:Pago[" + (i + 1) + "]");
            }
        });
    }

    private static Rule porDocto(String code, DoctoCheck check) {
        return porPago(code, (ctx, p, path) -> {
            for (int j = 0; j < p.doctoRelacionado().size(); j++) {
                check.check(ctx, p, p.doctoRelacionado().get(j), path + "/pago20:DoctoRelacionado[" + (j + 1) + "]");
            }
        });
    }

    static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        rules.addAll(comprobanteBase());
        rules.addAll(totales());
        rules.addAll(pago());
        rules.addAll(documentos());
        rules.addAll(impuestosP());
        rules.add(documented("CRP20999", RuleStatus.NOT_APPLICABLE, "Código del PAC para errores no clasificados."));
        return List.copyOf(rules);
    }

    // --- CRP20101 to CRP20123: the comprobante that carries the complement ---

    private static List<Rule> comprobanteBase() {
        return List.of(
                rule("CRP20101", (ctx, p) -> igual(ctx, "CRP20101", COMPROBANTE + "/@TipoDeComprobante", "P", ctx.comprobante().tipoDeComprobante())),
                rule("CRP20102", (ctx, p) -> igual(ctx, "CRP20102", COMPROBANTE + "/@Exportacion", Exportacion.NO_APLICA.clave(), ctx.comprobante().exportacion())),
                rule("CRP20103", (ctx, p) -> cero(ctx, "CRP20103", COMPROBANTE + "/@SubTotal", ctx.comprobante().subTotal())),
                rule("CRP20104", (ctx, p) -> igual(ctx, "CRP20104", COMPROBANTE + "/@Moneda", "XXX", ctx.comprobante().moneda())),
                rule("CRP20105", (ctx, p) -> ausente(ctx, "CRP20105", COMPROBANTE + "/@FormaPago", ctx.comprobante().formaPago())),
                rule("CRP20106", (ctx, p) -> ausente(ctx, "CRP20106", COMPROBANTE + "/@MetodoPago", ctx.comprobante().metodoPago())),
                rule("CRP20107", (ctx, p) -> ausente(ctx, "CRP20107", COMPROBANTE + "/@CondicionesDePago", ctx.comprobante().condicionesDePago())),
                rule("CRP20108", (ctx, p) -> ausente(ctx, "CRP20108", COMPROBANTE + "/@Descuento", ctx.comprobante().descuento())),
                rule("CRP20109", (ctx, p) -> ausente(ctx, "CRP20109", COMPROBANTE + "/@TipoCambio", ctx.comprobante().tipoCambio())),
                rule("CRP20110", (ctx, p) -> cero(ctx, "CRP20110", COMPROBANTE + "/@Total", ctx.comprobante().total())),
                rule("CRP20111", (ctx, p) -> {
                    if (ctx.comprobante().conceptos().size() != 1) {
                        ctx.fail("CRP20111", COMPROBANTE + "/cfdi:Conceptos", "detalle.valor", ctx.comprobante().conceptos().size());
                    }
                }),
                concepto("CRP20112", (ctx, c) -> {
                    if (c.impuestos().isPresent() || !c.informacionAduanera().isEmpty() || !c.cuentaPredial().isEmpty()
                            || !c.complementoConcepto().isEmpty() || !c.parte().isEmpty()) {
                        ctx.fail("CRP20112", CONCEPTO, "detalle.omitir", "Impuestos/InformacionAduanera/CuentaPredial/ComplementoConcepto/Parte");
                    }
                }),
                concepto("CRP20113", (ctx, c) -> igual(ctx, "CRP20113", CONCEPTO + "/@ClaveProdServ", "84111506", c.claveProdServ())),
                concepto("CRP20114", (ctx, c) -> ausente(ctx, "CRP20114", CONCEPTO + "/@NoIdentificacion", c.noIdentificacion())),
                concepto("CRP20115", (ctx, c) -> {
                    if (c.cantidad().compareTo(BigDecimal.ONE) != 0) {
                        ctx.fail("CRP20115", CONCEPTO + "/@Cantidad", "detalle.esperado", "1", c.cantidad().toPlainString());
                    }
                }),
                concepto("CRP20116", (ctx, c) -> igual(ctx, "CRP20116", CONCEPTO + "/@ClaveUnidad", "ACT", c.claveUnidad())),
                concepto("CRP20117", (ctx, c) -> ausente(ctx, "CRP20117", CONCEPTO + "/@Unidad", c.unidad())),
                concepto("CRP20118", (ctx, c) -> igual(ctx, "CRP20118", CONCEPTO + "/@Descripcion", "Pago", c.descripcion())),
                concepto("CRP20119", (ctx, c) -> cero(ctx, "CRP20119", CONCEPTO + "/@ValorUnitario", c.valorUnitario())),
                concepto("CRP20120", (ctx, c) -> cero(ctx, "CRP20120", CONCEPTO + "/@Importe", c.importe())),
                concepto("CRP20121", (ctx, c) -> ausente(ctx, "CRP20121", CONCEPTO + "/@Descuento", c.descuento())),
                concepto("CRP20122", (ctx, c) -> igual(ctx, "CRP20122", CONCEPTO + "/@ObjetoImp", ObjetoImp.NO_OBJETO.clave(), c.objetoImp())),
                concepto("CRP20123", (ctx, c) -> ausente(ctx, "CRP20123", CONCEPTO + "/cfdi:Impuestos", c.impuestos())));
    }

    private static Rule concepto(String code, BiConsumer<ValidationContext, Concepto> check) {
        return rule(code, (ctx, p) -> ctx.comprobante().conceptos().stream().findFirst().ifPresent(c -> check.accept(ctx, c)));
    }

    // --- CRP20201 to CRP20211: Totales ---

    private static List<Rule> totales() {
        return List.of(
                total("CRP20201", "TotalRetencionesIVA", Pagos.Totales::totalRetencionesIVA, retencion(Impuesto.IVA)),
                total("CRP20202", "TotalRetencionesISR", Pagos.Totales::totalRetencionesISR, retencion(Impuesto.ISR)),
                total("CRP20203", "TotalRetencionesIEPS", Pagos.Totales::totalRetencionesIEPS, retencion(Impuesto.IEPS)),
                total("CRP20204", "TotalTrasladosBaseIVA16", Pagos.Totales::totalTrasladosBaseIVA16, trasladoIva(IVA16, true)),
                total("CRP20205", "TotalTrasladosImpuestoIVA16", Pagos.Totales::totalTrasladosImpuestoIVA16, trasladoIva(IVA16, false)),
                total("CRP20206", "TotalTrasladosBaseIVA8", Pagos.Totales::totalTrasladosBaseIVA8, trasladoIva(IVA8, true)),
                total("CRP20207", "TotalTrasladosImpuestoIVA8", Pagos.Totales::totalTrasladosImpuestoIVA8, trasladoIva(IVA8, false)),
                total("CRP20208", "TotalTrasladosBaseIVA0", Pagos.Totales::totalTrasladosBaseIVA0, trasladoIva(BigDecimal.ZERO, true)),
                total("CRP20209", "TotalTrasladosImpuestoIVA0", Pagos.Totales::totalTrasladosImpuestoIVA0, trasladoIva(BigDecimal.ZERO, false)),
                total("CRP20210", "TotalTrasladosBaseIVAExento", Pagos.Totales::totalTrasladosBaseIVAExento, pago -> pago.impuestosP().stream()
                        .flatMap(i -> i.trasladosP().stream())
                        .filter(t -> t.impuestoP().equals(Impuesto.IVA) && t.tipoFactorP().equals(TipoFactor.EXENTO))
                        .map(Pagos.TrasladoP::baseP)),
                rule("CRP20211", RuleStatus.IMPLEMENTED, NOTA_TOTALES, (ctx, pagos) -> verificaTotal(ctx, "CRP20211",
                        PAGOS + "/pago20:Totales/@MontoTotalPagos", pagos.totales().montoTotalPagos(), pagos, p -> Stream.of(p.monto()))));
    }

    private static Function<Pagos.Pago, Stream<BigDecimal>> retencion(Impuesto impuesto) {
        return pago -> pago.impuestosP().stream().flatMap(i -> i.retencionesP().stream())
                .filter(r -> r.impuestoP().equals(impuesto)).map(Pagos.RetencionP::importeP);
    }

    private static Function<Pagos.Pago, Stream<BigDecimal>> trasladoIva(BigDecimal tasa, boolean base) {
        return pago -> pago.impuestosP().stream().flatMap(i -> i.trasladosP().stream())
                .filter(t -> t.impuestoP().equals(Impuesto.IVA) && t.tipoFactorP().equals(TipoFactor.TASA)
                        && t.tasaOCuotaP().filter(v -> v.compareTo(tasa) == 0).isPresent())
                .map(t -> base ? t.baseP() : t.importeP().orElse(BigDecimal.ZERO));
    }

    /**
     * A total of Totales must equal the rounded sum of the values × TipoCambioP of every Pago, and
     * must be omitted when there are no such values ("En caso contrario no se debe registrar").
     */
    private static Rule total(String code, String attribute, Function<Pagos.Totales, Optional<BigDecimal>> registrado,
            Function<Pagos.Pago, Stream<BigDecimal>> valores) {
        return rule(code, RuleStatus.IMPLEMENTED, NOTA_TOTALES, (ctx, pagos) -> {
            String location = PAGOS + "/pago20:Totales/@" + attribute;
            boolean hayValores = pagos.pago().stream().anyMatch(p -> valores.apply(p).findAny().isPresent());
            Optional<BigDecimal> total = registrado.apply(pagos.totales());
            if (!hayValores) {
                total.ifPresent(v -> ctx.fail(code, location, "detalle.omitir", attribute));
                return;
            }
            if (total.isEmpty()) {
                ctx.fail(code, location, "detalle.requerido", attribute);
            } else {
                verificaTotal(ctx, code, location, total.get(), pagos, valores);
            }
        });
    }

    /** Compares a total with Σ value × TipoCambioP, with the tolerance of {@link #NOTA_TOTALES}. */
    private static void verificaTotal(ValidationContext ctx, String code, String location, BigDecimal total, Pagos pagos,
            Function<Pagos.Pago, Stream<BigDecimal>> valores) {
        BigDecimal exacto = BigDecimal.ZERO;
        BigDecimal tolerancia = Aritmetica.mediaUnidad(Aritmetica.DECIMALES_MXN);
        for (Pagos.Pago p : pagos.pago()) {
            OptionalInt dp = p.monedaP().decimales();
            if (p.tipoCambioP().isEmpty() || dp.isEmpty()) {
                return; // CRP20214 or CRP20219 reports it.
            }
            BigDecimal tc = p.tipoCambioP().get();
            for (BigDecimal v : valores.apply(p).toList()) {
                exacto = exacto.add(v.multiply(tc));
                tolerancia = tolerancia.add(Aritmetica.mediaUnidad(Aritmetica.precision(v, dp.getAsInt())).multiply(tc));
            }
        }
        if (total.subtract(exacto).abs().compareTo(tolerancia) > 0) {
            ctx.fail(code, location, "detalle.esperado", Aritmetica.totalMxn(exacto).toPlainString(), total.toPlainString());
        }
    }

    // --- CRP20212 to CRP20235, CRP20275 to CRP20277: Pago ---

    private static List<Rule> pago() {
        List<Rule> rules = new ArrayList<>(List.of(
                porPago("CRP20212", (ctx, p, path) -> {
                    if (p.formaDePagoP().equals(FormaPago.POR_DEFINIR)) {
                        ctx.fail("CRP20212", path + "/@FormaDePagoP", "detalle.valor", p.formaDePagoP());
                    }
                }),
                porPago("CRP20213", (ctx, p, path) -> {
                    if (p.monedaP().equals(Moneda.XXX)) {
                        ctx.fail("CRP20213", path + "/@MonedaP", "detalle.valor", p.monedaP());
                    }
                }),
                porPago("CRP20214", (ctx, p, path) -> {
                    if (!p.monedaP().equals(Moneda.MXN) && p.tipoCambioP().isEmpty()) {
                        ctx.fail("CRP20214", path + "/@TipoCambioP", "detalle.requerido", "TipoCambioP");
                    }
                }),
                porPago("CRP20215", (ctx, p, path) -> {
                    if (p.monedaP().equals(Moneda.MXN) && p.tipoCambioP().filter(t -> t.compareTo(BigDecimal.ONE) == 0).isEmpty()) {
                        ctx.fail("CRP20215", path + "/@TipoCambioP", "detalle.esperado", "1", p.tipoCambioP().map(BigDecimal::toPlainString).orElse("-"));
                    }
                }),
                documented("CRP20216", RuleStatus.EXTERNAL_SERVICE,
                        "Requiere el tipo de cambio publicado para la fecha y la clave de confirmación del PAC."),
                porPago("CRP20217", RuleStatus.IMPLEMENTED, "Cuando todos los documentos están en la moneda del pago.", (ctx, p, path) -> {
                    if (p.doctoRelacionado().stream().allMatch(d -> d.monedaDR().equals(p.monedaP()))) {
                        BigDecimal suma = p.doctoRelacionado().stream().map(Pagos.DoctoRelacionado::impPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
                        if (suma.compareTo(p.monto()) > 0) {
                            ctx.fail("CRP20217", path + "/@Monto", "detalle.maximo", p.monto().toPlainString(), suma.toPlainString());
                        }
                    }
                }),
                porPago("CRP20218", (ctx, p, path) -> {
                    if (p.monto().signum() <= 0) {
                        ctx.fail("CRP20218", path + "/@Monto", "detalle.valor", p.monto().toPlainString());
                    }
                }),
                porPago("CRP20219", (ctx, p, path) -> decimales(ctx, "CRP20219", path + "/@Monto", p.monto(), p.monedaP())),
                porPago("CRP20220", RuleStatus.PARTIAL,
                        "Se compara el equivalente en MXN con el valor máximo de c_TipoDeComprobante para \"P\"; los límites "
                                + "personalizados de l_RFC requieren un servicio del SAT.",
                        (ctx, p, path) -> {
                            Optional<BigDecimal> maximo = TipoDeComprobante.PAGO.valorMaximo();
                            if (maximo.isPresent() && p.tipoCambioP().isPresent()
                                    && p.monto().multiply(p.tipoCambioP().get()).compareTo(maximo.get()) > 0 && ctx.comprobante().confirmacion().isEmpty()) {
                                ctx.fail("CRP20220", path + "/@Monto", "detalle.maximo", maximo.get().toPlainString(), p.monto().toPlainString());
                            }
                        }),
                porPago("CRP20221", (ctx, p, path) -> soloCon(ctx, "CRP20221", path, "RfcEmisorCtaOrd", p.rfcEmisorCtaOrd(), p, FORMAS_ORDENANTE)),
                porPago("CRP20222", RuleStatus.EXTERNAL_SERVICE,
                        "Si el RFC no es XEXX010101000 se valida con un TaxpayerRegistry; sin él se reporta como no verificado.",
                        (ctx, p, path) -> p.rfcEmisorCtaOrd().filter(r -> !"XEXX010101000".equals(r)).ifPresent(rfc -> {
                            if (ctx.taxpayers().isEmpty()) {
                                ctx.notVerified("CRP20222", path + "/@RfcEmisorCtaOrd", "no.verificado.padron");
                            } else if (ctx.taxpayers().get().find(rfc).isEmpty()) {
                                ctx.fail("CRP20222", path + "/@RfcEmisorCtaOrd", "detalle.no.inscrito", rfc);
                            }
                        })),
                porPago("CRP20223", (ctx, p, path) -> {
                    if (p.rfcEmisorCtaOrd().filter("XEXX010101000"::equals).isPresent() && p.nomBancoOrdExt().isEmpty()) {
                        ctx.fail("CRP20223", path + "/@NomBancoOrdExt", "detalle.requerido", "NomBancoOrdExt");
                    }
                }),
                porPago("CRP20224", (ctx, p, path) -> soloCon(ctx, "CRP20224", path, "CtaOrdenante", p.ctaOrdenante(), p, FORMAS_ORDENANTE)),
                porPago("CRP20225", (ctx, p, path) -> patron(ctx, "CRP20225", path + "/@CtaOrdenante", p.ctaOrdenante(),
                        p.formaDePagoP().patronCuentaOrdenante())),
                porPago("CRP20226", (ctx, p, path) -> patron(ctx, "CRP20226", path + "/@CtaBeneficiario", p.ctaBeneficiario(),
                        p.formaDePagoP().patronCuentaBeneficiaria())),
                porPago("CRP20227", (ctx, p, path) -> soloCon(ctx, "CRP20227", path, "RfcEmisorCtaBen", p.rfcEmisorCtaBen(), p, FORMAS_BENEFICIARIO)),
                porPago("CRP20228", (ctx, p, path) -> soloCon(ctx, "CRP20228", path, "CtaBeneficiario", p.ctaBeneficiario(), p, FORMAS_BENEFICIARIO)),
                porPago("CRP20229", (ctx, p, path) -> soloCon(ctx, "CRP20229", path, "TipoCadPago", p.tipoCadPago(), p, Set.of("03"))),
                porDocto("CRP20275", (ctx, p, d, path) -> {
                    if (!d.monedaDR().equals(p.monedaP()) && d.equivalenciaDR().isPresent()) {
                        OptionalInt dp = p.monedaP().decimales();
                        if (dp.isPresent()) {
                            BigDecimal convertido = Aritmetica.aMonedaPago(d.impPagado(), d.equivalenciaDR().get())
                                    .setScale(dp.getAsInt(), java.math.RoundingMode.DOWN);
                            Aritmetica.Limites l = limites(p, d, dp.getAsInt());
                            if (!l.contiene(convertido)) {
                                ctx.fail("CRP20275", path + "/@ImpPagado", "detalle.limites", l.inferior().toPlainString(), l.superior().toPlainString(),
                                        convertido.toPlainString());
                            }
                        }
                    }
                }),
                porPago("CRP20276", RuleStatus.IMPLEMENTED,
                        "Cuando algún documento está en otra moneda: si la suma de ImpPagado / EquivalenciaDR (truncada) excede Monto, "
                                + "Monto debe estar entre la suma de los límites inferiores y la de los superiores (Guía de llenado, página 33; "
                                + "el límite superior se redondea al medio).",
                        Pagos20Rules::montoConMargen),
                documented("CRP20277", RuleStatus.IMPLEMENTED,
                        "Se aplica dentro del cálculo de CRP20275 y CRP20276: una EquivalenciaDR de 1 cuenta con 10 decimales.")));
        rules.addAll(cadena("CRP20230", "CRP20231", "CertPago", Pagos.Pago::certPago));
        rules.addAll(cadena("CRP20232", "CRP20233", "CadPago", Pagos.Pago::cadPago));
        rules.addAll(cadena("CRP20234", "CRP20235", "SelloPago", Pagos.Pago::selloPago));
        return rules;
    }

    private static Aritmetica.Limites limites(Pagos.Pago p, Pagos.DoctoRelacionado d, int decimalesP) {
        BigDecimal eq = d.equivalenciaDR().orElse(BigDecimal.ONE);
        boolean mixto = p.doctoRelacionado().stream().anyMatch(x -> !x.monedaDR().equals(p.monedaP()));
        int decimalesEq = mixto && eq.compareTo(BigDecimal.ONE) == 0 ? 10 : Importes.decimales(eq);
        return Aritmetica.limitesImpPagado(d.impPagado(), eq, decimalesEq, decimalesP);
    }

    private static void montoConMargen(ValidationContext ctx, Pagos.Pago p, String path) {
        OptionalInt dp = p.monedaP().decimales();
        boolean mixto = p.doctoRelacionado().stream().anyMatch(d -> !d.monedaDR().equals(p.monedaP()));
        if (!mixto || dp.isEmpty() || p.doctoRelacionado().stream().anyMatch(d -> d.equivalenciaDR().isEmpty())) {
            return;
        }
        BigDecimal suma = p.doctoRelacionado().stream().map(d -> Aritmetica.aMonedaPago(d.impPagado(), d.equivalenciaDR().get()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(dp.getAsInt(), java.math.RoundingMode.DOWN);
        if (suma.compareTo(p.monto()) <= 0) {
            return;
        }
        BigDecimal inferior = BigDecimal.ZERO;
        BigDecimal superior = BigDecimal.ZERO;
        for (Pagos.DoctoRelacionado d : p.doctoRelacionado()) {
            Aritmetica.Limites l = limites(p, d, dp.getAsInt());
            inferior = inferior.add(l.inferior());
            superior = superior.add(l.superior());
        }
        if (p.monto().compareTo(inferior) < 0 || p.monto().compareTo(superior) > 0) {
            ctx.fail("CRP20276", path + "/@Monto", "detalle.limites", inferior.toPlainString(), superior.toPlainString(), p.monto().toPlainString());
        }
    }

    /** CRP20230/31-style pair: with TipoCadPago the attribute is required, without it the attribute is forbidden. */
    private static List<Rule> cadena(String requerido, String prohibido, String attribute, Function<Pagos.Pago, Optional<String>> valor) {
        return List.of(
                porPago(requerido, (ctx, p, path) -> {
                    if (p.tipoCadPago().isPresent() && valor.apply(p).isEmpty()) {
                        ctx.fail(requerido, path + "/@" + attribute, "detalle.requerido", attribute);
                    }
                }),
                porPago(prohibido, (ctx, p, path) -> {
                    if (p.tipoCadPago().isEmpty() && valor.apply(p).isPresent()) {
                        ctx.fail(prohibido, path + "/@" + attribute, "detalle.omitir", attribute);
                    }
                }));
    }

    private static void soloCon(ValidationContext ctx, String code, String path, String attribute, Optional<String> valor, Pagos.Pago p, Set<String> formas) {
        if (valor.isPresent() && !formas.contains(p.formaDePagoP().clave())) {
            ctx.fail(code, path + "/@" + attribute, "detalle.omitir", attribute);
        }
    }

    private static void patron(ValidationContext ctx, String code, String location, Optional<String> valor, Optional<String> patron) {
        if (valor.isEmpty() || patron.isEmpty()) {
            return;
        }
        try {
            if (!Pattern.compile(patron.get()).matcher(valor.get()).matches()) {
                ctx.fail(code, location, "detalle.patron", valor.get(), patron.get());
            }
        } catch (PatternSyntaxException e) {
            // A malformed catalog pattern cannot be applied.
        }
    }

    // --- CRP20236 to CRP20261, CRP20278, CRP20279: DoctoRelacionado ---

    private static List<Rule> documentos() {
        return List.of(
                porDocto("CRP20236", (ctx, p, d, path) -> {
                    if (d.monedaDR().equals(Moneda.XXX)) {
                        ctx.fail("CRP20236", path + "/@MonedaDR", "detalle.valor", d.monedaDR());
                    }
                }),
                porDocto("CRP20237", (ctx, p, d, path) -> {
                    if (!d.monedaDR().equals(p.monedaP()) && d.equivalenciaDR().isEmpty()) {
                        ctx.fail("CRP20237", path + "/@EquivalenciaDR", "detalle.requerido", "EquivalenciaDR");
                    }
                }),
                porDocto("CRP20238", (ctx, p, d, path) -> {
                    if (d.monedaDR().equals(p.monedaP()) && d.equivalenciaDR().filter(e -> e.compareTo(BigDecimal.ONE) == 0).isEmpty()) {
                        ctx.fail("CRP20238", path + "/@EquivalenciaDR", "detalle.esperado", "1",
                                d.equivalenciaDR().map(BigDecimal::toPlainString).orElse("-"));
                    }
                }),
                porDocto("CRP20239", (ctx, p, d, path) -> positivo(ctx, "CRP20239", path + "/@ImpSaldoAnt", d.impSaldoAnt())),
                porDocto("CRP20240", (ctx, p, d, path) -> decimales(ctx, "CRP20240", path + "/@ImpSaldoAnt", d.impSaldoAnt(), d.monedaDR())),
                porDocto("CRP20241", (ctx, p, d, path) -> positivo(ctx, "CRP20241", path + "/@ImpPagado", d.impPagado())),
                porDocto("CRP20242", (ctx, p, d, path) -> decimales(ctx, "CRP20242", path + "/@ImpPagado", d.impPagado(), d.monedaDR())),
                porDocto("CRP20243", (ctx, p, d, path) -> decimales(ctx, "CRP20243", path + "/@ImpSaldoInsoluto", d.impSaldoInsoluto(), d.monedaDR())),
                porDocto("CRP20244", (ctx, p, d, path) -> {
                    BigDecimal esperado = d.impSaldoAnt().subtract(d.impPagado());
                    if (d.impSaldoInsoluto().signum() < 0 || d.impSaldoInsoluto().compareTo(esperado) != 0) {
                        ctx.fail("CRP20244", path + "/@ImpSaldoInsoluto", "detalle.esperado", esperado.toPlainString(), d.impSaldoInsoluto().toPlainString());
                    }
                }),
                porDocto("CRP20245", (ctx, p, d, path) -> catalogo(ctx, "CRP20245", path + "/@ObjetoImpDR", d.objetoImpDR(), p.fechaPago().toLocalDate())),
                porDocto("CRP20246", (ctx, p, d, path) -> {
                    if (d.objetoImpDR().equals(ObjetoImp.SI_OBJETO) && d.impuestosDR().isEmpty()) {
                        ctx.fail("CRP20246", path + "/pago20:ImpuestosDR", "detalle.requerido", "ImpuestosDR");
                    }
                }),
                porDocto("CRP20247", (ctx, p, d, path) -> {
                    Set<ObjetoImp> sin = Set.of(ObjetoImp.NO_OBJETO, ObjetoImp.SI_OBJETO_NO_DESGLOSE, ObjetoImp.SI_OBJETO_NO_CAUSA, ObjetoImp.SI_OBJETO_IVA_PODEBI);
                    if (sin.contains(d.objetoImpDR()) && d.impuestosDR().isPresent()) {
                        ctx.fail("CRP20247", path + "/pago20:ImpuestosDR", "detalle.omitir", "ImpuestosDR");
                    }
                }),
                porDocto("CRP20248", (ctx, p, d, path) -> d.impuestosDR()
                        .filter(i -> i.retencionesDR().isEmpty() && i.trasladosDR().isEmpty())
                        .ifPresent(i -> ctx.fail("CRP20248", path + "/pago20:ImpuestosDR", "detalle.requerido", "RetencionesDR/TrasladosDR"))),
                retencionDR("CRP20249", (ctx, r, path, fecha) -> positivo(ctx, "CRP20249", path + "/@BaseDR", r.baseDR())),
                retencionDR("CRP20250", (ctx, r, path, fecha) -> catalogo(ctx, "CRP20250", path + "/@ImpuestoDR", r.impuestoDR(), fecha)),
                retencionDR("CRP20251", (ctx, r, path, fecha) -> catalogo(ctx, "CRP20251", path + "/@TipoFactorDR", r.tipoFactorDR(), fecha)),
                retencionDR("CRP20252", (ctx, r, path, fecha) -> {
                    if (r.tipoFactorDR().equals(TipoFactor.EXENTO)) {
                        ctx.fail("CRP20252", path + "/@TipoFactorDR", "detalle.valor", r.tipoFactorDR());
                    }
                }),
                retencionDR("CRP20253", (ctx, r, path, fecha) -> {
                    if (!r.tipoFactorDR().equals(TipoFactor.EXENTO) && !enTasaOCuota(fecha, r.impuestoDR(), r.tipoFactorDR(), r.tasaOCuotaDR(), false)) {
                        ctx.fail("CRP20253", path + "/@TasaOCuotaDR", "detalle.tasa", r.tasaOCuotaDR().toPlainString(), r.impuestoDR(), r.tipoFactorDR());
                    }
                }),
                retencionDR("CRP20254", (ctx, r, path, fecha) -> limitesImpuesto(ctx, "CRP20254", path + "/@ImporteDR", r.baseDR(), r.tasaOCuotaDR(), r.importeDR())),
                trasladoDR("CRP20255", (ctx, t, path, fecha) -> positivo(ctx, "CRP20255", path + "/@BaseDR", t.baseDR())),
                trasladoDR("CRP20256", (ctx, t, path, fecha) -> catalogo(ctx, "CRP20256", path + "/@ImpuestoDR", t.impuestoDR(), fecha)),
                trasladoDR("CRP20257", (ctx, t, path, fecha) -> catalogo(ctx, "CRP20257", path + "/@TipoFactorDR", t.tipoFactorDR(), fecha)),
                trasladoDR("CRP20258", (ctx, t, path, fecha) -> {
                    if (t.tipoFactorDR().equals(TipoFactor.EXENTO) && (t.tasaOCuotaDR().isPresent() || t.importeDR().isPresent())) {
                        ctx.fail("CRP20258", path, "detalle.omitir", "TasaOCuotaDR/ImporteDR");
                    }
                }),
                trasladoDR("CRP20259", (ctx, t, path, fecha) -> {
                    boolean tasaOCuota = t.tipoFactorDR().equals(TipoFactor.TASA) || t.tipoFactorDR().equals(TipoFactor.CUOTA);
                    if (tasaOCuota && (t.tasaOCuotaDR().isEmpty() || t.importeDR().isEmpty())) {
                        ctx.fail("CRP20259", path, "detalle.requerido", "TasaOCuotaDR/ImporteDR");
                    }
                }),
                trasladoDR("CRP20260", (ctx, t, path, fecha) -> t.tasaOCuotaDR()
                        .filter(v -> !t.tipoFactorDR().equals(TipoFactor.EXENTO))
                        .filter(v -> !enTasaOCuota(fecha, t.impuestoDR(), t.tipoFactorDR(), v, true))
                        .ifPresent(v -> ctx.fail("CRP20260", path + "/@TasaOCuotaDR", "detalle.tasa", v.toPlainString(), t.impuestoDR(), t.tipoFactorDR()))),
                trasladoDR("CRP20261", (ctx, t, path, fecha) -> {
                    if (t.tasaOCuotaDR().isPresent() && t.importeDR().isPresent()) {
                        limitesImpuesto(ctx, "CRP20261", path + "/@ImporteDR", t.baseDR(), t.tasaOCuotaDR().get(), t.importeDR().get());
                    }
                }),
                porDocto("CRP20278", (ctx, p, d, path) -> {
                    if (d.objetoImpDR().equals(ObjetoImp.SI_OBJETO_IVA_NO_TRASLADO) || d.objetoImpDR().equals(ObjetoImp.NO_TRASLADO_IVA_NO_DESGLOSE_IEPS)) {
                        for (Impuesto i : List.of(Impuesto.IVA, Impuesto.IEPS)) {
                            if (trasladaDR(d, i) || retieneDR(d, i)) {
                                ctx.fail("CRP20278", path + "/pago20:ImpuestosDR", "detalle.impuesto.prohibido", i);
                            }
                        }
                    }
                }),
                porDocto("CRP20279", (ctx, p, d, path) -> {
                    if (d.objetoImpDR().equals(ObjetoImp.NO_TRASLADO_IVA_SI_DESGLOSE_IEPS)) {
                        if (trasladaDR(d, Impuesto.IVA) || retieneDR(d, Impuesto.IVA)) {
                            ctx.fail("CRP20279", path + "/pago20:ImpuestosDR", "detalle.impuesto.prohibido", Impuesto.IVA);
                        }
                        if (!trasladaDR(d, Impuesto.IEPS)) {
                            ctx.fail("CRP20279", path + "/pago20:ImpuestosDR", "detalle.impuesto.requerido", Impuesto.IEPS);
                        }
                    }
                }));
    }

    @FunctionalInterface
    interface ImpuestoDRCheck<T> {
        void check(ValidationContext ctx, T tax, String path, LocalDate fecha);
    }

    private static Rule retencionDR(String code, ImpuestoDRCheck<Pagos.RetencionDR> check) {
        return porDocto(code, (ctx, p, d, path) -> {
            List<Pagos.RetencionDR> list = d.impuestosDR().map(Pagos.ImpuestosDR::retencionesDR).orElse(List.of());
            for (int k = 0; k < list.size(); k++) {
                check.check(ctx, list.get(k), path + "/pago20:ImpuestosDR/pago20:RetencionesDR/pago20:RetencionDR[" + (k + 1) + "]", p.fechaPago().toLocalDate());
            }
        });
    }

    private static Rule trasladoDR(String code, ImpuestoDRCheck<Pagos.TrasladoDR> check) {
        return porDocto(code, (ctx, p, d, path) -> {
            List<Pagos.TrasladoDR> list = d.impuestosDR().map(Pagos.ImpuestosDR::trasladosDR).orElse(List.of());
            for (int k = 0; k < list.size(); k++) {
                check.check(ctx, list.get(k), path + "/pago20:ImpuestosDR/pago20:TrasladosDR/pago20:TrasladoDR[" + (k + 1) + "]", p.fechaPago().toLocalDate());
            }
        });
    }

    private static boolean trasladaDR(Pagos.DoctoRelacionado d, Impuesto impuesto) {
        return d.impuestosDR().stream().flatMap(i -> i.trasladosDR().stream()).anyMatch(t -> t.impuestoDR().equals(impuesto));
    }

    private static boolean retieneDR(Pagos.DoctoRelacionado d, Impuesto impuesto) {
        return d.impuestosDR().stream().flatMap(i -> i.retencionesDR().stream()).anyMatch(r -> r.impuestoDR().equals(impuesto));
    }

    // --- CRP20262 to CRP20274: ImpuestosP ---

    private static List<Rule> impuestosP() {
        return List.of(
                retencionP("CRP20262", (ctx, p, r, path) -> catalogo(ctx, "CRP20262", path + "/@ImpuestoP", r.impuestoP(), p.fechaPago().toLocalDate())),
                porPago("CRP20263", (ctx, p, path) -> {
                    Set<Impuesto> vistos = new HashSet<>();
                    for (Pagos.RetencionP r : p.impuestosP().map(Pagos.ImpuestosP::retencionesP).orElse(List.of())) {
                        if (!vistos.add(r.impuestoP())) {
                            ctx.fail("CRP20263", path + "/pago20:ImpuestosP/pago20:RetencionesP", "detalle.duplicado", r.impuestoP());
                        }
                    }
                }),
                rule("CRP20264", (ctx, pagos) -> {
                    boolean retenciones = pagos.pago().stream().anyMatch(p -> p.impuestosP().filter(i -> !i.retencionesP().isEmpty()).isPresent());
                    Pagos.Totales t = pagos.totales();
                    if (retenciones && t.totalRetencionesIVA().isEmpty() && t.totalRetencionesISR().isEmpty() && t.totalRetencionesIEPS().isEmpty()) {
                        ctx.fail("CRP20264", PAGOS + "/pago20:Totales", "detalle.requerido", "TotalRetencionesIVA/ISR/IEPS");
                    }
                }),
                retencionP("CRP20265", RuleStatus.IMPLEMENTED, Pagos20Rules.NOTA_CONVERSION, (ctx, p, r, path) -> convertido(ctx, "CRP20265", path + "/@ImporteP",
                        r.importeP(), p, d -> d.retencionesDR().stream().filter(x -> x.impuestoDR().equals(r.impuestoP())).map(Pagos.RetencionDR::importeDR))),
                trasladoP("CRP20266", (ctx, p, t, path) -> {
                    boolean soloExentos = trasladosDR(p).findAny().isPresent() && trasladosDR(p).allMatch(x -> x.tipoFactorDR().equals(TipoFactor.EXENTO));
                    if (soloExentos && (t.tasaOCuotaP().isPresent() || t.importeP().isPresent())) {
                        ctx.fail("CRP20266", path, "detalle.omitir", "TasaOCuotaP/ImporteP");
                    }
                }),
                rule("CRP20267", (ctx, pagos) -> {
                    boolean iva = pagos.pago().stream().flatMap(p -> p.impuestosP().stream()).flatMap(i -> i.trasladosP().stream())
                            .anyMatch(t -> t.impuestoP().equals(Impuesto.IVA));
                    Pagos.Totales t = pagos.totales();
                    if (iva && Stream.of(t.totalTrasladosBaseIVA16(), t.totalTrasladosBaseIVA8(), t.totalTrasladosBaseIVA0(), t.totalTrasladosBaseIVAExento())
                            .allMatch(Optional::isEmpty)) {
                        ctx.fail("CRP20267", PAGOS + "/pago20:Totales", "detalle.requerido", "TotalTrasladosBaseIVA16/8/0/Exento");
                    }
                }),
                trasladoP("CRP20268", RuleStatus.IMPLEMENTED, Pagos20Rules.NOTA_CONVERSION, (ctx, p, t, path) -> {
                    if (!t.tipoFactorP().equals(TipoFactor.EXENTO)) {
                        convertido(ctx, "CRP20268", path + "/@BaseP", t.baseP(), p, d -> d.trasladosDR().stream().filter(mismoGrupo(t)).map(Pagos.TrasladoDR::baseDR));
                    }
                }),
                trasladoP("CRP20269", RuleStatus.IMPLEMENTED, Pagos20Rules.NOTA_CONVERSION, (ctx, p, t, path) -> {
                    if (t.tipoFactorP().equals(TipoFactor.EXENTO)) {
                        convertido(ctx, "CRP20269", path + "/@BaseP", t.baseP(), p, d -> d.trasladosDR().stream()
                                .filter(x -> x.impuestoDR().equals(t.impuestoP()) && x.tipoFactorDR().equals(TipoFactor.EXENTO)).map(Pagos.TrasladoDR::baseDR));
                    }
                }),
                trasladoP("CRP20270", (ctx, p, t, path) -> catalogo(ctx, "CRP20270", path + "/@ImpuestoP", t.impuestoP(), p.fechaPago().toLocalDate())),
                porPago("CRP20271", (ctx, p, path) -> {
                    Set<List<Object>> vistos = new HashSet<>();
                    for (Pagos.TrasladoP t : p.impuestosP().map(Pagos.ImpuestosP::trasladosP).orElse(List.of())) {
                        List<Object> clave = java.util.Arrays.asList(t.impuestoP(), t.tipoFactorP(), t.tasaOCuotaP().map(BigDecimal::stripTrailingZeros).orElse(null));
                        if (!vistos.add(clave)) {
                            ctx.fail("CRP20271", path + "/pago20:ImpuestosP/pago20:TrasladosP", "detalle.duplicado", t.impuestoP() + "/" + t.tipoFactorP());
                        }
                    }
                }),
                trasladoP("CRP20272", (ctx, p, t, path) -> t.tasaOCuotaP()
                        .filter(v -> !enTasaOCuota(p.fechaPago().toLocalDate(), t.impuestoP(), t.tipoFactorP(), v, true))
                        .ifPresent(v -> ctx.fail("CRP20272", path + "/@TasaOCuotaP", "detalle.tasa", v.toPlainString(), t.impuestoP(), t.tipoFactorP()))),
                rule("CRP20273", (ctx, pagos) -> {
                    boolean iva = pagos.pago().stream().flatMap(p -> p.impuestosP().stream()).flatMap(i -> i.trasladosP().stream())
                            .anyMatch(t -> t.impuestoP().equals(Impuesto.IVA) && !t.tipoFactorP().equals(TipoFactor.EXENTO));
                    Pagos.Totales t = pagos.totales();
                    if (iva && Stream.of(t.totalTrasladosImpuestoIVA16(), t.totalTrasladosImpuestoIVA8(), t.totalTrasladosImpuestoIVA0())
                            .allMatch(Optional::isEmpty)) {
                        ctx.fail("CRP20273", PAGOS + "/pago20:Totales", "detalle.requerido", "TotalTrasladosImpuestoIVA16/8/0");
                    }
                }),
                trasladoP("CRP20274", RuleStatus.IMPLEMENTED, Pagos20Rules.NOTA_CONVERSION, (ctx, p, t, path) -> t.importeP().ifPresent(v ->
                        convertido(ctx, "CRP20274", path + "/@ImporteP", v, p, d -> d.trasladosDR().stream().filter(mismoGrupo(t))
                                .flatMap(x -> x.importeDR().stream())))));
    }

    static final String NOTA_CONVERSION = "Se compara con la suma de los valores de los documentos divididos entre su EquivalenciaDR "
            + "(estándar, revisión B). El estándar no fija el redondeo; como en el ejemplo de la Guía de llenado (página 77), el valor "
            + "se acepta redondeado a los decimales de la moneda del pago.";

    static final String NOTA_TOTALES = "Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado "
            + "(página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de "
            + "cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final.";

    @FunctionalInterface
    interface ImpuestoPCheck<T> {
        void check(ValidationContext ctx, Pagos.Pago p, T tax, String path);
    }

    private static Rule retencionP(String code, ImpuestoPCheck<Pagos.RetencionP> check) {
        return retencionP(code, RuleStatus.IMPLEMENTED, "", check);
    }

    private static Rule retencionP(String code, RuleStatus status, String nota, ImpuestoPCheck<Pagos.RetencionP> check) {
        return porPago(code, status, nota, (ctx, p, path) -> {
            List<Pagos.RetencionP> list = p.impuestosP().map(Pagos.ImpuestosP::retencionesP).orElse(List.of());
            for (int k = 0; k < list.size(); k++) {
                check.check(ctx, p, list.get(k), path + "/pago20:ImpuestosP/pago20:RetencionesP/pago20:RetencionP[" + (k + 1) + "]");
            }
        });
    }

    private static Rule trasladoP(String code, ImpuestoPCheck<Pagos.TrasladoP> check) {
        return trasladoP(code, RuleStatus.IMPLEMENTED, "", check);
    }

    private static Rule trasladoP(String code, RuleStatus status, String nota, ImpuestoPCheck<Pagos.TrasladoP> check) {
        return porPago(code, status, nota, (ctx, p, path) -> {
            List<Pagos.TrasladoP> list = p.impuestosP().map(Pagos.ImpuestosP::trasladosP).orElse(List.of());
            for (int k = 0; k < list.size(); k++) {
                check.check(ctx, p, list.get(k), path + "/pago20:ImpuestosP/pago20:TrasladosP/pago20:TrasladoP[" + (k + 1) + "]");
            }
        });
    }

    private static Stream<Pagos.TrasladoDR> trasladosDR(Pagos.Pago p) {
        return p.doctoRelacionado().stream().flatMap(d -> d.impuestosDR().stream()).flatMap(i -> i.trasladosDR().stream());
    }

    private static Predicate<Pagos.TrasladoDR> mismoGrupo(Pagos.TrasladoP t) {
        return x -> x.impuestoDR().equals(t.impuestoP()) && x.tipoFactorDR().equals(t.tipoFactorP())
                && x.tasaOCuotaDR().isPresent() == t.tasaOCuotaP().isPresent()
                && (x.tasaOCuotaDR().isEmpty() || x.tasaOCuotaDR().get().compareTo(t.tasaOCuotaP().get()) == 0);
    }

    /** The registered value must equal Σ (value / EquivalenciaDR) of the documents, to its own last decimal. */
    private static void convertido(ValidationContext ctx, String code, String location, BigDecimal registrado, Pagos.Pago p,
            Function<Pagos.ImpuestosDR, Stream<BigDecimal>> valores) {
        BigDecimal suma = BigDecimal.ZERO;
        for (Pagos.DoctoRelacionado d : p.doctoRelacionado()) {
            if (d.impuestosDR().isEmpty()) {
                continue;
            }
            Optional<BigDecimal> eq = d.equivalenciaDR();
            if (eq.isEmpty()) {
                return; // CRP20237 reports it.
            }
            suma = suma.add(valores.apply(d.impuestosDR().get()).map(v -> Aritmetica.aMonedaPago(v, eq.get())).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        OptionalInt dp = p.monedaP().decimales();
        if (dp.isPresent() && !Aritmetica.igualConPrecision(registrado, suma, dp.getAsInt())) {
            ctx.fail(code, location, "detalle.esperado", Aritmetica.importe(suma, dp.getAsInt()).toPlainString(), registrado.toPlainString());
        }
    }

    // --- shared checks ---

    private static void igual(ValidationContext ctx, String code, String location, String esperado, Object valor) {
        if (!esperado.equals(Objects.toString(valor))) {
            ctx.fail(code, location, "detalle.esperado", esperado, valor);
        }
    }

    private static void cero(ValidationContext ctx, String code, String location, BigDecimal valor) {
        if (valor.signum() != 0) {
            ctx.fail(code, location, "detalle.esperado", "0", valor.toPlainString());
        }
    }

    private static void ausente(ValidationContext ctx, String code, String location, Optional<?> valor) {
        valor.ifPresent(v -> ctx.fail(code, location, "detalle.omitir", location.substring(location.lastIndexOf('/') + 1)));
    }

    private static void positivo(ValidationContext ctx, String code, String location, BigDecimal valor) {
        if (valor.signum() <= 0) {
            ctx.fail(code, location, "detalle.valor", valor.toPlainString());
        }
    }

    private static void decimales(ValidationContext ctx, String code, String location, BigDecimal valor, Moneda moneda) {
        OptionalInt d = moneda.decimales();
        if (d.isPresent() && Importes.decimales(valor) > d.getAsInt()) {
            ctx.fail(code, location, "detalle.decimales", valor.toPlainString(), d.getAsInt());
        }
    }

    private static void catalogo(ValidationContext ctx, String code, String location, CatalogKey key, LocalDate fecha) {
        if (!key.isVigente(fecha)) {
            ctx.fail(code, location, "detalle.valor", key);
        }
    }

    private static boolean enTasaOCuota(LocalDate fecha, Impuesto impuesto, TipoFactor factor, BigDecimal valor, boolean traslado) {
        return TasaOCuota.reglas().stream().anyMatch(r -> r.impuesto().equals(impuesto) && r.tipoFactor().equals(factor)
                && (traslado ? r.traslado() : r.retencion()) && r.vigencia().contains(fecha) && r.admite(valor));
    }

    private static void limitesImpuesto(ValidationContext ctx, String code, String location, BigDecimal base, BigDecimal tasa, BigDecimal importe) {
        Importes.Limites l = Importes.limitesImporteImpuesto(base, tasa, Importes.decimales(importe));
        if (!l.contiene(importe)) {
            ctx.fail(code, location, "detalle.limites", l.inferior().toPlainString(), l.superior().toPlainString(), importe.toPlainString());
        }
    }
}
