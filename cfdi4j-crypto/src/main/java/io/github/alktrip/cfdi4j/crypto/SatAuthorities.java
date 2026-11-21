package io.github.alktrip.cfdi4j.crypto;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Certificates of certification authorities, used to check that a CSD was issued by the SAT.
 *
 * <p>{@link #production()} and {@link #test()} are the certificates the SAT publishes on its CSD
 * page, bundled unmodified ({@code Cert_Prod.zip} and {@code Certificados_P.zip}; origin and hashes
 * in {@code specs/SOURCES.md}). A certificate is accepted when its signature verifies with the
 * public key of one of these authorities whose subject is the certificate's issuer. Validity
 * dates and revocation are not checked here: the CFDI rules check the validity of the CSD at
 * {@code Fecha}, and revocation needs a {@code CertificateStatusProvider}.
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class SatAuthorities {

    private static final String PRODUCTION = "sat/ac/produccion/";
    private static final String TEST = "sat/ac/pruebas/";

    private static final List<String> PRODUCTION_FILES = List.of(
            "AC0_SAT.cer", "AC1_SAT.cer", "AC2_SAT.cer", "AC3_SAT.cer", "AC4_SAT.cer", "AC5_SAT.cer", "AC5_SAT.crt",
            "AC6_SAT.crt", "AC7.crt", "ARC0_IES.cer", "ARC1_IES.cer", "ARC2_IES.crt", "ARC3_IES.crt", "ARC4_IES.crt",
            "ARC5_IES.cer", "ARC5_IES.crt", "ARC6_IES.crt", "ARC7_IES.crt");

    private static final List<String> TEST_FILES = List.of(
            "ac2_4096_DER.cer", "ac2_4096_PEM.crt", "AC_4096_2018.cer", "AC_4096_2018.crt", "AC_UAT_2022.crt", "ARC_UAT.crt",
            "ARC_UAT_2022.crt", "arcbeta_crt_raiz.crt");

    private static final class Production {
        private static final SatAuthorities INSTANCE = load(PRODUCTION, PRODUCTION_FILES);
    }

    private static final class Test {
        private static final SatAuthorities INSTANCE = load(TEST, TEST_FILES);
    }

    private final List<X509Certificate> certificates;

    private SatAuthorities(Collection<X509Certificate> certificates) {
        Map<String, X509Certificate> unique = new LinkedHashMap<>();
        for (X509Certificate c : certificates) {
            unique.putIfAbsent(Certificates.toBase64(Objects.requireNonNull(c, "certificate")), c);
        }
        this.certificates = List.copyOf(unique.values());
    }

    /**
     * Returns the authorities that issue the production certificates (CSD and e.firma).
     *
     * @return the authorities of {@code Cert_Prod.zip}
     */
    public static SatAuthorities production() {
        return Production.INSTANCE;
    }

    /**
     * Returns the authorities that issued the SAT public test certificates, for test suites that
     * use them. Never use them to accept production documents.
     *
     * @return the authorities of {@code Certificados_P.zip}
     */
    public static SatAuthorities test() {
        return Test.INSTANCE;
    }

    /**
     * Uses other authority certificates, for example a newer set than the bundled one.
     *
     * @param certificates the certificates of the authorities
     * @return the authorities
     */
    public static SatAuthorities of(Collection<X509Certificate> certificates) {
        return new SatAuthorities(List.copyOf(certificates));
    }

    /**
     * Returns the certificates of the authorities, without duplicates.
     *
     * @return the certificates
     */
    public List<X509Certificate> certificates() {
        return certificates;
    }

    /**
     * Tells whether one of these authorities issued the certificate: its subject is the
     * certificate's issuer and its public key verifies the certificate's signature.
     *
     * @param certificate the certificate to check, for example the {@code Certificado} of a CFDI
     * @return {@code true} if issued by one of these authorities
     */
    public boolean issued(X509Certificate certificate) {
        Objects.requireNonNull(certificate, "certificate");
        for (X509Certificate authority : certificates) {
            if (!authority.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) {
                continue;
            }
            try {
                certificate.verify(authority.getPublicKey());
                return true;
            } catch (GeneralSecurityException | RuntimeException e) {
                // Another key of an authority with the same name may have signed it.
            }
        }
        return false;
    }

    private static SatAuthorities load(String directory, List<String> files) {
        List<X509Certificate> certificates = new ArrayList<>();
        for (String file : files) {
            String path = directory + file;
            try (InputStream in = SatAuthorities.class.getModule().getResourceAsStream(path)) {
                if (in == null) {
                    throw new Cfdi4jException("Missing bundled certificate " + path);
                }
                certificates.add(parse(in.readAllBytes(), path));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return new SatAuthorities(certificates);
    }

    /** Parses DER or PEM, as the SAT publishes both. */
    private static X509Certificate parse(byte[] bytes, String path) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException | ClassCastException e) {
            throw new Cfdi4jException("Cannot read the bundled certificate " + path, e);
        }
    }
}
