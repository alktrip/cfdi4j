package io.github.alktrip.cfdi4j;

import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import io.github.alktrip.cfdi4j.crypto.Csd;
import java.nio.file.Path;
import java.time.LocalDateTime;

/** SAT public test data: emisor EKU9003173C9 and its test CSD (password from the SAT archive). */
final class Fixtures {

    static final Path CSD_DIR = Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9");
    static final Path GOLDEN = Path.of("..", "cfdi4j-xml", "src", "test", "resources", "golden", "own");
    static final Csd CSD = Csd.load(CSD_DIR.resolve("csd.cer"), CSD_DIR.resolve("csd.key"), "12345678a".toCharArray());

    private Fixtures() {}

    static ComprobanteBuilder base() {
        return Comprobante.builder().serie("A").folio("123").fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0))
                .formaPago(FormaPago.TRANSFERENCIA).metodoPago(MetodoPago.PUE).moneda(Moneda.MXN)
                .tipoDeComprobante(TipoDeComprobante.INGRESO).exportacion(Exportacion.NO_APLICA).lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                        .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.GASTOS_EN_GENERAL));
    }

    static Comprobante simple() {
        return base().concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16()).build();
    }
}
