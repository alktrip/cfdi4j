package io.github.alktrip.cfdi4j.core.validation;

import java.time.Instant;

/**
 * Port to the revocation status of SAT certificates (ADR 0004). Without an implementation the
 * revocation check reports "not verified".
 */
@FunctionalInterface
public interface CertificateStatusProvider {

    /** Revocation status of a certificate at a moment. */
    enum Revocation {
        /** Not revoked. */
        VALID,
        /** Revoked. */
        REVOKED,
        /** The provider cannot tell. */
        UNKNOWN
    }

    /**
     * Returns the status of a certificate.
     *
     * @param noCertificado the 20-digit certificate number
     * @param at the moment of interest
     * @return the status
     */
    Revocation status(String noCertificado, Instant at);
}
