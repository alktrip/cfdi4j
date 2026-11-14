# cfdi4j — Phase 1 implementation plan

> Working document. Before the first push, the relevant parts move to
> `ROADMAP.md` and `docs/adr/`, and this file is deleted.

Status: **approved 2026-11-14**. Date: 2026-11-14.

Decisions already taken are recorded as ADRs:

| ADR | Decision |
|-----|----------|
| [0001](adr/0001-build-coordinates-and-baseline.md) | Coordinates `io.github.alktrip`, Java 21 baseline, Maven Wrapper |
| [0002](adr/0002-language-conventions.md) | Spanish domain vocabulary, English technical API, localized rule messages |
| [0003](adr/0003-catalog-modeling.md) | Open-enum catalogs, `CatalogProvider`, generated catalog code |
| [0004](adr/0004-validation-codes-and-sat-ports.md) | Official vs. own error codes, local certificate rules, ports for SAT services |
| [0005](adr/0005-golden-file-strategy.md) | Three-layer golden files and differential tests |
| [0006](adr/0006-facade-and-complement-discovery.md) | Facade module and complement discovery |
| [0007](adr/0007-bundled-sat-artifacts.md) | Bundled SAT XSD/XSLT, offline resolution |
| [0008](adr/0008-catalog-generation-and-naming.md) | Catalog generator and constant naming |
| [0009](adr/0009-derived-totals.md) | Derived totals in the builder |

---

## 1. Module structure

```
cfdi4j/                         (parent POM, Maven Wrapper)
├── cfdi4j-bom                  BOM for consumers
├── cfdi4j-core                 model, builders, catalogs (small), rules, errors, ports. No XML, no I/O.
├── cfdi4j-xml                  serializer, parser, XSD validation, cadena original (Saxon-HE)
├── cfdi4j-crypto               CSD loading, signing, sello verification, certificate inspection (JCA)
├── cfdi4j-pagos20              Complemento de Pagos 2.0: model, XML mapping, rules
├── cfdi4j                      facade: Cfdi4j.validate/seal/verify (depends on core, xml, crypto)
├── cfdi4j-catalogs             optional CatalogProvider with c_ClaveProdServ, c_ClaveUnidad, c_CodigoPostal
├── cfdi4j-examples             runnable examples (not published)
└── tools/catalog-generator     build-time generator: SAT .xls → Java sources (not published)
specs/                          official artifacts + SOURCES.md (URL, version, retrieval date, SHA-256)
docs/adr/                       architecture decision records
```

Dependency graph (arrows = "depends on"):

```
cfdi4j (facade) ──► cfdi4j-xml ──► cfdi4j-core
        └─────────► cfdi4j-crypto ─► cfdi4j-core
cfdi4j-pagos20 ──► cfdi4j-core, cfdi4j-xml
cfdi4j-catalogs ─► cfdi4j-core
```

- JPMS module names: `io.github.alktrip.cfdi4j.core`, `.xml`, `.crypto`, `.pagos20`, `.catalogs`, `io.github.alktrip.cfdi4j`.
- Public packages are exported; implementation lives under `...internal` and is never exported.
- Generated catalog sources are committed (builds never need network) and carry a header with the
  source file name and version.
- Certificate rules live in `core` against a `CertificateInfo` value (RFC, serial, validity, key
  usage) that `crypto` produces, so `core` stays free of I/O and X.509 parsing.

### External dependencies (latest stable checked on 2026-11-14 in Maven Central)

| Dependency | Version | Scope | Why |
|------------|---------|-------|-----|
| `net.sf.saxon:Saxon-HE` | 13.1 | `xml` | The official cadena original XSLT is version 2.0; the JDK only ships XSLT 1.0. Bytecode targets Java 17, MPL-2.0, automatic module `net.sf.saxon`. |
| `org.junit:junit-bom` | 6.1.3 | test | |
| `net.jqwik:jqwik` | 1.10.2 | test | Compatibility with the JUnit 6 platform to be checked in step 0. |
| `org.assertj:assertj-core` | 3.27.7 | test | 4.0.0-M1 is a milestone, not used. |
| `org.apache.poi:poi` | (check at step 0) | generator only | Reading the SAT `.xls` catalogs and error matrix. Never a runtime dependency. |

Plugins: compiler 3.16.0 (4.0.0 is still beta), surefire 3.6.1, javadoc 3.12.0, gpg 3.2.8,
jacoco 0.8.16, central-publishing 0.12.0, wrapper 3.3.4, Error Prone 2.52.0. Versions are
re-checked when each one is actually added.

`cfdi4j-crypto` uses **JCA only**. If decrypting the SAT test `.key` (encrypted PKCS#8) turns out
not to be supported by the JDK provider, that is reported and decided in an ADR before adding any
dependency (see risk R2).

---

## 2. Public API sketch

```java
// Loading credentials (crypto)
Csd csd = Csd.load(Path.of("csd.cer"), Path.of("csd.key"), password);      // char[] password
Csd csd = Csd.load(cerBytes, keyBytes, password);

// Building (core)
Comprobante cfdi = Cfdi40.builder()
    .serie("A").folio("123")
    .fecha(LocalDateTime.of(2026, 11, 14, 10, 0))
    .formaPago(FormaPago.TRANSFERENCIA_ELECTRONICA)                         // open-enum constants
    .metodoPago(MetodoPago.PUE)
    .lugarExpedicion("72000")
    .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE")
                  .regimenFiscal(RegimenFiscal.GENERAL_LEY_PERSONAS_MORALES))
    .receptor(r -> r.rfc("XAXX010101000").nombre("PUBLICO EN GENERAL")
                    .domicilioFiscalReceptor("72000")
                    .regimenFiscalReceptor(RegimenFiscal.SIN_OBLIGACIONES_FISCALES)
                    .usoCfdi(UsoCfdi.SIN_EFECTOS_FISCALES))
    .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT")
                    .descripcion("Servicio").valorUnitario("100.00")
                    .objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
    .build();                                     // SubTotal, Total and Impuestos totals derived

// Facade
ValidationResult result = Cfdi4j.validate(cfdi);                 // XSD + business rules
SealedCfdi sealed      = Cfdi4j.seal(cfdi, csd);                 // throws if result has errors
String xml             = sealed.toXml();
String cadena          = sealed.cadenaOriginal();
VerificationResult v   = Cfdi4j.verify(xml);                     // v.isValid(), v.failures()

// Configurable instance (thread-safe, reusable; static facade delegates to a default one)
Cfdi4j engine = Cfdi4j.configure()
    .clock(Clock.systemUTC())
    .catalogProvider(SatCatalogs.provider())                     // optional module
    .taxpayerRegistry(myRegistry)                                // optional port
    .certificateStatusProvider(myOcspLikeChecker)                // optional port
    .locale(Locale.forLanguageTag("es-MX"))
    .build();
```

Catalog constant names above are illustrative; real names are keys or curated short names
(ADR 0008). RFCs and names are SAT test data, to be confirmed from the official CSD zip.

Key types:

- `Comprobante`, `Emisor`, `Receptor`, `Concepto`, `Impuestos`, `Traslado`, `Retencion`,
  `CfdiRelacionados`, `InformacionGlobal`, `ACuentaTerceros`, `Parte`, `Complemento`:
  immutable, names identical to Anexo 20 / XSD.
- Amounts: `BigDecimal` that **preserves the scale read or written** (`"100.00"` stays `100.00`),
  so round-trip and cadena original are exact. `String` overloads in builders parse with
  `new BigDecimal(String)`, never `double`.
- `ValidationResult` → `List<Violation>`; `Violation(code, severity, status, path, args)` with
  `message(Locale)`. Severity: `ERROR`, `WARNING`. Status: `FAILED`, `NOT_VERIFIED`.
- Exceptions (all unchecked, rooted at `Cfdi4jException`): `CfdiParseException`,
  `CsdException`, `SealingException`, `InvalidCfdiException` (carries the `ValidationResult`).
- Complements: `Complemento` SPI in `core`; XML mapping SPI in `xml`; `pagos20` provides both.
  Discovery via JPMS `provides/uses` (ServiceLoader), overridable with explicit registration
  (ADR 0006).
- `TimbreFiscalDigital` is parsed and verified (`Cfdi4j.verify` reports both CFDI sello and TFD
  `SelloCFD` consistency; TFD sello verification needs the PAC/SAT certificate supplied by caller).

---

## 3. Official sources to obtain

All reachable from this environment on 2026-11-14. Each will be stored under `specs/` with URL,
version/date published and SHA-256 in `specs/SOURCES.md`.

| Artifact | Source |
|----------|--------|
| Anexo 20 (PDF) and Guía de llenado CFDI 4.0 | `omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Anexo20_2022.pdf`, `Anexo_20_Guia_de_llenado_CFDI.pdf` |
| Guías de llenado CFDI global, FAQ CFDI 4.0 | same page (`Guia_llenado_CFDI_global.pdf`, `PregFrecCFDIVer4_0.pdf`) |
| Catálogos CFDI 4.0 | `documentos/catCFDI_V_4_20261106.xls` |
| Matriz de errores CFDI 4.0 | `documentos/MatrizDeErrores_CFDI_v40_20260325.xls` |
| XSD CFDI 4.0 + imports | `sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd`, `cfd/catalogos/catCFDI.xsd`, `cfd/tipoDatos/tdCFDI/tdCFDI.xsd` |
| Cadena original 4.0 | `cfd/4/cadenaoriginal_4_0/cadenaoriginal_4_0.xslt` (XSLT **2.0**) + `cfd/2/cadenaoriginal_2_0/utilerias.xslt` + **33 `xsl:include`s** of complement XSLTs |
| Pagos 2.0 | `cfd/Pagos/Pagos20.xsd`, `cfd/Pagos/Pagos20.xslt`, catálogos de Pagos, Guía de llenado de Pagos (URL to locate: the old `complemento_pagos.htm` returns 404) |
| TFD 1.1 | `cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd`, `cadenaoriginal_TFD_1_1.xslt` |
| CSD de prueba | `omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Certificados_de_Prueba.zip` (linked from `certificado_sello_digital.htm`); the RFC `EKU9003173C9` and password are confirmed from its contents, not from memory |

---

## 4. Rule implementation strategy

1. **Inventory first.** The generator reads the error matrix `.xls` and emits `MatrizErroresCfdi40`
   (code, official description, matrix version). This is the authoritative list of `CFDI40xxx`.
2. **One rule = one class** implementing `Rule` (`code()`, `check(ValidationContext)`), stateless
   and thread-safe. Rules for the base CFDI live in `core`; Pagos 2.0 rules in `pagos20`.
3. `ValidationContext` gives the rule: the model, the `Clock`, the `CatalogProvider`,
   `TaxpayerRegistry`, `CertificateStatusProvider` (all optional) and `CertificateInfo` when sealed.
4. **Codes:** official `CFDI40xxx` only for rules that implement that matrix entry; own checks use
   `CFDI4J-xxx`. Messages in `messages_es.properties` (`messages.properties` = English fallback).
5. **External dependencies:** rules needing a missing port emit `WARNING / NOT_VERIFIED`, never pass
   silently.
6. **Arithmetic rules** (decimals per `c_Moneda`, lower/upper limits for Base × TasaOCuota, totals)
   are implemented only from the formulas written in the Anexo 20 / Guía de llenado and cited in the
   Javadoc of each rule. Rounding mode is taken from the spec text, not assumed.
7. **Coverage report:** a rule registry lists every rule with its status; a test writes
   `docs/rules-coverage.md` (implemented / partial / requires external service / not applicable) for
   every code in the matrix, and CI fails if the committed file is out of date.
8. Order of implementation inside the "business rules" step: structural/format rules → catalog rules
   → arithmetic and tax rules → cross-field rules → certificate rules → port-backed rules.

---

## 5. Test strategy

| Kind | Where | Tooling |
|------|-------|---------|
| Rule unit tests: ≥1 valid and ≥1 invalid case per rule | core, pagos20 | JUnit 6, AssertJ |
| Property-based: amounts, scale, rounding, tax totals | core | jqwik |
| Round-trip: build → serialize → parse → equals | xml, pagos20 | JUnit |
| XSD: generated XML validates; invalid fixtures rejected | xml | JDK `javax.xml.validation` |
| Golden files (own, layer 1) | xml, crypto | fixtures produced by an independent pipeline: Saxon CLI + official XSLT + `openssl dgst -sha256 -sign` |
| Golden files (third-party MIT, layer 2) | xml, crypto | `eclipxe13/CfdiUtils` (MIT, verified via GitHub API) per-file license check |
| External vectors (layer 3) | `-Pexternal-vectors` | CryptoSys examples, downloaded at test time, never committed |
| Differential | `-Pdifferential` CI job | `SAT-CFDI/python-satcfdi` (MIT): compare cadena original and sello |
| Signature: sign → verify; tamper one attribute → fails | crypto, facade | JUnit |
| Time-dependent rules and expired test CSD | all | fixed `Clock` |
| Coverage ≥ 90 % (instructions and branches) on core, xml, crypto | CI | JaCoCo `check` |
| Static analysis | CI | Error Prone (compile time) |
| README quick start | CI | an `examples` test compiles and runs the exact README snippet |

---

## 6. Incremental steps

| # | Step | Exit criterion |
|---|------|----------------|
| 0 | Scaffolding: parent POM, wrapper, modules, JPMS, CI (21 + 25), JaCoCo, Error Prone, `specs/` downloaded + `SOURCES.md` | `./mvnw verify` green on both JDKs in CI |
| 1 | Core model + builder + generated small catalogs | unit + property tests |
| 2 | XML serialization and parsing | round-trip tests |
| 3 | XSD validation (bundled, resolver, no network) | valid/invalid fixtures |
| 4 | Cadena original (Saxon, bundled XSLTs, compiled once) | golden layer 1 + 2 |
| 5 | Crypto: CSD load, sign | golden sello matches `openssl` |
| 6 | Verification (sello + TFD) | tamper tests |
| 7 | Business rules + `rules-coverage.md` | per-rule tests |
| 8 | Pagos 2.0 | round-trip, golden, rules |
| 9 | Docs, `cfdi4j-catalogs`, release workflow, issues | DoD checklist |

A short report is delivered at the end of each step.

---

## 7. Risks

| # | Risk | Mitigation |
|---|------|-----------|
| R1 | `cadenaoriginal_4_0.xslt` includes 33 complement XSLTs by absolute `http://` URL. | Bundle the official files **unmodified** and resolve them with a `URIResolver` that maps SAT URLs to classpath resources; fail on any unmapped URL (never fetch at runtime). SHA-256 of each file in `SOURCES.md`. |
| R2 | The SAT `.key` encryption scheme may not be supported by the JDK's `EncryptedPrivateKeyInfo`. | Verify with the official test CSD in step 5. If unsupported, ADR before adding any dependency. |
| R3 | SAT test CSDs expire. | Fixed `Clock` in tests; certificate validity rule tested with both in-range and out-of-range clocks. |
| R4 | Redistribution terms of SAT XSD/XSLT/catalog files are not stated explicitly. | Files bundled unmodified with source and hash (ADR 0007). Precedent: `phpcfdi/resources-sat-xml` redistributes them under Unlicense. |
| R5 | SAT republishes catalogs and the matrix frequently (current: 2026-11-06 and 2026-03-25). | Generator is re-runnable; version recorded in `SOURCES.md` and exposed at runtime. |
| R6 | Arithmetic rules (limits, rounding) are easy to get subtly wrong. | Implement only from cited formulas; property tests; differential tests against python-satcfdi. |
| R7 | Saxon-HE is an automatic module (no `module-info`) with transitive deps (xmlresolver). | Check `jlink`/JPMS behaviour in step 0; document in README. |
| R8 | The cadena original depends on the exact XML (scale of amounts, whitespace normalization). | Scale-preserving model; golden tests on edge cases (trailing zeros, special characters, `&`, `|`). |

---

## 8. Answered questions (2026-11-14)

| # | Answer | ADR |
|---|--------|-----|
| Q1 | Facade module `cfdi4j` added; `cfdi4j-core` must remain usable on its own. | [0006](adr/0006-facade-and-complement-discovery.md) |
| Q2 | Complements discovered via `ServiceLoader`, explicit registration allowed; deterministic load order; clear error on duplicated namespaces. | [0006](adr/0006-facade-and-complement-discovery.md) |
| Q3 | Official XSD/XSLT bundled unmodified; absolute SAT URLs resolved to jar resources via `URIResolver`/`LSResourceResolver`; no network at runtime; test with network blocked; hash test against `SOURCES.md`. | [0007](adr/0007-bundled-sat-artifacts.md) |
| Q4 | Generator in Java, in a non-published module that does not run on every build; generated code committed; CI checks regeneration produces no diff. | [0008](adr/0008-catalog-generation-and-naming.md) |
| Q5 | Builder derives totals using exactly the official rounding, currency decimals and tolerances; explicit values are never overwritten, mismatches are violations; property-based tests. | [0009](adr/0009-derived-totals.md) |
| Q6 | Constants named by key when readable (Moneda, Pais), otherwise short curated names in `catalog-names.yaml`; generator fails on an unnamed new key; Javadoc carries key + official description; `of(String)` always available. | [0008](adr/0008-catalog-generation-and-naming.md) |
