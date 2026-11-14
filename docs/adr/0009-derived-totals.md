# ADR 0009: Derived totals in the builder

- Status: Accepted
- Date: 2026-11-14

## Context

`SubTotal`, `Descuento`, `Total` and the `Impuestos` totals are derivable from the concepts. Asking
users to compute them by hand is error-prone; silently recomputing values a user supplied would hide
their errors.

## Decision

1. The builder **derives** totals that the user did not set, applying **exactly** the official
   rules for rounding, decimals per currency (`c_Moneda`) and tolerances, as written in the Anexo 20
   and its Guía de llenado. Each formula cites its source in Javadoc.
2. **An explicit value is never overwritten.** If it does not match the official calculation within
   the official tolerance, validation reports a violation.
3. The behaviour is covered with **property-based tests** (jqwik): derived values always pass the
   validation rules, and any explicit value outside the tolerance is always reported.

## Consequences

- The quick-start case needs no arithmetic from the user.
- Parsing an existing CFDI never alters its amounts.
