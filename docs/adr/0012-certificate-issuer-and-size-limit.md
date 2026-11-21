# ADR 0012: Issuer of the certificate and size limit of documents

- Status: Accepted
- Date: 2026-11-20

## Context

`verify` checked the seal against the certificate inside the document. That proves integrity,
not authenticity: anyone can create a certificate with any RFC.

The SAT publishes, on its CSD page, the certificates of its certification authorities:
- `Cert_Prod.zip`: "Certificados para validar la cadena de confianza de los certificados de
  producción".
- `Certificados_P.zip`: the parents of the public test certificates.

Separately, a service that validates documents received from others needs a bound on their size.
Until now, that bound was only advised in `SECURITY.md`.

## Decision

1. **Bundled authorities.** The AC and ARC certificates of both archives are bundled unmodified
   in `cfdi4j-crypto`, with their hashes in `specs/SOURCES.md` (as in ADR 0007). They are exposed
   as `SatAuthorities.production()` and `SatAuthorities.test()`; `SatAuthorities.of(...)` accepts
   another set.
2. **Check.** A certificate is accepted when one of the authorities has the certificate's issuer
   as its subject and its public key verifies the certificate's signature.
   - This identifies the issuing AC. The AC certificates are trusted because they come from the
     SAT's archive, pinned by hash.
   - The validity of the CSD at `Fecha` is a CFDI rule (CFDI40106). Revocation needs a
     `CertificateStatusProvider`. Neither is repeated here.
3. **Default: not verified.**
   - Without `CfdiEngine.Builder.certificateAuthorities(...)`, `verify` reports `CFDI4J-108` as
     a warning, not verified.
   - With it, a certificate from another issuer is an error, `CFDI4J-108`.
   - It is not on by default with the production authorities, because every document sealed with
     the SAT public test CSDs would then fail. Following ADR 0004, the check is never passed
     silently.
4. **Size limit.** `validate(byte[])` and `verify` do not read documents larger than
   `maxDocumentSize`, and report `CFDI4J-109` instead.
   - The default is 20 MiB. The SAT publishes no limit for a CFDI; this value is cfdi4j's choice,
     far above ordinary invoices and configurable.

## Consequences

- `verify` can prove that the SAT issued the certificate, when production authorities are
  configured.
- New authorities published by the SAT require a release of cfdi4j, or `SatAuthorities.of(...)`.
- The OCSP responder certificates of `Cert_Prod.zip` are used only in tests: they were issued by
  AC5 and AC7, and so give real production cases for the check.
