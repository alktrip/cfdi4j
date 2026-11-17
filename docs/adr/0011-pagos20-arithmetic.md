# ADR 0011: Arithmetic of the complemento de pagos 2.0

- Status: Accepted
- Date: 2026-11-17

## Context

The complemento de pagos 2.0 converts amounts between the currency of each paid document and the
currency of the payment, and then to MXN for `Totales`. The sources are:

- The standard, revision B (`Complemento_de_Pagos20_RevB.pdf`):
  - `BaseP` = Σ `BaseDR` / `EquivalenciaDR`; `ImporteP` = Σ `ImporteDR` / `EquivalenciaDR`.
  - Each total of `Totales` is the sum of the corresponding values × `TipoCambioP`.
- The Guía de llenado (`Guia_llenado_pagos.pdf`):
  - Page 33 gives the margin of `ImpPagado` converted to the payment currency, as an image.
  - Pages 76 and 77 give a worked example.

The worked example does not follow the literal formulas. A payment of 19,624.32 USD at
`TipoCambioP` 17.7331 pays an invoice of 348,000.00 MXN with IVA 16 % on a base of 300,000.00:

| Value | Literal formula | Example |
|---|---|---|
| `BaseP` | 300,000.00 / 17.7331 = 16,917.5158… | 16,917.520000 |
| `TotalTrasladosBaseIVA16` | 16,917.52 × 17.7331 = 300,000.07 | 300,000.00 |
| `MontoTotalPagos` | 19,624.32 × 17.7331 = 348,000.03 | 348,000.00 |

## Decision

1. **Converted values** (`BaseP`, `ImporteP`, retentions):
   - The builder rounds them half up to the decimals of the payment currency.
   - The rules accept any value within half a unit of its own last decimal, but no more precise
     than the currency allows.
2. **Totales:**
   - The builder derives them from the unrounded converted sums, which gives the figures of the
     example (300,000.00 and 48,000.00).
   - The rules accept a difference of up to half a unit of each value's precision × `TipoCambioP`,
     plus half a cent. This tolerance covers both the literal formula and the figures of the
     example.
3. **`Monto` is never derived.** It is the amount actually paid, and the margin rule (CRP20276)
   only checks it when the documents are in another currency.
4. **Margin of `ImpPagado`** (Guía, page 33):
   - Lower limit: truncated.
   - Upper limit: "redondear", taken as half up, as in ADR 0009.
   - The constant subtracted, read from the image, is 10^-16.
   - An `EquivalenciaDR` of 1 counts as 10 decimals when the payment mixes currencies (CRP20277).
5. **CRP20275** checks one document against its margin, using the same `ImpPagado` /
   `EquivalenciaDR` that defines the margin. It is kept for completeness but cannot fail for
   well-formed values. The meta-test of the rules lists it as an exception.

## Consequences

- Recibos built with `PagosBuilder` and the example of the Guía both pass every CRP rule. A
  property test checks this for random amounts and exchange rates.
- If the SAT publishes a stricter criterion, only `Aritmetica` and the notes `NOTA_CONVERSION` and
  `NOTA_TOTALES` of the rules need to change.

Accepted on 2026-11-17, including the tolerance of points 1 and 2, half up for "redondear" and the
constant 10^-16 of point 4, and keeping CRP20275 (point 5).
