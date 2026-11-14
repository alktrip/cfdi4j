package io.github.alktrip.cfdi4j;

import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.crypto.Csd;

/**
 * Entry point of cfdi4j, with the default configuration (see {@link CfdiEngine.Builder}).
 *
 * <pre>{@code
 * Csd csd = Csd.load(Path.of("csd.cer"), Path.of("csd.key"), password);
 * Comprobante cfdi = Comprobante.builder()...build();
 *
 * ValidationResult result = Cfdi4j.validate(cfdi);
 * String xml = Cfdi4j.seal(cfdi, csd).toXml();
 * boolean intact = Cfdi4j.verify(xml).isValid();
 * }</pre>
 */
public final class Cfdi4j {

    private static final class Default {
        private static final CfdiEngine ENGINE = CfdiEngine.builder().build();
    }

    private Cfdi4j() {}

    /**
     * Returns the default engine.
     *
     * @return the engine
     */
    public static CfdiEngine engine() {
        return Default.ENGINE;
    }

    /**
     * Starts configuring an engine.
     *
     * @return a builder
     */
    public static CfdiEngine.Builder configure() {
        return CfdiEngine.builder();
    }

    /**
     * Validates a comprobante before sealing. See {@link CfdiEngine#validate(Comprobante)}.
     *
     * @param comprobante the comprobante
     * @return every problem found
     */
    public static ValidationResult validate(Comprobante comprobante) {
        return engine().validate(comprobante);
    }

    /**
     * Seals a comprobante. See {@link CfdiEngine#seal(Comprobante, Csd)}.
     *
     * @param comprobante the comprobante
     * @param csd the CSD of the emisor
     * @return the sealed comprobante
     */
    public static SealedCfdi seal(Comprobante comprobante, Csd csd) {
        return engine().seal(comprobante, csd);
    }

    /**
     * Verifies that a CFDI was not altered. See {@link CfdiEngine#verify(byte[])}.
     *
     * @param xml the document
     * @return the result
     */
    public static VerificationResult verify(String xml) {
        return engine().verify(xml);
    }

    /**
     * Verifies that a CFDI was not altered. See {@link CfdiEngine#verify(byte[])}.
     *
     * @param xml the document
     * @return the result
     */
    public static VerificationResult verify(byte[] xml) {
        return engine().verify(xml);
    }
}
