package io.github.alktrip.cfdi4j.core.validation;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.catalog.CatalogProvider;
import io.github.alktrip.cfdi4j.core.internal.Messages;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Applies the business rules of CFDI 4.0 (the SAT error matrix, ADR 0004) to a comprobante and
 * returns every violation. Immutable and thread-safe.
 *
 * <p>Rules that need a SAT catalog, the taxpayer list or the certificate status use the configured
 * ports; without them they report "not verified" warnings. Time-dependent rules use the
 * configured {@link Clock}.
 */
public final class Cfdi40Validator {

    private final List<Rule> rules;
    private final Clock clock;
    private final CatalogProvider catalogs;
    private final TaxpayerRegistry taxpayers;
    private final CertificateStatusProvider certificateStatus;
    private final Messages messages;

    private Cfdi40Validator(Builder b) {
        this.rules = List.copyOf(b.rules);
        this.clock = b.clock;
        this.catalogs = b.catalogs;
        this.taxpayers = b.taxpayers;
        this.certificateStatus = b.certificateStatus;
        this.messages = Messages.load(name -> Cfdi40Validator.class.getResourceAsStream(name), "messages", b.locale);
    }

    /**
     * Starts a configuration with every rule, the system UTC clock, no ports and Spanish messages.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the rules applied, in order.
     *
     * @return the rules
     */
    public List<Rule> rules() {
        return rules;
    }

    /**
     * Validates a comprobante before it is sealed: certificate rules are skipped.
     *
     * @param comprobante the comprobante
     * @return every violation found
     */
    public ValidationResult validate(Comprobante comprobante) {
        return run(comprobante, null);
    }

    /**
     * Validates a comprobante together with the certificate that seals it.
     *
     * @param comprobante the comprobante
     * @param certificate the certificate data
     * @return every violation found
     */
    public ValidationResult validate(Comprobante comprobante, CertificateInfo certificate) {
        return run(comprobante, Objects.requireNonNull(certificate, "certificate"));
    }

    private ValidationResult run(Comprobante comprobante, CertificateInfo certificate) {
        ValidationContext context = new ValidationContext(comprobante, clock, catalogs, taxpayers, certificateStatus, certificate, messages);
        for (Rule rule : rules) {
            rule.check(context);
        }
        return context.result();
    }

    /** Configures a {@link Cfdi40Validator}. */
    public static final class Builder {
        private List<Rule> rules = Cfdi40Rules.all();
        private Clock clock = Clock.systemUTC();
        private CatalogProvider catalogs;
        private TaxpayerRegistry taxpayers;
        private CertificateStatusProvider certificateStatus;
        private Locale locale = Locale.forLanguageTag("es-MX");

        private Builder() {}

        /**
         * Replaces the rules, for example to add custom ones to {@link Cfdi40Rules#all()}.
         *
         * @param rules the rules
         * @return this builder
         */
        public Builder rules(List<Rule> rules) {
            this.rules = List.copyOf(rules);
            return this;
        }

        /**
         * Sets the clock; use a fixed clock in tests.
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Sets the provider of the large catalogs.
         *
         * @param catalogs the provider
         * @return this builder
         */
        public Builder catalogProvider(CatalogProvider catalogs) {
            this.catalogs = catalogs;
            return this;
        }

        /**
         * Sets the taxpayer registry.
         *
         * @param taxpayers the registry
         * @return this builder
         */
        public Builder taxpayerRegistry(TaxpayerRegistry taxpayers) {
            this.taxpayers = taxpayers;
            return this;
        }

        /**
         * Sets the certificate status provider.
         *
         * @param certificateStatus the provider
         * @return this builder
         */
        public Builder certificateStatusProvider(CertificateStatusProvider certificateStatus) {
            this.certificateStatus = certificateStatus;
            return this;
        }

        /**
         * Sets the language of the details (the official descriptions are in Spanish).
         *
         * @param locale the language
         * @return this builder
         */
        public Builder locale(Locale locale) {
            this.locale = Objects.requireNonNull(locale, "locale");
            return this;
        }

        /**
         * Creates the validator.
         *
         * @return the validator
         */
        public Cfdi40Validator build() {
            return new Cfdi40Validator(this);
        }
    }
}
