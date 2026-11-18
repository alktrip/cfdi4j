package io.github.alktrip.cfdi4j.core.validation;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.catalog.CatalogProvider;
import io.github.alktrip.cfdi4j.core.internal.Messages;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
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

    /** Code of the error reported when a rule throws an exception instead of giving a result. */
    public static final String RULE_FAILED = "CFDI4J-107";

    private final List<Rule> rules;
    private final Map<String, MatrizErrores.Entrada> oficiales;
    private final Clock clock;
    private final CatalogProvider catalogs;
    private final TaxpayerRegistry taxpayers;
    private final CertificateStatusProvider certificateStatus;
    private final Messages messages;

    private Cfdi40Validator(Builder b) {
        List<RuleSet> sets = b.ruleSets != null ? b.ruleSets : discover();
        List<Rule> all = new ArrayList<>(b.rules);
        Map<String, MatrizErrores.Entrada> entries = new HashMap<>();
        MatrizErrores.entradas().forEach(e -> entries.put(e.codigo(), e));
        for (RuleSet set : sets) {
            all.addAll(set.rules());
            set.matriz().forEach(e -> entries.putIfAbsent(e.codigo(), e));
        }
        this.rules = List.copyOf(all);
        this.oficiales = Map.copyOf(entries);
        this.clock = b.clock;
        this.catalogs = b.catalogsSet ? b.catalogs : discoverCatalogs();
        this.taxpayers = b.taxpayers;
        this.certificateStatus = b.certificateStatus;
        this.messages = Messages.load(name -> Cfdi40Validator.class.getResourceAsStream(name), "messages", b.locale);
    }

    /** The first {@link CatalogProvider} found with {@link ServiceLoader}, by class name; {@code null} if none. */
    private static CatalogProvider discoverCatalogs() {
        return ServiceLoader.load(CatalogProvider.class).stream().map(ServiceLoader.Provider::get)
                .min(Comparator.comparing(p -> p.getClass().getName())).orElse(null);
    }

    private static List<RuleSet> discover() {
        List<RuleSet> sets = new ArrayList<>(ServiceLoader.load(RuleSet.class).stream().map(ServiceLoader.Provider::get).toList());
        sets.sort(Comparator.comparing(RuleSet::name));
        return sets;
    }

    /**
     * Starts a configuration with every rule (CFDI 4.0 and the {@link RuleSet} providers found
     * with {@link ServiceLoader}), the {@link CatalogProvider} found with {@link ServiceLoader} if
     * any (for example the one of {@code cfdi4j-catalogs}), the system UTC clock, no other ports and
     * Spanish messages.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the rules applied, in order: CFDI 4.0 first, then each rule set.
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
        ValidationContext context = new ValidationContext(comprobante, clock, catalogs, taxpayers, certificateStatus, certificate, messages,
                oficiales);
        for (Rule rule : rules) {
            try {
                rule.check(context);
            } catch (RuntimeException e) {
                // A rule that breaks on unexpected data must not hide the other results, and must
                // never count as passed.
                context.fail(RULE_FAILED, "/cfdi:Comprobante", null, rule.code(), e.getClass().getSimpleName());
            }
        }
        return context.result();
    }

    /** Configures a {@link Cfdi40Validator}. */
    public static final class Builder {
        private List<Rule> rules = Cfdi40Rules.all();
        private List<RuleSet> ruleSets;
        private Clock clock = Clock.systemUTC();
        private CatalogProvider catalogs;
        private boolean catalogsSet;
        private TaxpayerRegistry taxpayers;
        private CertificateStatusProvider certificateStatus;
        private Locale locale = Locale.forLanguageTag("es-MX");

        private Builder() {}

        /**
         * Uses these rule sets instead of the ones found with {@link ServiceLoader}.
         *
         * @param ruleSets the rule sets, applied in the given order
         * @return this builder
         */
        public Builder ruleSets(List<RuleSet> ruleSets) {
            this.ruleSets = List.copyOf(ruleSets);
            return this;
        }

        /**
         * Replaces the CFDI 4.0 rules, for example to add custom ones to {@link Cfdi40Rules#all()}.
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
         * Sets the provider of the large catalogs, replacing the one found with {@link ServiceLoader}.
         *
         * @param catalogs the provider, or {@code null} to validate without one
         * @return this builder
         */
        public Builder catalogProvider(CatalogProvider catalogs) {
            this.catalogs = catalogs;
            this.catalogsSet = true;
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
