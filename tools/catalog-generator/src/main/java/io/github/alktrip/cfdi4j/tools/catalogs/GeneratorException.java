package io.github.alktrip.cfdi4j.tools.catalogs;

/** Stops the generator because the input does not have the expected shape. */
final class GeneratorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    GeneratorException(String message) {
        super(message);
    }
}
