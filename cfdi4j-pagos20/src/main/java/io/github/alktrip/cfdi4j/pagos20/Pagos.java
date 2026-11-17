package io.github.alktrip.cfdi4j.pagos20;

import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.model.Complemento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Node {@code pago20:Pagos} of the complemento para recepción de pagos 2.0 ({@code Pagos20.xsd}).
 * Names follow the XSD exactly (ADR 0002). Create instances with {@link #builder()}.
 *
 * @param totales node {@code pago20:Totales}
 * @param pago nodes {@code pago20:Pago}
 */
public record Pagos(Totales totales, List<Pago> pago) implements Complemento {

    /** Namespace of the complement. */
    public static final String NAMESPACE = "http://www.sat.gob.mx/Pagos20";
    /** Value of the {@code Version} attribute. */
    public static final String VERSION = "2.0";

    /** Checks that no component is null and copies the list. */
    public Pagos {
        Objects.requireNonNull(totales, "totales");
        pago = List.copyOf(pago);
    }

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    /**
     * Starts building the complement.
     *
     * @return a new builder
     */
    public static PagosBuilder builder() {
        return new PagosBuilder();
    }

    /**
     * Node {@code pago20:Totales}. Amounts are in MXN.
     *
     * @param totalRetencionesIVA attribute {@code TotalRetencionesIVA}
     * @param totalRetencionesISR attribute {@code TotalRetencionesISR}
     * @param totalRetencionesIEPS attribute {@code TotalRetencionesIEPS}
     * @param totalTrasladosBaseIVA16 attribute {@code TotalTrasladosBaseIVA16}
     * @param totalTrasladosImpuestoIVA16 attribute {@code TotalTrasladosImpuestoIVA16}
     * @param totalTrasladosBaseIVA8 attribute {@code TotalTrasladosBaseIVA8}
     * @param totalTrasladosImpuestoIVA8 attribute {@code TotalTrasladosImpuestoIVA8}
     * @param totalTrasladosBaseIVA0 attribute {@code TotalTrasladosBaseIVA0}
     * @param totalTrasladosImpuestoIVA0 attribute {@code TotalTrasladosImpuestoIVA0}
     * @param totalTrasladosBaseIVAExento attribute {@code TotalTrasladosBaseIVAExento}
     * @param montoTotalPagos attribute {@code MontoTotalPagos}
     */
    public record Totales(
            Optional<BigDecimal> totalRetencionesIVA,
            Optional<BigDecimal> totalRetencionesISR,
            Optional<BigDecimal> totalRetencionesIEPS,
            Optional<BigDecimal> totalTrasladosBaseIVA16,
            Optional<BigDecimal> totalTrasladosImpuestoIVA16,
            Optional<BigDecimal> totalTrasladosBaseIVA8,
            Optional<BigDecimal> totalTrasladosImpuestoIVA8,
            Optional<BigDecimal> totalTrasladosBaseIVA0,
            Optional<BigDecimal> totalTrasladosImpuestoIVA0,
            Optional<BigDecimal> totalTrasladosBaseIVAExento,
            BigDecimal montoTotalPagos) {

        /** Checks that no component is null. */
        public Totales {
            Objects.requireNonNull(totalRetencionesIVA, "totalRetencionesIVA");
            Objects.requireNonNull(totalRetencionesISR, "totalRetencionesISR");
            Objects.requireNonNull(totalRetencionesIEPS, "totalRetencionesIEPS");
            Objects.requireNonNull(totalTrasladosBaseIVA16, "totalTrasladosBaseIVA16");
            Objects.requireNonNull(totalTrasladosImpuestoIVA16, "totalTrasladosImpuestoIVA16");
            Objects.requireNonNull(totalTrasladosBaseIVA8, "totalTrasladosBaseIVA8");
            Objects.requireNonNull(totalTrasladosImpuestoIVA8, "totalTrasladosImpuestoIVA8");
            Objects.requireNonNull(totalTrasladosBaseIVA0, "totalTrasladosBaseIVA0");
            Objects.requireNonNull(totalTrasladosImpuestoIVA0, "totalTrasladosImpuestoIVA0");
            Objects.requireNonNull(totalTrasladosBaseIVAExento, "totalTrasladosBaseIVAExento");
            Objects.requireNonNull(montoTotalPagos, "montoTotalPagos");
        }
    }

    /**
     * Node {@code pago20:Pago}.
     *
     * @param fechaPago attribute {@code FechaPago}
     * @param formaDePagoP attribute {@code FormaDePagoP}
     * @param monedaP attribute {@code MonedaP}
     * @param tipoCambioP attribute {@code TipoCambioP}
     * @param monto attribute {@code Monto}
     * @param numOperacion attribute {@code NumOperacion}
     * @param rfcEmisorCtaOrd attribute {@code RfcEmisorCtaOrd}
     * @param nomBancoOrdExt attribute {@code NomBancoOrdExt}
     * @param ctaOrdenante attribute {@code CtaOrdenante}
     * @param rfcEmisorCtaBen attribute {@code RfcEmisorCtaBen}
     * @param ctaBeneficiario attribute {@code CtaBeneficiario}
     * @param tipoCadPago attribute {@code TipoCadPago} (catálogo c_TipoCadenaPago)
     * @param certPago attribute {@code CertPago}
     * @param cadPago attribute {@code CadPago}
     * @param selloPago attribute {@code SelloPago}
     * @param doctoRelacionado nodes {@code pago20:DoctoRelacionado}
     * @param impuestosP node {@code pago20:ImpuestosP}
     */
    public record Pago(
            LocalDateTime fechaPago,
            FormaPago formaDePagoP,
            Moneda monedaP,
            Optional<BigDecimal> tipoCambioP,
            BigDecimal monto,
            Optional<String> numOperacion,
            Optional<String> rfcEmisorCtaOrd,
            Optional<String> nomBancoOrdExt,
            Optional<String> ctaOrdenante,
            Optional<String> rfcEmisorCtaBen,
            Optional<String> ctaBeneficiario,
            Optional<String> tipoCadPago,
            Optional<String> certPago,
            Optional<String> cadPago,
            Optional<String> selloPago,
            List<DoctoRelacionado> doctoRelacionado,
            Optional<ImpuestosP> impuestosP) {

        /** Checks that no component is null and copies the list. */
        public Pago {
            Objects.requireNonNull(fechaPago, "fechaPago");
            Objects.requireNonNull(formaDePagoP, "formaDePagoP");
            Objects.requireNonNull(monedaP, "monedaP");
            Objects.requireNonNull(tipoCambioP, "tipoCambioP");
            Objects.requireNonNull(monto, "monto");
            Objects.requireNonNull(numOperacion, "numOperacion");
            Objects.requireNonNull(rfcEmisorCtaOrd, "rfcEmisorCtaOrd");
            Objects.requireNonNull(nomBancoOrdExt, "nomBancoOrdExt");
            Objects.requireNonNull(ctaOrdenante, "ctaOrdenante");
            Objects.requireNonNull(rfcEmisorCtaBen, "rfcEmisorCtaBen");
            Objects.requireNonNull(ctaBeneficiario, "ctaBeneficiario");
            Objects.requireNonNull(tipoCadPago, "tipoCadPago");
            Objects.requireNonNull(certPago, "certPago");
            Objects.requireNonNull(cadPago, "cadPago");
            Objects.requireNonNull(selloPago, "selloPago");
            doctoRelacionado = List.copyOf(doctoRelacionado);
            Objects.requireNonNull(impuestosP, "impuestosP");
        }
    }

    /**
     * Node {@code pago20:DoctoRelacionado}.
     *
     * @param idDocumento attribute {@code IdDocumento}
     * @param serie attribute {@code Serie}
     * @param folio attribute {@code Folio}
     * @param monedaDR attribute {@code MonedaDR}
     * @param equivalenciaDR attribute {@code EquivalenciaDR}
     * @param numParcialidad attribute {@code NumParcialidad}
     * @param impSaldoAnt attribute {@code ImpSaldoAnt}
     * @param impPagado attribute {@code ImpPagado}
     * @param impSaldoInsoluto attribute {@code ImpSaldoInsoluto}
     * @param objetoImpDR attribute {@code ObjetoImpDR}
     * @param impuestosDR node {@code pago20:ImpuestosDR}
     */
    public record DoctoRelacionado(
            String idDocumento,
            Optional<String> serie,
            Optional<String> folio,
            Moneda monedaDR,
            Optional<BigDecimal> equivalenciaDR,
            int numParcialidad,
            BigDecimal impSaldoAnt,
            BigDecimal impPagado,
            BigDecimal impSaldoInsoluto,
            ObjetoImp objetoImpDR,
            Optional<ImpuestosDR> impuestosDR) {

        /** Checks that no component is null. */
        public DoctoRelacionado {
            Objects.requireNonNull(idDocumento, "idDocumento");
            Objects.requireNonNull(serie, "serie");
            Objects.requireNonNull(folio, "folio");
            Objects.requireNonNull(monedaDR, "monedaDR");
            Objects.requireNonNull(equivalenciaDR, "equivalenciaDR");
            Objects.requireNonNull(impSaldoAnt, "impSaldoAnt");
            Objects.requireNonNull(impPagado, "impPagado");
            Objects.requireNonNull(impSaldoInsoluto, "impSaldoInsoluto");
            Objects.requireNonNull(objetoImpDR, "objetoImpDR");
            Objects.requireNonNull(impuestosDR, "impuestosDR");
        }
    }

    /**
     * Node {@code pago20:ImpuestosDR}.
     *
     * @param retencionesDR nodes {@code RetencionesDR/RetencionDR}; empty when absent
     * @param trasladosDR nodes {@code TrasladosDR/TrasladoDR}; empty when absent
     */
    public record ImpuestosDR(List<RetencionDR> retencionesDR, List<TrasladoDR> trasladosDR) {

        /** Copies the lists. */
        public ImpuestosDR {
            retencionesDR = List.copyOf(retencionesDR);
            trasladosDR = List.copyOf(trasladosDR);
        }
    }

    /**
     * Node {@code pago20:RetencionDR}.
     *
     * @param baseDR attribute {@code BaseDR}
     * @param impuestoDR attribute {@code ImpuestoDR}
     * @param tipoFactorDR attribute {@code TipoFactorDR}
     * @param tasaOCuotaDR attribute {@code TasaOCuotaDR}
     * @param importeDR attribute {@code ImporteDR}
     */
    public record RetencionDR(BigDecimal baseDR, Impuesto impuestoDR, TipoFactor tipoFactorDR, BigDecimal tasaOCuotaDR, BigDecimal importeDR) {

        /** Checks that no component is null. */
        public RetencionDR {
            Objects.requireNonNull(baseDR, "baseDR");
            Objects.requireNonNull(impuestoDR, "impuestoDR");
            Objects.requireNonNull(tipoFactorDR, "tipoFactorDR");
            Objects.requireNonNull(tasaOCuotaDR, "tasaOCuotaDR");
            Objects.requireNonNull(importeDR, "importeDR");
        }
    }

    /**
     * Node {@code pago20:TrasladoDR}.
     *
     * @param baseDR attribute {@code BaseDR}
     * @param impuestoDR attribute {@code ImpuestoDR}
     * @param tipoFactorDR attribute {@code TipoFactorDR}
     * @param tasaOCuotaDR attribute {@code TasaOCuotaDR}; absent for Exento
     * @param importeDR attribute {@code ImporteDR}; absent for Exento
     */
    public record TrasladoDR(
            BigDecimal baseDR, Impuesto impuestoDR, TipoFactor tipoFactorDR, Optional<BigDecimal> tasaOCuotaDR, Optional<BigDecimal> importeDR) {

        /** Checks that no component is null. */
        public TrasladoDR {
            Objects.requireNonNull(baseDR, "baseDR");
            Objects.requireNonNull(impuestoDR, "impuestoDR");
            Objects.requireNonNull(tipoFactorDR, "tipoFactorDR");
            Objects.requireNonNull(tasaOCuotaDR, "tasaOCuotaDR");
            Objects.requireNonNull(importeDR, "importeDR");
        }
    }

    /**
     * Node {@code pago20:ImpuestosP}.
     *
     * @param retencionesP nodes {@code RetencionesP/RetencionP}; empty when absent
     * @param trasladosP nodes {@code TrasladosP/TrasladoP}; empty when absent
     */
    public record ImpuestosP(List<RetencionP> retencionesP, List<TrasladoP> trasladosP) {

        /** Copies the lists. */
        public ImpuestosP {
            retencionesP = List.copyOf(retencionesP);
            trasladosP = List.copyOf(trasladosP);
        }
    }

    /**
     * Node {@code pago20:RetencionP}.
     *
     * @param impuestoP attribute {@code ImpuestoP}
     * @param importeP attribute {@code ImporteP}
     */
    public record RetencionP(Impuesto impuestoP, BigDecimal importeP) {

        /** Checks that no component is null. */
        public RetencionP {
            Objects.requireNonNull(impuestoP, "impuestoP");
            Objects.requireNonNull(importeP, "importeP");
        }
    }

    /**
     * Node {@code pago20:TrasladoP}.
     *
     * @param baseP attribute {@code BaseP}
     * @param impuestoP attribute {@code ImpuestoP}
     * @param tipoFactorP attribute {@code TipoFactorP}
     * @param tasaOCuotaP attribute {@code TasaOCuotaP}; absent for Exento
     * @param importeP attribute {@code ImporteP}; absent for Exento
     */
    public record TrasladoP(
            BigDecimal baseP, Impuesto impuestoP, TipoFactor tipoFactorP, Optional<BigDecimal> tasaOCuotaP, Optional<BigDecimal> importeP) {

        /** Checks that no component is null. */
        public TrasladoP {
            Objects.requireNonNull(baseP, "baseP");
            Objects.requireNonNull(impuestoP, "impuestoP");
            Objects.requireNonNull(tipoFactorP, "tipoFactorP");
            Objects.requireNonNull(tasaOCuotaP, "tasaOCuotaP");
            Objects.requireNonNull(importeP, "importeP");
        }
    }
}
