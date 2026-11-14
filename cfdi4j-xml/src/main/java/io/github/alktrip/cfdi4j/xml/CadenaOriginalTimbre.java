package io.github.alktrip.cfdi4j.xml;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.core.model.TimbreFiscalDigital;
import io.github.alktrip.cfdi4j.xml.internal.SecureXml;
import io.github.alktrip.cfdi4j.xml.internal.TimbreFiscalDigitalXml;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Produces the cadena original of a TimbreFiscalDigital 1.1 with the official SAT stylesheet
 * {@code cadenaoriginal_TFD_1_1.xslt} (bundled, compiled once; thread-safe). The PAC seals this
 * cadena: the result is checked against {@code SelloSAT} with the certificate
 * {@code NoCertificadoSAT}.
 */
public final class CadenaOriginalTimbre {

    private static final class Shared {
        private static final CadenaOriginalTimbre INSTANCE = new CadenaOriginalTimbre(CadenaOriginal.compile(CadenaOriginal.TFD_XSLT_URL));
    }

    private static final TimbreFiscalDigitalXml MAPPER = new TimbreFiscalDigitalXml();

    private final CadenaOriginal stylesheet;

    private CadenaOriginalTimbre(CadenaOriginal stylesheet) {
        this.stylesheet = stylesheet;
    }

    /**
     * Returns the shared instance.
     *
     * @return the instance
     */
    public static CadenaOriginalTimbre defaults() {
        return Shared.INSTANCE;
    }

    /**
     * Returns the cadena original of a timbre.
     *
     * @param timbre the timbre
     * @return the cadena, starting and ending with {@code ||}
     */
    public String of(TimbreFiscalDigital timbre) {
        Objects.requireNonNull(timbre, "timbre");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XMLStreamWriter writer = SecureXml.outputFactory().createXMLStreamWriter(out, "UTF-8");
            MAPPER.writeDocument(timbre, writer);
            writer.close();
        } catch (XMLStreamException e) {
            throw new Cfdi4jException("Cannot write the TimbreFiscalDigital", e);
        }
        return stylesheet.of(out.toByteArray());
    }
}
