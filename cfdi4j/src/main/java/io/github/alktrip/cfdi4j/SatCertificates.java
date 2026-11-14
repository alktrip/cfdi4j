package io.github.alktrip.cfdi4j;

import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Port to the SAT certificates that seal TimbreFiscalDigital stamps ({@code NoCertificadoSAT}).
 *
 * <p>cfdi4j does not download them. Without an implementation, {@code SelloSAT} is reported as not
 * verified (ADR 0004).
 */
@FunctionalInterface
public interface SatCertificates {

    /**
     * Finds a SAT certificate.
     *
     * @param noCertificadoSAT the 20-digit certificate number
     * @return the certificate, if known
     */
    Optional<X509Certificate> find(String noCertificadoSAT);

    /**
     * Returns the implementation that knows no certificate.
     *
     * @return the empty implementation
     */
    static SatCertificates none() {
        return noCertificado -> Optional.empty();
    }
}
