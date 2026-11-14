package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.IncompleteModelException;
import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Meses;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.Pais;
import io.github.alktrip.cfdi4j.core.catalog.Periodicidad;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.catalog.TipoRelacion;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import io.github.alktrip.cfdi4j.core.model.CfdiRelacionados.CfdiRelacionado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Builds a {@link Comprobante}.
 *
 * <p>The builder never guesses fiscal data: every attribute the XSD requires must be set, and
 * {@link #build()} reports all missing ones at once in an {@link IncompleteModelException}. It only
 * derives arithmetic (ADR 0009):
 *
 * <ul>
 *   <li>{@code Concepto/@Importe} = Cantidad × ValorUnitario;
 *   <li>the {@code Base} of a concept tax, when not set, = Importe − Descuento of the concept;
 *   <li>the {@code Importe} of a concept tax = Base × TasaOCuota (not for TipoFactor Exento);
 *   <li>{@code SubTotal}, {@code Descuento}, the {@code Impuestos} summary and {@code Total},
 *       rounded at the end to the decimals of the currency.
 * </ul>
 *
 * <p>A value set explicitly is never overwritten, even if it does not match the calculation;
 * validation reports the mismatch. Builders are mutable and not thread-safe.
 */
public final class ComprobanteBuilder {

    private String serie;
    private String folio;
    private LocalDateTime fecha;
    private FormaPago formaPago;
    private String condicionesDePago;
    private BigDecimal subTotal;
    private BigDecimal descuento;
    private Moneda moneda;
    private BigDecimal tipoCambio;
    private BigDecimal total;
    private TipoDeComprobante tipoDeComprobante;
    private Exportacion exportacion;
    private MetodoPago metodoPago;
    private String lugarExpedicion;
    private String confirmacion;
    private InformacionGlobal informacionGlobal;
    private final List<CfdiRelacionados> cfdiRelacionados = new ArrayList<>();
    private EmisorBuilder emisor;
    private ReceptorBuilder receptor;
    private final List<ConceptoBuilder> conceptos = new ArrayList<>();
    private Impuestos impuestos;
    private final List<Complemento> complemento = new ArrayList<>();
    private Addenda addenda;

    ComprobanteBuilder() {}

    /**
     * Sets {@code Serie}.
     *
     * @param serie the value
     * @return this builder
     */
    public ComprobanteBuilder serie(String serie) {
        this.serie = serie;
        return this;
    }

    /**
     * Sets {@code Folio}.
     *
     * @param folio the value
     * @return this builder
     */
    public ComprobanteBuilder folio(String folio) {
        this.folio = folio;
        return this;
    }

    /**
     * Sets {@code Fecha}: the local date and time where the comprobante is issued. Fractions of a
     * second are dropped, because the XSD format has second precision.
     *
     * @param fecha the value
     * @return this builder
     */
    public ComprobanteBuilder fecha(LocalDateTime fecha) {
        this.fecha = fecha == null ? null : fecha.truncatedTo(ChronoUnit.SECONDS);
        return this;
    }

    /**
     * Sets {@code FormaPago}.
     *
     * @param formaPago the value
     * @return this builder
     */
    public ComprobanteBuilder formaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
        return this;
    }

    /**
     * Sets {@code CondicionesDePago}.
     *
     * @param condicionesDePago the value
     * @return this builder
     */
    public ComprobanteBuilder condicionesDePago(String condicionesDePago) {
        this.condicionesDePago = condicionesDePago;
        return this;
    }

    /**
     * Sets {@code SubTotal} explicitly instead of deriving it.
     *
     * @param subTotal the value
     * @return this builder
     */
    public ComprobanteBuilder subTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
        return this;
    }

    /**
     * Sets {@code SubTotal} explicitly, keeping the decimals as written.
     *
     * @param subTotal the value, for example {@code "100.00"}
     * @return this builder
     */
    public ComprobanteBuilder subTotal(String subTotal) {
        return subTotal(decimal(subTotal));
    }

    /**
     * Sets {@code Descuento} explicitly instead of deriving it.
     *
     * @param descuento the value
     * @return this builder
     */
    public ComprobanteBuilder descuento(BigDecimal descuento) {
        this.descuento = descuento;
        return this;
    }

    /**
     * Sets {@code Descuento} explicitly, keeping the decimals as written.
     *
     * @param descuento the value
     * @return this builder
     */
    public ComprobanteBuilder descuento(String descuento) {
        return descuento(decimal(descuento));
    }

    /**
     * Sets {@code Moneda}.
     *
     * @param moneda the value
     * @return this builder
     */
    public ComprobanteBuilder moneda(Moneda moneda) {
        this.moneda = moneda;
        return this;
    }

    /**
     * Sets {@code TipoCambio}.
     *
     * @param tipoCambio the value
     * @return this builder
     */
    public ComprobanteBuilder tipoCambio(BigDecimal tipoCambio) {
        this.tipoCambio = tipoCambio;
        return this;
    }

    /**
     * Sets {@code TipoCambio}, keeping the decimals as written.
     *
     * @param tipoCambio the value
     * @return this builder
     */
    public ComprobanteBuilder tipoCambio(String tipoCambio) {
        return tipoCambio(decimal(tipoCambio));
    }

    /**
     * Sets {@code Total} explicitly instead of deriving it. Needed, for example, when local taxes
     * (complemento de impuestos locales) are part of the total.
     *
     * @param total the value
     * @return this builder
     */
    public ComprobanteBuilder total(BigDecimal total) {
        this.total = total;
        return this;
    }

    /**
     * Sets {@code Total} explicitly, keeping the decimals as written.
     *
     * @param total the value
     * @return this builder
     */
    public ComprobanteBuilder total(String total) {
        return total(decimal(total));
    }

    /**
     * Sets {@code TipoDeComprobante}.
     *
     * @param tipoDeComprobante the value
     * @return this builder
     */
    public ComprobanteBuilder tipoDeComprobante(TipoDeComprobante tipoDeComprobante) {
        this.tipoDeComprobante = tipoDeComprobante;
        return this;
    }

    /**
     * Sets {@code Exportacion}.
     *
     * @param exportacion the value
     * @return this builder
     */
    public ComprobanteBuilder exportacion(Exportacion exportacion) {
        this.exportacion = exportacion;
        return this;
    }

    /**
     * Sets {@code MetodoPago}.
     *
     * @param metodoPago the value
     * @return this builder
     */
    public ComprobanteBuilder metodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
        return this;
    }

    /**
     * Sets {@code LugarExpedicion}: the postal code of the place of issue.
     *
     * @param lugarExpedicion the value
     * @return this builder
     */
    public ComprobanteBuilder lugarExpedicion(String lugarExpedicion) {
        this.lugarExpedicion = lugarExpedicion;
        return this;
    }

    /**
     * Sets {@code Confirmacion}.
     *
     * @param confirmacion the value
     * @return this builder
     */
    public ComprobanteBuilder confirmacion(String confirmacion) {
        this.confirmacion = confirmacion;
        return this;
    }

    /**
     * Sets the node {@code InformacionGlobal}.
     *
     * @param periodicidad attribute {@code Periodicidad}
     * @param meses attribute {@code Meses}
     * @param anio attribute {@code Año}
     * @return this builder
     */
    public ComprobanteBuilder informacionGlobal(Periodicidad periodicidad, Meses meses, int anio) {
        this.informacionGlobal = new InformacionGlobal(periodicidad, meses, anio);
        return this;
    }

    /**
     * Adds a node {@code CfdiRelacionados}.
     *
     * @param tipoRelacion attribute {@code TipoRelacion}
     * @param uuids folios fiscales of the related CFDI
     * @return this builder
     */
    public ComprobanteBuilder cfdiRelacionados(TipoRelacion tipoRelacion, String... uuids) {
        cfdiRelacionados.add(new CfdiRelacionados(tipoRelacion, Arrays.stream(uuids).map(CfdiRelacionado::new).toList()));
        return this;
    }

    /**
     * Sets the node {@code Emisor}.
     *
     * @param spec fills the emisor
     * @return this builder
     */
    public ComprobanteBuilder emisor(Consumer<EmisorBuilder> spec) {
        EmisorBuilder builder = new EmisorBuilder();
        spec.accept(builder);
        this.emisor = builder;
        return this;
    }

    /**
     * Sets the node {@code Receptor}.
     *
     * @param spec fills the receptor
     * @return this builder
     */
    public ComprobanteBuilder receptor(Consumer<ReceptorBuilder> spec) {
        ReceptorBuilder builder = new ReceptorBuilder();
        spec.accept(builder);
        this.receptor = builder;
        return this;
    }

    /**
     * Adds a node {@code Concepto}.
     *
     * @param spec fills the concepto
     * @return this builder
     */
    public ComprobanteBuilder concepto(Consumer<ConceptoBuilder> spec) {
        ConceptoBuilder builder = new ConceptoBuilder();
        spec.accept(builder);
        conceptos.add(builder);
        return this;
    }

    /**
     * Sets the tax summary node {@code Impuestos} explicitly instead of deriving it.
     *
     * @param impuestos the value
     * @return this builder
     */
    public ComprobanteBuilder impuestos(Impuestos impuestos) {
        this.impuestos = impuestos;
        return this;
    }

    /**
     * Adds a complement.
     *
     * @param complemento the complement
     * @return this builder
     */
    public ComprobanteBuilder complemento(Complemento complemento) {
        this.complemento.add(Objects.requireNonNull(complemento, "complemento"));
        return this;
    }

    /**
     * Sets the node {@code Addenda}.
     *
     * @param addenda the value
     * @return this builder
     */
    public ComprobanteBuilder addenda(Addenda addenda) {
        this.addenda = addenda;
        return this;
    }

    /**
     * Builds the comprobante, deriving the amounts that were not set.
     *
     * @return the comprobante
     * @throws IncompleteModelException if required data is missing, or if amounts must be derived
     *     but the currency is not in c_Moneda
     */
    public Comprobante build() {
        List<String> missing = new ArrayList<>();
        require(fecha, "@Fecha", missing);
        require(moneda, "@Moneda", missing);
        require(tipoDeComprobante, "@TipoDeComprobante", missing);
        require(exportacion, "@Exportacion", missing);
        require(lugarExpedicion, "@LugarExpedicion", missing);
        if (emisor == null) {
            missing.add("cfdi:Emisor");
        } else {
            emisor.check(missing);
        }
        if (receptor == null) {
            missing.add("cfdi:Receptor");
        } else {
            receptor.check(missing);
        }
        if (conceptos.isEmpty()) {
            missing.add("cfdi:Conceptos/cfdi:Concepto");
        }
        for (int i = 0; i < conceptos.size(); i++) {
            conceptos.get(i).check("cfdi:Conceptos/cfdi:Concepto[" + (i + 1) + "]", missing);
        }
        if (!missing.isEmpty()) {
            throw new IncompleteModelException(missing);
        }

        IntSupplier decimales = this::decimalesMoneda;
        List<Concepto> builtConceptos = conceptos.stream().map(c -> c.build(decimales)).toList();
        boolean montosEnCero = tipoDeComprobante.equals(TipoDeComprobante.TRASLADO) || tipoDeComprobante.equals(TipoDeComprobante.PAGO);

        BigDecimal builtSubTotal = subTotal != null
                ? subTotal
                : montosEnCero ? cero(decimales) : Importes.redondear(sum(builtConceptos.stream().map(Concepto::importe).toList()), decimales.getAsInt());

        Optional<BigDecimal> builtDescuento = Optional.ofNullable(descuento);
        List<BigDecimal> descuentos = builtConceptos.stream().flatMap(c -> c.descuento().stream()).toList();
        if (descuento == null && !montosEnCero && !descuentos.isEmpty()) {
            builtDescuento = Optional.of(Importes.redondear(sum(descuentos), decimales.getAsInt()));
        }

        Optional<Impuestos> builtImpuestos = impuestos != null ? Optional.of(impuestos) : resumenImpuestos(builtConceptos, decimales);

        BigDecimal builtTotal = total;
        if (builtTotal == null) {
            if (montosEnCero) {
                builtTotal = cero(decimales);
            } else {
                BigDecimal calculado = builtSubTotal.subtract(builtDescuento.orElse(BigDecimal.ZERO));
                if (builtImpuestos.isPresent()) {
                    calculado = calculado
                            .add(builtImpuestos.get().totalImpuestosTrasladados().orElse(BigDecimal.ZERO))
                            .subtract(builtImpuestos.get().totalImpuestosRetenidos().orElse(BigDecimal.ZERO));
                }
                builtTotal = Importes.redondear(calculado, decimales.getAsInt());
            }
        }

        return new Comprobante(
                Optional.ofNullable(serie),
                Optional.ofNullable(folio),
                fecha,
                Optional.empty(),
                Optional.ofNullable(formaPago),
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(condicionesDePago),
                builtSubTotal,
                builtDescuento,
                moneda,
                Optional.ofNullable(tipoCambio),
                builtTotal,
                tipoDeComprobante,
                exportacion,
                Optional.ofNullable(metodoPago),
                lugarExpedicion,
                Optional.ofNullable(confirmacion),
                Optional.ofNullable(informacionGlobal),
                cfdiRelacionados,
                emisor.build(),
                receptor.build(),
                builtConceptos,
                builtImpuestos,
                complemento,
                Optional.ofNullable(addenda));
    }

    private int decimalesMoneda() {
        return moneda.decimales().orElseThrow(() -> new IncompleteModelException(
                "Cannot derive amounts: Moneda '" + moneda.clave() + "' is not in c_Moneda, so its decimals are unknown."
                        + " Set Importe, the concept taxes, SubTotal, Total and Impuestos explicitly."));
    }

    private static BigDecimal cero(IntSupplier decimales) {
        return BigDecimal.ZERO.setScale(decimales.getAsInt());
    }

    /**
     * Derives the tax summary: traslados grouped by Impuesto, TipoFactor and TasaOCuota, retenciones
     * grouped by Impuesto; Base and Importe are the rounded sums of the concept values.
     */
    private static Optional<Impuestos> resumenImpuestos(List<Concepto> conceptos, IntSupplier decimales) {
        Map<TrasladoKey, TrasladoAcumulado> traslados = new LinkedHashMap<>();
        Map<Impuesto, BigDecimal> retenciones = new LinkedHashMap<>();
        for (Concepto concepto : conceptos) {
            if (concepto.impuestos().isEmpty()) {
                continue;
            }
            for (Concepto.Traslado t : concepto.impuestos().get().traslados()) {
                TrasladoKey key = new TrasladoKey(t.impuesto(), t.tipoFactor(), t.tasaOCuota().map(BigDecimal::stripTrailingZeros).orElse(null));
                traslados.computeIfAbsent(key, k -> new TrasladoAcumulado(t.tasaOCuota())).add(t);
            }
            for (Concepto.Retencion r : concepto.impuestos().get().retenciones()) {
                retenciones.merge(r.impuesto(), r.importe(), BigDecimal::add);
            }
        }
        if (traslados.isEmpty() && retenciones.isEmpty()) {
            return Optional.empty();
        }
        int d = decimales.getAsInt();
        List<Impuestos.Traslado> resumenTraslados = new ArrayList<>();
        BigDecimal totalTrasladados = null;
        for (Map.Entry<TrasladoKey, TrasladoAcumulado> e : traslados.entrySet()) {
            TrasladoKey key = e.getKey();
            TrasladoAcumulado acc = e.getValue();
            boolean exento = key.tipoFactor().equals(TipoFactor.EXENTO);
            Optional<BigDecimal> importe = exento || acc.importe == null ? Optional.empty() : Optional.of(Importes.redondear(acc.importe, d));
            resumenTraslados.add(new Impuestos.Traslado(
                    Importes.redondear(acc.base, d), key.impuesto(), key.tipoFactor(), exento ? Optional.empty() : acc.tasaOCuota, importe));
            if (importe.isPresent()) {
                totalTrasladados = totalTrasladados == null ? importe.get() : totalTrasladados.add(importe.get());
            }
        }
        List<Impuestos.Retencion> resumenRetenciones = new ArrayList<>();
        BigDecimal totalRetenidos = null;
        for (Map.Entry<Impuesto, BigDecimal> e : retenciones.entrySet()) {
            BigDecimal importe = Importes.redondear(e.getValue(), d);
            resumenRetenciones.add(new Impuestos.Retencion(e.getKey(), importe));
            totalRetenidos = totalRetenidos == null ? importe : totalRetenidos.add(importe);
        }
        return Optional.of(new Impuestos(
                Optional.ofNullable(totalRetenidos), Optional.ofNullable(totalTrasladados), resumenRetenciones, resumenTraslados));
    }

    private record TrasladoKey(Impuesto impuesto, TipoFactor tipoFactor, BigDecimal tasaNormalizada) {}

    private static final class TrasladoAcumulado {
        private final Optional<BigDecimal> tasaOCuota;
        private BigDecimal base = BigDecimal.ZERO;
        private BigDecimal importe;

        TrasladoAcumulado(Optional<BigDecimal> tasaOCuota) {
            this.tasaOCuota = tasaOCuota;
        }

        void add(Concepto.Traslado t) {
            base = base.add(t.base());
            if (t.importe().isPresent()) {
                importe = importe == null ? t.importe().get() : importe.add(t.importe().get());
            }
        }
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    static void require(Object value, String location, List<String> missing) {
        if (value == null) {
            missing.add(location);
        }
    }

    /** Builds the node {@code Emisor}. */
    public static final class EmisorBuilder {
        private String rfc;
        private String nombre;
        private RegimenFiscal regimenFiscal;
        private String facAtrAdquirente;

        EmisorBuilder() {}

        /**
         * Sets {@code Rfc}.
         *
         * @param rfc the value
         * @return this builder
         */
        public EmisorBuilder rfc(String rfc) {
            this.rfc = rfc;
            return this;
        }

        /**
         * Sets {@code Nombre}.
         *
         * @param nombre the value
         * @return this builder
         */
        public EmisorBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        /**
         * Sets {@code RegimenFiscal}.
         *
         * @param regimenFiscal the value
         * @return this builder
         */
        public EmisorBuilder regimenFiscal(RegimenFiscal regimenFiscal) {
            this.regimenFiscal = regimenFiscal;
            return this;
        }

        /**
         * Sets {@code FacAtrAdquirente}.
         *
         * @param facAtrAdquirente the value
         * @return this builder
         */
        public EmisorBuilder facAtrAdquirente(String facAtrAdquirente) {
            this.facAtrAdquirente = facAtrAdquirente;
            return this;
        }

        void check(List<String> missing) {
            require(rfc, "cfdi:Emisor/@Rfc", missing);
            require(nombre, "cfdi:Emisor/@Nombre", missing);
            require(regimenFiscal, "cfdi:Emisor/@RegimenFiscal", missing);
        }

        Emisor build() {
            return new Emisor(rfc, nombre, regimenFiscal, Optional.ofNullable(facAtrAdquirente));
        }
    }

    /** Builds the node {@code Receptor}. */
    public static final class ReceptorBuilder {
        private String rfc;
        private String nombre;
        private String domicilioFiscalReceptor;
        private Pais residenciaFiscal;
        private String numRegIdTrib;
        private RegimenFiscal regimenFiscalReceptor;
        private UsoCFDI usoCFDI;

        ReceptorBuilder() {}

        /**
         * Sets {@code Rfc}.
         *
         * @param rfc the value
         * @return this builder
         */
        public ReceptorBuilder rfc(String rfc) {
            this.rfc = rfc;
            return this;
        }

        /**
         * Sets {@code Nombre}.
         *
         * @param nombre the value
         * @return this builder
         */
        public ReceptorBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        /**
         * Sets {@code DomicilioFiscalReceptor}: the receptor's postal code.
         *
         * @param domicilioFiscalReceptor the value
         * @return this builder
         */
        public ReceptorBuilder domicilioFiscalReceptor(String domicilioFiscalReceptor) {
            this.domicilioFiscalReceptor = domicilioFiscalReceptor;
            return this;
        }

        /**
         * Sets {@code ResidenciaFiscal}.
         *
         * @param residenciaFiscal the value
         * @return this builder
         */
        public ReceptorBuilder residenciaFiscal(Pais residenciaFiscal) {
            this.residenciaFiscal = residenciaFiscal;
            return this;
        }

        /**
         * Sets {@code NumRegIdTrib}.
         *
         * @param numRegIdTrib the value
         * @return this builder
         */
        public ReceptorBuilder numRegIdTrib(String numRegIdTrib) {
            this.numRegIdTrib = numRegIdTrib;
            return this;
        }

        /**
         * Sets {@code RegimenFiscalReceptor}.
         *
         * @param regimenFiscalReceptor the value
         * @return this builder
         */
        public ReceptorBuilder regimenFiscalReceptor(RegimenFiscal regimenFiscalReceptor) {
            this.regimenFiscalReceptor = regimenFiscalReceptor;
            return this;
        }

        /**
         * Sets {@code UsoCFDI}.
         *
         * @param usoCFDI the value
         * @return this builder
         */
        public ReceptorBuilder usoCFDI(UsoCFDI usoCFDI) {
            this.usoCFDI = usoCFDI;
            return this;
        }

        void check(List<String> missing) {
            require(rfc, "cfdi:Receptor/@Rfc", missing);
            require(nombre, "cfdi:Receptor/@Nombre", missing);
            require(domicilioFiscalReceptor, "cfdi:Receptor/@DomicilioFiscalReceptor", missing);
            require(regimenFiscalReceptor, "cfdi:Receptor/@RegimenFiscalReceptor", missing);
            require(usoCFDI, "cfdi:Receptor/@UsoCFDI", missing);
        }

        Receptor build() {
            return new Receptor(rfc, nombre, domicilioFiscalReceptor, Optional.ofNullable(residenciaFiscal),
                    Optional.ofNullable(numRegIdTrib), regimenFiscalReceptor, usoCFDI);
        }
    }

    /** Builds a node {@code Concepto}. */
    public static final class ConceptoBuilder {
        private String claveProdServ;
        private String noIdentificacion;
        private BigDecimal cantidad;
        private String claveUnidad;
        private String unidad;
        private String descripcion;
        private BigDecimal valorUnitario;
        private BigDecimal importe;
        private BigDecimal descuento;
        private ObjetoImp objetoImp;
        private final List<TrasladoBuilder> traslados = new ArrayList<>();
        private final List<RetencionBuilder> retenciones = new ArrayList<>();
        private Concepto.ACuentaTerceros aCuentaTerceros;
        private final List<Concepto.InformacionAduanera> informacionAduanera = new ArrayList<>();
        private final List<Concepto.CuentaPredial> cuentaPredial = new ArrayList<>();
        private final List<Complemento> complementoConcepto = new ArrayList<>();
        private final List<ParteBuilder> partes = new ArrayList<>();

        ConceptoBuilder() {}

        /**
         * Sets {@code ClaveProdServ}.
         *
         * @param claveProdServ the value
         * @return this builder
         */
        public ConceptoBuilder claveProdServ(String claveProdServ) {
            this.claveProdServ = claveProdServ;
            return this;
        }

        /**
         * Sets {@code NoIdentificacion}.
         *
         * @param noIdentificacion the value
         * @return this builder
         */
        public ConceptoBuilder noIdentificacion(String noIdentificacion) {
            this.noIdentificacion = noIdentificacion;
            return this;
        }

        /**
         * Sets {@code Cantidad}.
         *
         * @param cantidad the value
         * @return this builder
         */
        public ConceptoBuilder cantidad(BigDecimal cantidad) {
            this.cantidad = cantidad;
            return this;
        }

        /**
         * Sets {@code Cantidad}, keeping the decimals as written.
         *
         * @param cantidad the value, for example {@code "1"}
         * @return this builder
         */
        public ConceptoBuilder cantidad(String cantidad) {
            return cantidad(decimal(cantidad));
        }

        /**
         * Sets {@code ClaveUnidad}.
         *
         * @param claveUnidad the value
         * @return this builder
         */
        public ConceptoBuilder claveUnidad(String claveUnidad) {
            this.claveUnidad = claveUnidad;
            return this;
        }

        /**
         * Sets {@code Unidad}.
         *
         * @param unidad the value
         * @return this builder
         */
        public ConceptoBuilder unidad(String unidad) {
            this.unidad = unidad;
            return this;
        }

        /**
         * Sets {@code Descripcion}.
         *
         * @param descripcion the value
         * @return this builder
         */
        public ConceptoBuilder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        /**
         * Sets {@code ValorUnitario}.
         *
         * @param valorUnitario the value
         * @return this builder
         */
        public ConceptoBuilder valorUnitario(BigDecimal valorUnitario) {
            this.valorUnitario = valorUnitario;
            return this;
        }

        /**
         * Sets {@code ValorUnitario}, keeping the decimals as written.
         *
         * @param valorUnitario the value, for example {@code "100.00"}
         * @return this builder
         */
        public ConceptoBuilder valorUnitario(String valorUnitario) {
            return valorUnitario(decimal(valorUnitario));
        }

        /**
         * Sets {@code Importe} explicitly instead of deriving Cantidad × ValorUnitario.
         *
         * @param importe the value
         * @return this builder
         */
        public ConceptoBuilder importe(BigDecimal importe) {
            this.importe = importe;
            return this;
        }

        /**
         * Sets {@code Importe} explicitly, keeping the decimals as written.
         *
         * @param importe the value
         * @return this builder
         */
        public ConceptoBuilder importe(String importe) {
            return importe(decimal(importe));
        }

        /**
         * Sets {@code Descuento}.
         *
         * @param descuento the value
         * @return this builder
         */
        public ConceptoBuilder descuento(BigDecimal descuento) {
            this.descuento = descuento;
            return this;
        }

        /**
         * Sets {@code Descuento}, keeping the decimals as written.
         *
         * @param descuento the value
         * @return this builder
         */
        public ConceptoBuilder descuento(String descuento) {
            return descuento(decimal(descuento));
        }

        /**
         * Sets {@code ObjetoImp}.
         *
         * @param objetoImp the value
         * @return this builder
         */
        public ConceptoBuilder objetoImp(ObjetoImp objetoImp) {
            this.objetoImp = objetoImp;
            return this;
        }

        /**
         * Adds a transferred tax. Base defaults to Importe − Descuento and Importe to
         * Base × TasaOCuota; both can be set in {@code spec}.
         *
         * @param spec fills the traslado
         * @return this builder
         */
        public ConceptoBuilder traslado(Consumer<TrasladoBuilder> spec) {
            TrasladoBuilder builder = new TrasladoBuilder();
            spec.accept(builder);
            traslados.add(builder);
            return this;
        }

        /**
         * Adds a transferred tax with derived Base and Importe.
         *
         * @param impuesto attribute {@code Impuesto}
         * @param tipoFactor attribute {@code TipoFactor}
         * @param tasaOCuota attribute {@code TasaOCuota}, for example {@code "0.160000"}
         * @return this builder
         */
        public ConceptoBuilder traslado(Impuesto impuesto, TipoFactor tipoFactor, String tasaOCuota) {
            return traslado(t -> t.impuesto(impuesto).tipoFactor(tipoFactor).tasaOCuota(tasaOCuota));
        }

        /**
         * Adds IVA transferred at the 16 % rate ({@code 0.160000}, c_TasaOCuota), with derived Base
         * and Importe.
         *
         * @return this builder
         */
        public ConceptoBuilder trasladoIva16() {
            return traslado(Impuesto.IVA, TipoFactor.TASA, "0.160000");
        }

        /**
         * Adds an exempt transferred tax (TipoFactor Exento: no TasaOCuota and no Importe), with
         * derived Base.
         *
         * @param impuesto attribute {@code Impuesto}
         * @return this builder
         */
        public ConceptoBuilder trasladoExento(Impuesto impuesto) {
            return traslado(t -> t.impuesto(impuesto).tipoFactor(TipoFactor.EXENTO));
        }

        /**
         * Adds a withheld tax. Base defaults to Importe − Descuento and Importe to
         * Base × TasaOCuota; both can be set in {@code spec}.
         *
         * @param spec fills the retencion
         * @return this builder
         */
        public ConceptoBuilder retencion(Consumer<RetencionBuilder> spec) {
            RetencionBuilder builder = new RetencionBuilder();
            spec.accept(builder);
            retenciones.add(builder);
            return this;
        }

        /**
         * Adds a withheld tax with derived Base and Importe.
         *
         * @param impuesto attribute {@code Impuesto}
         * @param tipoFactor attribute {@code TipoFactor}
         * @param tasaOCuota attribute {@code TasaOCuota}
         * @return this builder
         */
        public ConceptoBuilder retencion(Impuesto impuesto, TipoFactor tipoFactor, String tasaOCuota) {
            return retencion(r -> r.impuesto(impuesto).tipoFactor(tipoFactor).tasaOCuota(tasaOCuota));
        }

        /**
         * Sets the node {@code ACuentaTerceros}.
         *
         * @param rfc attribute {@code RfcACuentaTerceros}
         * @param nombre attribute {@code NombreACuentaTerceros}
         * @param regimenFiscal attribute {@code RegimenFiscalACuentaTerceros}
         * @param domicilioFiscal attribute {@code DomicilioFiscalACuentaTerceros}
         * @return this builder
         */
        public ConceptoBuilder aCuentaTerceros(String rfc, String nombre, RegimenFiscal regimenFiscal, String domicilioFiscal) {
            this.aCuentaTerceros = new Concepto.ACuentaTerceros(rfc, nombre, regimenFiscal, domicilioFiscal);
            return this;
        }

        /**
         * Adds a node {@code InformacionAduanera}.
         *
         * @param numeroPedimento attribute {@code NumeroPedimento}
         * @return this builder
         */
        public ConceptoBuilder informacionAduanera(String numeroPedimento) {
            informacionAduanera.add(new Concepto.InformacionAduanera(numeroPedimento));
            return this;
        }

        /**
         * Adds a node {@code CuentaPredial}.
         *
         * @param numero attribute {@code Numero}
         * @return this builder
         */
        public ConceptoBuilder cuentaPredial(String numero) {
            cuentaPredial.add(new Concepto.CuentaPredial(numero));
            return this;
        }

        /**
         * Adds a concept complement ({@code ComplementoConcepto}).
         *
         * @param complemento the complement
         * @return this builder
         */
        public ConceptoBuilder complementoConcepto(Complemento complemento) {
            complementoConcepto.add(Objects.requireNonNull(complemento, "complemento"));
            return this;
        }

        /**
         * Adds a node {@code Parte}.
         *
         * @param spec fills the parte
         * @return this builder
         */
        public ConceptoBuilder parte(Consumer<ParteBuilder> spec) {
            ParteBuilder builder = new ParteBuilder();
            spec.accept(builder);
            partes.add(builder);
            return this;
        }

        void check(String path, List<String> missing) {
            require(claveProdServ, path + "/@ClaveProdServ", missing);
            require(cantidad, path + "/@Cantidad", missing);
            require(claveUnidad, path + "/@ClaveUnidad", missing);
            require(descripcion, path + "/@Descripcion", missing);
            require(valorUnitario, path + "/@ValorUnitario", missing);
            require(objetoImp, path + "/@ObjetoImp", missing);
            for (int i = 0; i < traslados.size(); i++) {
                traslados.get(i).check(path + "/cfdi:Impuestos/cfdi:Traslados/cfdi:Traslado[" + (i + 1) + "]", missing);
            }
            for (int i = 0; i < retenciones.size(); i++) {
                retenciones.get(i).check(path + "/cfdi:Impuestos/cfdi:Retenciones/cfdi:Retencion[" + (i + 1) + "]", missing);
            }
            for (int i = 0; i < partes.size(); i++) {
                partes.get(i).check(path + "/cfdi:Parte[" + (i + 1) + "]", missing);
            }
        }

        Concepto build(IntSupplier decimales) {
            BigDecimal builtImporte = importe != null ? importe : Importes.producto(cantidad, valorUnitario, decimales.getAsInt());
            BigDecimal baseDerivada = descuento == null ? builtImporte : builtImporte.subtract(descuento);
            List<Concepto.Traslado> builtTraslados = traslados.stream().map(t -> t.build(baseDerivada, decimales)).toList();
            List<Concepto.Retencion> builtRetenciones = retenciones.stream().map(r -> r.build(baseDerivada, decimales)).toList();
            Optional<Concepto.Impuestos> builtImpuestos = builtTraslados.isEmpty() && builtRetenciones.isEmpty()
                    ? Optional.empty()
                    : Optional.of(new Concepto.Impuestos(builtTraslados, builtRetenciones));
            return new Concepto(claveProdServ, Optional.ofNullable(noIdentificacion), cantidad, claveUnidad,
                    Optional.ofNullable(unidad), descripcion, valorUnitario, builtImporte, Optional.ofNullable(descuento),
                    objetoImp, builtImpuestos, Optional.ofNullable(aCuentaTerceros), informacionAduanera, cuentaPredial,
                    complementoConcepto, partes.stream().map(ParteBuilder::build).toList());
        }
    }

    /** Builds a concept-level {@code Traslado}. */
    public static final class TrasladoBuilder {
        private BigDecimal base;
        private Impuesto impuesto;
        private TipoFactor tipoFactor;
        private BigDecimal tasaOCuota;
        private BigDecimal importe;

        TrasladoBuilder() {}

        /**
         * Sets {@code Base} explicitly instead of Importe − Descuento of the concept.
         *
         * @param base the value
         * @return this builder
         */
        public TrasladoBuilder base(String base) {
            this.base = decimal(base);
            return this;
        }

        /**
         * Sets {@code Impuesto}.
         *
         * @param impuesto the value
         * @return this builder
         */
        public TrasladoBuilder impuesto(Impuesto impuesto) {
            this.impuesto = impuesto;
            return this;
        }

        /**
         * Sets {@code TipoFactor}.
         *
         * @param tipoFactor the value
         * @return this builder
         */
        public TrasladoBuilder tipoFactor(TipoFactor tipoFactor) {
            this.tipoFactor = tipoFactor;
            return this;
        }

        /**
         * Sets {@code TasaOCuota}, keeping the decimals as written.
         *
         * @param tasaOCuota the value, for example {@code "0.160000"}
         * @return this builder
         */
        public TrasladoBuilder tasaOCuota(String tasaOCuota) {
            this.tasaOCuota = decimal(tasaOCuota);
            return this;
        }

        /**
         * Sets {@code Importe} explicitly instead of Base × TasaOCuota.
         *
         * @param importe the value
         * @return this builder
         */
        public TrasladoBuilder importe(String importe) {
            this.importe = decimal(importe);
            return this;
        }

        void check(String path, List<String> missing) {
            require(impuesto, path + "/@Impuesto", missing);
            require(tipoFactor, path + "/@TipoFactor", missing);
        }

        Concepto.Traslado build(BigDecimal baseDerivada, IntSupplier decimales) {
            BigDecimal builtBase = base != null ? base : baseDerivada;
            BigDecimal builtImporte = importe;
            if (builtImporte == null && tasaOCuota != null && !tipoFactor.equals(TipoFactor.EXENTO)) {
                builtImporte = Importes.producto(builtBase, tasaOCuota, decimales.getAsInt());
            }
            return new Concepto.Traslado(builtBase, impuesto, tipoFactor, Optional.ofNullable(tasaOCuota), Optional.ofNullable(builtImporte));
        }
    }

    /** Builds a concept-level {@code Retencion}. */
    public static final class RetencionBuilder {
        private BigDecimal base;
        private Impuesto impuesto;
        private TipoFactor tipoFactor;
        private BigDecimal tasaOCuota;
        private BigDecimal importe;

        RetencionBuilder() {}

        /**
         * Sets {@code Base} explicitly instead of Importe − Descuento of the concept.
         *
         * @param base the value
         * @return this builder
         */
        public RetencionBuilder base(String base) {
            this.base = decimal(base);
            return this;
        }

        /**
         * Sets {@code Impuesto}.
         *
         * @param impuesto the value
         * @return this builder
         */
        public RetencionBuilder impuesto(Impuesto impuesto) {
            this.impuesto = impuesto;
            return this;
        }

        /**
         * Sets {@code TipoFactor}.
         *
         * @param tipoFactor the value
         * @return this builder
         */
        public RetencionBuilder tipoFactor(TipoFactor tipoFactor) {
            this.tipoFactor = tipoFactor;
            return this;
        }

        /**
         * Sets {@code TasaOCuota}, keeping the decimals as written.
         *
         * @param tasaOCuota the value
         * @return this builder
         */
        public RetencionBuilder tasaOCuota(String tasaOCuota) {
            this.tasaOCuota = decimal(tasaOCuota);
            return this;
        }

        /**
         * Sets {@code Importe} explicitly instead of Base × TasaOCuota.
         *
         * @param importe the value
         * @return this builder
         */
        public RetencionBuilder importe(String importe) {
            this.importe = decimal(importe);
            return this;
        }

        void check(String path, List<String> missing) {
            require(impuesto, path + "/@Impuesto", missing);
            require(tipoFactor, path + "/@TipoFactor", missing);
            require(tasaOCuota, path + "/@TasaOCuota", missing);
        }

        Concepto.Retencion build(BigDecimal baseDerivada, IntSupplier decimales) {
            BigDecimal builtBase = base != null ? base : baseDerivada;
            BigDecimal builtImporte = importe != null ? importe : Importes.producto(builtBase, tasaOCuota, decimales.getAsInt());
            return new Concepto.Retencion(builtBase, impuesto, tipoFactor, tasaOCuota, builtImporte);
        }
    }

    /** Builds a node {@code Parte}. Its Importe is optional in the XSD and is not derived. */
    public static final class ParteBuilder {
        private String claveProdServ;
        private String noIdentificacion;
        private BigDecimal cantidad;
        private String unidad;
        private String descripcion;
        private BigDecimal valorUnitario;
        private BigDecimal importe;
        private final List<Concepto.InformacionAduanera> informacionAduanera = new ArrayList<>();

        ParteBuilder() {}

        /**
         * Sets {@code ClaveProdServ}.
         *
         * @param claveProdServ the value
         * @return this builder
         */
        public ParteBuilder claveProdServ(String claveProdServ) {
            this.claveProdServ = claveProdServ;
            return this;
        }

        /**
         * Sets {@code NoIdentificacion}.
         *
         * @param noIdentificacion the value
         * @return this builder
         */
        public ParteBuilder noIdentificacion(String noIdentificacion) {
            this.noIdentificacion = noIdentificacion;
            return this;
        }

        /**
         * Sets {@code Cantidad}, keeping the decimals as written.
         *
         * @param cantidad the value
         * @return this builder
         */
        public ParteBuilder cantidad(String cantidad) {
            this.cantidad = decimal(cantidad);
            return this;
        }

        /**
         * Sets {@code Unidad}.
         *
         * @param unidad the value
         * @return this builder
         */
        public ParteBuilder unidad(String unidad) {
            this.unidad = unidad;
            return this;
        }

        /**
         * Sets {@code Descripcion}.
         *
         * @param descripcion the value
         * @return this builder
         */
        public ParteBuilder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        /**
         * Sets {@code ValorUnitario}, keeping the decimals as written.
         *
         * @param valorUnitario the value
         * @return this builder
         */
        public ParteBuilder valorUnitario(String valorUnitario) {
            this.valorUnitario = decimal(valorUnitario);
            return this;
        }

        /**
         * Sets {@code Importe}, keeping the decimals as written.
         *
         * @param importe the value
         * @return this builder
         */
        public ParteBuilder importe(String importe) {
            this.importe = decimal(importe);
            return this;
        }

        /**
         * Adds a node {@code InformacionAduanera}.
         *
         * @param numeroPedimento attribute {@code NumeroPedimento}
         * @return this builder
         */
        public ParteBuilder informacionAduanera(String numeroPedimento) {
            informacionAduanera.add(new Concepto.InformacionAduanera(numeroPedimento));
            return this;
        }

        void check(String path, List<String> missing) {
            require(claveProdServ, path + "/@ClaveProdServ", missing);
            require(cantidad, path + "/@Cantidad", missing);
            require(descripcion, path + "/@Descripcion", missing);
        }

        Concepto.Parte build() {
            return new Concepto.Parte(claveProdServ, Optional.ofNullable(noIdentificacion), cantidad, Optional.ofNullable(unidad),
                    descripcion, Optional.ofNullable(valorUnitario), Optional.ofNullable(importe), informacionAduanera);
        }
    }
}
