package io.github.alktrip.cfdi4j.pagos20;

import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;

/** Entry point of the complemento de pagos 2.0. */
public final class Pagos20 {

    private Pagos20() {}

    /**
     * Starts a comprobante de recepción de pagos with the fixed values the SAT requires
     * (error matrix CRP20101 to CRP20123): TipoDeComprobante "P", Exportacion "01", Moneda "XXX",
     * SubTotal and Total 0, and the single concepto 84111506 / 1 / ACT / "Pago" / 0 / ObjetoImp
     * "01". Set Fecha, LugarExpedicion, Emisor and Receptor, and add the {@link Pagos} complement.
     *
     * <pre>{@code
     * Comprobante recibo = Pagos20.comprobante()
     *     .fecha(...).lugarExpedicion("72000").emisor(...).receptor(...)
     *     .complemento(Pagos.builder().pago(...).build())
     *     .build();
     * }</pre>
     *
     * @return a builder with the fixed values set
     */
    public static ComprobanteBuilder comprobante() {
        return Comprobante.builder()
                .tipoDeComprobante(TipoDeComprobante.PAGO)
                .exportacion(Exportacion.NO_APLICA)
                .moneda(Moneda.XXX)
                .concepto(c -> c.claveProdServ("84111506").cantidad("1").claveUnidad("ACT").descripcion("Pago")
                        .valorUnitario("0").objetoImp(ObjetoImp.NO_OBJETO));
    }
}
