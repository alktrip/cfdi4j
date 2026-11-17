package io.github.alktrip.cfdi4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.CertificateType;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.validation.CertificateStatusProvider;
import io.github.alktrip.cfdi4j.core.validation.Status;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.xml.CfdiSchema;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** The facade applies the SAT error-matrix rules on top of the schema. */
class BusinessValidationTest {

    @Test
    void validateReportsBusinessRules() {
        var invalid = Fixtures.base().subTotal("90.00")
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();

        assertThat(Cfdi4j.validate(invalid).errors()).extracting(Violation::code).contains("CFDI40108");
        assertThat(Cfdi4j.validate(Fixtures.simple()).isValid()).isTrue();
        assertThat(Cfdi4j.validate(Fixtures.simple()).warnings()).allSatisfy(v -> assertThat(v.status()).isEqualTo(Status.NOT_VERIFIED));
    }

    @Test
    void sealRefusesBusinessRuleErrors() {
        var invalid = Fixtures.base().total("1.00")
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();

        assertThatThrownBy(() -> Cfdi4j.seal(invalid, Fixtures.CSD))
                .isInstanceOfSatisfying(InvalidCfdiException.class, e -> assertThat(e.result().has("CFDI40119")).isTrue());
    }

    @Test
    void sealAppliesTheCertificateRules() {
        // The test CSD expires on 2027-05-18; a Fecha after that is outside its validity.
        var late = Fixtures.base().fecha(java.time.LocalDateTime.of(2027, 6, 1, 10, 0))
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();

        assertThatThrownBy(() -> Cfdi4j.seal(late, Fixtures.CSD))
                .isInstanceOfSatisfying(InvalidCfdiException.class, e -> assertThat(e.result().errors()).extracting(Violation::code).contains("CFDI40106"));
        assertThat(Fixtures.CSD.info().type()).isEqualTo(CertificateType.CSD);
    }

    @Test
    void validateOfASealedDocumentChecksSchemaRulesAndCertificate() {
        byte[] sealed = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).toBytes();
        CfdiEngine engine = Cfdi4j.configure().clock(Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneOffset.UTC))
                .certificateStatusProvider((n, at) -> CertificateStatusProvider.Revocation.REVOKED).build();

        assertThat(Cfdi4j.validate(sealed).errors()).isEmpty();
        assertThat(engine.validate(sealed).errors()).extracting(Violation::code).containsExactly("CFDI40106");
        String altered = new String(sealed, StandardCharsets.UTF_8).replace("SubTotal=\"100.00\"", "SubTotal=\"90.00\"");
        assertThat(Cfdi4j.validate(altered.getBytes(StandardCharsets.UTF_8)).errors()).extracting(Violation::code).contains("CFDI40108");
        String badCertificate = new String(sealed, StandardCharsets.UTF_8).replaceAll("Certificado=\"[^\"]*\"", "Certificado=\"bm8=\"");
        assertThat(Cfdi4j.validate(badCertificate.getBytes(StandardCharsets.UTF_8)).errors()).extracting(Violation::code).doesNotContain("CFDI40106");
    }

    @Test
    void validateOfUnreadableDocumentsReportsTheirProblem() {
        assertThat(Cfdi4j.validate("<no".getBytes(StandardCharsets.UTF_8)).errors()).extracting(Violation::code)
                .containsExactly(CfdiSchema.XML_MAL_FORMADO);
        assertThat(Cfdi4j.validate("<otro/>".getBytes(StandardCharsets.UTF_8)).errors()).extracting(Violation::code)
                .contains(CfdiSchema.XSD_INVALIDO, CfdiEngine.NO_LEGIBLE);
    }
}
