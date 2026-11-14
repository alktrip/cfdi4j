package io.github.alktrip.cfdi4j.xml.internal;

/** Namespaces and schema locations of CFDI 4.0 (Anexo 20; {@code cfdv40.xsd}). */
public final class Namespaces {

    /** Namespace of CFDI 4.0. */
    public static final String CFDI = "http://www.sat.gob.mx/cfd/4";

    /** Namespace of CFDI 3.3, recognized only to report that it is not supported. */
    public static final String CFDI_33 = "http://www.sat.gob.mx/cfd/3";

    /** Location of the CFDI 4.0 schema, as published by the SAT. */
    public static final String CFDI_SCHEMA_LOCATION = "http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd";

    /** XML Schema instance namespace. */
    public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private Namespaces() {}
}
