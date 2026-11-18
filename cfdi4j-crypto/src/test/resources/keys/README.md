# Test-only keys

Throwaway RSA keys generated on 2026-11-14 for the PBES2 decryption tests. They belong to no
taxpayer and protect nothing. The unencrypted key was not kept; only its SHA-256 is.

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -outform DER -out plain.p8
sha256sum plain.p8 | cut -d' ' -f1 > test-only-plain.p8.sha256
openssl pkcs8 -topk8 -inform DER -in plain.p8 -outform DER -v2 aes-256-cbc -v2prf hmacWithSHA256 -iter 2048 -passout pass:secreto -out test-only-aes256-sha256.key
openssl pkcs8 -topk8 -inform DER -in plain.p8 -outform DER -v2 des3 -iter 2048 -passout pass:secreto -out test-only-des3.key
rm plain.p8
```

`../ec-prueba.cer` is a self-signed certificate with an EC (P-256) key, `CN=PRUEBA EC`, used to
check that a seal cannot verify under a certificate whose key is not RSA. Its private key was
not kept.

```sh
openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes -keyout ec.key -out ec.pem -days 36500 -subj "/CN=PRUEBA EC"
openssl x509 -in ec.pem -outform DER -out ec-prueba.cer
rm ec.key ec.pem
```
