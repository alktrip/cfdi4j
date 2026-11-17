# Architecture decision records

Each record states a decision, its context and its consequences. Records are not rewritten: later
changes are added as dated notes or amendments.

| ADR | Decision |
|-----|----------|
| [0001](0001-build-coordinates-and-baseline.md) | Coordinates `io.github.alktrip`, Java 21 baseline, Maven Wrapper, Semantic Versioning |
| [0002](0002-language-conventions.md) | Spanish domain vocabulary, English technical API, localized rule messages |
| [0003](0003-catalog-modeling.md) | Open-enum catalogs, `CatalogProvider`, generated catalog code and resources |
| [0004](0004-validation-codes-and-sat-ports.md) | Official vs. own error codes, local certificate rules, ports for SAT services |
| [0005](0005-golden-file-strategy.md) | Golden files in three layers and differential tests |
| [0006](0006-facade-and-complement-discovery.md) | Facade module; complements and rule sets discovered with `ServiceLoader` |
| [0007](0007-bundled-sat-artifacts.md) | Bundled SAT XSD/XSLT, resolved offline |
| [0008](0008-catalog-generation-and-naming.md) | Catalog generator and constant naming |
| [0009](0009-derived-totals.md) | Totals derived by the builder with the official rounding |
| [0010](0010-csd-key-decryption-with-jca.md) | CSD key decryption with the JCA only |
| [0011](0011-pagos20-arithmetic.md) | Arithmetic of the complemento de pagos 2.0 |
