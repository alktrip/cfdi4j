package io.github.alktrip.cfdi4j.examples;

import io.github.alktrip.cfdi4j.Cfdi4j;
import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.crypto.Csd;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * The quick start of the README: build, validate, seal and verify a factura with the SAT test CSD
 * of EKU9003173C9. A test checks that the README shows exactly this code.
 *
 * <p>Usage: {@code QuickStart <directory with csd.cer and csd.key>}.
 */
public final class QuickStart {

    private QuickStart() {}

    /**
     * Runs the quick start.
     *
     * @param dir directory with the SAT test {@code csd.cer} and {@code csd.key}
     * @return the sealed XML
     */
    public static String run(Path dir) {
        // tag::quickstart[]
        Csd csd = Csd.load(dir.resolve("csd.cer"), dir.resolve("csd.key"), "12345678a".toCharArray());
        Comprobante cfdi = Comprobante.builder().fecha(LocalDateTime.of(2026, 11, 14, 10, 30)).lugarExpedicion("72000")
            .tipoDeComprobante(TipoDeComprobante.INGRESO).exportacion(Exportacion.NO_APLICA).moneda(Moneda.MXN)
            .formaPago(FormaPago.TRANSFERENCIA).metodoPago(MetodoPago.PUE)
            .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
            .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.GASTOS_EN_GENERAL))
            .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
            .build();

        ValidationResult result = Cfdi4j.validate(cfdi);        // business rules + XSD
        String xml = Cfdi4j.seal(cfdi, csd).toXml();             // cadena original + SHA256withRSA
        boolean intact = Cfdi4j.verify(xml).isValid();           // not altered since it was sealed
        // end::quickstart[]
        System.out.println("errors: " + result.errors().size() + ", intact: " + intact);
        return xml;
    }

    /**
     * Entry point.
     *
     * @param args the directory with the test CSD
     */
    public static void main(String[] args) {
        System.out.println(run(Path.of(args.length > 0 ? args[0] : "specs/sat/csd-pruebas/EKU9003173C9")));
    }
}
