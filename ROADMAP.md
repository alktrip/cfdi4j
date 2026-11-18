# Roadmap

This roadmap lists what is planned after `0.1.0`. It is a direction, not a commitment to dates.
Every item follows the same rule as the rest of the project: it is implemented only from public
SAT documents.

## Next

- **Releases that follow the SAT.** Regenerate the catalogs and error matrices whenever the SAT
  publishes a new version, with the version shown in the release notes.
- **Customs catalogs.** Validate the `NumeroPedimento` of `InformacionAduanera` against c_Aduana and
  c_PatenteAduanal (CFDI40194 is partial today).
- **Differential tests.** Compare the cadena original and the seal with python-satcfdi or another open-source
  implementation in a CI job, on generated documents.
- **Messages.** Localize the XSD messages, which today come from the JDK in English.

## Complements

The base model keeps any complement as XML. These are planned as optional modules, each with its
model, XML mapping, XSD and the rules of its error matrix:

- Nómina 1.2
- Carta Porte 3.1
- Comercio Exterior 2.0
- Impuestos locales

## Later

- Retenciones e información de pagos 2.0 (a separate document type from CFDI).
- Reference adapters for the ports, for example a `TaxpayerRegistry` backed by a file of the SAT's
  list of registered RFCs.
- Benchmarks of validation and sealing throughput (JMH).

## Out of scope

- Stamping (timbrado) and cancellation: they are services of an authorized PAC.
- SAT web services (status queries, downloads, LCO, lists of RFCs). The library never uses the
  network; such data enters through the ports `TaxpayerRegistry` and `CertificateStatusProvider`.
- PDF generation, or any other printed representation of a CFDI.
- CFDI 3.3 and earlier versions.

Ideas and votes are welcome in the
[issues labeled `enhancement`](https://github.com/alktrip/cfdi4j/issues?q=is%3Aissue+label%3Aenhancement).
