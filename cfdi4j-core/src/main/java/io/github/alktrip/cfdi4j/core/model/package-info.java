/**
 * Immutable model of CFDI 4.0 (Anexo 20). Type, attribute and node names follow the XSD
 * {@code cfdv40.xsd} exactly (ADR 0002).
 *
 * <p>Optional attributes are {@link java.util.Optional}; repeated nodes are immutable lists, empty
 * when the node is absent. Amounts are {@link java.math.BigDecimal} values whose scale is kept
 * exactly as written or read, because the scale is part of the cadena original.
 */
package io.github.alktrip.cfdi4j.core.model;
