package io.github.alktrip.cfdi4j.core.internal.rules;

import static io.github.alktrip.cfdi4j.core.internal.rules.Fiscal.at;
import static io.github.alktrip.cfdi4j.core.internal.rules.SimpleRule.rule;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.CertificateType;
import io.github.alktrip.cfdi4j.core.catalog.CodigoPostalInfo;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.validation.CertificateStatusProvider.Revocation;
import io.github.alktrip.cfdi4j.core.validation.Rule;
import io.github.alktrip.cfdi4j.core.validation.RuleStatus;
import io.github.alktrip.cfdi4j.core.validation.ValidationContext;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Certificate rules (ADR 0004), checked when the certificate is known: CFDI40106 for the
 * conditions the Anexo 20 lists under {@code Certificado} that can be checked locally, and
 * CFDI4J-103 for {@code NoCertificado}.
 */
final class CertificadoRules {

    /**
     * Offsets that local times in Mexico can have (c_CodigoPostal, Diferencia_Horaria_Verano and
     * Diferencia_Horaria_Invierno range from -8 to -5 hours).
     */
    private static final List<ZoneOffset> MEXICO = List.of(ZoneOffset.ofHours(-8), ZoneOffset.ofHours(-7), ZoneOffset.ofHours(-6), ZoneOffset.ofHours(-5));

    private CertificadoRules() {}

    static List<Rule> rules() {
        return List.of(
                rule("CFDI40106", RuleStatus.PARTIAL,
                        "Con el certificado que sella: el RFC del certificado es el del emisor, la Fecha cae en su vigencia, es un CSD "
                                + "y no una e.firma, y no está revocado (con un CertificateStatusProvider). No se valida que lo haya emitido "
                                + "el SAT ni las excepciones de los proveedores del sector primario o de expedición a través del adquirente. "
                                + "Al verificar (Cfdi4j.verify) se valida además que el atributo contenga un certificado legible.",
                        ctx -> ctx.certificate().ifPresent(info -> check(ctx, info))),
                rule("CFDI4J-103", RuleStatus.IMPLEMENTED, "NoCertificado debe ser el número del certificado que sella.",
                        ctx -> ctx.certificate().ifPresent(info -> ctx.comprobante().noCertificado()
                                .filter(n -> !n.equals(info.noCertificado()))
                                .ifPresent(n -> ctx.fail("CFDI4J-103", at("NoCertificado"), null, n, info.noCertificado())))));
    }

    private static void check(ValidationContext ctx, CertificateInfo info) {
        Comprobante c = ctx.comprobante();
        String location = at("Certificado");
        if (!info.rfc().equals(c.emisor().rfc())) {
            ctx.fail("CFDI40106", location, "certificado.rfc", info.rfc(), c.emisor().rfc());
        }
        if (info.type() == CertificateType.FIEL) {
            ctx.fail("CFDI40106", location, "certificado.fiel");
        } else if (info.type() == CertificateType.UNKNOWN) {
            ctx.notVerified("CFDI40106", location, "no.verificado.tipo.certificado");
        }
        List<ZoneOffset> offsets = offsets(ctx);
        long dentro = offsets.stream().filter(o -> info.isValidAt(c.fecha().toInstant(o))).count();
        if (dentro == 0) {
            ctx.fail("CFDI40106", location, "certificado.vigencia", c.fecha(), info.validFrom(), info.validUntil());
        } else if (dentro < offsets.size()) {
            ctx.notVerified("CFDI40106", location, "no.verificado.huso");
        }
        if (ctx.certificateStatus().isEmpty()) {
            ctx.notVerified("CFDI40106", location, "no.verificado.estado.certificado");
        } else {
            Instant at = c.fecha().toInstant(offsets.get(0));
            Revocation status = ctx.certificateStatus().get().status(info.noCertificado(), at);
            if (status == Revocation.REVOKED) {
                ctx.fail("CFDI40106", location, "certificado.revocado");
            } else if (status == Revocation.UNKNOWN) {
                ctx.notVerified("CFDI40106", location, "no.verificado.estado.certificado");
            }
        }
    }

    /** The offsets of LugarExpedicion when the catalog provider knows them, else every offset of Mexico. */
    private static List<ZoneOffset> offsets(ValidationContext ctx) {
        Optional<CodigoPostalInfo> cp = ctx.catalogs().flatMap(p -> p.codigoPostal(ctx.comprobante().lugarExpedicion()));
        return cp.map(i -> List.of(i.diferenciaHorariaInvierno(), i.diferenciaHorariaVerano()).stream().distinct().toList()).orElse(MEXICO);
    }
}
