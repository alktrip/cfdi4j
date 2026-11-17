# ADR 0007: Bundled SAT XSD/XSLT and offline resolution

- Status: Accepted
- Date: 2026-11-14

## Context

The official CFDI 4.0 schema imports other schemas, and `cadenaoriginal_4_0.xslt` (XSLT 2.0)
includes `utilerias.xslt` plus 33 complement stylesheets, all by absolute `http://www.sat.gob.mx/...`
URLs. Fetching them at runtime would make the library slow, fragile and dependent on SAT
availability, and would allow the result to change without a release.

## Decision

1. The official files are **bundled in the jar unmodified** (byte-for-byte as downloaded). Their URL,
   publication version, retrieval date and SHA-256 are recorded in `specs/SOURCES.md`.
2. Absolute SAT URLs in `xsl:include`/`xsl:import` and `xs:import`/`xs:include` are resolved to jar
   resources by a `URIResolver` (XSLT, Saxon) and an `LSResourceResolver` (XSD, JAXP), backed by an
   explicit URL → resource map.
3. **No network access at runtime.** An unmapped URL is an error, never a fallback download.
   External entity and DTD loading are disabled on every parser and factory.
4. Tests:
   - A test compiles the schema and the stylesheets with **network access blocked** for the JVM,
     proving nothing is fetched.
   - A test computes the SHA-256 of every bundled file and compares it with `specs/SOURCES.md`.

## Consequences

- Updating an official file is a deliberate change: download, update `SOURCES.md`, release.
- The jar carries all 33 complement XSLTs even though only some complements are modeled, because
  the official stylesheet does not compile without them.
- The SAT does not state redistribution terms for these files. They are bundled unmodified, with
  their origin and hash, and the [NOTICE](../../NOTICE) names them. Other open-source projects
  redistribute the same files (for example `phpcfdi/resources-sat-xml`, under the Unlicense). If
  the SAT ever objects, the files can move to a separate artifact without changing the API.
- Saxon-HE is an automatic module (no `module-info`). `cfdi4j-xml` requires it as
  `net.sf.saxon` and suppresses the `requires-automatic` warning. `jlink` images that include
  cfdi4j must add Saxon to the class path or use a tool that handles automatic modules.
