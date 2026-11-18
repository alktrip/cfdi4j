package io.github.alktrip.cfdi4j.crypto;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.CertificateType;
import io.github.alktrip.cfdi4j.crypto.internal.Der;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reads SAT X.509 certificates and extracts the data CFDI rules need. */
public final class Certificates {

    private static final String X500_UNIQUE_IDENTIFIER = "2.5.4.45";
    private static final String NAME = "2.5.4.41";
    private static final String COMMON_NAME = "2.5.4.3";

    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");

    private Certificates() {}

    /**
     * Parses a certificate in DER or PEM form (a SAT {@code .cer} file is DER).
     *
     * @param encoded the certificate
     * @return the certificate
     * @throws CsdException if it is not an X.509 certificate
     */
    public static X509Certificate parse(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encoded));
        } catch (CertificateException | ClassCastException e) {
            throw new CsdException("Not an X.509 certificate", e);
        }
    }

    /**
     * Parses the {@code Certificado} attribute of a CFDI: the DER certificate in Base64.
     *
     * @param base64 the attribute value
     * @return the certificate
     * @throws CsdException if the value is not Base64 or not a certificate
     */
    public static X509Certificate fromBase64(String base64) {
        Objects.requireNonNull(base64, "base64");
        try {
            return parse(Base64.getDecoder().decode(WHITESPACE.matcher(base64).replaceAll("")));
        } catch (IllegalArgumentException e) {
            throw new CsdException("Certificado is not valid Base64", e);
        }
    }

    /**
     * Encodes a certificate as the {@code Certificado} attribute expects: DER in Base64, one line.
     *
     * @param certificate the certificate
     * @return the Base64 text
     */
    public static String toBase64(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new CsdException("Cannot encode the certificate", e);
        }
    }

    /**
     * Returns the {@code NoCertificado} of a SAT certificate. The SAT writes the 20-digit
     * certificate number as the ASCII bytes of the X.509 serial number: serial
     * {@code 0x3330303031...} is {@code "30001..."}.
     *
     * @param certificate the certificate
     * @return the certificate number
     * @throws CsdException if the serial number is not ASCII digits
     */
    public static String noCertificado(X509Certificate certificate) {
        return noCertificado(certificate.getSerialNumber());
    }

    static String noCertificado(BigInteger serial) {
        // ASCII digits are below 0x80, so a valid serial never has a leading sign byte.
        String text = new String(serial.toByteArray(), StandardCharsets.US_ASCII);
        if (!text.matches("[0-9]+")) {
            throw new CsdException("The serial number " + serial.toString(16) + " is not a SAT certificate number");
        }
        return text;
    }

    /**
     * Returns the data CFDI rules need.
     *
     * @param certificate the certificate
     * @return the information
     * @throws CsdException if the serial number is not a SAT certificate number or the subject
     *     cannot be read
     */
    public static CertificateInfo info(X509Certificate certificate) {
        Map<String, String> subject;
        try {
            subject = subject(certificate);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new CsdException("Cannot read the subject of the certificate", e);
        }
        String unique = subject.getOrDefault(X500_UNIQUE_IDENTIFIER, "");
        // Persona moral certificates hold "RFC / RFC of the legal representative".
        String rfc = unique.contains("/") ? unique.substring(0, unique.indexOf('/')).strip() : unique.strip();
        String nombre = subject.getOrDefault(NAME, subject.getOrDefault(COMMON_NAME, ""));
        return new CertificateInfo(noCertificado(certificate), rfc, nombre, certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant(), type(certificate), certificate.getIssuerX500Principal().getName());
    }

    /**
     * Tells a CSD from an e.firma by its key usage.
     *
     * <p>The SAT does not publish this criterion. It is taken from the SAT public test certificates
     * ({@code Certificados_de_Prueba.zip}, see {@code specs/SOURCES.md}), checked on 2026-11-14: the 34
     * CSDs have key usage digitalSignature and nonRepudiation only and no extended key usage; the 30
     * e.firma certificates add dataEncipherment and keyAgreement, and the extended key usages
     * emailProtection and clientAuth.
     *
     * @param certificate the certificate
     * @return CSD, FIEL, or UNKNOWN when neither pattern matches
     */
    public static CertificateType type(X509Certificate certificate) {
        try {
            return type(certificate.getKeyUsage(), certificate.getExtendedKeyUsage());
        } catch (CertificateParsingException e) {
            return CertificateType.UNKNOWN;
        }
    }

    /**
     * @param usage key usage bits (RFC 5280 order), or null
     * @param extended extended key usage OIDs, or null when the extension is absent
     */
    static CertificateType type(boolean[] usage, List<String> extended) {
        if (usage == null || usage.length < 5) {
            return CertificateType.UNKNOWN;
        }
        boolean signing = usage[0] && usage[1];
        boolean encryption = usage[3] || usage[4];
        if (signing && !encryption && extended == null) {
            return CertificateType.CSD;
        }
        if (signing && usage[3] && usage[4]) {
            return CertificateType.FIEL;
        }
        return CertificateType.UNKNOWN;
    }

    /** Subject attributes by OID, first value of each, decoded from the DER name. */
    static Map<String, String> subject(X509Certificate certificate) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (Der.Tlv rdn : Der.parse(certificate.getSubjectX500Principal().getEncoded()).expect(Der.SEQUENCE).children()) {
            for (Der.Tlv atv : rdn.expect(Der.SET).children()) {
                List<Der.Tlv> pair = atv.expect(Der.SEQUENCE).children();
                try {
                    attributes.putIfAbsent(pair.get(0).oid(), pair.get(1).text());
                } catch (IllegalArgumentException e) {
                    // An attribute with a non-string value is not needed.
                }
            }
        }
        return attributes;
    }
}
