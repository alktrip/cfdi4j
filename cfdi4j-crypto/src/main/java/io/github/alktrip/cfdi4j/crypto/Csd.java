package io.github.alktrip.cfdi4j.crypto;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.crypto.internal.PrivateKeys;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;

/**
 * A Certificado de Sello Digital: the certificate ({@code .cer}) and its private key
 * ({@code .key}).
 *
 * <p>The {@code .key} is an encrypted PKCS#8 file; it is decrypted with the JCA only (ADR 0010). The
 * password is not kept. Instances are immutable and thread-safe. Loading does not check validity
 * dates or the certificate type: those are validation rules, evaluated with a {@link java.time.Clock}.
 *
 * <pre>{@code
 * Csd csd = Csd.load(Path.of("csd.cer"), Path.of("csd.key"), password);
 * String sello = csd.sign(cadenaOriginal);
 * }</pre>
 */
public final class Csd {

    private final X509Certificate certificate;
    private final PrivateKey key;
    private final CertificateInfo info;
    private final String certificadoBase64;

    private Csd(X509Certificate certificate, PrivateKey key) {
        this.certificate = certificate;
        this.key = key;
        this.info = Certificates.info(certificate);
        this.certificadoBase64 = Certificates.toBase64(certificate);
    }

    /**
     * Loads a CSD from files.
     *
     * @param cer the certificate file (DER or PEM)
     * @param key the encrypted private key file (DER, or PEM {@code ENCRYPTED PRIVATE KEY})
     * @param password the key password; the caller may clear the array afterwards
     * @return the CSD
     * @throws CsdException if the files cannot be read as a CSD, the password is wrong, or the key
     *     does not belong to the certificate
     * @throws UncheckedIOException if a file cannot be read
     */
    public static Csd load(Path cer, Path key, char[] password) {
        try {
            return load(Files.readAllBytes(cer), Files.readAllBytes(key), password);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Loads a CSD from bytes.
     *
     * @param cer the certificate (DER or PEM)
     * @param key the encrypted private key (DER, or PEM {@code ENCRYPTED PRIVATE KEY})
     * @param password the key password; the caller may clear the array afterwards
     * @return the CSD
     * @throws CsdException if the data cannot be read as a CSD, the password is wrong, or the key
     *     does not belong to the certificate
     */
    public static Csd load(byte[] cer, byte[] key, char[] password) {
        Objects.requireNonNull(cer, "cer");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(password, "password");
        X509Certificate certificate = Certificates.parse(cer);
        PrivateKey privateKey;
        try {
            privateKey = PrivateKeys.decrypt(fromPem(key), password, "RSA");
        } catch (GeneralSecurityException e) {
            throw new CsdException("Cannot decrypt the private key: wrong password or damaged file", e);
        } catch (IllegalArgumentException e) {
            throw new CsdException("Not a supported encrypted PKCS#8 private key: " + e.getMessage(), e);
        }
        if (!(certificate.getPublicKey() instanceof RSAPublicKey publicKey)
                || !(privateKey instanceof RSAPrivateCrtKey rsaKey)
                || !publicKey.getModulus().equals(rsaKey.getModulus())
                || !publicKey.getPublicExponent().equals(rsaKey.getPublicExponent())) {
            throw new CsdException("The private key does not belong to certificate " + Certificates.noCertificado(certificate));
        }
        return new Csd(certificate, privateKey);
    }

    /**
     * Returns the certificate.
     *
     * @return the certificate
     */
    public X509Certificate certificate() {
        return certificate;
    }

    /**
     * Returns the certificate data used by validation rules.
     *
     * @return the information
     */
    public CertificateInfo info() {
        return info;
    }

    /**
     * Returns the value of the {@code NoCertificado} attribute.
     *
     * @return the 20-digit certificate number
     */
    public String noCertificado() {
        return info.noCertificado();
    }

    /**
     * Returns the value of the {@code Certificado} attribute: the DER certificate in Base64.
     *
     * @return the Base64 text
     */
    public String certificadoBase64() {
        return certificadoBase64;
    }

    /**
     * Seals a cadena original: SHA256withRSA over its UTF-8 bytes, in Base64.
     *
     * @param cadenaOriginal the cadena original
     * @return the value of the {@code Sello} attribute
     */
    public String sign(String cadenaOriginal) {
        return Seals.sign(Objects.requireNonNull(cadenaOriginal, "cadenaOriginal"), key);
    }

    /** Never prints key material. */
    @Override
    public String toString() {
        return "Csd[noCertificado=" + info.noCertificado() + ", rfc=" + info.rfc() + "]";
    }

    private static byte[] fromPem(byte[] key) {
        String text = new String(key, StandardCharsets.US_ASCII);
        String begin = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
        if (!text.startsWith(begin)) {
            return key;
        }
        int end = text.indexOf("-----END");
        if (end < 0) {
            throw new IllegalArgumentException("PEM key without END line");
        }
        return Base64.getMimeDecoder().decode(text.substring(begin.length(), end));
    }
}
