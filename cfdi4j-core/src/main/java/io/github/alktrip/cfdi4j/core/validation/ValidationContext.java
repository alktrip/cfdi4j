package io.github.alktrip.cfdi4j.core.validation;

import io.github.alktrip.cfdi4j.core.CertificateInfo;
import io.github.alktrip.cfdi4j.core.catalog.CatalogProvider;
import io.github.alktrip.cfdi4j.core.internal.Messages;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a {@link Rule} sees: the comprobante, the clock, the optional ports and the certificate,
 * and where it reports violations. One context serves one validation; it is not thread-safe.
 */
public final class ValidationContext {

    private final Comprobante comprobante;
    private final Clock clock;
    private final CatalogProvider catalogs;
    private final TaxpayerRegistry taxpayers;
    private final CertificateStatusProvider certificateStatus;
    private final CertificateInfo certificate;
    private final Messages messages;
    private final List<Violation> violations = new ArrayList<>();
    private final Set<String> notVerifiedOnce = new HashSet<>();
    private final Map<String, MatrizErrores.Entrada> oficiales;

    ValidationContext(Comprobante comprobante, Clock clock, CatalogProvider catalogs, TaxpayerRegistry taxpayers,
            CertificateStatusProvider certificateStatus, CertificateInfo certificate, Messages messages,
            Map<String, MatrizErrores.Entrada> oficiales) {
        this.comprobante = Objects.requireNonNull(comprobante, "comprobante");
        this.clock = clock;
        this.catalogs = catalogs;
        this.taxpayers = taxpayers;
        this.certificateStatus = certificateStatus;
        this.certificate = certificate;
        this.messages = messages;
        this.oficiales = oficiales;
    }

    /**
     * Returns the comprobante being validated.
     *
     * @return the comprobante
     */
    public Comprobante comprobante() {
        return comprobante;
    }

    /**
     * Returns the clock of the validation.
     *
     * @return the clock
     */
    public Clock clock() {
        return clock;
    }

    /**
     * Returns the catalog provider, if configured.
     *
     * @return the provider
     */
    public Optional<CatalogProvider> catalogs() {
        return Optional.ofNullable(catalogs);
    }

    /**
     * Returns the taxpayer registry, if configured.
     *
     * @return the registry
     */
    public Optional<TaxpayerRegistry> taxpayers() {
        return Optional.ofNullable(taxpayers);
    }

    /**
     * Returns the certificate status provider, if configured.
     *
     * @return the provider
     */
    public Optional<CertificateStatusProvider> certificateStatus() {
        return Optional.ofNullable(certificateStatus);
    }

    /**
     * Returns the certificate that seals (or will seal) the comprobante, if known.
     *
     * @return the certificate data
     */
    public Optional<CertificateInfo> certificate() {
        return Optional.ofNullable(certificate);
    }

    /**
     * Reports a failed check. The message is the official description of the code (or the cfdi4j
     * message for {@code CFDI4J} codes) followed by a detail.
     *
     * @param code the code
     * @param location XPath-like location
     * @param detailKey key of the detail message, or {@code null} for none
     * @param args arguments of the detail message
     */
    public void fail(String code, String location, String detailKey, Object... args) {
        violations.add(new Violation(code, Severity.ERROR, Status.FAILED, location, message(code, detailKey, args)));
    }

    /**
     * Reports a check that could not run.
     *
     * @param code the code
     * @param location XPath-like location
     * @param detailKey key of the reason, for example {@code no.verificado.catalogo}
     * @param args arguments of the reason
     */
    public void notVerified(String code, String location, String detailKey, Object... args) {
        violations.add(new Violation(code, Severity.WARNING, Status.NOT_VERIFIED, location, message(code, detailKey, args)));
    }

    /**
     * Reports a check that could not run, once per code: used for checks repeated on every
     * concepto, to avoid one warning per line.
     *
     * @param code the code
     * @param location location of the first occurrence
     * @param detailKey key of the reason
     * @param args arguments of the reason
     */
    public void notVerifiedOnce(String code, String location, String detailKey, Object... args) {
        if (notVerifiedOnce.add(code)) {
            notVerified(code, location, detailKey, args);
        }
    }

    ValidationResult result() {
        return new ValidationResult(violations);
    }

    private String message(String code, String detailKey, Object... args) {
        Optional<MatrizErrores.Entrada> oficial = Optional.ofNullable(oficiales.get(code));
        String base = oficial.isPresent() ? oficial.get().mensaje() : messages.format(code, args);
        return detailKey == null ? base : base + " " + messages.format(detailKey, args);
    }
}
