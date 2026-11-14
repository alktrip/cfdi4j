package io.github.alktrip.cfdi4j.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;

/**
 * The CFDI 4.0 seal: SHA256withRSA (RSASSA-PKCS1-v1_5 with SHA-256) over the UTF-8 bytes of the
 * cadena original, encoded in Base64 (Anexo 20; error matrix CFDI40102).
 */
public final class Seals {

    /** JCA name of the signature algorithm. */
    public static final String ALGORITHM = "SHA256withRSA";

    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");

    private Seals() {}

    static String sign(String cadenaOriginal, PrivateKey key) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(key);
            signature.update(cadenaOriginal.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new CsdException("Cannot sign with the CSD key", e);
        }
    }

    /**
     * Verifies a seal.
     *
     * @param cadenaOriginal the cadena original
     * @param sello the {@code Sello} attribute (Base64)
     * @param certificate the certificate whose public key made the seal
     * @return {@code true} if the seal matches the cadena; {@code false} if it does not, or if the
     *     seal is not valid Base64
     */
    public static boolean verify(String cadenaOriginal, String sello, X509Certificate certificate) {
        Objects.requireNonNull(cadenaOriginal, "cadenaOriginal");
        Objects.requireNonNull(sello, "sello");
        Objects.requireNonNull(certificate, "certificate");
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(WHITESPACE.matcher(sello).replaceAll(""));
        } catch (IllegalArgumentException e) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(certificate.getPublicKey());
            signature.update(cadenaOriginal.getBytes(StandardCharsets.UTF_8));
            return signature.verify(bytes);
        } catch (java.security.SignatureException e) {
            return false;
        } catch (GeneralSecurityException e) {
            throw new CsdException("Cannot verify with the certificate key", e);
        }
    }
}
