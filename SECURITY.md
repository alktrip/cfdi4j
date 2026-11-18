# Security policy

## Supported versions

Only the latest released version receives fixes. Before `1.0.0`, fixes are released as a new minor
or patch version; there are no backports.

## Reporting a vulnerability

**Do not open a public issue.** Report it privately through
[GitHub's private vulnerability reporting](https://github.com/alktrip/cfdi4j/security/advisories/new).

Please include the affected version, a description, and if possible a minimal input that
reproduces it. Use only SAT public test data. Never send real certificates, private keys or
taxpayer data.

You will receive an answer within 7 days. A fix and an advisory are published together, and the
reporter is credited unless they prefer otherwise.

## Scope

These are in scope:

- Parsing untrusted XML: entity expansion, external entities, DOCTYPE, resource exhaustion.
- Seal and certificate verification that accepts a document it should reject.
- Leaks of private key material or passwords, for example in exceptions, logs or `toString`.
- Network access at run time. The library is designed never to make network calls.

A business rule that gives a wrong result is a bug, not a vulnerability: open a regular issue with
the SAT source.

## Using cfdi4j with untrusted documents

- **Limit the size of the input.** The XML parsers reject DOCTYPE declarations and the JDK limits
  entity expansion, but a document is read into memory whole. Reject oversized requests before
  calling cfdi4j; real CFDIs are rarely above a few megabytes.
- **`verify` proves integrity, not authenticity.** It checks the seal against the certificate
  inside the document. It does not prove that the SAT issued that certificate, and anyone can
  create a certificate with any RFC. Use `validate` for the certificate rules (RFC, CSD, validity),
  plug in a `CertificateStatusProvider` for revocation, and treat the certificate chain as not
  validated.
- `validate` and `verify` return violations for malformed documents instead of throwing. A
  regression test mutates sealed documents with hostile values (huge numbers, exponents, wrong
  encodings, foreign certificates) to keep it that way. If a rule ever fails unexpectedly, it is
  reported as error `CFDI4J-107`, never as passed.

## Design measures

- XML is parsed with secure processing, DOCTYPE declarations are rejected, and SAT schemas and
  stylesheets are resolved only from bundled copies.
- Private keys are decrypted with the JDK's JCA only. Passwords are passed as `char[]`.
- Decimal values are read only in the lexical form of the SAT schemas: no exponents.
- Encrypted keys with an absurd PBKDF2 iteration count are rejected before deriving the key.
- Dependencies are checked by GitHub's dependency review on every pull request and kept current
  by Dependabot. GitHub Actions are pinned to commit SHAs.
