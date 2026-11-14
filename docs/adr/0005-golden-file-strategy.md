# ADR 0005: Golden-file strategy

- Status: Accepted
- Date: 2026-11-14

## Context

Correctness of the cadena original and the sello must be proven against references produced
independently of this library, without redistributing material we are not allowed to.

## Decision

Golden files in three layers, plus differential tests:

1. **Own fixtures (primary source).** Generated with the official SAT test CSDs and an
   **independent pipeline**: Saxon CLI + official `cadenaoriginal_4_0.xslt` +
   `openssl dgst -sha256 -sign`. The inputs, the commands and the outputs (XML, cadena original,
   SHA-256, sello) are committed, and the generation script is kept in the repository.
2. **Third-party fixtures under MIT.** Examples from `eclipxe13/CfdiUtils` (repository license MIT,
   checked 2026-11-14). The license of **each file** is verified before copying. Files are stored in
   `src/test/resources/third-party/` with the license notice and registered in `specs/SOURCES.md`.
3. **External, non-redistributable vectors.** The CryptoSys examples (XML + cadena original +
   SHA-256, including Pagos 2.0) are not redistributable. They are **never committed**; an optional
   Maven profile `-Pexternal-vectors` downloads them at test time.
4. **Differential tests** against `SAT-CFDI/python-satcfdi` (MIT, checked 2026-11-14), comparing
   cadena original and sello for the same inputs, in a dedicated CI job.

All tests that depend on validity periods use a **fixed `Clock`**, because SAT test CSDs expire.

## Consequences

- The default build is fully offline and reproducible.
- Layers 3 and the differential job need network and Python; they are optional locally and run in
  CI.
- The license terms of the CryptoSys material must be confirmed before writing the download profile.
