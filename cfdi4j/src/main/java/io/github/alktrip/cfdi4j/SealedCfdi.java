package io.github.alktrip.cfdi4j;

import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** A sealed comprobante with its XML and the cadena original that was signed. */
public final class SealedCfdi {

    private final Comprobante comprobante;
    private final String cadenaOriginal;
    private final byte[] xml;

    SealedCfdi(Comprobante comprobante, String cadenaOriginal, byte[] xml) {
        this.comprobante = Objects.requireNonNull(comprobante);
        this.cadenaOriginal = Objects.requireNonNull(cadenaOriginal);
        this.xml = xml.clone();
    }

    /**
     * Returns the comprobante with {@code NoCertificado}, {@code Certificado} and {@code Sello}.
     *
     * @return the comprobante
     */
    public Comprobante comprobante() {
        return comprobante;
    }

    /**
     * Returns the cadena original that was signed.
     *
     * @return the cadena
     */
    public String cadenaOriginal() {
        return cadenaOriginal;
    }

    /**
     * Returns the {@code Sello}.
     *
     * @return the seal, Base64
     */
    public String sello() {
        return comprobante.sello().orElseThrow();
    }

    /**
     * Returns the XML, UTF-8 encoded.
     *
     * @return a copy of the bytes
     */
    public byte[] toBytes() {
        return xml.clone();
    }

    /**
     * Returns the XML.
     *
     * @return the XML text
     */
    public String toXml() {
        return new String(xml, StandardCharsets.UTF_8);
    }
}
