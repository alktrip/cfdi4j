package io.github.alktrip.cfdi4j.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.CertificateType;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

class CertificatesTest {

    @Test
    void efirmaOfTheSameTaxpayerIsRecognized() throws IOException {
        var fiel = Certificates.parse(Files.readAllBytes(CsdTest.DIR.resolve("fiel.cer")));

        assertThat(Certificates.type(fiel)).isEqualTo(CertificateType.FIEL);
        assertThat(Certificates.info(fiel).rfc()).isEqualTo("EKU9003173C9");
        assertThat(Certificates.noCertificado(fiel)).isEqualTo("30001000000500003415");
    }

    @Test
    void readsTheCertificadoAttribute() {
        Csd csd = CsdTest.load();

        assertThat(Certificates.fromBase64(csd.certificadoBase64())).isEqualTo(csd.certificate());
        assertThatThrownBy(() -> Certificates.fromBase64("no base64 ***")).isInstanceOf(CsdException.class).hasMessageContaining("Base64");
    }

    @Test
    void noCertificadoIsTheSerialReadAsAsciiDigits() {
        assertThat(Certificates.noCertificado(new BigInteger(1, "30001000000500003416".getBytes(StandardCharsets.US_ASCII))))
                .isEqualTo("30001000000500003416");
        assertThatThrownBy(() -> Certificates.noCertificado(BigInteger.valueOf(0x1234)))
                .isInstanceOf(CsdException.class).hasMessageContaining("is not a SAT certificate number");
    }

    @Test
    void typeFollowsTheKeyUsageOfTheSatTestCertificates() {
        boolean[] csd = {true, true, false, false, false, false, false, false, false};
        boolean[] fiel = {true, true, false, true, true, false, false, false, false};

        assertThat(Certificates.type(csd, null)).isEqualTo(CertificateType.CSD);
        assertThat(Certificates.type(fiel, List.of("1.3.6.1.5.5.7.3.4", "1.3.6.1.5.5.7.3.2"))).isEqualTo(CertificateType.FIEL);
        assertThat(Certificates.type(csd, List.of("1.3.6.1.5.5.7.3.4"))).isEqualTo(CertificateType.UNKNOWN);
        assertThat(Certificates.type(new boolean[] {true, false, false, false, false}, null)).isEqualTo(CertificateType.UNKNOWN);
        assertThat(Certificates.type(new boolean[] {true, true, false, true, false}, null)).isEqualTo(CertificateType.UNKNOWN);
        assertThat(Certificates.type(new boolean[] {true, true}, null)).isEqualTo(CertificateType.UNKNOWN);
        assertThat(Certificates.type(null, null)).isEqualTo(CertificateType.UNKNOWN);
    }

    @Test
    void notACertificate() {
        assertThatThrownBy(() -> Certificates.parse(new byte[] {1, 2, 3})).isInstanceOf(CsdException.class);
    }
}
