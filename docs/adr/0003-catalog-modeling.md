# ADR 0003: Catalog modeling

- Status: Accepted
- Date: 2026-11-14

## Context

The SAT publishes the CFDI 4.0 catalogs (`catCFDI_V_4_<date>.xls`) and updates them often. Some
are small (c_FormaPago, c_UsoCFDI, c_RegimenFiscal); others are large (c_ClaveProdServ, c_ClaveUnidad,
c_CodigoPostal). Entries have validity dates, and some apply only to personas físicas or morales.
Parsing a CFDI issued with a newer catalog must not crash.

## Decision

1. **Small catalogs are "open enums", not Java `enum`s**: a final class with known constants,
   `of(String)` that never throws, and `isKnown()`. An unknown key is a **rule violation**, not a
   parse error.
2. Each entry carries its **validity period** (`fechaInicioVigencia` / `fechaFinVigencia`),
   **applicability** to personas físicas / morales, and the **cross-catalog relations** defined by
   the SAT (for example c_UsoCFDI ↔ c_RegimenFiscal receptor).
3. **c_TasaOCuota** is modeled with fixed values **and ranges**, as published (rango / fijo).
4. **Large catalogs** (c_ClaveProdServ, c_ClaveUnidad, c_CodigoPostal) are accessed through a
   `CatalogProvider` port in `core`. A default implementation lives in the optional module
   `cfdi4j-catalogs`. **Without a provider, the rule reports `WARNING / NOT_VERIFIED`; it never
   passes silently.**
5. **c_CodigoPostal includes the time-zone data** (huso horario, daylight-saving fields) needed to
   validate `Fecha` against `LugarExpedicion`.
6. **All catalog code is generated** by a re-runnable generator from the official SAT file. The
   generated sources are committed, carry a header with source file and version, and the version
   is recorded in `specs/SOURCES.md`. Hand edits to generated code are forbidden.

## Consequences

- Updating to a new SAT catalog is: download, record in `SOURCES.md`, regenerate, review the diff.
- `core` stays small; the large data set is opt-in.
- Open enums cannot be used in `switch` exhaustiveness checks; this is the accepted trade-off for
  forward compatibility.
