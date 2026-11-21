package io.github.alktrip.cfdi4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.TimbreFiscalDigital;
import io.github.alktrip.cfdi4j.core.validation.Severity;
import io.github.alktrip.cfdi4j.core.validation.Status;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.xml.CadenaOriginalTimbre;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import io.github.alktrip.cfdi4j.crypto.SatAuthorities;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The SAT certificate of real timbres is not available in tests, so the test CSD plays the SAT: it
 * signs the cadena original of the timbre, and the port returns its certificate.
 */
class TimbreVerificationTest {

    private static final SatCertificates TEST_SAT = n -> n.equals(Fixtures.CSD.noCertificado())
            ? Optional.of(Fixtures.CSD.certificate()) : Optional.empty();

    private static Comprobante stamped(String selloCFD, boolean validSelloSat) {
        Comprobante sealed = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).comprobante();
        TimbreFiscalDigital unsigned = new TimbreFiscalDigital("1.1", "5FB2822E-396D-4725-8521-CDC4BDD20CCF",
                LocalDateTime.of(2026, 11, 14, 10, 31, 0), "KIJ0906199R1", Optional.empty(),
                selloCFD == null ? sealed.sello().orElseThrow() : selloCFD, Fixtures.CSD.noCertificado(), "pendiente");
        String selloSat = validSelloSat ? Fixtures.CSD.sign(CadenaOriginalTimbre.defaults().of(unsigned)) : Fixtures.CSD.sign("otra cosa");
        TimbreFiscalDigital timbre = new TimbreFiscalDigital(unsigned.version(), unsigned.uuid(), unsigned.fechaTimbrado(),
                unsigned.rfcProvCertif(), unsigned.leyenda(), unsigned.selloCFD(), unsigned.noCertificadoSAT(), selloSat);
        return sealed.withComplemento(timbre);
    }

    @Test
    void stampedCfdiVerifiesBothSeals() {
        byte[] xml = CfdiXml.defaults().toBytes(stamped(null, true));

        VerificationResult result = Cfdi4j.configure().satCertificates(TEST_SAT).certificateAuthorities(SatAuthorities.test()).build().verify(xml);

        assertThat(result.result().violations()).isEmpty();
        assertThat(result.comprobante().orElseThrow().timbreFiscalDigital()).isPresent();
    }

    @Test
    void addingTheTimbreDoesNotChangeTheCadenaOriginal() {
        Comprobante cfdi = stamped(null, true);

        assertThat(Cfdi4j.verify(CfdiXml.defaults().toBytes(cfdi)).cadenaOriginal())
                .contains(Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).cadenaOriginal());
    }

    @Test
    void withoutTheSatCertificateSelloSatIsNotVerifiedButTheDocumentIsStillValid() {
        VerificationResult result = Cfdi4j.configure().certificateAuthorities(SatAuthorities.test()).build()
                .verify(CfdiXml.defaults().toBytes(stamped(null, true)));

        assertThat(result.isValid()).isTrue();
        assertThat(result.result().warnings()).singleElement().satisfies(v -> {
            assertThat(v.code()).isEqualTo(CfdiEngine.SELLO_SAT_NO_VERIFICADO);
            assertThat(v.severity()).isEqualTo(Severity.WARNING);
            assertThat(v.status()).isEqualTo(Status.NOT_VERIFIED);
            assertThat(v.message()).contains(Fixtures.CSD.noCertificado());
        });
    }

    @Test
    void timbreOfAnotherComprobanteIsDetected() {
        VerificationResult result = Cfdi4j.configure().satCertificates(TEST_SAT).certificateAuthorities(SatAuthorities.test()).build()
                .verify(CfdiXml.defaults().toBytes(stamped("b3Rybw==", true)));

        assertThat(result.failures()).extracting(Violation::code).containsExactly(CfdiEngine.SELLO_CFD_DISTINTO);
    }

    @Test
    void alteredTimbreIsDetected() {
        VerificationResult result = Cfdi4j.configure().satCertificates(TEST_SAT).certificateAuthorities(SatAuthorities.test()).build()
                .verify(CfdiXml.defaults().toBytes(stamped(null, false)));

        assertThat(result.failures()).extracting(Violation::code).containsExactly(CfdiEngine.SELLO_SAT_INVALIDO);
    }
}
