package io.github.alktrip.cfdi4j.core.internal.rules;

import static io.github.alktrip.cfdi4j.core.internal.rules.Fiscal.at;
import static io.github.alktrip.cfdi4j.core.internal.rules.SimpleRule.documented;
import static io.github.alktrip.cfdi4j.core.internal.rules.SimpleRule.rule;

import io.github.alktrip.cfdi4j.core.catalog.CodigoPostalInfo;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import io.github.alktrip.cfdi4j.core.model.Impuestos;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.core.validation.ValidationContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Rules of the attributes of cfdi:Comprobante (error matrix CFDI40101 to CFDI40129, CFDI40222). */
final class ComprobanteRules {

    private ComprobanteRules() {}

    static List<Rule> rules() {
        return List.of(
                rule("CFDI40101", RuleStatus.PARTIAL,
                        "Se valida el patrón (años 2010 a 2099). No se valida que corresponda con la hora local donde se expide, "
                                + "que el PAC comprueba al timbrar.",
                        ctx -> {
                            int year = ctx.comprobante().fecha().getYear();
                            if (year < 2010 || year > 2099) {
                                ctx.fail("CFDI40101", at("Fecha"), "detalle.valor", ctx.comprobante().fecha());
                            }
                        }),
                documented("CFDI40102", RuleStatus.IMPLEMENTED,
                        "Se valida al verificar el sello (Cfdi4j.verify), sobre el documento tal como se recibió."),
                rule("CFDI40103", ctx -> {
                    Comprobante c = ctx.comprobante();
                    boolean prohibido = c.tipoDeComprobante().equals(TipoDeComprobante.TRASLADO)
                            || c.tipoDeComprobante().equals(TipoDeComprobante.NOMINA)
                            || c.tipoDeComprobante().equals(TipoDeComprobante.PAGO);
                    if (prohibido && c.formaPago().isPresent()) {
                        ctx.fail("CFDI40103", at("FormaPago"), "detalle.valor", c.formaPago().get());
                    }
                }),
                rule("CFDI40104", ctx -> ctx.comprobante().formaPago()
                        .filter(f -> !Fiscal.enCatalogo(ctx.comprobante(), f))
                        .ifPresent(f -> ctx.fail("CFDI40104", at("FormaPago"), "detalle.valor", f))),
                rule("CFDI40105", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (c.metodoPago().filter(MetodoPago.PPD::equals).isPresent()
                            && c.formaPago().filter(f -> !f.equals(FormaPago.POR_DEFINIR)).isPresent()) {
                        ctx.fail("CFDI40105", at("FormaPago"), "detalle.esperado", "99", c.formaPago().get());
                    }
                }),
                // CFDI40106 is checked by CertificadoRules.
                rule("CFDI40107", ctx -> decimales(ctx, "CFDI40107", "SubTotal", ctx.comprobante().subTotal())),
                rule("CFDI40108", ctx -> {
                    Comprobante c = ctx.comprobante();
                    OptionalInt d = Fiscal.decimalesMoneda(c);
                    if (Fiscal.ingresoEgresoNomina(c) && d.isPresent()) {
                        BigDecimal esperado = Importes.redondear(Fiscal.suma(c.conceptos().stream().map(Concepto::importe)), d.getAsInt());
                        if (!Fiscal.iguales(esperado, c.subTotal())) {
                            ctx.fail("CFDI40108", at("SubTotal"), "detalle.esperado", esperado.toPlainString(), c.subTotal().toPlainString());
                        }
                    }
                }),
                rule("CFDI40109", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (Fiscal.trasladoOPago(c) && c.subTotal().signum() != 0) {
                        ctx.fail("CFDI40109", at("SubTotal"), "detalle.esperado", "0", c.subTotal().toPlainString());
                    }
                }),
                rule("CFDI40110", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (c.descuento().filter(d -> d.compareTo(c.subTotal()) > 0).isPresent()) {
                        ctx.fail("CFDI40110", at("Descuento"), "detalle.maximo", c.subTotal().toPlainString(), c.descuento().get().toPlainString());
                    }
                }),
                rule("CFDI40111", ComprobanteRules::descuento),
                rule("CFDI40112", ctx -> ctx.comprobante().descuento().ifPresent(d -> decimales(ctx, "CFDI40112", "Descuento", d))),
                rule("CFDI40113", ctx -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), ctx.comprobante().moneda())) {
                        ctx.fail("CFDI40113", at("Moneda"), "detalle.valor", ctx.comprobante().moneda());
                    }
                }),
                rule("CFDI40114", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (c.moneda().equals(Moneda.MXN) && c.tipoCambio().filter(t -> t.compareTo(BigDecimal.ONE) != 0).isPresent()) {
                        ctx.fail("CFDI40114", at("TipoCambio"), "detalle.esperado", "1", c.tipoCambio().get().toPlainString());
                    }
                }),
                rule("CFDI40115", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (!c.moneda().equals(Moneda.MXN) && !c.moneda().equals(Moneda.XXX) && c.tipoCambio().isEmpty()) {
                        ctx.fail("CFDI40115", at("TipoCambio"), "detalle.requerido", "TipoCambio");
                    }
                }),
                rule("CFDI40116", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (c.moneda().equals(Moneda.XXX) && c.tipoCambio().isPresent()) {
                        ctx.fail("CFDI40116", at("TipoCambio"), "detalle.valor", c.tipoCambio().get().toPlainString());
                    }
                }),
                rule("CFDI40117", ctx -> ctx.comprobante().tipoCambio().ifPresent(t -> {
                    // Pattern [0-9]{1,18}(.[0-9]{1,6})?
                    int enteros = t.precision() - t.scale();
                    if (t.signum() < 0 || t.scale() > 6 || enteros > 18) {
                        ctx.fail("CFDI40117", at("TipoCambio"), "detalle.valor", t.toPlainString());
                    }
                })),
                documented("CFDI40118", RuleStatus.EXTERNAL_SERVICE,
                        "Requiere el tipo de cambio FIX publicado y la clave de confirmación del PAC. La Guía de llenado indica "
                                + "que esta validación aplica cuando el SAT publique el procedimiento."),
                rule("CFDI40119", RuleStatus.PARTIAL,
                        "Se valida contra SubTotal, Descuento y los totales de impuestos federales. Con el complemento de "
                                + "impuestos locales se reporta como no verificado.",
                        ComprobanteRules::total),
                rule("CFDI40120", RuleStatus.PARTIAL,
                        "Se valida contra el valor máximo de c_TipoDeComprobante. Los límites personalizados de l_RFC requieren "
                                + "un servicio del SAT.",
                        ctx -> {
                            Comprobante c = ctx.comprobante();
                            Optional<BigDecimal> maximo = c.tipoDeComprobante().valorMaximo();
                            if (maximo.isPresent() && c.total().compareTo(maximo.get()) > 0 && c.confirmacion().isEmpty()) {
                                ctx.fail("CFDI40120", at("Total"), "detalle.maximo", maximo.get().toPlainString(), c.total().toPlainString());
                            }
                        }),
                rule("CFDI40121", ctx -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), ctx.comprobante().tipoDeComprobante())) {
                        ctx.fail("CFDI40121", at("TipoDeComprobante"), "detalle.valor", ctx.comprobante().tipoDeComprobante());
                    }
                }),
                documented("CFDI40122", RuleStatus.IMPLEMENTED,
                        "El modelo, el lector XML y el XSD exigen el atributo; su valor se valida con CFDI40123."),
                rule("CFDI40123", ctx -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), ctx.comprobante().exportacion())) {
                        ctx.fail("CFDI40123", at("Exportacion"), "detalle.valor", ctx.comprobante().exportacion());
                    }
                }),
                rule("CFDI40124", ctx -> ctx.comprobante().metodoPago()
                        .filter(m -> !Fiscal.enCatalogo(ctx.comprobante(), m))
                        .ifPresent(m -> ctx.fail("CFDI40124", at("MetodoPago"), "detalle.valor", m))),
                rule("CFDI40125", ctx -> {
                    if (Fiscal.trasladoOPago(ctx.comprobante()) && ctx.comprobante().metodoPago().isPresent()) {
                        ctx.fail("CFDI40125", at("MetodoPago"), "detalle.valor", ctx.comprobante().metodoPago().get());
                    }
                }),
                rule("CFDI40126", RuleStatus.IMPLEMENTED,
                        "Con un CatalogProvider. Sin él se reporta como no verificado; la validación XSD también comprueba la clave "
                                + "contra la enumeración de catCFDI.xsd.",
                        ctx -> {
                            String cp = ctx.comprobante().lugarExpedicion();
                            if (ctx.catalogs().isEmpty()) {
                                ctx.notVerified("CFDI40126", at("LugarExpedicion"), "no.verificado.catalogo", "c_CodigoPostal");
                                return;
                            }
                            Optional<CodigoPostalInfo> info = ctx.catalogs().get().codigoPostal(cp);
                            if (info.isEmpty() || !info.get().vigencia().contains(ctx.comprobante().fecha().toLocalDate())) {
                                ctx.fail("CFDI40126", at("LugarExpedicion"), "detalle.valor", cp);
                            }
                        }),
                documented("CFDI40127", RuleStatus.EXTERNAL_SERVICE,
                        "Depende de los rangos de CFDI40118 y CFDI40120, que requieren datos del SAT."),
                documented("CFDI40128", RuleStatus.NOT_APPLICABLE, "Validación que realiza el PAC."),
                documented("CFDI40129", RuleStatus.NOT_APPLICABLE, "Validación que realiza el PAC."),
                rule("CFDI40222", ctx -> {
                    Comprobante c = ctx.comprobante();
                    String clave = c.exportacion().clave();
                    if (("01".equals(clave) || "03".equals(clave)) && Fiscal.tieneComercioExterior(c)) {
                        ctx.fail("CFDI40222", at("Exportacion"), "detalle.valor", clave);
                    }
                }));
    }

    private static void decimales(ValidationContext ctx, String code, String attribute, BigDecimal value) {
        if (Fiscal.excedeDecimales(ctx.comprobante(), value)) {
            ctx.fail(code, at(attribute), "detalle.decimales", value.toPlainString(), Fiscal.decimalesMoneda(ctx.comprobante()).getAsInt());
        }
    }

    private static void descuento(ValidationContext ctx) {
        Comprobante c = ctx.comprobante();
        List<BigDecimal> descuentos = c.conceptos().stream().flatMap(x -> x.descuento().stream()).toList();
        if (Fiscal.ingresoEgresoNomina(c) && !descuentos.isEmpty()) {
            OptionalInt d = Fiscal.decimalesMoneda(c);
            if (c.descuento().isEmpty()) {
                ctx.fail("CFDI40111", at("Descuento"), "detalle.requerido", "Descuento");
            } else if (d.isPresent()) {
                BigDecimal esperado = Importes.redondear(Fiscal.suma(descuentos.stream()), d.getAsInt());
                if (!Fiscal.iguales(esperado, c.descuento().get())) {
                    ctx.fail("CFDI40111", at("Descuento"), "detalle.esperado", esperado.toPlainString(), c.descuento().get().toPlainString());
                }
            }
        } else if (c.descuento().isPresent()) {
            ctx.fail("CFDI40111", at("Descuento"), "detalle.omitir", "Descuento");
        }
    }

    private static void total(ValidationContext ctx) {
        Comprobante c = ctx.comprobante();
        OptionalInt d = Fiscal.decimalesMoneda(c);
        if (d.isEmpty()) {
            return;
        }
        if (c.complemento().stream().anyMatch(x -> Fiscal.IMPUESTOS_LOCALES.equals(x.namespace()))) {
            ctx.notVerified("CFDI40119", at("Total"), "no.verificado.impuestos.locales");
            return;
        }
        BigDecimal trasladados = c.impuestos().flatMap(Impuestos::totalImpuestosTrasladados).orElse(BigDecimal.ZERO);
        BigDecimal retenidos = c.impuestos().flatMap(Impuestos::totalImpuestosRetenidos).orElse(BigDecimal.ZERO);
        BigDecimal esperado = Importes.redondear(
                c.subTotal().subtract(c.descuento().orElse(BigDecimal.ZERO)).add(trasladados).subtract(retenidos), d.getAsInt());
        if (!Fiscal.iguales(esperado, c.total())) {
            ctx.fail("CFDI40119", at("Total"), "detalle.esperado", esperado.toPlainString(), c.total().toPlainString());
        }
    }
}
