# ADR 0010: CSD key decryption with the JCA only

- Status: Accepted
- Date: 2026-11-14

## Context

A CSD `.key` file is an encrypted PKCS#8 `EncryptedPrivateKeyInfo` (DER). The SAT public test
keys (`Certificados_de_Prueba.zip`) use **PBES2** (RFC 8018) with **PBKDF2** (HMAC-SHA1, 2048
iterations) and **des-ede3-cbc**.

`javax.crypto.EncryptedPrivateKeyInfo` cannot decrypt them on JDK 21: its PBES2 parameter parser
accepts only AES encryption schemes and fails with
`PBE parameter parsing error: expecting the object identifier for AES cipher` (checked 2026-11-14
with OpenJDK 21.0.12). The usual alternative, Bouncy Castle, is a large dependency for a few lines
of logic.

## Decision

- Decode the PBES2 parameters with a minimal internal DER reader, then run the scheme with
  standard JCA primitives: `SecretKeyFactory` `PBKDF2WithHmacSHA1/256/384/512` and `Cipher`
  `DESede/CBC/PKCS5Padding` or `AES/CBC/PKCS5Padding` (128, 192, 256).
- Delegate any non-PBES2 scheme to `EncryptedPrivateKeyInfo`.
- Reject unknown OIDs with a clear message; never guess.
- No Bouncy Castle: `cfdi4j-crypto` depends only on `cfdi4j-core`.
- Clear the derived key, the decrypted PKCS#8 bytes and the `PBEKeySpec` password after use; never
  keep the caller's password.

## Consequences

- Verified against the SAT test key and against keys encrypted by OpenSSL with AES-256/HMAC-SHA256
  and 3DES (`src/test/resources/keys`).
- The DER reader is intentionally small (single-byte tags, lengths up to 3 bytes) and is also used
  to read the `x500UniqueIdentifier` (RFC) from certificate subjects.
- If a future SAT key uses another scheme, the error names its OID, and support is one table entry.
