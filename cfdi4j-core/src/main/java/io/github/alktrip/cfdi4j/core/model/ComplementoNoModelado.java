package io.github.alktrip.cfdi4j.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A complement for which no model is registered, kept as XML so that nothing is lost when a CFDI
 * is parsed and written again (for example the TimbreFiscalDigital, or Carta Porte).
 *
 * @param namespace XML namespace of the complement element
 * @param xml the complement element, self-contained: it declares every namespace it uses
 * @param schemaLocation location of its XSD, as declared in {@code xsi:schemaLocation} of the
 *     comprobante, if it was declared there
 */
public record ComplementoNoModelado(String namespace, String xml, Optional<String> schemaLocation) implements Complemento {

    /** Checks that no component is null. */
    public ComplementoNoModelado {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(xml, "xml");
        Objects.requireNonNull(schemaLocation, "schemaLocation");
    }
}
