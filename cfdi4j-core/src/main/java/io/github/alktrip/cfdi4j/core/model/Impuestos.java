package io.github.alktrip.cfdi4j.core.model;

import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Node {@code cfdi:Impuestos} of the comprobante: the tax summary.
 *
 * @param totalImpuestosRetenidos attribute {@code TotalImpuestosRetenidos}
 * @param totalImpuestosTrasladados attribute {@code TotalImpuestosTrasladados}
 * @param retenciones nodes {@code cfdi:Retenciones/cfdi:Retencion}; empty when the node is absent
 * @param traslados nodes {@code cfdi:Traslados/cfdi:Traslado}; empty when the node is absent
 */
public record Impuestos(
        Optional<BigDecimal> totalImpuestosRetenidos,
        Optional<BigDecimal> totalImpuestosTrasladados,
        List<Retencion> retenciones,
        List<Traslado> traslados) {

    /** Checks that no component is null and copies the lists. */
    public Impuestos {
        Objects.requireNonNull(totalImpuestosRetenidos, "totalImpuestosRetenidos");
        Objects.requireNonNull(totalImpuestosTrasladados, "totalImpuestosTrasladados");
        retenciones = List.copyOf(retenciones);
        traslados = List.copyOf(traslados);
    }

    /**
     * Node {@code cfdi:Impuestos/cfdi:Retenciones/cfdi:Retencion}.
     *
     * @param impuesto attribute {@code Impuesto}
     * @param importe attribute {@code Importe}
     */
    public record Retencion(Impuesto impuesto, BigDecimal importe) {

        /** Checks that no component is null. */
        public Retencion {
            Objects.requireNonNull(impuesto, "impuesto");
            Objects.requireNonNull(importe, "importe");
        }
    }

    /**
     * Node {@code cfdi:Impuestos/cfdi:Traslados/cfdi:Traslado}.
     *
     * @param base attribute {@code Base}
     * @param impuesto attribute {@code Impuesto}
     * @param tipoFactor attribute {@code TipoFactor}
     * @param tasaOCuota attribute {@code TasaOCuota}
     * @param importe attribute {@code Importe}
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
}
