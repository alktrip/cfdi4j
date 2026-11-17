# ADR 0006: Facade module and complement discovery

- Status: Accepted
- Date: 2026-11-14

## Context

The target usage (`Cfdi4j.validate`, `Cfdi4j.seal`, `Cfdi4j.verify`) needs XML processing and
cryptography at the same time, but `cfdi4j-core` must stay free of XML and I/O. Complements
(Pagos 2.0 now, others later) add model types, XML mappings and rules that the base modules must
not know about at compile time.

## Decision

1. A **facade module `cfdi4j`** (JPMS `io.github.alktrip.cfdi4j`) depends on `core`, `xml` and
   `crypto` and exposes `Cfdi4j`. It contains orchestration only, no business logic.
2. **`cfdi4j-core` remains usable on its own**: model, builders, catalogs and rules work without
   any other cfdi4j module on the class or module path. A test in `core` runs with only `core`
   available.
3. **Complements are discovered with `java.util.ServiceLoader`** (`uses`/`provides` in
   `module-info.java`, `META-INF/services` for class-path users). Explicit registration through the
   configuration builder is also supported and replaces discovery when used.
4. **Deterministic order:** discovered providers are sorted by their XML namespace URI, never by
   class-path order.
5. **Duplicated namespaces are an error:** two providers for the same namespace fail fast with an
   exception that names both provider classes.

## Consequences

- Adding a complement is adding a jar; no change to base modules.
- Users who only need the model and rules depend on `cfdi4j-core` alone.

## Implementation notes (2026-11-17)

- **Rules of a complement.** A module contributes its rules through the SPI
  `io.github.alktrip.cfdi4j.core.validation.RuleSet` (`name()`, `rules()`, `matriz()`), discovered
  with `ServiceLoader` and applied after the CFDI 4.0 rules in order of `name()`.
  `Cfdi40Validator.Builder.ruleSets(List)` replaces discovery. `matriz()` gives the official
  messages of the codes the set reports, so the core does not need to know other matrices.
- **Pagos 2.0** (`cfdi4j-pagos20`) is the first provider of both SPIs: `ComplementoXml` for the
  node `pago20:Pagos` and `RuleSet` for the CRP matrix (`docs/rules-coverage-pagos20.md`).
