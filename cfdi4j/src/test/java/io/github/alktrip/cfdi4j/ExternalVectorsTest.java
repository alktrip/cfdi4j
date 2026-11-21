package io.github.alktrip.cfdi4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.xml.CfdiSchema;
import io.github.alktrip.cfdi4j.crypto.SatAuthorities;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Layer 2 of ADR 0005: vectors of eclipxe13/CfdiUtils (MIT), downloaded from a pinned commit and
 * checked by SHA-256, never committed because they contain RFCs that are not SAT test data. Runs
 * only with {@code -Pexternal-vectors}.
 */
@Tag("external")
class ExternalVectorsTest {

    private static final String COMMIT = "f2a03dc254348a8fb9e50374ecdbe49e7dd43f9b";
    private static final String CFDI40_VALID = "https://raw.githubusercontent.com/eclipxe13/CfdiUtils/" + COMMIT + "/tests/assets/cfdi40-valid.xml";
    private static final String CFDI40_VALID_SHA256 = "66056c12f2fc9db3650421eefb762226c04d25cb2fcfb74ee050e8e44f3ff531";

    /**
     * The document was sealed by another tool with the SAT test CSD. Its seal verifies only if
     * cfdi4j produces exactly the same cadena original.
     */
    @Test
    void sealMadeByAnotherToolVerifiesWithOurCadena() throws IOException {
        byte[] xml = download(CFDI40_VALID, CFDI40_VALID_SHA256);

        // Sealed with a SAT public test CSD: its issuer is one of the test authorities.
        VerificationResult result = Cfdi4j.configure().certificateAuthorities(SatAuthorities.test()).build().verify(xml);

        assertThat(result.failures()).isEmpty();
        // The real PAC certificate is not available: SelloSAT is the only thing not verified.
        assertThat(result.result().warnings()).extracting(Violation::code).containsExactly(CfdiEngine.SELLO_SAT_NO_VERIFICADO);
        assertThat(result.comprobante().orElseThrow().timbreFiscalDigital()).isPresent();
        assertThat(CfdiSchema.defaults().validate(xml).violations()).isEmpty();
    }

    private static byte[] download(String url, String sha256) throws IOException {
        Path cache = Path.of("target", "external-vectors", Path.of(URI.create(url).getPath()).getFileName().toString());
        if (!Files.isRegularFile(cache)) {
            Files.createDirectories(cache.getParent());
            try (InputStream in = URI.create(url).toURL().openStream()) {
                Files.write(cache, in.readAllBytes());
            }
        }
        byte[] content = Files.readAllBytes(cache);
        try {
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)))
                    .as("SHA-256 of %s", url).isEqualTo(sha256);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        return content;
    }
}
