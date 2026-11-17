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

## Design measures

- XML is parsed with secure processing, DOCTYPE declarations are rejected, and SAT schemas and
  stylesheets are resolved only from bundled copies.
- Private keys are decrypted with the JDK's JCA only. Passwords are passed as `char[]`.
- Dependencies are checked by GitHub's dependency review on every pull request.
