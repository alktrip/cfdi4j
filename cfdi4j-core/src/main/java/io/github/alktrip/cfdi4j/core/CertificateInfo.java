package io.github.alktrip.cfdi4j.core;

import java.time.Instant;
import java.util.Objects;

/**
 * The data of an X.509 certificate that CFDI rules need, without any X.509 dependency in
 * {@code cfdi4j-core}. Produced by {@code cfdi4j-crypto}.
 *
 * @param noCertificado the certificate number as written in {@code NoCertificado}: the serial
 *     number of the certificate read as ASCII digits
 * @param rfc the RFC of the holder (first value of {@code x500UniqueIdentifier}, OID 2.5.4.45)
 * @param nombre the holder name (subject {@code name}, else {@code CN})
 * @param validFrom start of validity ({@code notBefore})
 * @param validUntil end of validity ({@code notAfter})
 * @param type CSD, e.firma or unknown
 * @param issuer the issuer distinguished name
 */
public record CertificateInfo(
        String noCertificado, String rfc, String nombre, Instant validFrom, Instant validUntil, CertificateType type, String issuer) {

    /** Checks that no component is null. */
    public CertificateInfo {
        Objects.requireNonNull(noCertificado, "noCertificado");
        Objects.requireNonNull(rfc, "rfc");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(validFrom, "validFrom");
        Objects.requireNonNull(validUntil, "validUntil");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(issuer, "issuer");
    }

    /**
     * Tells whether an instant falls within the validity period, both ends inclusive.
     *
     * @param instant the instant
     * @return {@code true} if valid at that instant
     */
    public boolean isValidAt(Instant instant) {
        return !instant.isBefore(validFrom) && !instant.isAfter(validUntil);
    }
}
