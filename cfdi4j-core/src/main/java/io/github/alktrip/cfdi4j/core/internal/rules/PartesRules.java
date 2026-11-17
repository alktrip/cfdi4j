package io.github.alktrip.cfdi4j.core.internal.rules;

import static io.github.alktrip.cfdi4j.core.internal.rules.Fiscal.COMPROBANTE;
import static io.github.alktrip.cfdi4j.core.internal.rules.SimpleRule.rule;

import io.github.alktrip.cfdi4j.core.catalog.Pais;
import io.github.alktrip.cfdi4j.core.catalog.Periodicidad;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.model.CfdiRelacionados;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.Emisor;
import io.github.alktrip.cfdi4j.core.model.InformacionGlobal;
import io.github.alktrip.cfdi4j.core.model.Receptor;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.core.validation.TaxpayerRegistry.Contribuyente;
import io.github.alktrip.cfdi4j.core.validation.ValidationContext;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Rules of InformacionGlobal, CfdiRelacionados, Emisor and Receptor (CFDI40130 to CFDI40161). */
final class PartesRules {

    private static final String GLOBAL = COMPROBANTE + "/cfdi:InformacionGlobal";
    private static final String EMISOR = COMPROBANTE + "/cfdi:Emisor";
    private static final String RECEPTOR = COMPROBANTE + "/cfdi:Receptor";
    private static final String PADRON = "Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado.";

    private PartesRules() {}

    static List<Rule> rules() {
        return List.of(
                rule("CFDI40130", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (c.tipoDeComprobante().equals(TipoDeComprobante.INGRESO)
                            && Fiscal.RFC_GENERICO_NACIONAL.equals(c.receptor().rfc())
                            && Fiscal.mismoNombre(Fiscal.PUBLICO_EN_GENERAL, c.receptor().nombre())
                            && c.informacionGlobal().isEmpty()) {
                        ctx.fail("CFDI40130", GLOBAL, "detalle.requerido", "InformacionGlobal");
                    }
                }),
                global("CFDI40131", (ctx, g) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), g.periodicidad())) {
                        ctx.fail("CFDI40131", GLOBAL + "/@Periodicidad", "detalle.valor", g.periodicidad());
                    }
                }),
                global("CFDI40132", (ctx, g) -> {
                    if (g.periodicidad().equals(Periodicidad.BIMESTRAL)
                            && !ctx.comprobante().emisor().regimenFiscal().equals(RegimenFiscal.INCORPORACION_FISCAL)) {
                        ctx.fail("CFDI40132", EMISOR + "/@RegimenFiscal", "detalle.esperado", "621", ctx.comprobante().emisor().regimenFiscal());
                    }
                }),
                global("CFDI40133", (ctx, g) -> {
                    if (!Fiscal.enCatalogo(ctx.comprobante(), g.meses())) {
                        ctx.fail("CFDI40133", GLOBAL + "/@Meses", "detalle.valor", g.meses());
                    }
                }),
                global("CFDI40134", (ctx, g) -> {
                    if (!g.periodicidad().equals(Periodicidad.BIMESTRAL) && !between(g.meses().clave(), 1, 12)) {
                        ctx.fail("CFDI40134", GLOBAL + "/@Meses", "detalle.valor", g.meses());
                    }
                }),
                global("CFDI40135", (ctx, g) -> {
                    if (g.periodicidad().equals(Periodicidad.BIMESTRAL) && !between(g.meses().clave(), 13, 18)) {
                        ctx.fail("CFDI40135", GLOBAL + "/@Meses", "detalle.valor", g.meses());
                    }
                }),
                rule("CFDI40136", RuleStatus.IMPLEMENTED,
                        "El año en curso se toma de la Fecha del comprobante: se aceptan ese año y hasta 5 ejercicios anteriores.",
                        ctx -> ctx.comprobante().informacionGlobal().ifPresent(g -> {
                            int year = ctx.comprobante().fecha().getYear();
                            if (g.anio() > year || g.anio() < year - 5) {
                                ctx.fail("CFDI40136", GLOBAL + "/@Año", "detalle.valor", Integer.toString(g.anio()));
                            }
                        })),
                rule("CFDI40137", ctx -> {
                    List<CfdiRelacionados> relacionados = ctx.comprobante().cfdiRelacionados();
                    for (int i = 0; i < relacionados.size(); i++) {
                        if (!Fiscal.enCatalogo(ctx.comprobante(), relacionados.get(i).tipoRelacion())) {
                            ctx.fail("CFDI40137", COMPROBANTE + "/cfdi:CfdiRelacionados[" + (i + 1) + "]/@TipoRelacion", "detalle.valor",
                                    relacionados.get(i).tipoRelacion());
                        }
                    }
                }),
                rule("CFDI40138", RuleStatus.EXTERNAL_SERVICE, PADRON, ctx -> padron(ctx, "CFDI40138", ctx.comprobante().emisor().rfc(),
                        EMISOR + "/@Rfc", (c, p) -> { })),
                rule("CFDI40139", RuleStatus.EXTERNAL_SERVICE, PADRON, ctx -> padron(ctx, "CFDI40139", ctx.comprobante().emisor().rfc(),
                        EMISOR + "/@Nombre", (c, p) -> {
                            if (!Fiscal.mismoNombre(p.nombre(), c.comprobante().emisor().nombre())) {
                                c.fail("CFDI40139", EMISOR + "/@Nombre", "detalle.esperado", p.nombre(), c.comprobante().emisor().nombre());
                            }
                        }, true)),
                rule("CFDI40140", ctx -> {
                    Emisor e = ctx.comprobante().emisor();
                    if (!Fiscal.enCatalogo(ctx.comprobante(), e.regimenFiscal())) {
                        ctx.fail("CFDI40140", EMISOR + "/@RegimenFiscal", "detalle.valor", e.regimenFiscal());
                    }
                }),
                rule("CFDI40141", ctx -> {
                    Emisor e = ctx.comprobante().emisor();
                    if (e.regimenFiscal().isKnown() && !aplica(e.rfc(), e.regimenFiscal())) {
                        ctx.fail("CFDI40141", EMISOR + "/@RegimenFiscal", "detalle.tipo.persona", e.regimenFiscal(), e.rfc());
                    }
                }),
                rule("CFDI40142", RuleStatus.EXTERNAL_SERVICE,
                        "Requiere el servicio del Validador de RFC para emitir facturas a través del adquirente; se reporta como no "
                                + "verificado cuando el atributo existe.",
                        ctx -> ctx.comprobante().emisor().facAtrAdquirente().ifPresent(
                                f -> ctx.notVerified("CFDI40142", EMISOR + "/@FacAtrAdquirente", "no.verificado.servicio"))),
                rule("CFDI40143", RuleStatus.EXTERNAL_SERVICE, PADRON, ctx -> receptorPadron(ctx, "CFDI40143", "Rfc", (c, p) -> { }, false)),
                rule("CFDI40144", RuleStatus.EXTERNAL_SERVICE, PADRON, ctx -> receptorPadron(ctx, "CFDI40144", "Nombre", (c, p) -> { }, false)),
                rule("CFDI40145", RuleStatus.EXTERNAL_SERVICE, PADRON, ctx -> receptorPadron(ctx, "CFDI40145", "Nombre", (c, p) -> {
                    if (!Fiscal.mismoNombre(p.nombre(), c.comprobante().receptor().nombre())) {
                        c.fail("CFDI40145", RECEPTOR + "/@Nombre", "detalle.esperado", p.nombre(), c.comprobante().receptor().nombre());
                    }
                }, true)),
                rule("CFDI40146", ctx -> {
                    Receptor r = ctx.comprobante().receptor();
                    if (Fiscal.mismoNombre(Fiscal.PUBLICO_EN_GENERAL, r.nombre()) && !Fiscal.RFC_GENERICO_NACIONAL.equals(r.rfc())) {
                        ctx.fail("CFDI40146", RECEPTOR + "/@Rfc", "detalle.esperado", Fiscal.RFC_GENERICO_NACIONAL, r.rfc());
                    }
                }),
                rule("CFDI40147", RuleStatus.EXTERNAL_SERVICE, PADRON,
                        ctx -> receptorPadron(ctx, "CFDI40147", "DomicilioFiscalReceptor", (c, p) -> { }, false)),
                rule("CFDI40148", RuleStatus.EXTERNAL_SERVICE, PADRON, ctx -> receptorPadron(ctx, "CFDI40148", "DomicilioFiscalReceptor", (c, p) -> {
                    if (!p.codigoPostal().equals(c.comprobante().receptor().domicilioFiscalReceptor())) {
                        c.fail("CFDI40148", RECEPTOR + "/@DomicilioFiscalReceptor", "detalle.esperado", p.codigoPostal(),
                                c.comprobante().receptor().domicilioFiscalReceptor());
                    }
                }, true)),
                rule("CFDI40149", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (Fiscal.generico(c.receptor().rfc()) && !c.receptor().domicilioFiscalReceptor().equals(c.lugarExpedicion())) {
                        ctx.fail("CFDI40149", RECEPTOR + "/@DomicilioFiscalReceptor", "detalle.esperado", c.lugarExpedicion(),
                                c.receptor().domicilioFiscalReceptor());
                    }
                }),
                rule("CFDI40150", ctx -> ctx.comprobante().receptor().residenciaFiscal()
                        .filter(p -> !p.isKnown())
                        .ifPresent(p -> ctx.fail("CFDI40150", RECEPTOR + "/@ResidenciaFiscal", "detalle.valor", p))),
                rule("CFDI40151", RuleStatus.IMPLEMENTED,
                        "Un RFC que no es el genérico extranjero se trata como RFC del SAT o genérico nacional.",
                        ctx -> {
                            Receptor r = ctx.comprobante().receptor();
                            if (!Fiscal.RFC_GENERICO_EXTRANJERO.equals(r.rfc()) && r.residenciaFiscal().isPresent()) {
                                ctx.fail("CFDI40151", RECEPTOR + "/@ResidenciaFiscal", "detalle.omitir", "ResidenciaFiscal");
                            }
                        }),
                rule("CFDI40152", ctx -> ctx.comprobante().receptor().residenciaFiscal()
                        .filter(Pais.MEX::equals)
                        .ifPresent(p -> ctx.fail("CFDI40152", RECEPTOR + "/@ResidenciaFiscal", "detalle.valor", p))),
                rule("CFDI40153", ctx -> {
                    Comprobante c = ctx.comprobante();
                    Receptor r = c.receptor();
                    if (Fiscal.RFC_GENERICO_EXTRANJERO.equals(r.rfc()) && (Fiscal.tieneComercioExterior(c) || r.numRegIdTrib().isPresent())
                            && r.residenciaFiscal().isEmpty()) {
                        ctx.fail("CFDI40153", RECEPTOR + "/@ResidenciaFiscal", "detalle.requerido", "ResidenciaFiscal");
                    }
                }),
                rule("CFDI40154", RuleStatus.IMPLEMENTED,
                        "Un RFC que no es el genérico extranjero se trata como RFC del SAT o genérico nacional.",
                        ctx -> {
                            Receptor r = ctx.comprobante().receptor();
                            if (!Fiscal.RFC_GENERICO_EXTRANJERO.equals(r.rfc()) && r.numRegIdTrib().isPresent()) {
                                ctx.fail("CFDI40154", RECEPTOR + "/@NumRegIdTrib", "detalle.omitir", "NumRegIdTrib");
                            }
                        }),
                rule("CFDI40155", ctx -> {
                    Comprobante c = ctx.comprobante();
                    if (Fiscal.RFC_GENERICO_EXTRANJERO.equals(c.receptor().rfc()) && Fiscal.tieneComercioExterior(c)
                            && c.receptor().numRegIdTrib().isEmpty()) {
                        ctx.fail("CFDI40155", RECEPTOR + "/@NumRegIdTrib", "detalle.requerido", "NumRegIdTrib");
                    }
                }),
                rule("CFDI40156", RuleStatus.PARTIAL,
                        "Se valida el patrón de la columna \"Formato de Registro de Identidad Tributaria\" de c_Pais. La validación "
                                + "contra listas del país (columna \"Validación del Registro de Identidad Tributaria\") requiere un servicio "
                                + "externo.",
                        PartesRules::numRegIdTrib),
                rule("CFDI40157", ctx -> {
                    Receptor r = ctx.comprobante().receptor();
                    if (!Fiscal.enCatalogo(ctx.comprobante(), r.regimenFiscalReceptor())) {
                        ctx.fail("CFDI40157", RECEPTOR + "/@RegimenFiscalReceptor", "detalle.valor", r.regimenFiscalReceptor());
                    }
                }),
                rule("CFDI40158", ctx -> {
                    Receptor r = ctx.comprobante().receptor();
                    if (!Fiscal.generico(r.rfc()) && r.regimenFiscalReceptor().isKnown() && !aplica(r.rfc(), r.regimenFiscalReceptor())) {
                        ctx.fail("CFDI40158", RECEPTOR + "/@RegimenFiscalReceptor", "detalle.tipo.persona", r.regimenFiscalReceptor(), r.rfc());
                    }
                }),
                rule("CFDI40159", ctx -> {
                    Receptor r = ctx.comprobante().receptor();
                    if (Fiscal.generico(r.rfc()) && !r.regimenFiscalReceptor().equals(RegimenFiscal.SIN_OBLIGACIONES_FISCALES)) {
                        ctx.fail("CFDI40159", RECEPTOR + "/@RegimenFiscalReceptor", "detalle.esperado", "616", r.regimenFiscalReceptor());
                    }
                }),
                rule("CFDI40160", ctx -> {
                    Receptor r = ctx.comprobante().receptor();
                    if (!Fiscal.enCatalogo(ctx.comprobante(), r.usoCFDI())) {
                        ctx.fail("CFDI40160", RECEPTOR + "/@UsoCFDI", "detalle.valor", r.usoCFDI());
                    }
                }),
                rule("CFDI40161", ctx -> {
                    Receptor r = ctx.comprobante().receptor();
                    if (!r.usoCFDI().isKnown()) {
                        return;
                    }
                    boolean persona = Fiscal.personaMoral(r.rfc()) ? r.usoCFDI().personaMoral() : r.usoCFDI().personaFisica();
                    if (!persona || !r.usoCFDI().regimenesFiscalesReceptor().contains(r.regimenFiscalReceptor())) {
                        ctx.fail("CFDI40161", RECEPTOR + "/@UsoCFDI", "detalle.uso", r.usoCFDI(), r.regimenFiscalReceptor(), r.rfc());
                    }
                }));
    }

    private static Rule global(String code, BiConsumer<ValidationContext, InformacionGlobal> check) {
        return rule(code, ctx -> ctx.comprobante().informacionGlobal().ifPresent(g -> check.accept(ctx, g)));
    }

    private static boolean between(String clave, int min, int max) {
        try {
            int n = Integer.parseInt(clave);
            return n >= min && n <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** A regime applies to the kind of person of the RFC: 12 characters is a persona moral, 13 a persona física. */
    static boolean aplica(String rfc, RegimenFiscal regimen) {
        return Fiscal.personaMoral(rfc) ? regimen.personaMoral() : Fiscal.personaFisica(rfc) && regimen.personaFisica();
    }

    private static void padron(ValidationContext ctx, String code, String rfc, String location, BiConsumer<ValidationContext, Contribuyente> found) {
        padron(ctx, code, rfc, location, found, false);
    }

    /**
     * Looks the RFC up in the registry. Without a registry: not verified. Not registered: the
     * code fails, unless {@code onlyWhenFound} (the "name matches" rules leave that to the
     * "is registered" rule of the same node).
     */
    private static void padron(ValidationContext ctx, String code, String rfc, String location,
            BiConsumer<ValidationContext, Contribuyente> found, boolean onlyWhenFound) {
        if (ctx.taxpayers().isEmpty()) {
            ctx.notVerified(code, location, "no.verificado.padron");
            return;
        }
        Optional<Contribuyente> contribuyente = ctx.taxpayers().get().find(rfc);
        if (contribuyente.isPresent()) {
            found.accept(ctx, contribuyente.get());
        } else if (!onlyWhenFound) {
            ctx.fail(code, location, "detalle.no.inscrito", rfc);
        }
    }

    private static void receptorPadron(ValidationContext ctx, String code, String attribute,
            BiConsumer<ValidationContext, Contribuyente> found, boolean onlyWhenFound) {
        String rfc = ctx.comprobante().receptor().rfc();
        if (!Fiscal.generico(rfc)) {
            padron(ctx, code, rfc, RECEPTOR + "/@" + attribute, found, onlyWhenFound);
        }
    }

    private static void numRegIdTrib(ValidationContext ctx) {
        Receptor r = ctx.comprobante().receptor();
        if (r.numRegIdTrib().isEmpty() || r.residenciaFiscal().isEmpty()) {
            return;
        }
        Optional<String> formato = r.residenciaFiscal().get().formatoRegistroIdentidadTributaria();
        if (formato.isEmpty()) {
            return;
        }
        boolean valido;
        try {
            valido = Pattern.compile(formato.get()).matcher(r.numRegIdTrib().get()).matches();
        } catch (PatternSyntaxException e) {
            return;
        }
        if (!valido) {
            ctx.fail("CFDI40156", RECEPTOR + "/@NumRegIdTrib", "detalle.patron", r.numRegIdTrib().get(), formato.get());
        }
    }
}
