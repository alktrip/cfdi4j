package io.github.alktrip.cfdi4j.core.model;

import java.util.Objects;

/**
 * Node {@code cfdi:Addenda}: free content outside the fiscal data, kept as XML.
 *
 * @param contenido the child elements of {@code cfdi:Addenda}, each self-contained (declaring every
 *     namespace it uses); may be empty
 */
public record Addenda(String contenido) {

    /** Checks that no component is null. */
    public Addenda {
        Objects.requireNonNull(contenido, "contenido");
    }
}
