package io.github.alktrip.cfdi4j.crypto.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PrivateKeysTest {

    private static final char[] PASSWORD = "secreto".toCharArray();
    private static KeyPair pair;

    @BeforeAll
    static void generate() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        pair = generator.generateKeyPair();
    }

    @Test
    void decryptsTheSatTestKeyWithPbes2AndTripleDes() throws Exception {
        byte[] der = Files.readAllBytes(Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9", "csd.key"));

        PrivateKey key = PrivateKeys.decrypt(der, "12345678a".toCharArray(), "RSA");

        assertThat(key.getAlgorithm()).isEqualTo("RSA");
    }

    /** Keys encrypted by OpenSSL (src/test/resources/keys/README.md). */
    @Test
    void decryptsPbes2KeysEncryptedByOpenSsl() throws Exception {
        Path keys = Path.of("src", "test", "resources", "keys");
        String plainSha256 = Files.readString(keys.resolve("test-only-plain.p8.sha256")).strip();

        for (String file : new String[] {"test-only-aes256-sha256.key", "test-only-des3.key"}) {
            PrivateKey key = PrivateKeys.decrypt(Files.readAllBytes(keys.resolve(file)), PASSWORD, "RSA");
            assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(key.getEncoded())))
                    .as(file).isEqualTo(plainSha256);
        }
    }

    @Test
    void decryptsOtherSchemesThroughTheJca() throws Exception {
        byte[] der = encryptWithJca("PBEWithSHA1AndDESede");

        assertThat(PrivateKeys.decrypt(der, PASSWORD, "RSA")).isEqualTo(pair.getPrivate());
    }

    @Test
    void wrongPasswordFails() throws Exception {
        byte[] der = Files.readAllBytes(Path.of("src", "test", "resources", "keys", "test-only-aes256-sha256.key"));

        assertThatThrownBy(() -> PrivateKeys.decrypt(der, "otra".toCharArray(), "RSA")).isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    void decodesOptionalKeyLengthAndRejectsUnsupportedSchemes() {
        byte[] salt = new byte[8];
        byte[] iv = new byte[8];
        byte[] withKeyLength = epki(pbes2(pbkdf2(salt, 2048, 24, null), "1.2.840.113549.3.7", iv), new byte[16]);
        byte[] unknownCipher = epki(pbes2(pbkdf2(salt, 2048, -1, null), "1.2.3.4", iv), new byte[16]);
        byte[] unknownPrf = epki(pbes2(pbkdf2(salt, 2048, -1, "1.2.3.5"), "1.2.840.113549.3.7", iv), new byte[16]);
        byte[] unknownKdf = epki(pbes2(seq(oid("1.2.3.6"), seq()), "1.2.840.113549.3.7", iv), new byte[16]);

        assertThatThrownBy(() -> PrivateKeys.decrypt(withKeyLength, PASSWORD, "RSA")).isInstanceOf(GeneralSecurityException.class);
        assertThatThrownBy(() -> PrivateKeys.decrypt(unknownCipher, PASSWORD, "RSA")).hasMessageContaining("unsupported PBES2 encryption scheme 1.2.3.4");
        assertThatThrownBy(() -> PrivateKeys.decrypt(unknownPrf, PASSWORD, "RSA")).hasMessageContaining("pseudo-random function 1.2.3.5");
        assertThatThrownBy(() -> PrivateKeys.decrypt(unknownKdf, PASSWORD, "RSA")).hasMessageContaining("key derivation function 1.2.3.6");
        assertThatThrownBy(() -> PrivateKeys.decrypt(seq(seq(oid("1.2.840.113549.1.5.13"))), PASSWORD, "RSA"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not an EncryptedPrivateKeyInfo");
        assertThatThrownBy(() -> PrivateKeys.decrypt(epki(seq(oid("1.2.3.7")), new byte[8]), PASSWORD, "RSA"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unsupported key encryption algorithm");
        assertThatThrownBy(() -> PrivateKeys.decrypt(epki(seq(tlv(Der.OCTET_STRING, new byte[1])), new byte[8]), PASSWORD, "RSA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] encryptWithJca(String algorithm) throws GeneralSecurityException, java.io.IOException {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        PBEParameterSpec spec = new PBEParameterSpec(salt, 1000);
        var secret = SecretKeyFactory.getInstance(algorithm).generateSecret(new PBEKeySpec(PASSWORD));
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, secret, spec);
        byte[] encrypted = cipher.doFinal(pair.getPrivate().getEncoded());
        AlgorithmParameters params = cipher.getParameters();
        return new EncryptedPrivateKeyInfo(params, encrypted).getEncoded();
    }

    // --- tiny DER writer for crafted structures ---

    static byte[] epki(byte[] algorithm, byte[] encrypted) {
        return seq(algorithm, tlv(Der.OCTET_STRING, encrypted));
    }

    static byte[] pbes2(byte[] kdf, String cipherOid, byte[] iv) {
        return seq(oid("1.2.840.113549.1.5.13"), seq(kdf, seq(oid(cipherOid), tlv(Der.OCTET_STRING, iv))));
    }

    static byte[] pbkdf2(byte[] salt, int iterations, int keyLength, String prfOid) {
        ByteArrayOutputStream params = new ByteArrayOutputStream();
        params.writeBytes(tlv(Der.OCTET_STRING, salt));
        params.writeBytes(tlv(Der.INTEGER, BigInteger.valueOf(iterations).toByteArray()));
        if (keyLength > 0) {
            params.writeBytes(tlv(Der.INTEGER, BigInteger.valueOf(keyLength).toByteArray()));
        }
        if (prfOid != null) {
            params.writeBytes(seq(oid(prfOid), tlv(Der.NULL, new byte[0])));
        }
        return seq(oid("1.2.840.113549.1.5.12"), tlv(Der.SEQUENCE, params.toByteArray()));
    }

    static byte[] seq(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return tlv(Der.SEQUENCE, out.toByteArray());
    }

    static byte[] oid(String dotted) {
        String[] arcs = dotted.split("\\.", -1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        base128(out, Long.parseLong(arcs[0]) * 40 + Long.parseLong(arcs[1]));
        for (int i = 2; i < arcs.length; i++) {
            base128(out, Long.parseLong(arcs[i]));
        }
        return tlv(Der.OID, out.toByteArray());
    }

    private static void base128(ByteArrayOutputStream out, long value) {
        int groups = Math.max(1, (64 - Long.numberOfLeadingZeros(value) + 6) / 7);
        for (int i = groups - 1; i >= 0; i--) {
            int b = (int) ((value >>> (7 * i)) & 0x7f);
            out.write(i == 0 ? b : b | 0x80);
        }
    }

    static byte[] tlv(int tag, byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (value.length < 0x80) {
            out.write(value.length);
        } else if (value.length < 0x100) {
            out.write(0x81);
            out.write(value.length);
        } else {
            out.write(0x82);
            out.write(value.length >> 8);
            out.write(value.length & 0xff);
        }
        out.writeBytes(value);
        return out.toByteArray();
    }
}
