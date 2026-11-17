package io.github.alktrip.cfdi4j;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.catalog.CatalogProvider;
import io.github.alktrip.cfdi4j.core.internal.Messages;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.TimbreFiscalDigital;
import io.github.alktrip.cfdi4j.core.validation.CertificateStatusProvider;
import io.github.alktrip.cfdi4j.core.validation.Cfdi40Validator;
import io.github.alktrip.cfdi4j.core.validation.Severity;
import io.github.alktrip.cfdi4j.core.validation.Status;
import io.github.alktrip.cfdi4j.core.validation.TaxpayerRegistry;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import io.github.alktrip.cfdi4j.crypto.Certificates;
import io.github.alktrip.cfdi4j.crypto.Csd;
import io.github.alktrip.cfdi4j.crypto.CsdException;
import io.github.alktrip.cfdi4j.crypto.Seals;
import io.github.alktrip.cfdi4j.xml.CadenaOriginal;
import io.github.alktrip.cfdi4j.xml.CadenaOriginalTimbre;
import io.github.alktrip.cfdi4j.xml.CfdiParseException;
import io.github.alktrip.cfdi4j.xml.CfdiSchema;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates, seals and verifies CFDI 4.0 with a given configuration. Immutable and thread-safe;
 * {@link Cfdi4j} delegates to a default instance.
 */
public final class CfdiEngine {

    /** Code: the seal does not match the cadena original (official, error matrix CFDI 4.0). */
    public static final String SELLO_INVALIDO = "CFDI40102";
    /** Code: the Certificado attribute is not a valid certificate (official). */
    public static final String CERTIFICADO_INVALIDO = "CFDI40106";
    /** Code: the document cannot be read as CFDI 4.0. */
    public static final String NO_LEGIBLE = "CFDI4J-101";
    /** Code: an attribute needed to verify the seal is missing. */
    public static final String FALTA_ATRIBUTO_SELLO = "CFDI4J-102";
    /** Code: NoCertificado does not match the certificate number. */
    public static final String NO_CERTIFICADO_DISTINTO = "CFDI4J-103";
    /** Code: SelloCFD of the timbre is not the Sello of the comprobante. */
    public static final String SELLO_CFD_DISTINTO = "CFDI4J-104";
    /** Code: SelloSAT not verified because the SAT certificate is not available (warning). */
    public static final String SELLO_SAT_NO_VERIFICADO = "CFDI4J-105";
    /** Code: SelloSAT does not match the cadena original of the timbre. */
    public static final String SELLO_SAT_INVALIDO = "CFDI4J-106";

    private static final Locale SPANISH = Locale.forLanguageTag("es-MX");

    private final CfdiXml xml;
    private final CfdiSchema schema;
    private final Cfdi40Validator rules;
    private final SatCertificates satCertificates;
    private final Clock clock;
    private final Messages messages;

    private CfdiEngine(Builder b) {
        this.xml = b.xml;
        this.schema = CfdiSchema.defaults().withLocale(b.locale);
        this.rules = Cfdi40Validator.builder().clock(b.clock).locale(b.locale).catalogProvider(b.catalogProvider)
                .taxpayerRegistry(b.taxpayerRegistry).certificateStatusProvider(b.certificateStatusProvider).build();
        this.satCertificates = b.satCertificates;
        this.clock = b.clock;
        this.messages = Messages.load(name -> CfdiEngine.class.getResourceAsStream(name), "messages", b.locale);
    }

    /**
     * Starts a configuration with the defaults: {@link CfdiXml#defaults()}, no SAT certificates,
     * the system UTC clock and Spanish messages.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the clock time-dependent rules use.
     *
     * @return the clock
     */
    public Clock clock() {
        return clock;
    }

    /**
     * Validates a comprobante before sealing: its XML against the official schema (without
     * requiring {@code Sello}, {@code NoCertificado} or {@code Certificado}) and the business rules
     * of the SAT error matrix. Certificate rules run when sealing.
     *
     * @param comprobante the comprobante
     * @return every problem found
     */
    public ValidationResult validate(Comprobante comprobante) {
        Objects.requireNonNull(comprobante, "comprobante");
        return schema.validateUnsealed(xml.toBytes(comprobante)).and(rules.validate(comprobante));
    }

    /**
     * Validates a sealed or stamped CFDI document: the schema (strict) and the business rules,
     * including the certificate rules with the certificate of its {@code Certificado} attribute.
     * Use {@link #verify(byte[])} to check that it was not altered.
     *
     * @param document the document
     * @return every problem found
     */
    public ValidationResult validate(byte[] document) {
        Objects.requireNonNull(document, "document");
        ValidationResult result = schema.validate(document);
        if (result.has(CfdiSchema.XML_MAL_FORMADO)) {
            return result;
        }
        Comprobante cfdi;
        try {
            cfdi = xml.parse(document);
        } catch (CfdiParseException e) {
            return result.and(new ValidationResult(List.of(error(NO_LEGIBLE, e.location(), e.getMessage()))));
        }
        Optional<CertificateInfo> certificate = cfdi.certificado().flatMap(c -> {
            try {
                return Optional.of(Certificates.info(Certificates.fromBase64(c)));
            } catch (CsdException e) {
                return Optional.empty();
            }
        });
        return result.and(certificate.isPresent() ? rules.validate(cfdi, certificate.get()) : rules.validate(cfdi));
    }

    /**
     * Seals a comprobante: sets {@code NoCertificado} and {@code Certificado}, validates it (schema
     * and business rules, including the certificate rules for this CSD), computes the cadena
     * original, signs it and sets {@code Sello}. Warnings (checks not verified) do not prevent
     * sealing.
     *
     * @param comprobante the comprobante
     * @param csd the CSD of the emisor
     * @return the sealed comprobante
     * @throws InvalidCfdiException if validation finds errors; nothing is signed then
     */
    public SealedCfdi seal(Comprobante comprobante, Csd csd) {
        Objects.requireNonNull(comprobante, "comprobante");
        Objects.requireNonNull(csd, "csd");
        Comprobante conCertificado = comprobante.withCertificado(csd.noCertificado(), csd.certificadoBase64());
        byte[] unsealed = xml.toBytes(conCertificado);
        ValidationResult result = schema.validateUnsealed(unsealed).and(rules.validate(conCertificado, csd.info()));
        if (!result.isValid()) {
            throw new InvalidCfdiException(result);
        }
        String cadena = CadenaOriginal.defaults().of(unsealed);
        Comprobante sellado = conCertificado.withSello(csd.sign(cadena));
        return new SealedCfdi(sellado, cadena, xml.toBytes(sellado));
    }

    /**
     * Verifies a CFDI as text. See {@link #verify(byte[])}.
     *
     * @param xmlText the document
     * @return the result
     */
    public VerificationResult verify(String xmlText) {
        return verify(Objects.requireNonNull(xmlText, "xml").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verifies that a CFDI was not altered: the cadena original of the document, exactly as given,
     * must match its {@code Sello} under its {@code Certificado}, and {@code NoCertificado} must
     * be the number of that certificate. If the CFDI is stamped, {@code SelloCFD} must be its
     * {@code Sello} and {@code SelloSAT} must match the cadena original of the timbre; the latter
     * needs the SAT certificate and is reported as not verified without it.
     *
     * <p>This checks integrity only. Use validation for the content rules.
     *
     * @param document the document
     * @return the result; never throws for a bad document
     */
    public VerificationResult verify(byte[] document) {
        Objects.requireNonNull(document, "document");
        List<Violation> violations = new ArrayList<>();
        Comprobante cfdi;
        String cadena;
        try {
            cfdi = xml.parse(document);
            cadena = CadenaOriginal.defaults().of(document);
        } catch (CfdiParseException e) {
            violations.add(error(NO_LEGIBLE, e.location(), e.getMessage()));
            return new VerificationResult(new ValidationResult(violations), Optional.empty(), Optional.empty());
        }
        Optional<Comprobante> parsed = Optional.of(cfdi);
        requireAttribute("Sello", cfdi.sello(), violations);
        requireAttribute("NoCertificado", cfdi.noCertificado(), violations);
        requireAttribute("Certificado", cfdi.certificado(), violations);
        if (!violations.isEmpty()) {
            return new VerificationResult(new ValidationResult(violations), parsed, Optional.of(cadena));
        }
        X509Certificate certificate;
        String numero;
        try {
            certificate = Certificates.fromBase64(cfdi.certificado().get());
            numero = Certificates.noCertificado(certificate);
        } catch (CsdException e) {
            violations.add(error(CERTIFICADO_INVALIDO, "/cfdi:Comprobante/@Certificado", e.getMessage()));
            return new VerificationResult(new ValidationResult(violations), parsed, Optional.of(cadena));
        }
        if (!numero.equals(cfdi.noCertificado().get())) {
            violations.add(error(NO_CERTIFICADO_DISTINTO, "/cfdi:Comprobante/@NoCertificado", cfdi.noCertificado().get(), numero));
        }
        if (!Seals.verify(cadena, cfdi.sello().get(), certificate)) {
            violations.add(error(SELLO_INVALIDO, "/cfdi:Comprobante/@Sello"));
        }
        cfdi.timbreFiscalDigital().ifPresent(timbre -> verifyTimbre(cfdi, timbre, violations));
        return new VerificationResult(new ValidationResult(violations), parsed, Optional.of(cadena));
    }

    private void verifyTimbre(Comprobante cfdi, TimbreFiscalDigital timbre, List<Violation> violations) {
        String location = "/cfdi:Comprobante/cfdi:Complemento/tfd:TimbreFiscalDigital";
        if (!compact(timbre.selloCFD()).equals(compact(cfdi.sello().get()))) {
            violations.add(error(SELLO_CFD_DISTINTO, location + "/@SelloCFD"));
        }
        Optional<X509Certificate> sat = satCertificates.find(timbre.noCertificadoSAT());
        if (sat.isEmpty()) {
            violations.add(new Violation(SELLO_SAT_NO_VERIFICADO, Severity.WARNING, Status.NOT_VERIFIED, location + "/@SelloSAT",
                    messages.format(SELLO_SAT_NO_VERIFICADO, timbre.noCertificadoSAT())));
        } else if (!Seals.verify(CadenaOriginalTimbre.defaults().of(timbre), timbre.selloSAT(), sat.get())) {
            violations.add(error(SELLO_SAT_INVALIDO, location + "/@SelloSAT"));
        }
    }

    private void requireAttribute(String name, Optional<String> value, List<Violation> violations) {
        if (value.isEmpty()) {
            violations.add(error(FALTA_ATRIBUTO_SELLO, "/cfdi:Comprobante/@" + name, name));
        }
    }

    private static String compact(String base64) {
        return base64.replaceAll("\\s+", "");
    }

    private Violation error(String code, String location, Object... args) {
        return new Violation(code, Severity.ERROR, Status.FAILED, location, messages.format(code, args));
    }

    /** Configures a {@link CfdiEngine}. */
    public static final class Builder {
        private CfdiXml xml = CfdiXml.defaults();
        private SatCertificates satCertificates = SatCertificates.none();
        // Instants only; rules convert to the time zone of LugarExpedicion (c_CodigoPostal).
        private Clock clock = Clock.systemUTC();
        private Locale locale = SPANISH;
        private CatalogProvider catalogProvider;
        private TaxpayerRegistry taxpayerRegistry;
        private CertificateStatusProvider certificateStatusProvider;

        private Builder() {}

        /**
         * Sets the provider of the large SAT catalogs (c_ClaveProdServ, c_ClaveUnidad,
         * c_CodigoPostal), for example the one of {@code cfdi4j-catalogs}.
         *
         * @param catalogProvider the provider
         * @return this builder
         */
        public Builder catalogProvider(CatalogProvider catalogProvider) {
            this.catalogProvider = catalogProvider;
            return this;
        }

        /**
         * Sets the port to the SAT list of registered RFCs.
         *
         * @param taxpayerRegistry the registry
         * @return this builder
         */
        public Builder taxpayerRegistry(TaxpayerRegistry taxpayerRegistry) {
            this.taxpayerRegistry = taxpayerRegistry;
            return this;
        }

        /**
         * Sets the port to the revocation status of certificates.
         *
         * @param certificateStatusProvider the provider
         * @return this builder
         */
        public Builder certificateStatusProvider(CertificateStatusProvider certificateStatusProvider) {
            this.certificateStatusProvider = certificateStatusProvider;
            return this;
        }

        /**
         * Sets the XML reader and writer, for example one with extra complement mappers.
         *
         * @param xml the instance
         * @return this builder
         */
        public Builder xml(CfdiXml xml) {
            this.xml = Objects.requireNonNull(xml, "xml");
            return this;
        }

        /**
         * Sets where the SAT certificates of timbres are found.
         *
         * @param satCertificates the port implementation
         * @return this builder
         */
        public Builder satCertificates(SatCertificates satCertificates) {
            this.satCertificates = Objects.requireNonNull(satCertificates, "satCertificates");
            return this;
        }

        /**
         * Sets the clock for time-dependent rules; use a fixed clock in tests.
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Sets the language of the messages (Spanish by default; English is also included).
         *
         * @param locale the language
         * @return this builder
         */
        public Builder locale(Locale locale) {
            this.locale = Objects.requireNonNull(locale, "locale");
            return this;
        }

        /**
         * Creates the engine.
         *
         * @return the engine
         */
        public CfdiEngine build() {
            return new CfdiEngine(this);
        }
    }
}
