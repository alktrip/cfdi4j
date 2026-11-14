# ADR 0002: Language conventions

- Status: Accepted
- Date: 2026-11-14

## Context

CFDI is a Mexican standard whose specification (Anexo 20), XML schema and catalogs are written in
Spanish. The library targets an international Java audience as well as Mexican developers.

## Decision

- **Domain vocabulary in Spanish, identical to Anexo 20 and the XML**: types, fields and builder
  methods use the official names (`Comprobante`, `Emisor`, `Receptor`, `Concepto`, `formaPago`,
  `usoCFDI`, `regimenFiscalReceptor`, `UsoCFDI`, ...), with the capitalization of the XML. No
  translation, no paraphrase.
  - **Only exception: non-ASCII letters.** Java identifiers use the ASCII spelling (`Año` →
    `anio`), because Error Prone rejects non-ASCII identifiers (`UnicodeInCode`) and they are hard
    to type on many keyboards. The Javadoc names the exact XML attribute.
- **Technical API in English**: infrastructure types and verbs (`builder`, `build`, `validate`,
  `seal`, `verify`, `ValidationResult`, `Violation`, `CatalogProvider`, exceptions).
- **Javadoc, commit messages and ADRs in English.**
- **Rule messages in Spanish** in `messages_es.properties`, loaded through `ResourceBundle`; an
  English `messages.properties` serves as fallback. The **stable contract is the error code**
  (`CFDI40xxx` or `CFDI4J-xxx`), never the message text, which may change between versions.
- **Documentation:** `README.md` in English, with a complete Spanish translation in `README.es.md`
  linked from its first line.

## Consequences

- Users can map every API element to the spec without guessing.
- Mixed-language identifiers are intentional and documented in `CONTRIBUTING.md`.
- Tests assert on error codes, not on message text.
