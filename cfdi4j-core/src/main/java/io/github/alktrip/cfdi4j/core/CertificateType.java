package io.github.alktrip.cfdi4j.core;

/** Kind of SAT certificate. */
public enum CertificateType {
    /** Certificado de Sello Digital: the only kind that may seal a CFDI. */
    CSD,
    /** e.firma (formerly FIEL): the taxpayer's advanced electronic signature; it must not seal a CFDI. */
    FIEL,
    /** Neither pattern matches. */
    UNKNOWN
}
