# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - not yet released

First public version.

### Added

- `cfdi4j-core`:
  - CFDI 4.0 model and builder. Importes, SubTotal, Descuento, Total and taxes are derived with the
    rounding of the Anexo 20 (ADR 0009).
  - 15 SAT catalogs as typed open enums, generated from `catCFDI_V_4_20261106.xls`.
  - Validation rules for the 129 codes of the CFDI 4.0 error matrix (2026-03-25), with a coverage
    report.
  - Ports `CatalogProvider`, `TaxpayerRegistry` and `CertificateStatusProvider` for checks that need
    SAT data.
  - SPI `RuleSet` for rules contributed by other modules.
- `cfdi4j-xml`:
  - XML writer and parser.
  - XSD validation with the bundled SAT schemas.
  - Cadena original with the official XSLT 2.0 through Saxon-HE, fully offline.
  - Cadena original of the TimbreFiscalDigital 1.1.
- `cfdi4j-crypto`:
  - Loading of CSD `.cer`/`.key` files (PBES2 with 3DES) with the JCA only.
  - SHA256withRSA seals and certificate inspection (CSD or e.firma, RFC, serial number, validity).
- `cfdi4j-pagos20`: complemento para recepción de pagos 2.0.
  - Model and builder.
  - XML mapping.
  - Rules for the 103 codes of its error matrix (revision B).
- `cfdi4j-catalogs`: c_ClaveProdServ, c_ClaveUnidad and c_CodigoPostal, found automatically with
  `ServiceLoader`.
- `cfdi4j`: the facade `Cfdi4j` (`validate`, `seal`, `verify`) and the configurable `CfdiEngine`,
  including verification of the TimbreFiscalDigital.
- `cfdi4j-bom` and runnable examples.

[Unreleased]: https://github.com/alktrip/cfdi4j/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/alktrip/cfdi4j/releases/tag/v0.1.0
