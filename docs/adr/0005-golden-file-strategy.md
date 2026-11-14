# ADR 0005: Golden-file strategy

- Status: Accepted (layer 2 amended 2026-11-14, see "Amendment")
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
   checked 2026-11-14). See the amendment below: they are downloaded at test time, not committed.
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

## Amendment (2026-11-14): layer 2 is downloaded, not committed

The CFDI 4.0 examples of `eclipxe13/CfdiUtils` contain RFCs that are not SAT test data (a
receptor RFC that is not in the SAT test list, a real PAC in the TimbreFiscalDigital, and an RFC
that appears to belong to a real person). This project never commits real RFCs or
personal data, whatever the license. CfdiUtils also publishes no expected cadena original for
CFDI 4.0.

Layer 2 therefore works like layer 3:

- The files are **downloaded at test time** in the optional profile `-Pexternal-vectors`, from a
  **pinned commit** (`f2a03dc254348a8fb9e50374ecdbe49e7dd43f9b`), and their SHA-256 is verified
  before use. They are never committed.
- `tests/assets/cfdi40-valid.xml` (SHA-256
  `66056c12f2fc9db3650421eefb762226c04d25cb2fcfb74ee050e8e44f3ff531`) is sealed with the SAT test
  CSD of EKU9003173C9. Verifying its `Sello` against the cadena original computed by cfdi4j proves
  that both tools produce the same cadena; this is used once verification exists (step 6).
