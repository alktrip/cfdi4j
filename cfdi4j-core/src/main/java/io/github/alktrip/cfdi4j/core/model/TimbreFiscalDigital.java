package io.github.alktrip.cfdi4j.core.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Complemento TimbreFiscalDigital 1.1 ({@code TimbreFiscalDigitalv11.xsd}): the stamp a PAC adds
 * to a sealed CFDI. Stamping is out of scope for this library; the timbre can be read and verified.
 *
 * @param version attribute {@code Version}, "1.1"
 * @param uuid attribute {@code UUID}, the folio fiscal
 * @param fechaTimbrado attribute {@code FechaTimbrado}
 * @param rfcProvCertif attribute {@code RfcProvCertif}, the RFC of the PAC
 * @param leyenda attribute {@code Leyenda}
 * @param selloCFD attribute {@code SelloCFD}: a copy of the {@code Sello} of the comprobante
 * @param noCertificadoSAT attribute {@code NoCertificadoSAT}
 * @param selloSAT attribute {@code SelloSAT}
 */
public record TimbreFiscalDigital(
        String version,
        String uuid,
        LocalDateTime fechaTimbrado,
        String rfcProvCertif,
        Optional<String> leyenda,
        String selloCFD,
        String noCertificadoSAT,
        String selloSAT) implements Complemento {

    /** Namespace of the complement. */
    public static final String NAMESPACE = "http://www.sat.gob.mx/TimbreFiscalDigital";

    /** Checks that no component is null. */
    public TimbreFiscalDigital {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(fechaTimbrado, "fechaTimbrado");
        Objects.requireNonNull(rfcProvCertif, "rfcProvCertif");
        Objects.requireNonNull(leyenda, "leyenda");
        Objects.requireNonNull(selloCFD, "selloCFD");
        Objects.requireNonNull(noCertificadoSAT, "noCertificadoSAT");
        Objects.requireNonNull(selloSAT, "selloSAT");
    }

    @Override
    public String namespace() {
        return NAMESPACE;
    }
}
