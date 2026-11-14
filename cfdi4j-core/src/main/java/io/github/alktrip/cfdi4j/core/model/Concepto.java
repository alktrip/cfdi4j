package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Node {@code cfdi:Conceptos/cfdi:Concepto}.
 *
 * <p>{@code ClaveProdServ} and {@code ClaveUnidad} are plain strings: their catalogs are large and
 * are checked through a catalog provider (ADR 0003).
 *
 * @param claveProdServ attribute {@code ClaveProdServ}
 * @param noIdentificacion attribute {@code NoIdentificacion}
 * @param cantidad attribute {@code Cantidad}
 * @param claveUnidad attribute {@code ClaveUnidad}
 * @param unidad attribute {@code Unidad}
 * @param descripcion attribute {@code Descripcion}
 * @param valorUnitario attribute {@code ValorUnitario}
 * @param importe attribute {@code Importe}
 * @param descuento attribute {@code Descuento}
 * @param objetoImp attribute {@code ObjetoImp}
 * @param impuestos node {@code cfdi:Impuestos}
 * @param aCuentaTerceros node {@code cfdi:ACuentaTerceros}
 * @param informacionAduanera nodes {@code cfdi:InformacionAduanera}
 * @param cuentaPredial nodes {@code cfdi:CuentaPredial}
 * @param complementoConcepto children of {@code cfdi:ComplementoConcepto}; empty when absent
 * @param parte nodes {@code cfdi:Parte}
 */
public record Concepto(
        String claveProdServ,
        Optional<String> noIdentificacion,
        BigDecimal cantidad,
        String claveUnidad,
        Optional<String> unidad,
        String descripcion,
        BigDecimal valorUnitario,
        BigDecimal importe,
        Optional<BigDecimal> descuento,
        ObjetoImp objetoImp,
        Optional<Impuestos> impuestos,
        Optional<ACuentaTerceros> aCuentaTerceros,
        List<InformacionAduanera> informacionAduanera,
        List<CuentaPredial> cuentaPredial,
        List<Complemento> complementoConcepto,
        List<Parte> parte) {

    /** Checks that no component is null and copies the lists. */
    public Concepto {
        Objects.requireNonNull(claveProdServ, "claveProdServ");
        Objects.requireNonNull(noIdentificacion, "noIdentificacion");
        Objects.requireNonNull(cantidad, "cantidad");
        Objects.requireNonNull(claveUnidad, "claveUnidad");
        Objects.requireNonNull(unidad, "unidad");
        Objects.requireNonNull(descripcion, "descripcion");
        Objects.requireNonNull(valorUnitario, "valorUnitario");
        Objects.requireNonNull(importe, "importe");
        Objects.requireNonNull(descuento, "descuento");
        Objects.requireNonNull(objetoImp, "objetoImp");
        Objects.requireNonNull(impuestos, "impuestos");
        Objects.requireNonNull(aCuentaTerceros, "aCuentaTerceros");
        informacionAduanera = List.copyOf(informacionAduanera);
        cuentaPredial = List.copyOf(cuentaPredial);
        complementoConcepto = List.copyOf(complementoConcepto);
        parte = List.copyOf(parte);
    }

    /**
     * Node {@code cfdi:Concepto/cfdi:Impuestos}.
     *
     * @param traslados nodes {@code cfdi:Traslados/cfdi:Traslado}; empty when the node is absent
     * @param retenciones nodes {@code cfdi:Retenciones/cfdi:Retencion}; empty when the node is absent
     */
    public record Impuestos(List<Traslado> traslados, List<Retencion> retenciones) {

        /** Copies the lists. */
        public Impuestos {
            traslados = List.copyOf(traslados);
            retenciones = List.copyOf(retenciones);
        }
    }

    /**
     * Node {@code cfdi:Concepto/cfdi:Impuestos/cfdi:Traslados/cfdi:Traslado}.
     *
     * @param base attribute {@code Base}
     * @param impuesto attribute {@code Impuesto}
     * @param tipoFactor attribute {@code TipoFactor}
     * @param tasaOCuota attribute {@code TasaOCuota}; absent when the factor is Exento
     * @param importe attribute {@code Importe}; absent when the factor is Exento
     */
    public record Traslado(
            BigDecimal base, Impuesto impuesto, TipoFactor tipoFactor, Optional<BigDecimal> tasaOCuota, Optional<BigDecimal> importe) {

        /** Checks that no component is null. */
        public Traslado {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(impuesto, "impuesto");
            Objects.requireNonNull(tipoFactor, "tipoFactor");
            Objects.requireNonNull(tasaOCuota, "tasaOCuota");
            Objects.requireNonNull(importe, "importe");
        }
    }

    /**
     * Node {@code cfdi:Concepto/cfdi:Impuestos/cfdi:Retenciones/cfdi:Retencion}.
     *
     * @param base attribute {@code Base}
     * @param impuesto attribute {@code Impuesto}
     * @param tipoFactor attribute {@code TipoFactor}
     * @param tasaOCuota attribute {@code TasaOCuota}
     * @param importe attribute {@code Importe}
     */
    public record Retencion(BigDecimal base, Impuesto impuesto, TipoFactor tipoFactor, BigDecimal tasaOCuota, BigDecimal importe) {

        /** Checks that no component is null. */
        public Retencion {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(impuesto, "impuesto");
            Objects.requireNonNull(tipoFactor, "tipoFactor");
            Objects.requireNonNull(tasaOCuota, "tasaOCuota");
            Objects.requireNonNull(importe, "importe");
        }
    }

    /**
     * Node {@code cfdi:ACuentaTerceros}.
     *
     * @param rfcACuentaTerceros attribute {@code RfcACuentaTerceros}
     * @param nombreACuentaTerceros attribute {@code NombreACuentaTerceros}
     * @param regimenFiscalACuentaTerceros attribute {@code RegimenFiscalACuentaTerceros}
     * @param domicilioFiscalACuentaTerceros attribute {@code DomicilioFiscalACuentaTerceros}
     */
    public record ACuentaTerceros(
            String rfcACuentaTerceros,
            String nombreACuentaTerceros,
            RegimenFiscal regimenFiscalACuentaTerceros,
            String domicilioFiscalACuentaTerceros) {

        /** Checks that no component is null. */
        public ACuentaTerceros {
            Objects.requireNonNull(rfcACuentaTerceros, "rfcACuentaTerceros");
            Objects.requireNonNull(nombreACuentaTerceros, "nombreACuentaTerceros");
            Objects.requireNonNull(regimenFiscalACuentaTerceros, "regimenFiscalACuentaTerceros");
            Objects.requireNonNull(domicilioFiscalACuentaTerceros, "domicilioFiscalACuentaTerceros");
        }
    }

    /**
     * Node {@code cfdi:InformacionAduanera}.
     *
     * @param numeroPedimento attribute {@code NumeroPedimento}
     */
    public record InformacionAduanera(String numeroPedimento) {

        /** Checks that no component is null. */
        public InformacionAduanera {
            Objects.requireNonNull(numeroPedimento, "numeroPedimento");
        }
    }

    /**
     * Node {@code cfdi:CuentaPredial}.
     *
     * @param numero attribute {@code Numero}
     */
    public record CuentaPredial(String numero) {

        /** Checks that no component is null. */
        public CuentaPredial {
            Objects.requireNonNull(numero, "numero");
        }
    }

    /**
     * Node {@code cfdi:Parte}.
     *
     * @param claveProdServ attribute {@code ClaveProdServ}
     * @param noIdentificacion attribute {@code NoIdentificacion}
     * @param cantidad attribute {@code Cantidad}
     * @param unidad attribute {@code Unidad}
     * @param descripcion attribute {@code Descripcion}
     * @param valorUnitario attribute {@code ValorUnitario}
     * @param importe attribute {@code Importe}
     * @param informacionAduanera nodes {@code cfdi:InformacionAduanera}
     */
    public record Parte(
            String claveProdServ,
            Optional<String> noIdentificacion,
            BigDecimal cantidad,
            Optional<String> unidad,
            String descripcion,
            Optional<BigDecimal> valorUnitario,
            Optional<BigDecimal> importe,
            List<InformacionAduanera> informacionAduanera) {

        /** Checks that no component is null and copies the list. */
        public Parte {
            Objects.requireNonNull(claveProdServ, "claveProdServ");
            Objects.requireNonNull(noIdentificacion, "noIdentificacion");
            Objects.requireNonNull(cantidad, "cantidad");
            Objects.requireNonNull(unidad, "unidad");
            Objects.requireNonNull(descripcion, "descripcion");
            Objects.requireNonNull(valorUnitario, "valorUnitario");
            Objects.requireNonNull(importe, "importe");
            informacionAduanera = List.copyOf(informacionAduanera);
        }
    }
}
