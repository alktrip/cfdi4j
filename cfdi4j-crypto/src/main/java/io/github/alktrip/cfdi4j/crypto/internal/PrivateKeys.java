package io.github.alktrip.cfdi4j.crypto.internal;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypts an encrypted PKCS#8 private key ({@code EncryptedPrivateKeyInfo}, DER) with the JCA
 * only (ADR 0010).
 *
 * <p>The SAT {@code .key} files use PBES2 (RFC 8018) with PBKDF2 and {@code des-ede3-cbc}. The JDK
 * parses PBES2 parameters only for AES, so PBES2 is decoded here and executed with standard JCA
 * primitives ({@code PBKDF2WithHmacSHA*}, {@code DESede/CBC/PKCS5Padding}, {@code AES/CBC/...}).
 * Other schemes are delegated to the JCA.
 */
public final class PrivateKeys {

    static final String PBES2 = "1.2.840.113549.1.5.13";
    static final String PBKDF2 = "1.2.840.113549.1.5.12";

    /** PBKDF2 pseudo-random functions (RFC 8018, appendix B.1). */
    private static final Map<String, String> PRFS = Map.of(
            "1.2.840.113549.2.7", "PBKDF2WithHmacSHA1",
            "1.2.840.113549.2.9", "PBKDF2WithHmacSHA256",
            "1.2.840.113549.2.10", "PBKDF2WithHmacSHA384",
            "1.2.840.113549.2.11", "PBKDF2WithHmacSHA512");

    /** Encryption schemes: OID to {JCA transformation, key algorithm, key length in bytes}. */
    private static final Map<String, List<Object>> CIPHERS = Map.of(
            "1.2.840.113549.3.7", List.of("DESede/CBC/PKCS5Padding", "DESede", 24),
            "2.16.840.1.101.3.4.1.2", List.of("AES/CBC/PKCS5Padding", "AES", 16),
            "2.16.840.1.101.3.4.1.22", List.of("AES/CBC/PKCS5Padding", "AES", 24),
            "2.16.840.1.101.3.4.1.42", List.of("AES/CBC/PKCS5Padding", "AES", 32));

    private PrivateKeys() {}

    /**
     * Decrypts the key.
     *
     * @param der the {@code EncryptedPrivateKeyInfo}
     * @param password the password
     * @param keyAlgorithm algorithm of the key, for example {@code RSA}
     * @return the private key
     * @throws GeneralSecurityException if the password is wrong or the key is damaged
     * @throws IllegalArgumentException if the structure is not supported
     */
    public static PrivateKey decrypt(byte[] der, char[] password, String keyAlgorithm) throws GeneralSecurityException {
        Der.Tlv info = Der.parse(der).expect(Der.SEQUENCE);
        List<Der.Tlv> parts = info.children();
        if (parts.size() != 2) {
            throw new IllegalArgumentException("not an EncryptedPrivateKeyInfo");
        }
        List<Der.Tlv> algorithm = parts.get(0).expect(Der.SEQUENCE).children();
        byte[] encrypted = parts.get(1).octets();
        byte[] pkcs8 = PBES2.equals(algorithm.get(0).oid())
                ? decryptPbes2(algorithm.get(1), encrypted, password)
                : decryptWithJca(der, password);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
        try {
            return KeyFactory.getInstance(keyAlgorithm).generatePrivate(spec);
        } finally {
            java.util.Arrays.fill(pkcs8, (byte) 0);
        }
    }

    private static byte[] decryptPbes2(Der.Tlv params, byte[] encrypted, char[] password) throws GeneralSecurityException {
        List<Der.Tlv> pbes2 = params.expect(Der.SEQUENCE).children();
        List<Der.Tlv> kdf = pbes2.get(0).expect(Der.SEQUENCE).children();
        List<Der.Tlv> scheme = pbes2.get(1).expect(Der.SEQUENCE).children();
        if (!PBKDF2.equals(kdf.get(0).oid())) {
            throw new IllegalArgumentException("unsupported key derivation function " + kdf.get(0).oid());
        }
        List<Der.Tlv> kdfParams = kdf.get(1).expect(Der.SEQUENCE).children();
        byte[] salt = kdfParams.get(0).octets();
        int iterations = kdfParams.get(1).integer().intValueExact();
        String prf = "PBKDF2WithHmacSHA1";
        int keyLength = -1;
        for (Der.Tlv optional : kdfParams.subList(2, kdfParams.size())) {
            if (optional.tag() == Der.INTEGER) {
                keyLength = optional.integer().intValueExact();
            } else {
                String oid = optional.expect(Der.SEQUENCE).children().get(0).oid();
                prf = PRFS.get(oid);
                if (prf == null) {
                    throw new IllegalArgumentException("unsupported PBKDF2 pseudo-random function " + oid);
                }
            }
        }
        String cipherOid = scheme.get(0).oid();
        List<Object> cipher = CIPHERS.get(cipherOid);
        if (cipher == null) {
            throw new IllegalArgumentException("unsupported PBES2 encryption scheme " + cipherOid);
        }
        int bytes = keyLength > 0 ? keyLength : (Integer) cipher.get(2);
        byte[] iv = scheme.get(1).octets();
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        byte[] key = SecretKeyFactory.getInstance(prf).generateSecret(keySpec).getEncoded();
        keySpec.clearPassword();
        try {
            Cipher c = Cipher.getInstance((String) cipher.get(0));
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, (String) cipher.get(1)), new IvParameterSpec(iv));
            return c.doFinal(encrypted);
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
    }

    private static byte[] decryptWithJca(byte[] der, char[] password) throws GeneralSecurityException {
        try {
            EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(der);
            PBEKeySpec keySpec = new PBEKeySpec(password);
            var secret = SecretKeyFactory.getInstance(epki.getAlgName()).generateSecret(keySpec);
            keySpec.clearPassword();
            Cipher cipher = Cipher.getInstance(epki.getAlgName());
            cipher.init(Cipher.DECRYPT_MODE, secret, epki.getAlgParameters());
            return epki.getKeySpec(cipher).getEncoded();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("not an EncryptedPrivateKeyInfo: " + e.getMessage(), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("unsupported key encryption algorithm: " + e.getMessage(), e);
        }
    }
}
