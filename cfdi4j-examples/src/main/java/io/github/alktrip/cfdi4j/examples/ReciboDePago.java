package io.github.alktrip.cfdi4j.examples;

import io.github.alktrip.cfdi4j.Cfdi4j;
import io.github.alktrip.cfdi4j.SealedCfdi;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.crypto.Csd;
import io.github.alktrip.cfdi4j.pagos20.Pagos;
import io.github.alktrip.cfdi4j.pagos20.Pagos20;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * A recibo electrónico de pago (complemento de pagos 2.0): a payment in dollars of an invoice in
 * pesos. The builder derives ImpuestosP and Totales from the paid document.
 *
 * <p>Usage: {@code ReciboDePago <directory with csd.cer and csd.key>}.
 */
public final class ReciboDePago {

    private ReciboDePago() {}

    /**
     * Builds and seals the recibo.
     *
     * @param dir directory with the SAT test {@code csd.cer} and {@code csd.key}
     * @return the sealed recibo
     */
    public static SealedCfdi run(Path dir) {
        Csd csd = Csd.load(dir.resolve("csd.cer"), dir.resolve("csd.key"), "12345678a".toCharArray());

        Pagos pagos = Pagos.builder()
                .pago(p -> p.fechaPago(LocalDateTime.of(2026, 11, 13, 12, 0)).formaDePagoP(FormaPago.TRANSFERENCIA)
                        .monedaP(Moneda.USD).tipoCambioP("17.7331").monto("19624.32")
                        .doctoRelacionado(d -> d.idDocumento("5FB2822E-396D-4725-8521-CDC4BDD20CCF").monedaDR(Moneda.MXN)
                                .equivalenciaDR("17.7331").numParcialidad(1).impSaldoAnt("348000.00").impPagado("348000.00")
                                .objetoImpDR(ObjetoImp.SI_OBJETO).trasladoDR("300000.00", Impuesto.IVA, TipoFactor.TASA, "0.160000")))
                .build();

        Comprobante recibo = Pagos20.comprobante()       // tipo P, moneda XXX and the fixed concepto
                .fecha(LocalDateTime.of(2026, 11, 14, 10, 30)).lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                        .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.PAGOS))
                .complemento(pagos)
                .build();

        return Cfdi4j.seal(recibo, csd);
    }

    /**
     * Entry point.
     *
     * @param args the directory with the test CSD
     */
    public static void main(String[] args) {
        System.out.println(run(Path.of(args.length > 0 ? args[0] : "specs/sat/csd-pruebas/EKU9003173C9")).toXml());
    }
}
