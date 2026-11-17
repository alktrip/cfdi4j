import io.github.alktrip.cfdi4j.core.catalog.*;
import io.github.alktrip.cfdi4j.core.model.*;
import io.github.alktrip.cfdi4j.pagos20.Pagos;
import io.github.alktrip.cfdi4j.pagos20.Pagos20;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Writes the input documents of the layer-1 golden files of the complemento de pagos 2.0
 * (ADR 0005), with the same SAT public test data as {@code GoldenInputs}. The second document uses
 * the amounts of the example of the Guía de llenado (pages 76 and 77) and nothing else from it.
 *
 * <p>Usage: {@code tools/golden/generate.sh}, after {@code ./mvnw -pl cfdi4j-pagos20 -am compile}.
 */
public class GoldenPagosInputs {

    static final String NO_CERTIFICADO = "30001000000500003416";
    static final LocalDateTime FECHA_PAGO = LocalDateTime.of(2026, 11, 13, 12, 0, 0);

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        String certificado = Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of("specs/sat/csd-pruebas/EKU9003173C9/csd.cer")));
        write(out, "pago-mxn", recibo(Pagos.builder().pago(p -> p.fechaPago(FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.MXN)
                .monto("106.00").numOperacion("OP-0001").ctaOrdenante("012345678901234567").ctaBeneficiario("0123456789")
                .doctoRelacionado(d -> d.idDocumento("5FB2822E-396D-4725-8521-CDC4BDD20CCF").serie("A").folio("1").monedaDR(Moneda.MXN)
                        .numParcialidad(1).impSaldoAnt("106.00").impPagado("106.00").objetoImpDR(ObjetoImp.SI_OBJETO)
                        .trasladoDR("100.00", Impuesto.IVA, TipoFactor.TASA, "0.160000")
                        .retencionDR("100.00", Impuesto.ISR, TipoFactor.TASA, "0.100000"))).build()), certificado);
        write(out, "pago-usd-factura-mxn", recibo(Pagos.builder().pago(p -> p.fechaPago(FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA)
                .monedaP(Moneda.USD).tipoCambioP("17.7331").monto("19624.32")
                .doctoRelacionado(d -> d.idDocumento("0A1B2C3D-4E5F-4A6B-8C7D-9E0F1A2B3C4D").monedaDR(Moneda.MXN).equivalenciaDR("17.7331")
                        .numParcialidad(1).impSaldoAnt("348000.00").impPagado("348000.00").objetoImpDR(ObjetoImp.SI_OBJETO)
                        .trasladoDR("300000.00", Impuesto.IVA, TipoFactor.TASA, "0.160000"))).build()), certificado);
    }

    static Comprobante recibo(Pagos pagos) {
        return Pagos20.comprobante().serie("P").folio("1").fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0)).lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                        .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.PAGOS))
                .complemento(pagos).build();
    }

    static void write(Path dir, String name, Comprobante c, String certificado) throws Exception {
        Files.write(dir.resolve(name + ".xml"), CfdiXml.defaults().toBytes(c.withCertificado(NO_CERTIFICADO, certificado)));
    }
}
