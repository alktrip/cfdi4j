package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.validation.Severity;
import io.github.alktrip.cfdi4j.core.validation.Status;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class CfdiSchemaTest {

    private final CfdiSchema schema = CfdiSchema.defaults();

    @Test
    void xmlWrittenByCfdiXmlIsValidAgainstTheOfficialSchema() {
        ValidationResult result = schema.validateUnsealed(Fixtures.bytes(Fixtures.simple()));

        assertThat(result.violations()).isEmpty();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void sealedDocumentIsValidatedAsItIs() {
        assertThat(schema.validate(Fixtures.sealedXml(Fixtures.simple())).violations()).isEmpty();
    }

    @Test
    void strictValidationReportsTheMissingSealAttributes() {
        ValidationResult result = schema.validate(Fixtures.bytes(Fixtures.simple()));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(3).allSatisfy(v -> {
            assertThat(v.code()).isEqualTo(CfdiSchema.XSD_INVALIDO);
            assertThat(v.status()).isEqualTo(Status.FAILED);
            assertThat(v.location()).startsWith("line 1, column ");
        });
        assertThat(result.errors()).extracting(Violation::message)
                .anySatisfy(m -> assertThat(m).contains("Sello"))
                .anySatisfy(m -> assertThat(m).contains("NoCertificado"))
                .anySatisfy(m -> assertThat(m).contains("Certificado"));
    }

    @Test
    void everyNodeOfTheCompleteFixtureIsValidAndTheUnbundledComplementIsReportedAsNotVerified() {
        ValidationResult result = schema.validateUnsealed(Fixtures.bytes(Fixtures.complete()));

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).singleElement().satisfies(v -> {
            assertThat(v.code()).isEqualTo(CfdiSchema.COMPLEMENTO_NO_VALIDADO);
            assertThat(v.severity()).isEqualTo(Severity.WARNING);
            assertThat(v.status()).isEqualTo(Status.NOT_VERIFIED);
            assertThat(v.message()).contains("http://www.sat.gob.mx/iedu");
        });
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void timbreFiscalDigitalIsValidatedStrictly() {
        String xml = new String(Fixtures.bytes(Fixtures.complete()), StandardCharsets.UTF_8)
                .replace("UUID=\"5FB2822E-396D-4725-8521-CDC4BDD20CCF\" FechaTimbrado", "UUID=\"no-es-uuid\" FechaTimbrado");

        ValidationResult result = schema.validateUnsealed(xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.errors()).isNotEmpty().allSatisfy(v -> assertThat(v.code()).isEqualTo(CfdiSchema.XSD_INVALIDO));
        assertThat(result.errors()).extracting(Violation::message).anySatisfy(m -> assertThat(m).contains("no-es-uuid"));
    }

    @Test
    void reportsEveryProblemNotOnlyTheFirst() {
        String xml = new String(Fixtures.bytes(Fixtures.simple()), StandardCharsets.UTF_8)
                .replace("LugarExpedicion=\"72000\"", "LugarExpedicion=\"7200\"")
                .replace("Rfc=\"XAXX010101000\"", "Rfc=\"NO-ES-RFC\"")
                .replace("ClaveProdServ=\"01010101\"", "ClaveProdServ=\"99999999\"");

        ValidationResult result = schema.validateUnsealed(xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.errors()).extracting(Violation::message)
                .anySatisfy(m -> assertThat(m).contains("7200"))
                .anySatisfy(m -> assertThat(m).contains("NO-ES-RFC"))
                // catCFDI.xsd enumerates c_ClaveProdServ, so an unknown key fails schema validation.
                .anySatisfy(m -> assertThat(m).contains("99999999"));
    }

    @Test
    void addendaContentIsNotValidated() {
        String xml = new String(Fixtures.bytes(Fixtures.complete()), StandardCharsets.UTF_8);

        assertThat(xml).contains("<cfdi:Addenda>");
        assertThat(schema.validateUnsealed(xml.getBytes(StandardCharsets.UTF_8)).errors()).isEmpty();
    }

    @Test
    void malformedXmlAndDoctypesAreReportedAsViolations() {
        ValidationResult malformed = schema.validate("<cfdi:Comprobante".getBytes(StandardCharsets.UTF_8));
        ValidationResult doctype = schema.validate(("<?xml version=\"1.0\"?><!DOCTYPE c [<!ENTITY x SYSTEM \"http://www.sat.gob.mx/x\">]>"
                + "<c>&x;</c>").getBytes(StandardCharsets.UTF_8));

        assertThat(malformed.violations()).singleElement().satisfies(v -> assertThat(v.code()).isEqualTo(CfdiSchema.XML_MAL_FORMADO));
        assertThat(doctype.violations()).singleElement().satisfies(v -> {
            assertThat(v.code()).isEqualTo(CfdiSchema.XML_MAL_FORMADO);
            assertThat(v.message()).contains("DOCTYPE");
        });
    }

    @Test
    void messagesAreSpanishByDefaultAndEnglishOnRequest() {
        byte[] invalid = Fixtures.bytes(Fixtures.simple());

        assertThat(schema.validate(invalid).errors().get(0).message()).startsWith("El XML no cumple con el esquema XSD del SAT: ");
        assertThat(schema.withLocale(Locale.ENGLISH).validate(invalid).errors().get(0).message())
                .startsWith("The XML does not conform to the SAT XSD schema: ");
        assertThat(schema.withLocale(Locale.FRENCH).validate(invalid).errors().get(0).message())
                .startsWith("The XML does not conform");
    }

    @Test
    void validatedNamespacesAreTheBundledSchemas() {
        assertThat(CfdiSchema.validatedNamespaces()).containsExactlyInAnyOrder(
                "http://www.sat.gob.mx/cfd/4", "http://www.sat.gob.mx/TimbreFiscalDigital", "http://www.sat.gob.mx/Pagos20");
    }
}
