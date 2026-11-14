# ADR 0008: Catalog generation and constant naming

- Status: Accepted
- Date: 2026-11-14
- Complements: [ADR 0003](0003-catalog-modeling.md)

## Context

ADR 0003 decided that catalog code is generated from the official SAT file. This ADR fixes where
the generator lives, how it is run and how constants are named. Deriving names from the official
description produces very long identifiers and breaks the API every time the SAT corrects a
description.

## Decision

### Generator

1. Written in **Java**, in a build-only module (`tools/catalog-generator`) that is **not published**
   and **does not run in the default build**; it is activated by a dedicated Maven profile.
2. The generated sources are **committed**.
3. CI runs the generator and fails if regeneration produces any diff.

### Naming

1. When the key is already readable, the **key is the constant name** (for example `Moneda.MXN`,
   `Pais.MEX`).
2. Otherwise, a **short curated name** is taken from `catalog-names.yaml`
   (for example `FormaPago` key `03` → a curated name such as `TRANSFERENCIA`).
3. The generator **fails if a key has no assigned name**, so a new SAT key always gets a deliberate,
   reviewed name.
4. Every constant's Javadoc carries the **official key and the official description**.
5. `of(String)` is always available, so code never needs a constant to handle a key.

## Consequences

- Constant names are stable across SAT wording changes; only new keys need a review.
- `catalog-names.yaml` becomes part of the public API surface and is reviewed like code.
