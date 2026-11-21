# ADR 0001: Build coordinates and Java baseline

- Status: Accepted
- Date: 2026-11-14

## Context

The library is published to Maven Central, which requires a verified namespace. The project must
be easy to build for contributors and reproducible in CI.

## Decision

- **groupId:** `io.github.alktrip`. Sonatype verifies `io.github.<user>` namespaces through the
  GitHub account, no domain is required.
- **artifactIds:** `cfdi4j-<module>` (`cfdi4j-core`, `cfdi4j-xml`, ...) plus `cfdi4j-bom`.
- **Java packages / JPMS modules:** rooted at `io.github.alktrip.cfdi4j`.
- **Baseline:** Java 25 (`maven.compiler.release=25`). CI runs the full test suite on Java 25.
- **Build:** Maven with the Maven Wrapper (`mvnw`) pinned to the Maven version used locally, so CI
  and contributors build with the same tool version.
- **Versioning:** Semantic Versioning; first public release `0.1.0`.

## Consequences

- Moving to an owned domain later would change every coordinate and package; this is acceptable
  only before `1.0.0`.
- Java 25 language features are available to the code.
