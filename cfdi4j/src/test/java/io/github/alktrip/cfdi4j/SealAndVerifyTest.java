package io.github.alktrip.cfdi4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.crypto.Certificates;
import io.github.alktrip.cfdi4j.xml.CfdiSchema;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import io.github.alktrip.cfdi4j.crypto.SatAuthorities;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SealAndVerifyTest {

    @Test
    void sealedCfdiIsValidAgainstTheSchemaAndVerifies() {
        SealedCfdi sealed = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD);

        assertThat(sealed.comprobante().noCertificado()).contains("30001000000500003416");
        assertThat(sealed.comprobante().certificado()).contains(Fixtures.CSD.certificadoBase64());
        assertThat(sealed.sello()).isEqualTo(Fixtures.CSD.sign(sealed.cadenaOriginal()));
        assertThat(sealed.toXml()).isEqualTo(new String(sealed.toBytes(), StandardCharsets.UTF_8));
        assertThat(CfdiSchema.defaults().validate(sealed.toBytes()).violations()).isEmpty();

        VerificationResult result = Cfdi4j.configure().certificateAuthorities(SatAuthorities.test()).build().verify(sealed.toXml());

        assertThat(result.isValid()).isTrue();
        assertThat(result.result().violations()).isEmpty();
        assertThat(result.cadenaOriginal()).contains(sealed.cadenaOriginal());
        assertThat(result.comprobante()).contains(sealed.comprobante());
    }

    static Stream<String> golden() throws IOException {
        try (Stream<java.nio.file.Path> files = Files.list(Fixtures.GOLDEN)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".xml")).map(n -> n.replace(".xml", "")).sorted().toList().stream();
        }
    }

    /** The facade seals each golden input exactly as OpenSSL signed the independently produced cadena. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("golden")
    void sealOfEachGoldenInputIsTheOpenSslSeal(String name) throws IOException {
        var input = CfdiXml.defaults().parse(Files.readAllBytes(Fixtures.GOLDEN.resolve(name + ".xml")));
        String expectedCadena = Files.readString(Fixtures.GOLDEN.resolve(name + ".cadena.txt"), StandardCharsets.UTF_8);
        String expectedSello = Files.readString(Fixtures.GOLDEN.resolve(name + ".sello"), StandardCharsets.US_ASCII).strip();

        SealedCfdi sealed = Cfdi4j.seal(input, Fixtures.CSD);

        assertThat(sealed.cadenaOriginal()).isEqualTo(expectedCadena);
        assertThat(sealed.sello()).isEqualTo(expectedSello);
        assertThat(Cfdi4j.verify(sealed.toBytes()).isValid()).isTrue();
    }

    @Test
    void changingAnyAttributeInTheCadenaBreaksTheSeal() {
        String xml = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).toXml();

        for (String[] change : new String[][] {
                {"Total=\"116.00\"", "Total=\"117.00\""},
                {"Descripcion=\"Servicio\"", "Descripcion=\"Servicios\""},
                {"Rfc=\"XIA190128J61\"", "Rfc=\"IIA040805DZ4\""},
                {"Fecha=\"2026-11-14T10:30:00\"", "Fecha=\"2026-11-14T10:30:01\""}}) {
            VerificationResult result = Cfdi4j.verify(xml.replace(change[0], change[1]));

            assertThat(result.isValid()).as(change[1]).isFalse();
            assertThat(result.failures()).extracting(Violation::code).as(change[1]).containsExactly(CfdiEngine.SELLO_INVALIDO);
        }
    }

    @Test
    void anotherCertificateIsDetected() throws IOException {
        String xml = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).toXml();
        String fiel = Base64.getEncoder().encodeToString(Files.readAllBytes(Fixtures.CSD_DIR.resolve("fiel.cer")));

        VerificationResult result = Cfdi4j.verify(xml.replace(Fixtures.CSD.certificadoBase64(), fiel));

        assertThat(result.failures()).extracting(Violation::code).containsExactly(CfdiEngine.NO_CERTIFICADO_DISTINTO, CfdiEngine.SELLO_INVALIDO);
        assertThat(result.failures().get(0).message()).contains("30001000000500003416").contains("30001000000500003415");
    }

    @Test
    void reportsWhatPreventsVerification() {
        String xml = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).toXml();

        assertThat(Cfdi4j.verify(xml.replaceAll(" Sello=\"[^\"]*\"", "")).failures()).extracting(Violation::code, Violation::location)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(CfdiEngine.FALTA_ATRIBUTO_SELLO, "/cfdi:Comprobante/@Sello"));
        assertThat(Cfdi4j.verify(CfdiXml.defaults().toBytes(Fixtures.simple())).failures()).hasSize(3);
        assertThat(Cfdi4j.verify(xml.replaceAll(" Certificado=\"[^\"]*\"", " Certificado=\"bm8=\"")).failures())
                .extracting(Violation::code).containsExactly(CfdiEngine.CERTIFICADO_INVALIDO);
        VerificationResult unreadable = Cfdi4j.verify("<no-es-cfdi/>");
        assertThat(unreadable.failures()).extracting(Violation::code).containsExactly(CfdiEngine.NO_LEGIBLE);
        assertThat(unreadable.comprobante()).isEmpty();
        assertThat(unreadable.cadenaOriginal()).isEmpty();
    }

    @Test
    void invalidComprobanteIsNotSealed() {
        var invalid = Fixtures.base().lugarExpedicion("7200")
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("1.00").objetoImp(io.github.alktrip.cfdi4j.core.catalog.ObjetoImp.NO_OBJETO))
                .build();

        assertThat(Cfdi4j.validate(invalid).isValid()).isFalse();
        assertThatThrownBy(() -> Cfdi4j.seal(invalid, Fixtures.CSD))
                .isInstanceOfSatisfying(InvalidCfdiException.class, e -> assertThat(e.result().has(CfdiSchema.XSD_INVALIDO)).isTrue())
                .hasMessageStartingWith("The comprobante is not valid: CFDI4J-001");
        assertThat(Cfdi4j.validate(Fixtures.simple()).isValid()).isTrue();
    }

    @Test
    void messagesFollowTheConfiguredLanguage() {
        String xml = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).toXml().replace("Total=\"116.00\"", "Total=\"1.00\"");

        assertThat(Cfdi4j.verify(xml).failures().get(0).message()).startsWith("El sello no corresponde");
        assertThat(Cfdi4j.configure().locale(Locale.ENGLISH).build().verify(xml).failures().get(0).message())
                .startsWith("The seal does not match");
    }

    @Test
    void certificateNumberHelperAgreesWithTheCsd() {
        assertThat(Certificates.noCertificado(Fixtures.CSD.certificate())).isEqualTo(Fixtures.CSD.noCertificado());
        assertThat(Cfdi4j.engine().clock()).isNotNull();
    }
}
