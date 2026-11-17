package io.github.alktrip.cfdi4j.pagos20;

import io.github.alktrip.cfdi4j.core.IncompleteModelException;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import io.github.alktrip.cfdi4j.pagos20.internal.Aritmetica;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Builds a {@link Pagos} complement.
 *
 * <p>Like the comprobante builder (ADR 0009), it never guesses fiscal data and derives only what
 * the standard defines as a calculation, never overwriting an explicit value:
 *
 * <ul>
 *   <li>{@code TipoCambioP} = 1 and {@code EquivalenciaDR} = 1 when the currencies are MXN or equal
 *       (CRP20215, CRP20238);
 *   <li>{@code ImpSaldoInsoluto} = ImpSaldoAnt − ImpPagado (CRP20244);
 *   <li>{@code ImporteDR} = BaseDR × TasaOCuotaDR, within the Anexo 20 limits;
 *   <li>{@code ImpuestosP}: BaseP and ImporteP = the sums of BaseDR / EquivalenciaDR and
 *       ImporteDR / EquivalenciaDR, grouped like the CFDI tax summary (standard rev. B), rounded to
 *       the decimals of MonedaP as in the worked example of the Guía de llenado;
 *   <li>{@code Totales}: the sums of ImporteP, BaseP and Monto × TipoCambioP, rounded to 2
 *       decimals of MXN.
 * </ul>
 *
 * <p>{@code Monto} and the taxes of each DoctoRelacionado (BaseDR) must be given: they are the
 * money received and how it applies to the invoice, not calculations.
 */
public final class PagosBuilder {

    private final List<PagoBuilder> pagos = new ArrayList<>();
    private Pagos.Totales totales;

    PagosBuilder() {}

    /**
     * Adds a node {@code Pago}.
     *
     * @param spec fills the pago
     * @return this builder
     */
    public PagosBuilder pago(Consumer<PagoBuilder> spec) {
        PagoBuilder builder = new PagoBuilder();
        spec.accept(builder);
        pagos.add(builder);
        return this;
    }

    /**
     * Sets {@code Totales} explicitly instead of deriving it.
     *
     * @param totales the value
     * @return this builder
     */
    public PagosBuilder totales(Pagos.Totales totales) {
        this.totales = totales;
        return this;
    }

    /**
     * Builds the complement.
     *
     * @return the complement
     * @throws IncompleteModelException if required data is missing, or if a value must be derived
     *     from data that is missing (for example an EquivalenciaDR between different currencies)
     */
    public Pagos build() {
        List<String> missing = new ArrayList<>();
        if (pagos.isEmpty()) {
            missing.add("pago20:Pago");
        }
        for (int i = 0; i < pagos.size(); i++) {
            pagos.get(i).check("pago20:Pago[" + (i + 1) + "]", missing);
        }
        if (!missing.isEmpty()) {
            throw new IncompleteModelException(missing);
        }
        List<Pagos.Pago> built = new ArrayList<>();
        for (int i = 0; i < pagos.size(); i++) {
            built.add(pagos.get(i).build("pago20:Pago[" + (i + 1) + "]"));
        }
        return new Pagos(totales != null ? totales : totales(built), built);
    }

    /**
     * Derives Totales: Σ value × TipoCambioP, rounded to cents. When ImpuestosP was derived too, the
     * values are taken before rounding them to the payment currency (Σ value of the documents /
     * EquivalenciaDR), as the example of the Guía de llenado does (page 77: TotalTrasladosBaseIVA16
     * is 300,000.00, not 16,917.52 × 17.7331).
     */
    private Pagos.Totales totales(List<Pagos.Pago> pagos) {
        Totales t = new Totales();
        for (int i = 0; i < pagos.size(); i++) {
            Pagos.Pago p = pagos.get(i);
            BigDecimal tc = p.tipoCambioP().orElseThrow(() -> new IncompleteModelException(
                    "Cannot derive Totales: TipoCambioP of a Pago in " + p.monedaP().clave() + " is missing."));
            t.monto = t.monto.add(p.monto().multiply(tc));
            if (this.pagos.get(i).impuestosP == null) {
                for (Pagos.DoctoRelacionado d : p.doctoRelacionado()) {
                    if (d.impuestosDR().isEmpty()) {
                        continue;
                    }
                    BigDecimal factor = tc.divide(d.equivalenciaDR().orElseThrow(), Aritmetica.PRECISION);
                    for (Pagos.RetencionDR r : d.impuestosDR().get().retencionesDR()) {
                        t.retencion(r.impuestoDR(), r.importeDR().multiply(factor));
                    }
                    for (Pagos.TrasladoDR x : d.impuestosDR().get().trasladosDR()) {
                        t.traslado(x.impuestoDR(), x.tipoFactorDR(), x.tasaOCuotaDR(), x.baseDR().multiply(factor),
                                x.importeDR().map(v -> v.multiply(factor)));
                    }
                }
            } else {
                for (Pagos.RetencionP r : p.impuestosP().map(Pagos.ImpuestosP::retencionesP).orElse(List.of())) {
                    t.retencion(r.impuestoP(), r.importeP().multiply(tc));
                }
                for (Pagos.TrasladoP x : p.impuestosP().map(Pagos.ImpuestosP::trasladosP).orElse(List.of())) {
                    t.traslado(x.impuestoP(), x.tipoFactorP(), x.tasaOCuotaP(), x.baseP().multiply(tc), x.importeP().map(v -> v.multiply(tc)));
                }
            }
        }
        return t.build();
    }

    /** The sums of Totales in MXN, before rounding. */
    private static final class Totales {
        private final Acumulador retIva = new Acumulador();
        private final Acumulador retIsr = new Acumulador();
        private final Acumulador retIeps = new Acumulador();
        private final Acumulador base16 = new Acumulador();
        private final Acumulador imp16 = new Acumulador();
        private final Acumulador base8 = new Acumulador();
        private final Acumulador imp8 = new Acumulador();
        private final Acumulador base0 = new Acumulador();
        private final Acumulador imp0 = new Acumulador();
        private final Acumulador exento = new Acumulador();
        private BigDecimal monto = BigDecimal.ZERO;

        void retencion(Impuesto impuesto, BigDecimal importe) {
            switch (impuesto.clave()) {
                case "002" -> retIva.add(importe);
                case "001" -> retIsr.add(importe);
                case "003" -> retIeps.add(importe);
                default -> { } // not in c_Impuesto: reported by CRP20262
            }
        }

        void traslado(Impuesto impuesto, TipoFactor factor, Optional<BigDecimal> tasaOCuota, BigDecimal base, Optional<BigDecimal> importe) {
            if (!impuesto.equals(Impuesto.IVA)) {
                return;
            }
            if (factor.equals(TipoFactor.EXENTO)) {
                exento.add(base);
            } else if (factor.equals(TipoFactor.TASA) && tasaOCuota.isPresent()) {
                BigDecimal tasa = tasaOCuota.get();
                BigDecimal valor = importe.orElse(BigDecimal.ZERO);
                if (tasa.compareTo(new BigDecimal("0.16")) == 0) {
                    base16.add(base);
                    imp16.add(valor);
                } else if (tasa.compareTo(new BigDecimal("0.08")) == 0) {
                    base8.add(base);
                    imp8.add(valor);
                } else if (tasa.signum() == 0) {
                    base0.add(base);
                    imp0.add(valor);
                }
            }
        }

        Pagos.Totales build() {
            return new Pagos.Totales(retIva.total(), retIsr.total(), retIeps.total(), base16.total(), imp16.total(), base8.total(), imp8.total(),
                    base0.total(), imp0.total(), exento.total(), Aritmetica.totalMxn(monto));
        }
    }

    /** A sum that is absent until something is added. */
    private static final class Acumulador {
        private BigDecimal suma;

        void add(BigDecimal valor) {
            suma = suma == null ? valor : suma.add(valor);
        }

        Optional<BigDecimal> total() {
            return Optional.ofNullable(suma).map(Aritmetica::totalMxn);
        }
    }

    static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    static void require(Object value, String location, List<String> missing) {
        if (value == null) {
            missing.add(location);
        }
    }

    static int decimales(Moneda moneda, String location) {
        return moneda.decimales().orElseThrow(() -> new IncompleteModelException(
                "Cannot derive amounts at " + location + ": Moneda '" + moneda.clave() + "' is not in c_Moneda."));
    }

    /** Builds a node {@code Pago}. */
    public static final class PagoBuilder {
        private LocalDateTime fechaPago;
        private FormaPago formaDePagoP;
        private Moneda monedaP;
        private BigDecimal tipoCambioP;
        private BigDecimal monto;
        private String numOperacion;
        private String rfcEmisorCtaOrd;
        private String nomBancoOrdExt;
        private String ctaOrdenante;
        private String rfcEmisorCtaBen;
        private String ctaBeneficiario;
        private String tipoCadPago;
        private String certPago;
        private String cadPago;
        private String selloPago;
        private final List<DoctoRelacionadoBuilder> documentos = new ArrayList<>();
        private Pagos.ImpuestosP impuestosP;

        PagoBuilder() {}

        /**
         * Sets {@code FechaPago}; fractions of a second are dropped.
         *
         * @param fechaPago the value
         * @return this builder
         */
        public PagoBuilder fechaPago(LocalDateTime fechaPago) {
            this.fechaPago = fechaPago == null ? null : fechaPago.truncatedTo(ChronoUnit.SECONDS);
            return this;
        }

        /**
         * Sets {@code FormaDePagoP}.
         *
         * @param formaDePagoP the value
         * @return this builder
         */
        public PagoBuilder formaDePagoP(FormaPago formaDePagoP) {
            this.formaDePagoP = formaDePagoP;
            return this;
        }

        /**
         * Sets {@code MonedaP}.
         *
         * @param monedaP the value
         * @return this builder
         */
        public PagoBuilder monedaP(Moneda monedaP) {
            this.monedaP = monedaP;
            return this;
        }

        /**
         * Sets {@code TipoCambioP}; for MXN it defaults to 1.
         *
         * @param tipoCambioP the value
         * @return this builder
         */
        public PagoBuilder tipoCambioP(String tipoCambioP) {
            this.tipoCambioP = decimal(tipoCambioP);
            return this;
        }

        /**
         * Sets {@code Monto}: the amount received, in MonedaP.
         *
         * @param monto the value
         * @return this builder
         */
        public PagoBuilder monto(String monto) {
            this.monto = decimal(monto);
            return this;
        }

        /**
         * Sets {@code NumOperacion}.
         *
         * @param numOperacion the value
         * @return this builder
         */
        public PagoBuilder numOperacion(String numOperacion) {
            this.numOperacion = numOperacion;
            return this;
        }

        /**
         * Sets {@code RfcEmisorCtaOrd}.
         *
         * @param rfcEmisorCtaOrd the value
         * @return this builder
         */
        public PagoBuilder rfcEmisorCtaOrd(String rfcEmisorCtaOrd) {
            this.rfcEmisorCtaOrd = rfcEmisorCtaOrd;
            return this;
        }

        /**
         * Sets {@code NomBancoOrdExt}.
         *
         * @param nomBancoOrdExt the value
         * @return this builder
         */
        public PagoBuilder nomBancoOrdExt(String nomBancoOrdExt) {
            this.nomBancoOrdExt = nomBancoOrdExt;
            return this;
        }

        /**
         * Sets {@code CtaOrdenante}.
         *
         * @param ctaOrdenante the value
         * @return this builder
         */
        public PagoBuilder ctaOrdenante(String ctaOrdenante) {
            this.ctaOrdenante = ctaOrdenante;
            return this;
        }

        /**
         * Sets {@code RfcEmisorCtaBen}.
         *
         * @param rfcEmisorCtaBen the value
         * @return this builder
         */
        public PagoBuilder rfcEmisorCtaBen(String rfcEmisorCtaBen) {
            this.rfcEmisorCtaBen = rfcEmisorCtaBen;
            return this;
        }

        /**
         * Sets {@code CtaBeneficiario}.
         *
         * @param ctaBeneficiario the value
         * @return this builder
         */
        public PagoBuilder ctaBeneficiario(String ctaBeneficiario) {
            this.ctaBeneficiario = ctaBeneficiario;
            return this;
        }

        /**
         * Sets {@code TipoCadPago}, {@code CertPago}, {@code CadPago} and {@code SelloPago}.
         *
         * @param tipoCadPago c_TipoCadenaPago key, for example {@code 01} (SPEI)
         * @param certPago the certificate of the payment, Base64
         * @param cadPago the payment string
         * @param selloPago the payment seal, Base64
         * @return this builder
         */
        public PagoBuilder cadenaPago(String tipoCadPago, String certPago, String cadPago, String selloPago) {
            this.tipoCadPago = tipoCadPago;
            this.certPago = certPago;
            this.cadPago = cadPago;
            this.selloPago = selloPago;
            return this;
        }

        /**
         * Adds a node {@code DoctoRelacionado}.
         *
         * @param spec fills the document
         * @return this builder
         */
        public PagoBuilder doctoRelacionado(Consumer<DoctoRelacionadoBuilder> spec) {
            DoctoRelacionadoBuilder builder = new DoctoRelacionadoBuilder();
            spec.accept(builder);
            documentos.add(builder);
            return this;
        }

        /**
         * Sets {@code ImpuestosP} explicitly instead of deriving it.
         *
         * @param impuestosP the value
         * @return this builder
         */
        public PagoBuilder impuestosP(Pagos.ImpuestosP impuestosP) {
            this.impuestosP = impuestosP;
            return this;
        }

        void check(String path, List<String> missing) {
            require(fechaPago, path + "/@FechaPago", missing);
            require(formaDePagoP, path + "/@FormaDePagoP", missing);
            require(monedaP, path + "/@MonedaP", missing);
            require(monto, path + "/@Monto", missing);
            if (documentos.isEmpty()) {
                missing.add(path + "/pago20:DoctoRelacionado");
            }
            for (int i = 0; i < documentos.size(); i++) {
                documentos.get(i).check(path + "/pago20:DoctoRelacionado[" + (i + 1) + "]", missing);
            }
        }

        Pagos.Pago build(String path) {
            BigDecimal tc = tipoCambioP != null ? tipoCambioP : monedaP.equals(Moneda.MXN) ? BigDecimal.ONE : null;
            List<Pagos.DoctoRelacionado> docs = new ArrayList<>();
            for (int i = 0; i < documentos.size(); i++) {
                docs.add(documentos.get(i).build(monedaP, path + "/pago20:DoctoRelacionado[" + (i + 1) + "]"));
            }
            BigDecimal builtMonto = monto;
            Optional<Pagos.ImpuestosP> builtImpuestos = impuestosP != null ? Optional.of(impuestosP) : impuestosP(docs, path);
            return new Pagos.Pago(fechaPago, formaDePagoP, monedaP, Optional.ofNullable(tc), builtMonto, Optional.ofNullable(numOperacion),
                    Optional.ofNullable(rfcEmisorCtaOrd), Optional.ofNullable(nomBancoOrdExt), Optional.ofNullable(ctaOrdenante),
                    Optional.ofNullable(rfcEmisorCtaBen), Optional.ofNullable(ctaBeneficiario), Optional.ofNullable(tipoCadPago),
                    Optional.ofNullable(certPago), Optional.ofNullable(cadPago), Optional.ofNullable(selloPago), docs, builtImpuestos);
        }

        private static BigDecimal equivalencia(Pagos.DoctoRelacionado d, String path) {
            return d.equivalenciaDR().orElseThrow(() -> new IncompleteModelException(List.of(
                    path + "/pago20:DoctoRelacionado[@IdDocumento='" + d.idDocumento() + "']/@EquivalenciaDR")));
        }

        private Optional<Pagos.ImpuestosP> impuestosP(List<Pagos.DoctoRelacionado> docs, String path) {
            Map<Impuesto, BigDecimal> retenciones = new LinkedHashMap<>();
            Map<List<Object>, BigDecimal[]> traslados = new LinkedHashMap<>();
            Map<List<Object>, Pagos.TrasladoDR> primero = new LinkedHashMap<>();
            for (Pagos.DoctoRelacionado d : docs) {
                if (d.impuestosDR().isEmpty()) {
                    continue;
                }
                BigDecimal eq = equivalencia(d, path);
                for (Pagos.RetencionDR r : d.impuestosDR().get().retencionesDR()) {
                    retenciones.merge(r.impuestoDR(), Aritmetica.aMonedaPago(r.importeDR(), eq), BigDecimal::add);
                }
                for (Pagos.TrasladoDR t : d.impuestosDR().get().trasladosDR()) {
                    List<Object> key = java.util.Arrays.asList(t.impuestoDR(), t.tipoFactorDR(),
                            t.tasaOCuotaDR().map(BigDecimal::stripTrailingZeros).orElse(null));
                    BigDecimal[] acc = traslados.computeIfAbsent(key, k -> new BigDecimal[] {BigDecimal.ZERO, null});
                    acc[0] = acc[0].add(Aritmetica.aMonedaPago(t.baseDR(), eq));
                    if (t.importeDR().isPresent()) {
                        BigDecimal importe = Aritmetica.aMonedaPago(t.importeDR().get(), eq);
                        acc[1] = acc[1] == null ? importe : acc[1].add(importe);
                    }
                    primero.putIfAbsent(key, t);
                }
            }
            if (retenciones.isEmpty() && traslados.isEmpty()) {
                return Optional.empty();
            }
            int d = decimales(monedaP, path);
            List<Pagos.RetencionP> retP = retenciones.entrySet().stream()
                    .map(e -> new Pagos.RetencionP(e.getKey(), Aritmetica.importe(e.getValue(), d))).toList();
            List<Pagos.TrasladoP> trasP = new ArrayList<>();
            traslados.forEach((key, acc) -> {
                Pagos.TrasladoDR t = primero.get(key);
                boolean exento = t.tipoFactorDR().equals(TipoFactor.EXENTO);
                trasP.add(new Pagos.TrasladoP(Aritmetica.importe(acc[0], d), t.impuestoDR(), t.tipoFactorDR(),
                        exento ? Optional.empty() : t.tasaOCuotaDR(),
                        exento || acc[1] == null ? Optional.empty() : Optional.of(Aritmetica.importe(acc[1], d))));
            });
            return Optional.of(new Pagos.ImpuestosP(retP, trasP));
        }
    }

    /** Builds a node {@code DoctoRelacionado}. */
    public static final class DoctoRelacionadoBuilder {
        private String idDocumento;
        private String serie;
        private String folio;
        private Moneda monedaDR;
        private BigDecimal equivalenciaDR;
        private Integer numParcialidad;
        private BigDecimal impSaldoAnt;
        private BigDecimal impPagado;
        private BigDecimal impSaldoInsoluto;
        private ObjetoImp objetoImpDR;
        private final List<TrasladoSpec> traslados = new ArrayList<>();
        private final List<RetencionSpec> retenciones = new ArrayList<>();

        private record TrasladoSpec(BigDecimal base, Impuesto impuesto, TipoFactor factor, BigDecimal tasa, BigDecimal importe) {}

        private record RetencionSpec(BigDecimal base, Impuesto impuesto, TipoFactor factor, BigDecimal tasa, BigDecimal importe) {}

        DoctoRelacionadoBuilder() {}

        /**
         * Sets {@code IdDocumento}: the folio fiscal (UUID) of the paid invoice.
         *
         * @param idDocumento the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder idDocumento(String idDocumento) {
            this.idDocumento = idDocumento;
            return this;
        }

        /**
         * Sets {@code Serie}.
         *
         * @param serie the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder serie(String serie) {
            this.serie = serie;
            return this;
        }

        /**
         * Sets {@code Folio}.
         *
         * @param folio the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder folio(String folio) {
            this.folio = folio;
            return this;
        }

        /**
         * Sets {@code MonedaDR}.
         *
         * @param monedaDR the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder monedaDR(Moneda monedaDR) {
            this.monedaDR = monedaDR;
            return this;
        }

        /**
         * Sets {@code EquivalenciaDR}: units of MonedaDR per unit of MonedaP; defaults to 1 when the
         * currencies are equal.
         *
         * @param equivalenciaDR the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder equivalenciaDR(String equivalenciaDR) {
            this.equivalenciaDR = decimal(equivalenciaDR);
            return this;
        }

        /**
         * Sets {@code NumParcialidad}.
         *
         * @param numParcialidad the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder numParcialidad(int numParcialidad) {
            this.numParcialidad = numParcialidad;
            return this;
        }

        /**
         * Sets {@code ImpSaldoAnt}.
         *
         * @param impSaldoAnt the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder impSaldoAnt(String impSaldoAnt) {
            this.impSaldoAnt = decimal(impSaldoAnt);
            return this;
        }

        /**
         * Sets {@code ImpPagado}.
         *
         * @param impPagado the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder impPagado(String impPagado) {
            this.impPagado = decimal(impPagado);
            return this;
        }

        /**
         * Sets {@code ImpSaldoInsoluto} explicitly instead of ImpSaldoAnt − ImpPagado.
         *
         * @param impSaldoInsoluto the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder impSaldoInsoluto(String impSaldoInsoluto) {
            this.impSaldoInsoluto = decimal(impSaldoInsoluto);
            return this;
        }

        /**
         * Sets {@code ObjetoImpDR}.
         *
         * @param objetoImpDR the value
         * @return this builder
         */
        public DoctoRelacionadoBuilder objetoImpDR(ObjetoImp objetoImpDR) {
            this.objetoImpDR = objetoImpDR;
            return this;
        }

        /**
         * Adds a {@code TrasladoDR}; ImporteDR is derived as BaseDR × TasaOCuotaDR unless Exento.
         *
         * @param baseDR attribute {@code BaseDR}
         * @param impuestoDR attribute {@code ImpuestoDR}
         * @param tipoFactorDR attribute {@code TipoFactorDR}
         * @param tasaOCuotaDR attribute {@code TasaOCuotaDR}, {@code null} for Exento
         * @return this builder
         */
        public DoctoRelacionadoBuilder trasladoDR(String baseDR, Impuesto impuestoDR, TipoFactor tipoFactorDR, String tasaOCuotaDR) {
            return trasladoDR(baseDR, impuestoDR, tipoFactorDR, tasaOCuotaDR, null);
        }

        /**
         * Adds a {@code TrasladoDR} with an explicit ImporteDR.
         *
         * @param baseDR attribute {@code BaseDR}
         * @param impuestoDR attribute {@code ImpuestoDR}
         * @param tipoFactorDR attribute {@code TipoFactorDR}
         * @param tasaOCuotaDR attribute {@code TasaOCuotaDR}, {@code null} for Exento
         * @param importeDR attribute {@code ImporteDR}
         * @return this builder
         */
        public DoctoRelacionadoBuilder trasladoDR(String baseDR, Impuesto impuestoDR, TipoFactor tipoFactorDR, String tasaOCuotaDR, String importeDR) {
            traslados.add(new TrasladoSpec(decimal(baseDR), impuestoDR, tipoFactorDR, decimal(tasaOCuotaDR), decimal(importeDR)));
            return this;
        }

        /**
         * Adds a {@code RetencionDR}; ImporteDR is derived as BaseDR × TasaOCuotaDR.
         *
         * @param baseDR attribute {@code BaseDR}
         * @param impuestoDR attribute {@code ImpuestoDR}
         * @param tipoFactorDR attribute {@code TipoFactorDR}
         * @param tasaOCuotaDR attribute {@code TasaOCuotaDR}
         * @return this builder
         */
        public DoctoRelacionadoBuilder retencionDR(String baseDR, Impuesto impuestoDR, TipoFactor tipoFactorDR, String tasaOCuotaDR) {
            return retencionDR(baseDR, impuestoDR, tipoFactorDR, tasaOCuotaDR, null);
        }

        /**
         * Adds a {@code RetencionDR} with an explicit ImporteDR.
         *
         * @param baseDR attribute {@code BaseDR}
         * @param impuestoDR attribute {@code ImpuestoDR}
         * @param tipoFactorDR attribute {@code TipoFactorDR}
         * @param tasaOCuotaDR attribute {@code TasaOCuotaDR}
         * @param importeDR attribute {@code ImporteDR}
         * @return this builder
         */
        public DoctoRelacionadoBuilder retencionDR(String baseDR, Impuesto impuestoDR, TipoFactor tipoFactorDR, String tasaOCuotaDR, String importeDR) {
            retenciones.add(new RetencionSpec(decimal(baseDR), impuestoDR, tipoFactorDR, decimal(tasaOCuotaDR), decimal(importeDR)));
            return this;
        }

        void check(String path, List<String> missing) {
            require(idDocumento, path + "/@IdDocumento", missing);
            require(monedaDR, path + "/@MonedaDR", missing);
            require(numParcialidad, path + "/@NumParcialidad", missing);
            require(impSaldoAnt, path + "/@ImpSaldoAnt", missing);
            require(impPagado, path + "/@ImpPagado", missing);
            require(objetoImpDR, path + "/@ObjetoImpDR", missing);
            boolean trasladoIncompleto = traslados.stream().anyMatch(t -> t.base() == null || t.impuesto() == null || t.factor() == null);
            boolean retencionIncompleta = retenciones.stream()
                    .anyMatch(r -> r.base() == null || r.impuesto() == null || r.factor() == null || r.tasa() == null);
            if (trasladoIncompleto || retencionIncompleta) {
                missing.add(path + "/pago20:ImpuestosDR");
            }
        }

        Pagos.DoctoRelacionado build(Moneda monedaP, String path) {
            BigDecimal eq = equivalenciaDR != null ? equivalenciaDR : monedaDR.equals(monedaP) ? BigDecimal.ONE : null;
            BigDecimal insoluto = impSaldoInsoluto != null ? impSaldoInsoluto : impSaldoAnt.subtract(impPagado);
            List<Pagos.TrasladoDR> trasladosDR = new ArrayList<>();
            for (TrasladoSpec t : traslados) {
                BigDecimal importe = t.importe();
                if (importe == null && t.tasa() != null && !t.factor().equals(TipoFactor.EXENTO)) {
                    importe = Importes.producto(t.base(), t.tasa(), decimales(monedaDR, path));
                }
                trasladosDR.add(new Pagos.TrasladoDR(t.base(), t.impuesto(), t.factor(), Optional.ofNullable(t.tasa()), Optional.ofNullable(importe)));
            }
            List<Pagos.RetencionDR> retencionesDR = new ArrayList<>();
            for (RetencionSpec r : retenciones) {
                retencionesDR.add(new Pagos.RetencionDR(r.base(), r.impuesto(), r.factor(), r.tasa(),
                        r.importe() != null ? r.importe() : Importes.producto(r.base(), r.tasa(), decimales(monedaDR, path))));
            }
            Optional<Pagos.ImpuestosDR> impuestos = trasladosDR.isEmpty() && retencionesDR.isEmpty()
                    ? Optional.empty() : Optional.of(new Pagos.ImpuestosDR(retencionesDR, trasladosDR));
            return new Pagos.DoctoRelacionado(idDocumento, Optional.ofNullable(serie), Optional.ofNullable(folio), monedaDR,
                    Optional.ofNullable(eq), numParcialidad, impSaldoAnt, impPagado, insoluto, objetoImpDR, impuestos);
        }
    }
}
