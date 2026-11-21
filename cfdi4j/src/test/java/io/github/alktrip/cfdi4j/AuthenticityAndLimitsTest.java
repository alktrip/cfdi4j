package io.github.alktrip.cfdi4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.validation.Severity;
import io.github.alktrip.cfdi4j.core.validation.Status;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.crypto.SatAuthorities;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Who issued the certificate of a CFDI, and the size limit for untrusted documents. */
class AuthenticityAndLimitsTest {

    private static final byte[] SELLADO = Cfdi4j.seal(Fixtures.simple(), Fixtures.CSD).toBytes();

    @Test
    void withoutAuthoritiesTheIssuerIsNotVerifiedButTheDocumentIsIntact() {
        VerificationResult result = Cfdi4j.verify(SELLADO);

        assertThat(result.isValid()).isTrue();
        assertThat(result.result().warnings()).singleElement().satisfies(v -> {
            assertThat(v.code()).isEqualTo(CfdiEngine.CERTIFICADO_NO_EMITIDO_POR_SAT);
            assertThat(v.status()).isEqualTo(Status.NOT_VERIFIED);
            assertThat(v.message()).contains("30001000000500003416").contains("SatAuthorities");
        });
    }

    /** The SAT public test CSD must never pass as a production certificate. */
    @Test
    void aTestCertificateIsRejectedByTheProductionAuthorities() {
        VerificationResult result = Cfdi4j.configure().certificateAuthorities(SatAuthorities.production()).build().verify(SELLADO);

        assertThat(result.isValid()).isFalse();
        assertThat(result.failures()).singleElement().satisfies(v -> {
            assertThat(v.code()).isEqualTo(CfdiEngine.CERTIFICADO_NO_EMITIDO_POR_SAT);
            assertThat(v.severity()).isEqualTo(Severity.ERROR);
            assertThat(v.location()).isEqualTo("/cfdi:Comprobante/@Certificado");
        });
    }

    @Test
    void theTestAuthoritiesAcceptTheTestCertificate() {
        VerificationResult result = Cfdi4j.configure().certificateAuthorities(SatAuthorities.test()).build().verify(SELLADO);

        assertThat(result.result().violations()).isEmpty();
    }

    @Test
    void aDocumentAboveTheLimitIsNotRead() {
        CfdiEngine engine = Cfdi4j.configure().maxDocumentSize(SELLADO.length - 1).build();

        assertThat(engine.verify(SELLADO).failures()).extracting(Violation::code).containsExactly(CfdiEngine.DOCUMENTO_DEMASIADO_GRANDE);
        assertThat(engine.verify(SELLADO).comprobante()).isEmpty();
        assertThat(engine.validate(SELLADO).errors()).singleElement()
                .satisfies(v -> assertThat(v.message()).contains(Integer.toString(SELLADO.length - 1).substring(0, 1)));
        assertThat(Cfdi4j.configure().maxDocumentSize(SELLADO.length).build().verify(SELLADO).isValid()).isTrue();
        assertThat(CfdiEngine.DEFAULT_MAX_DOCUMENT_SIZE).isEqualTo(20 * 1024 * 1024);
    }

    @Test
    void theLimitMustBePositiveAndMessagesAreLocalized() {
        assertThatThrownBy(() -> Cfdi4j.configure().maxDocumentSize(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Cfdi4j.configure().locale(Locale.ENGLISH).maxDocumentSize(10).build().verify(SELLADO).failures().get(0).message())
                .startsWith("The document has");
    }
}
