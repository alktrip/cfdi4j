package io.github.alktrip.cfdi4j.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.CertificateType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Uses the SAT public test CSD of EKU9003173C9 (specs/sat/csd-pruebas, password from the SAT archive). */
class CsdTest {

    static final Path DIR = Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9");
    static final char[] PASSWORD = "12345678a".toCharArray();

    static Csd load() {
        return Csd.load(DIR.resolve("csd.cer"), DIR.resolve("csd.key"), PASSWORD);
    }

    @Test
    void loadsTheSatTestCsd() throws IOException {
        Csd csd = load();

        assertThat(csd.noCertificado()).isEqualTo("30001000000500003416");
        CertificateInfo info = csd.info();
        assertThat(info.rfc()).isEqualTo("EKU9003173C9");
        assertThat(info.nombre()).isEqualTo("ESCUELA KEMPER URGATE SA DE CV");
        assertThat(info.type()).isEqualTo(CertificateType.CSD);
        // Validity printed by openssl x509 -dates (specs/SOURCES.md).
        assertThat(info.validFrom()).isEqualTo(Instant.parse("2023-05-18T11:43:51Z"));
        assertThat(info.validUntil()).isEqualTo(Instant.parse("2027-05-18T11:43:51Z"));
        assertThat(info.issuer()).contains("AC UAT");
        assertThat(Base64.getDecoder().decode(csd.certificadoBase64())).isEqualTo(Files.readAllBytes(DIR.resolve("csd.cer")));
        assertThat(csd.certificate().getSerialNumber().toString(16)).isEqualTo("3330303031303030303030353030303033343136");
    }

    @Test
    void toStringNeverShowsKeyMaterial() {
        assertThat(load().toString()).isEqualTo("Csd[noCertificado=30001000000500003416, rfc=EKU9003173C9]");
    }

    @Test
    void wrongPasswordIsReported() {
        assertThatThrownBy(() -> Csd.load(DIR.resolve("csd.cer"), DIR.resolve("csd.key"), "otra".toCharArray()))
                .isInstanceOf(CsdException.class)
                .hasMessageContaining("wrong password");
    }

    @Test
    void keyOfAnotherCertificateIsRejected() {
        assertThatThrownBy(() -> Csd.load(DIR.resolve("fiel.cer"), DIR.resolve("csd.key"), PASSWORD))
                .isInstanceOf(CsdException.class)
                .hasMessage("The private key does not belong to certificate 30001000000500003415");
    }

    @Test
    void damagedFilesAreReported() throws IOException {
        byte[] cer = Files.readAllBytes(DIR.resolve("csd.cer"));
        byte[] key = Files.readAllBytes(DIR.resolve("csd.key"));

        assertThatThrownBy(() -> Csd.load(cer, "no es llave".getBytes(StandardCharsets.US_ASCII), PASSWORD))
                .isInstanceOf(CsdException.class).hasMessageContaining("Not a supported encrypted PKCS#8 private key");
        assertThatThrownBy(() -> Csd.load("no es certificado".getBytes(StandardCharsets.US_ASCII), key, PASSWORD))
                .isInstanceOf(CsdException.class).hasMessage("Not an X.509 certificate");
        assertThatThrownBy(() -> Csd.load(DIR.resolve("no-existe.cer"), DIR.resolve("csd.key"), PASSWORD))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void malformedKeyStructuresAreReportedAsCsdException() throws IOException {
        byte[] cer = Files.readAllBytes(DIR.resolve("csd.cer"));
        byte[] pbes2 = {0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x05, 0x0d};
        // PBES2 without its parameters.
        byte[] sinParametros = der(0x30, der(0x30, pbes2), der(0x04, new byte[] {0}));

        assertThatThrownBy(() -> Csd.load(cer, sinParametros, PASSWORD))
                .isInstanceOf(CsdException.class).hasMessageContaining("Not a supported encrypted PKCS#8 private key");
    }

    @Test
    void anAbsurdIterationCountIsRejectedBeforeDerivingTheKey() throws IOException {
        byte[] cer = Files.readAllBytes(DIR.resolve("csd.cer"));
        byte[] pbes2 = {0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x05, 0x0d};
        byte[] pbkdf2 = {0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x05, 0x0c};
        byte[] des3 = {0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x03, 0x07};
        byte[] iteraciones = der(0x02, new byte[] {0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff});
        byte[] kdf = der(0x30, pbkdf2, der(0x30, der(0x04, new byte[8]), iteraciones));
        byte[] esquema = der(0x30, des3, der(0x04, new byte[8]));
        byte[] llave = der(0x30, der(0x30, pbes2, der(0x30, kdf, esquema)), der(0x04, new byte[16]));

        assertThatThrownBy(() -> Csd.load(cer, llave, PASSWORD))
                .isInstanceOf(CsdException.class).hasMessageContaining("unsupported PBKDF2 iteration count 2147483647");
    }

    /** A DER value with a short-form length (less than 128 bytes of content). */
    private static byte[] der(int tag, byte[]... parts) {
        java.io.ByteArrayOutputStream content = new java.io.ByteArrayOutputStream();
        for (byte[] part : parts) {
            content.writeBytes(part);
        }
        byte[] body = content.toByteArray();
        byte[] out = new byte[body.length + 2];
        out[0] = (byte) tag;
        out[1] = (byte) body.length;
        System.arraycopy(body, 0, out, 2, body.length);
        return out;
    }

    @Test
    void acceptsPemFiles() throws IOException {
        byte[] cer = Files.readAllBytes(DIR.resolve("csd.cer"));
        byte[] key = Files.readAllBytes(DIR.resolve("csd.key"));
        String pemCer = "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder().encodeToString(cer) + "\n-----END CERTIFICATE-----\n";
        String pemKey = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(key)
                + "\n-----END ENCRYPTED PRIVATE KEY-----\n";

        Csd csd = Csd.load(pemCer.getBytes(StandardCharsets.US_ASCII), pemKey.getBytes(StandardCharsets.US_ASCII), PASSWORD);

        assertThat(csd.noCertificado()).isEqualTo("30001000000500003416");
        assertThatThrownBy(() -> Csd.load(cer, "-----BEGIN ENCRYPTED PRIVATE KEY-----\nAAAA".getBytes(StandardCharsets.US_ASCII), PASSWORD))
                .isInstanceOf(CsdException.class).hasMessageContaining("without END line");
    }
}
