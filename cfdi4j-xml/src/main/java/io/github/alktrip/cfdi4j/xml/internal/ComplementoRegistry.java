package io.github.alktrip.cfdi4j.xml.internal;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.model.Complemento;
import io.github.alktrip.cfdi4j.xml.spi.ComplementoXml;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The complement mappers in use, keyed by namespace in a deterministic (sorted) order. Two mappers
 * for the same namespace are an error (ADR 0006).
 */
public final class ComplementoRegistry {

    private final Map<String, ComplementoXml> byNamespace;
    private final Map<Class<? extends Complemento>, ComplementoXml> byType;

    /**
     * Creates the registry.
     *
     * @param mappers the mappers, in any order
     * @throws Cfdi4jException if two mappers declare the same namespace or the same type
     */
    public ComplementoRegistry(Collection<? extends ComplementoXml> mappers) {
        Map<String, ComplementoXml> namespaces = new TreeMap<>();
        Map<Class<? extends Complemento>, ComplementoXml> types = new HashMap<>();
        for (ComplementoXml mapper : mappers) {
            ComplementoXml previous = namespaces.putIfAbsent(mapper.namespace(), mapper);
            if (previous != null) {
                throw new Cfdi4jException("Two complement mappers for namespace " + mapper.namespace() + ": "
                        + previous.getClass().getName() + " and " + mapper.getClass().getName());
            }
            ComplementoXml previousType = types.putIfAbsent(mapper.type(), mapper);
            if (previousType != null) {
                throw new Cfdi4jException("Two complement mappers for type " + mapper.type().getName() + ": "
                        + previousType.getClass().getName() + " and " + mapper.getClass().getName());
            }
        }
        this.byNamespace = Collections.unmodifiableMap(namespaces);
        this.byType = Map.copyOf(types);
    }

    /** Returns the mappers sorted by namespace. */
    public Collection<ComplementoXml> all() {
        return byNamespace.values();
    }

    /** Returns the mapper for a namespace. */
    public Optional<ComplementoXml> forNamespace(String namespace) {
        return Optional.ofNullable(byNamespace.get(namespace));
    }

    /** Returns the mapper that writes a complement. */
    public Optional<ComplementoXml> forType(Class<? extends Complemento> type) {
        return Optional.ofNullable(byType.get(type));
    }
}
