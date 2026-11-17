package io.github.alktrip.cfdi4j.core;

import java.util.List;

/**
 * Thrown when a builder cannot produce a model because required data is missing.
 *
 * <p>Builders report every missing item at once. The items are XPath-like locations such as
 * {@code cfdi:Conceptos/cfdi:Concepto[2]/@ObjetoImp}.
 */
public final class IncompleteModelException extends Cfdi4jException {

    private static final long serialVersionUID = 1L;

    /** Locations of the missing values. */
    @SuppressWarnings("serial") // List.copyOf returns a serializable list
    private final List<String> missing;

    /**
     * Creates the exception.
     *
     * @param missing locations of the missing items, never empty
     */
    public IncompleteModelException(List<String> missing) {
        super("Missing required data: " + String.join(", ", missing));
        this.missing = List.copyOf(missing);
    }

    /**
     * Creates the exception for a single problem that is not a missing field.
     *
     * @param message description of the problem
     */
    public IncompleteModelException(String message) {
        super(message);
        this.missing = List.of();
    }

    /**
     * Returns the locations of the missing items.
     *
     * @return the locations, empty when the problem is not a missing field
     */
    public List<String> missing() {
        return missing;
    }
}
