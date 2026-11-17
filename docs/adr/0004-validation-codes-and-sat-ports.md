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

## Implementation notes (2026-11-17)

- **Inventory.** `MatrizErrores` is generated from the official matrix file with the catalog
  generator (ADR 0008). The 2026-03-25 matrix numbers its last six rules `CFDI140223` to
  `CFDI140228`; they are kept exactly as published.
- **One rule per code.** `Cfdi40Rules.all()` has one rule for each of the 129 matrix codes plus
  `CFDI4J-103`; each declares a status (implemented, partial, requires external service, not
  applicable) and a note. `docs/rules-coverage.md` is generated from it.
- **Messages.** Official codes use the official description of the matrix (Spanish in every
  locale, since it is the SAT's text) followed by a localized detail with the values involved.
- **Ports.** `CatalogProvider` (in `core.catalog`), `TaxpayerRegistry` and
  `CertificateStatusProvider` (in `core.validation`). Checks repeated per concepto report "not
  verified" once per code, to avoid one warning per line.
- **Certificate rules** are reported as `CFDI40106`, the matrix code whose description covers the
  certificate conditions of the Anexo 20 ("El certificado no cumple con alguno de los valores
  permitidos"). Validity is checked against `Fecha` at every possible offset of Mexico (UTC-8 to
  UTC-5), or the offsets of `LugarExpedicion` when the catalog provider knows them; an inconclusive
  result is "not verified".
