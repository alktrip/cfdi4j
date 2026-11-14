# ADR 0004: Validation codes and ports for SAT services

- Status: Accepted
- Date: 2026-11-14

## Context

The SAT publishes an error matrix for CFDI 4.0 (`MatrizDeErrores_CFDI_v40_<date>.xls`) with
codes `CFDI40xxx`. Some checks can be done locally; others need SAT data (LCO, certificate
revocation, taxpayer registry). Validity checks depend on the current time.

## Decision

1. **Codes:**
   - Official `CFDI40xxx` codes are used **only** for rules that implement that matrix entry.
   - Checks that the library adds on its own use the prefix **`CFDI4J-xxx`**.
   - Codes with the official prefix are never invented.
2. **Certificate rules implemented locally** (no SAT access needed):
   - The RFC in the certificate matches `Emisor/@Rfc`.
   - `Fecha` falls within the certificate validity period.
   - The certificate is a **CSD, not a FIEL (e.firma)**.
   - `NoCertificado` matches the certificate serial number.
3. **SAT-dependent checks** (LCO, revocation, RFC status, receptor name / régimen / código postal
   match) are **not implemented**, but are modeled behind ports:
   - `TaxpayerRegistry`
   - `CertificateStatusProvider`

   Without an implementation, the rule reports **`WARNING / NOT_VERIFIED`**.
4. **Time:** every time-dependent rule receives a `java.time.Clock`; no rule calls `now()` directly.
5. **Coverage report:** `docs/rules-coverage.md` is generated from the code and lists every matrix
   code with its status: *implemented*, *partial*, *requires external service* or *not applicable*.
   CI fails if the committed report is stale.

## Consequences

- Users can rely on codes as a stable contract and see exactly which matrix entries are covered.
- Integrators (for example a PAC gateway) can plug real SAT-backed implementations into the ports.
- The exact criterion to distinguish CSD from FIEL must be taken from a public SAT source and cited
  in the rule.
