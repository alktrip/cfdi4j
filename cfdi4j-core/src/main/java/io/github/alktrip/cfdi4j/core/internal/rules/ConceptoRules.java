package io.github.alktrip.cfdi4j.core.internal.rules;

import static io.github.alktrip.cfdi4j.core.internal.rules.Fiscal.concepto;
import static io.github.alktrip.cfdi4j.core.internal.rules.SimpleRule.rule;

import io.github.alktrip.cfdi4j.core.catalog.ClaveProdServInfo;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.Requisito;
import io.github.alktrip.cfdi4j.core.catalog.TasaOCuota;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.core.validation.TaxpayerRegistry.Contribuyente;
import io.github.alktrip.cfdi4j.core.validation.ValidationContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rules of cfdi:Concepto and its children (CFDI40162 to CFDI40200, CFDI140223 to CFDI140228).
 */
final class ConceptoRules {

    /** NumeroPedimento pattern of cfdv40.xsd: year, aduana, patente and number separated by two spaces. */
    private static final Pattern PEDIMENTO = Pattern.compile("[0-9]{2}  [0-9]{2}  [0-9]{4}  [0-9]{7}");
    private static final Set<String> HIDROCARBUROS_CLAVES = Set.of("15101505", "15101514", "15101515");
    private static final String CATALOGO = "Se valida con un CatalogProvider; sin él se reporta como no verificado (una vez por código).";

    private ConceptoRules() {}

    /** A check of one concepto. */
    @FunctionalInterface
    interface ConceptoCheck {
        void check(ValidationContext ctx, Concepto c, String path);
    }

    static Rule porConcepto(String code, RuleStatus status, String nota, ConceptoCheck check) {
        return rule(code, status, nota, ctx -> {
            List<Concepto> conceptos = ctx.comprobante().conceptos();
            for (int i = 0; i < conceptos.size(); i++) {
                check.check(ctx, conceptos.get(i), concepto(i));
            }
        });
    }

    static Rule porConcepto(String code, ConceptoCheck check) {
        return porConcepto(code, RuleStatus.IMPLEMENTED, "", check);
    }

    static List<Rule> rules() {
        return List.of(
                porConcepto("CFDI40162", RuleStatus.IMPLEMENTED, CATALOGO + " La validación XSD también comprueba la clave.",
                        (ctx, c, p) -> claveProdServ(ctx, "CFDI40162", c.claveProdServ(), p + "/@ClaveProdServ")),
                porConcepto("CFDI40163", RuleStatus.PARTIAL,
                        "Con un CatalogProvider se identifica la clave que exige un complemento; como el catálogo nombra el "
                                + "complemento en texto, se reporta como no verificado.",
                        (ctx, c, p) -> conClave(ctx, "CFDI40163", c.claveProdServ(), p).flatMap(ClaveProdServInfo::complementoQueDebeIncluir)
                                .ifPresent(nombre -> ctx.notVerified("CFDI40163", p + "/@ClaveProdServ", "no.verificado.complemento", nombre))),
                porConcepto("CFDI40164", RuleStatus.IMPLEMENTED,
                        CATALOGO + " Cuando el catálogo indica \"Sí\" en Incluir IVA trasladado o Incluir IEPS trasladado, el concepto "
                                + "debe trasladar ese impuesto.",
                        (ctx, c, p) -> conClave(ctx, "CFDI40164", c.claveProdServ(), p).ifPresent(info -> {
                            if (info.incluirIvaTrasladado() == Requisito.SI && !traslada(c, Impuesto.IVA)) {
                                ctx.fail("CFDI40164", p, "detalle.impuesto.requerido", "002");
                            }
                            if (info.incluirIepsTrasladado() == Requisito.SI && !traslada(c, Impuesto.IEPS)) {
                                ctx.fail("CFDI40164", p, "detalle.impuesto.requerido", "003");
                            }
                        })),
                porConcepto("CFDI40165", RuleStatus.IMPLEMENTED, CATALOGO + " La validación XSD también comprueba la clave.",
                        (ctx, c, p) -> {
                            if (ctx.catalogs().isEmpty()) {
                                ctx.notVerifiedOnce("CFDI40165", p + "/@ClaveUnidad", "no.verificado.catalogo", "c_ClaveUnidad");
                            } else if (ctx.catalogs().get().claveUnidad(c.claveUnidad())
                                    .filter(u -> u.vigencia().contains(fecha(ctx))).isEmpty()) {
                                ctx.fail("CFDI40165", p + "/@ClaveUnidad", "detalle.valor", c.claveUnidad());
                            }
                        }),
                porConcepto("CFDI40166", (ctx, c, p) -> {
                    if (Fiscal.ingresoEgresoNomina(ctx.comprobante()) && c.valorUnitario().signum() <= 0) {
                        ctx.fail("CFDI40166", p + "/@ValorUnitario", "detalle.valor", c.valorUnitario().toPlainString());
                    }
                }),
                porConcepto("CFDI40167", (ctx, c, p) -> limites(ctx, "CFDI40167", p + "/@Importe", c.importe(),
                        Importes.limitesImporteConcepto(c.cantidad(), c.valorUnitario(), Importes.decimales(c.importe())))),
                porConcepto("CFDI40168", (ctx, c, p) -> c.descuento()
                        .filter(d -> Importes.decimales(d) > Importes.decimales(c.importe()))
                        .ifPresent(d -> ctx.fail("CFDI40168", p + "/@Descuento", "detalle.decimales", d.toPlainString(), Importes.decimales(c.importe())))),
                porConcepto("CFDI40169", (ctx, c, p) -> c.descuento()
                        .filter(d -> d.compareTo(c.importe()) > 0)
                        .ifPresent(d -> ctx.fail("CFDI40169", p + "/@Descuento", "detalle.maximo", c.importe().toPlainString(), d.toPlainString()))),
                porConcepto("CFDI40170", (ctx, c, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), c.objetoImp())) {
                        ctx.fail("CFDI40170", p + "/@ObjetoImp", "detalle.valor", c.objetoImp());
                    }
                }),
                porConcepto("CFDI40171", (ctx, c, p) -> {
                    if (c.objetoImp().equals(ObjetoImp.SI_OBJETO) && c.impuestos().isEmpty()) {
                        ctx.fail("CFDI40171", p + "/cfdi:Impuestos", "detalle.requerido", "Impuestos");
                    }
                }),
                porConcepto("CFDI40172", (ctx, c, p) -> {
                    Set<ObjetoImp> sinImpuestos = Set.of(ObjetoImp.NO_OBJETO, ObjetoImp.SI_OBJETO_NO_DESGLOSE, ObjetoImp.SI_OBJETO_NO_CAUSA,
                            ObjetoImp.SI_OBJETO_IVA_PODEBI);
                    if (sinImpuestos.contains(c.objetoImp()) && c.impuestos().isPresent()) {
                        ctx.fail("CFDI40172", p + "/cfdi:Impuestos", "detalle.omitir", "Impuestos");
                    }
                }),
                porConcepto("CFDI40173", (ctx, c, p) -> c.impuestos()
                        .filter(i -> i.traslados().isEmpty() && i.retenciones().isEmpty())
                        .ifPresent(i -> ctx.fail("CFDI40173", p + "/cfdi:Impuestos", "detalle.requerido", "Traslados/Retenciones"))),
                traslado("CFDI40174", (ctx, t, p) -> {
                    if (t.base().signum() <= 0) {
                        ctx.fail("CFDI40174", p + "/@Base", "detalle.valor", t.base().toPlainString());
                    }
                }),
                traslado("CFDI40175", (ctx, t, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), t.impuesto())) {
                        ctx.fail("CFDI40175", p + "/@Impuesto", "detalle.valor", t.impuesto());
                    }
                }),
                traslado("CFDI40176", (ctx, t, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), t.tipoFactor())) {
                        ctx.fail("CFDI40176", p + "/@TipoFactor", "detalle.valor", t.tipoFactor());
                    }
                }),
                traslado("CFDI40177", (ctx, t, p) -> {
                    if (t.tipoFactor().equals(TipoFactor.EXENTO) && (t.tasaOCuota().isPresent() || t.importe().isPresent())) {
                        ctx.fail("CFDI40177", p, "detalle.omitir", "TasaOCuota/Importe");
                    }
                }),
                traslado("CFDI40178", (ctx, t, p) -> {
                    boolean tasaOCuota = t.tipoFactor().equals(TipoFactor.TASA) || t.tipoFactor().equals(TipoFactor.CUOTA);
                    if (tasaOCuota && (t.tasaOCuota().isEmpty() || t.importe().isEmpty())) {
                        ctx.fail("CFDI40178", p, "detalle.requerido", "TasaOCuota/Importe");
                    }
                }),
                traslado("CFDI40179", (ctx, t, p) -> t.tasaOCuota()
                        .filter(v -> !t.tipoFactor().equals(TipoFactor.EXENTO))
                        .filter(v -> !enTasaOCuota(fecha(ctx), t.impuesto(), t.tipoFactor(), v, true))
                        .ifPresent(v -> ctx.fail("CFDI40179", p + "/@TasaOCuota", "detalle.tasa", v.toPlainString(), t.impuesto(), t.tipoFactor()))),
                traslado("CFDI40180", (ctx, t, p) -> {
                    if (t.tasaOCuota().isPresent() && t.importe().isPresent()) {
                        limites(ctx, "CFDI40180", p + "/@Importe", t.importe().get(),
                                Importes.limitesImporteImpuesto(t.base(), t.tasaOCuota().get(), Importes.decimales(t.importe().get())));
                    }
                }),
                retencion("CFDI40181", (ctx, r, p) -> {
                    if (r.base().signum() <= 0) {
                        ctx.fail("CFDI40181", p + "/@Base", "detalle.valor", r.base().toPlainString());
                    }
                }),
                retencion("CFDI40182", (ctx, r, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), r.impuesto())) {
                        ctx.fail("CFDI40182", p + "/@Impuesto", "detalle.valor", r.impuesto());
                    }
                }),
                retencion("CFDI40183", (ctx, r, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), r.tipoFactor())) {
                        ctx.fail("CFDI40183", p + "/@TipoFactor", "detalle.valor", r.tipoFactor());
                    }
                }),
                retencion("CFDI40184", (ctx, r, p) -> {
                    if (r.tipoFactor().equals(TipoFactor.EXENTO)) {
                        ctx.fail("CFDI40184", p + "/@TipoFactor", "detalle.valor", r.tipoFactor());
                    }
                }),
                retencion("CFDI40185", (ctx, r, p) -> {
                    if (!r.tipoFactor().equals(TipoFactor.EXENTO) && !enTasaOCuota(fecha(ctx), r.impuesto(), r.tipoFactor(), r.tasaOCuota(), false)) {
                        ctx.fail("CFDI40185", p + "/@TasaOCuota", "detalle.tasa", r.tasaOCuota().toPlainString(), r.impuesto(), r.tipoFactor());
                    }
                }),
                retencion("CFDI40186", (ctx, r, p) -> limites(ctx, "CFDI40186", p + "/@Importe", r.importe(),
                        Importes.limitesImporteImpuesto(r.base(), r.tasaOCuota(), Importes.decimales(r.importe())))),
                terceros("CFDI40187", RuleStatus.EXTERNAL_SERVICE,
                        "Si el RFC no es EXT990101NI1 se valida con un TaxpayerRegistry; sin él se reporta como no verificado.",
                        (ctx, t, p) -> {
                            if (!Fiscal.RFC_TERCERO_EXTRANJERO.equals(t.rfcACuentaTerceros())) {
                                tercero(ctx, "CFDI40187", t, p + "/@RfcACuentaTerceros");
                            }
                        }),
                terceros("CFDI40188", RuleStatus.IMPLEMENTED, "", (ctx, t, p) -> {
                    Comprobante c = ctx.comprobante();
                    if (t.rfcACuentaTerceros().equals(c.emisor().rfc()) || t.rfcACuentaTerceros().equals(c.receptor().rfc())) {
                        ctx.fail("CFDI40188", p + "/@RfcACuentaTerceros", "detalle.valor", t.rfcACuentaTerceros());
                    }
                }),
                terceros("CFDI40189", RuleStatus.PARTIAL,
                        "Con EXT990101NI1 se valida la descripción obligatoria; en otro caso se requiere un TaxpayerRegistry.",
                        (ctx, t, p) -> {
                            if (Fiscal.RFC_TERCERO_EXTRANJERO.equals(t.rfcACuentaTerceros())) {
                                if (!Fiscal.mismoNombre(Fiscal.NOMBRE_TERCERO_EXTRANJERO, t.nombreACuentaTerceros())) {
                                    ctx.fail("CFDI40189", p + "/@NombreACuentaTerceros", "detalle.esperado", Fiscal.NOMBRE_TERCERO_EXTRANJERO,
                                            t.nombreACuentaTerceros());
                                }
                            } else {
                                tercero(ctx, "CFDI40189", t, p + "/@NombreACuentaTerceros");
                            }
                        }),
                terceros("CFDI40190", RuleStatus.EXTERNAL_SERVICE, "Se valida con un TaxpayerRegistry; sin él se reporta como no verificado.",
                        (ctx, t, p) -> {
                            if (!Fiscal.RFC_TERCERO_EXTRANJERO.equals(t.rfcACuentaTerceros())) {
                                terceroEncontrado(ctx, "CFDI40190", t, p + "/@NombreACuentaTerceros").ifPresent(x -> {
                                    if (!Fiscal.mismoNombre(x.nombre(), t.nombreACuentaTerceros())) {
                                        ctx.fail("CFDI40190", p + "/@NombreACuentaTerceros", "detalle.esperado", x.nombre(), t.nombreACuentaTerceros());
                                    }
                                });
                            }
                        }),
                terceros("CFDI40191", RuleStatus.IMPLEMENTED, "", (ctx, t, p) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), t.regimenFiscalACuentaTerceros())) {
                        ctx.fail("CFDI40191", p + "/@RegimenFiscalACuentaTerceros", "detalle.valor", t.regimenFiscalACuentaTerceros());
                    }
                }),
                terceros("CFDI40192", RuleStatus.EXTERNAL_SERVICE, "Se valida con un TaxpayerRegistry; sin él se reporta como no verificado.",
                        (ctx, t, p) -> {
                            if (!Fiscal.RFC_TERCERO_EXTRANJERO.equals(t.rfcACuentaTerceros())) {
                                tercero(ctx, "CFDI40192", t, p + "/@DomicilioFiscalACuentaTerceros");
                            }
                        }),
                terceros("CFDI40193", RuleStatus.PARTIAL,
                        "Con EXT990101NI1 se compara con LugarExpedicion; en otro caso se requiere un TaxpayerRegistry.",
                        (ctx, t, p) -> {
                            String location = p + "/@DomicilioFiscalACuentaTerceros";
                            if (Fiscal.RFC_TERCERO_EXTRANJERO.equals(t.rfcACuentaTerceros())) {
                                if (!t.domicilioFiscalACuentaTerceros().equals(ctx.comprobante().lugarExpedicion())) {
                                    ctx.fail("CFDI40193", location, "detalle.esperado", ctx.comprobante().lugarExpedicion(), t.domicilioFiscalACuentaTerceros());
                                }
                            } else {
                                terceroEncontrado(ctx, "CFDI40193", t, location).ifPresent(x -> {
                                    if (!x.codigoPostal().equals(t.domicilioFiscalACuentaTerceros())) {
                                        ctx.fail("CFDI40193", location, "detalle.esperado", x.codigoPostal(), t.domicilioFiscalACuentaTerceros());
                                    }
                                });
                            }
                        }),
                terceros("CFDI140223", RuleStatus.IMPLEMENTED, "", (ctx, t, p) -> {
                    if (Fiscal.RFC_TERCERO_EXTRANJERO.equals(t.rfcACuentaTerceros())
                            && !t.regimenFiscalACuentaTerceros().equals(RegimenFiscal.SIN_OBLIGACIONES_FISCALES)) {
                        ctx.fail("CFDI140223", p + "/@RegimenFiscalACuentaTerceros", "detalle.esperado", "616", t.regimenFiscalACuentaTerceros());
                    }
                }),
                porConcepto("CFDI40194", RuleStatus.PARTIAL,
                        "Se valida la estructura del número de pedimento. La validación contra c_Aduana, c_PatenteAduanal y "
                                + "c_NumPedimentoAduana no está implementada.",
                        (ctx, c, p) -> pedimentos(ctx, "CFDI40194", c.informacionAduanera(), p)),
                porConcepto("CFDI40195", (ctx, c, p) -> {
                    if (Fiscal.tieneComercioExterior(ctx.comprobante()) && !c.informacionAduanera().isEmpty()) {
                        ctx.fail("CFDI40195", p + "/cfdi:InformacionAduanera", "detalle.omitir", "InformacionAduanera");
                    }
                }),
                parte("CFDI40196", RuleStatus.IMPLEMENTED, CATALOGO,
                        (ctx, parte, p) -> claveProdServ(ctx, "CFDI40196", parte.claveProdServ(), p + "/@ClaveProdServ")),
                parte("CFDI40197", RuleStatus.IMPLEMENTED, "", (ctx, parte, p) -> parte.valorUnitario()
                        .filter(v -> v.signum() <= 0)
                        .ifPresent(v -> ctx.fail("CFDI40197", p + "/@ValorUnitario", "detalle.valor", v.toPlainString()))),
                parte("CFDI40198", RuleStatus.IMPLEMENTED, "Se valida cuando la Parte registra ValorUnitario e Importe.", (ctx, parte, p) -> {
                    if (parte.importe().isPresent() && parte.valorUnitario().isPresent()) {
                        limites(ctx, "CFDI40198", p + "/@Importe", parte.importe().get(),
                                Importes.limitesImporteConcepto(parte.cantidad(), parte.valorUnitario().get(), Importes.decimales(parte.importe().get())));
                    }
                }),
                parte("CFDI40199", RuleStatus.PARTIAL, "Igual que CFDI40194.",
                        (ctx, parte, p) -> pedimentos(ctx, "CFDI40199", parte.informacionAduanera(), p)),
                parte("CFDI40200", RuleStatus.IMPLEMENTED, "", (ctx, parte, p) -> {
                    if (Fiscal.tieneComercioExterior(ctx.comprobante()) && !parte.informacionAduanera().isEmpty()) {
                        ctx.fail("CFDI40200", p + "/cfdi:InformacionAduanera", "detalle.omitir", "InformacionAduanera");
                    }
                }),
                retencionesResico("CFDI140224"),
                retencionesResico("CFDI140225"),
                porConcepto("CFDI140226", (ctx, c, p) -> {
                    if (c.objetoImp().equals(ObjetoImp.SI_OBJETO_IVA_NO_TRASLADO) || c.objetoImp().equals(ObjetoImp.NO_TRASLADO_IVA_NO_DESGLOSE_IEPS)) {
                        for (Impuesto impuesto : List.of(Impuesto.IVA, Impuesto.IEPS)) {
                            if (traslada(c, impuesto) || retiene(c, impuesto)) {
                                ctx.fail("CFDI140226", p + "/cfdi:Impuestos", "detalle.impuesto.prohibido", impuesto);
                            }
                        }
                    }
                }),
                porConcepto("CFDI140227", (ctx, c, p) -> {
                    if (c.objetoImp().equals(ObjetoImp.NO_TRASLADO_IVA_SI_DESGLOSE_IEPS)) {
                        if (traslada(c, Impuesto.IVA) || retiene(c, Impuesto.IVA)) {
                            ctx.fail("CFDI140227", p + "/cfdi:Impuestos", "detalle.impuesto.prohibido", Impuesto.IVA);
                        }
                        if (!traslada(c, Impuesto.IEPS)) {
                            ctx.fail("CFDI140227", p + "/cfdi:Impuestos", "detalle.impuesto.requerido", Impuesto.IEPS);
                        }
                    }
                }),
                porConcepto("CFDI140228", (ctx, c, p) -> {
                    TipoDeComprobante tipo = ctx.comprobante().tipoDeComprobante();
                    boolean ingresoEgreso = tipo.equals(TipoDeComprobante.INGRESO) || tipo.equals(TipoDeComprobante.EGRESO);
                    if (ingresoEgreso && HIDROCARBUROS_CLAVES.contains(c.claveProdServ())
                            && c.complementoConcepto().stream().noneMatch(x -> Fiscal.HIDROCARBUROS.equals(x.namespace()))) {
                        ctx.fail("CFDI140228", p + "/cfdi:ComplementoConcepto", "detalle.requerido", "hidrocarburospetroliferos");
                    }
                }));
    }

    // ---------------------------------------------------------------------------------------------

    @FunctionalInterface
    interface TrasladoCheck {
        void check(ValidationContext ctx, Concepto.Traslado t, String path);
    }

    @FunctionalInterface
    interface RetencionCheck {
        void check(ValidationContext ctx, Concepto.Retencion r, String path);
    }

    @FunctionalInterface
    interface TercerosCheck {
        void check(ValidationContext ctx, Concepto.ACuentaTerceros t, String path);
    }

    @FunctionalInterface
    interface ParteCheck {
        void check(ValidationContext ctx, Concepto.Parte parte, String path);
    }

    private static Rule traslado(String code, TrasladoCheck check) {
        return porConcepto(code, (ctx, c, p) -> {
            List<Concepto.Traslado> traslados = Fiscal.traslados(c);
            for (int j = 0; j < traslados.size(); j++) {
                check.check(ctx, traslados.get(j), p + "/cfdi:Impuestos/cfdi:Traslados/cfdi:Traslado[" + (j + 1) + "]");
            }
        });
    }

    private static Rule retencion(String code, RetencionCheck check) {
        return porConcepto(code, (ctx, c, p) -> {
            List<Concepto.Retencion> retenciones = Fiscal.retenciones(c);
            for (int j = 0; j < retenciones.size(); j++) {
                check.check(ctx, retenciones.get(j), p + "/cfdi:Impuestos/cfdi:Retenciones/cfdi:Retencion[" + (j + 1) + "]");
            }
        });
    }

    private static Rule terceros(String code, RuleStatus status, String nota, TercerosCheck check) {
        return porConcepto(code, status, nota, (ctx, c, p) -> c.aCuentaTerceros().ifPresent(t -> check.check(ctx, t, p + "/cfdi:ACuentaTerceros")));
    }

    private static Rule parte(String code, RuleStatus status, String nota, ParteCheck check) {
        return porConcepto(code, status, nota, (ctx, c, p) -> {
            for (int j = 0; j < c.parte().size(); j++) {
                check.check(ctx, c.parte().get(j), p + "/cfdi:Parte[" + (j + 1) + "]");
            }
        });
    }

    /**
     * CFDI140224 (mark 1: no ISR withholding) and CFDI140225 (mark 2: ISR withholding at 0.0125)
     * apply when the emisor is a persona física in régimen 626 and the receptor a persona moral.
     * The mark comes from the taxpayer list.
     */
    private static Rule retencionesResico(String code) {
        return rule(code, RuleStatus.EXTERNAL_SERVICE,
                "Requiere la marca \"Retención\" de la lista de RFC inscritos (TaxpayerRegistry); sin ella se reporta como no verificado "
                        + "cuando el emisor es persona física en el régimen 626 y el receptor es persona moral.",
                ctx -> {
                    Comprobante cfdi = ctx.comprobante();
                    if (!cfdi.emisor().regimenFiscal().equals(RegimenFiscal.RESICO) || !Fiscal.personaFisica(cfdi.emisor().rfc())
                            || !Fiscal.personaMoral(cfdi.receptor().rfc())) {
                        return;
                    }
                    if (ctx.taxpayers().isEmpty()) {
                        ctx.notVerified(code, Fiscal.COMPROBANTE + "/cfdi:Emisor/@Rfc", "no.verificado.padron");
                        return;
                    }
                    OptionalInt marca = ctx.taxpayers().get().find(cfdi.emisor().rfc()).map(Contribuyente::marcaRetencion).orElse(OptionalInt.empty());
                    BigDecimal tasa = new BigDecimal("0.0125");
                    for (int i = 0; i < cfdi.conceptos().size(); i++) {
                        Concepto c = cfdi.conceptos().get(i);
                        if ("CFDI140224".equals(code) && marca.orElse(0) == 1 && retiene(c, Impuesto.ISR)) {
                            ctx.fail(code, concepto(i) + "/cfdi:Impuestos/cfdi:Retenciones", "detalle.impuesto.prohibido", Impuesto.ISR);
                        }
                        if ("CFDI140225".equals(code) && marca.orElse(0) == 2 && Fiscal.retenciones(c).stream()
                                .noneMatch(r -> r.impuesto().equals(Impuesto.ISR) && r.tasaOCuota().compareTo(tasa) == 0)) {
                            ctx.fail(code, concepto(i) + "/cfdi:Impuestos/cfdi:Retenciones", "detalle.impuesto.requerido", "001 (0.0125)");
                        }
                    }
                });
    }

    private static boolean traslada(Concepto c, Impuesto impuesto) {
        return Fiscal.traslados(c).stream().anyMatch(t -> t.impuesto().equals(impuesto));
    }

    private static boolean retiene(Concepto c, Impuesto impuesto) {
        return Fiscal.retenciones(c).stream().anyMatch(r -> r.impuesto().equals(impuesto));
    }

    static LocalDate fecha(ValidationContext ctx) {
        return ctx.comprobante().fecha().toLocalDate();
    }

    /** Whether a value is allowed by c_TasaOCuota for the tax, factor and use, on the date. */
    static boolean enTasaOCuota(LocalDate fecha, Impuesto impuesto, TipoFactor factor, BigDecimal valor, boolean traslado) {
        return TasaOCuota.reglas().stream().anyMatch(r -> r.impuesto().equals(impuesto) && r.tipoFactor().equals(factor)
                && (traslado ? r.traslado() : r.retencion()) && r.vigencia().contains(fecha) && r.admite(valor));
    }

    private static void limites(ValidationContext ctx, String code, String location, BigDecimal valor, Importes.Limites limites) {
        if (!limites.contiene(valor)) {
            ctx.fail(code, location, "detalle.limites", limites.inferior().toPlainString(), limites.superior().toPlainString(), valor.toPlainString());
        }
    }

    private static void claveProdServ(ValidationContext ctx, String code, String clave, String location) {
        if (ctx.catalogs().isEmpty()) {
            ctx.notVerifiedOnce(code, location, "no.verificado.catalogo", "c_ClaveProdServ");
        } else if (ctx.catalogs().get().claveProdServ(clave).filter(i -> i.vigencia().contains(fecha(ctx))).isEmpty()) {
            ctx.fail(code, location, "detalle.valor", clave);
        }
    }

    /** The catalog entry of the concepto's key, or not verified without a provider. */
    private static Optional<ClaveProdServInfo> conClave(ValidationContext ctx, String code, String clave, String path) {
        if (ctx.catalogs().isEmpty()) {
            ctx.notVerifiedOnce(code, path + "/@ClaveProdServ", "no.verificado.catalogo", "c_ClaveProdServ");
            return Optional.empty();
        }
        return ctx.catalogs().get().claveProdServ(clave);
    }

    /** Looks the third party up: not verified without registry; fails if not registered. */
    private static Optional<Contribuyente> tercero(ValidationContext ctx, String code, Concepto.ACuentaTerceros t, String location) {
        if (ctx.taxpayers().isEmpty()) {
            ctx.notVerifiedOnce(code, location, "no.verificado.padron");
            return Optional.empty();
        }
        Optional<Contribuyente> found = ctx.taxpayers().get().find(t.rfcACuentaTerceros());
        if (found.isEmpty()) {
            ctx.fail(code, location, "detalle.no.inscrito", t.rfcACuentaTerceros());
        }
        return found;
    }

    /** Like {@link #tercero} but leaves "not registered" to the rule that checks registration. */
    private static Optional<Contribuyente> terceroEncontrado(ValidationContext ctx, String code, Concepto.ACuentaTerceros t, String location) {
        if (ctx.taxpayers().isEmpty()) {
            ctx.notVerifiedOnce(code, location, "no.verificado.padron");
            return Optional.empty();
        }
        return ctx.taxpayers().get().find(t.rfcACuentaTerceros());
    }

    private static void pedimentos(ValidationContext ctx, String code, List<Concepto.InformacionAduanera> pedimentos, String path) {
        for (int j = 0; j < pedimentos.size(); j++) {
            String numero = pedimentos.get(j).numeroPedimento();
            if (!PEDIMENTO.matcher(numero).matches()) {
                ctx.fail(code, path + "/cfdi:InformacionAduanera[" + (j + 1) + "]/@NumeroPedimento", "detalle.valor", numero);
            }
        }
    }
}
