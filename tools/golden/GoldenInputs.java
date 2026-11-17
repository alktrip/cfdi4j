import io.github.alktrip.cfdi4j.core.catalog.*;
import io.github.alktrip.cfdi4j.core.model.*;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Writes the input documents of the layer-1 golden files (ADR 0005).
 *
 * <p>Usage, from the repository root, after {@code ./mvnw -pl cfdi4j-xml -am test-compile}:
 * {@code tools/golden/generate.sh}. Every document uses only SAT public test data: emisor
 * EKU9003173C9 with its test CSD (specs/sat/csd-pruebas), receptor XIA190128J61 (SAT test
 * persona moral) and the generic RFCs XAXX010101000 and XEXX010101000. Every document passes the
 * business rules of cfdi4j-core.
 */
public class GoldenInputs {

    static final String NO_CERTIFICADO = "30001000000500003416";

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        String certificado = Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of("specs/sat/csd-pruebas/EKU9003173C9/csd.cer")));
        write(out, "ingreso-basico", basico(), certificado);
        write(out, "ingreso-completo", completo(), certificado);
        write(out, "espacios-y-caracteres", caracteres(), certificado);
        write(out, "extranjero-retenciones", extranjero(), certificado);
        write(out, "traslado", traslado(), certificado);
    }

    static void write(Path dir, String name, Comprobante c, String certificado) throws Exception {
        Files.write(dir.resolve(name + ".xml"), CfdiXml.defaults().toBytes(c.withCertificado(NO_CERTIFICADO, certificado)));
    }

    static ComprobanteBuilder base() {
        return Comprobante.builder().serie("A").folio("1").fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0))
                .formaPago(FormaPago.TRANSFERENCIA).metodoPago(MetodoPago.PUE).moneda(Moneda.MXN)
                .tipoDeComprobante(TipoDeComprobante.INGRESO).exportacion(Exportacion.NO_APLICA).lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                        .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.GASTOS_EN_GENERAL));
    }

    static Comprobante basico() {
        return base().concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16()).build();
    }

    static Comprobante completo() {
        // Factura global a público en general: requires InformacionGlobal (CFDI40130).
        return base().folio("2").condicionesDePago("Contado")
                .receptor(r -> r.rfc("XAXX010101000").nombre("PUBLICO EN GENERAL").domicilioFiscalReceptor("72000")
                        .regimenFiscalReceptor(RegimenFiscal.SIN_OBLIGACIONES_FISCALES).usoCFDI(UsoCFDI.SIN_EFECTOS_FISCALES))
                .informacionGlobal(Periodicidad.MENSUAL, Meses.OCTUBRE, 2026)
                .cfdiRelacionados(TipoRelacion.SUSTITUCION, "5FB2822E-396D-4725-8521-CDC4BDD20CCF")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM)
                        .facAtrAdquirente("0123456789"))
                .concepto(c -> c.claveProdServ("86121500").noIdentificacion("COL-1").cantidad("1.000000").claveUnidad("E48")
                        .unidad("Servicio").descripcion("Colegiatura").valorUnitario("2500.000000").descuento("250.00")
                        .objetoImp(ObjetoImp.SI_OBJETO).trasladoExento(Impuesto.IVA)
                        .aCuentaTerceros("XIA190128J61", "XENON INDUSTRIAL ARTICLES", RegimenFiscal.GENERAL_PM, "76343")
                        .informacionAduanera("21  47  3807  8003832")
                        .cuentaPredial("15956011002")
                        .complementoConcepto(new ComplementoNoModelado("http://www.sat.gob.mx/iedu",
                                "<iedu:instEducativas xmlns:iedu=\"http://www.sat.gob.mx/iedu\" version=\"1.0\" nombreAlumno=\"ALUMNO DE PRUEBA\" "
                                        + "CURP=\"XEXX010101HNEXXXA4\" nivelEducativo=\"Primaria\" autRVOE=\"12345\"/>",
                                Optional.of("http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xsd")))
                        .parte(p -> p.claveProdServ("86121500").cantidad("1").descripcion("Material").valorUnitario("10.00").importe("10.00")))
                .concepto(c -> c.claveProdServ("01010101").cantidad("3").claveUnidad("H87").descripcion("Cuaderno")
                        .valorUnitario("0.333333").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .addenda(new Addenda("<x:Pedido xmlns:x=\"http://example.com/x\" Numero=\"42\"/>"))
                .build();
    }

    static Comprobante caracteres() {
        return base().folio("3")
                .concepto(c -> c.claveProdServ("01010101").cantidad("2").claveUnidad("H87")
                        .descripcion("  Reparación   de \"lavadora\" & <secadora>\tñandú  ")
                        .valorUnitario("10.50").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();
    }

    static Comprobante extranjero() {
        return base().folio("4").moneda(Moneda.USD).tipoCambio("17.123456").formaPago(FormaPago.POR_DEFINIR).metodoPago(MetodoPago.PPD)
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XEXX010101000").nombre("CLIENTE EXTRANJERO").domicilioFiscalReceptor("72000")
                        .residenciaFiscal(Pais.USA).numRegIdTrib("123456789")
                        .regimenFiscalReceptor(RegimenFiscal.SIN_OBLIGACIONES_FISCALES).usoCFDI(UsoCFDI.SIN_EFECTOS_FISCALES))
                .concepto(c -> c.claveProdServ("84111506").cantidad("10").claveUnidad("E48").descripcion("Consultoría")
                        .valorUnitario("123.4567").descuento("34.567").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16()
                        .retencion(Impuesto.ISR, TipoFactor.TASA, "0.100000")
                        .retencion(Impuesto.IVA, TipoFactor.TASA, "0.106666"))
                .build();
    }

    static Comprobante traslado() {
        return base().folio("5").formaPago(null).metodoPago(null).tipoDeComprobante(TipoDeComprobante.TRASLADO)
                .concepto(c -> c.claveProdServ("01010101").cantidad("5").claveUnidad("H87").descripcion("Mercancía en tránsito")
                        .valorUnitario("200.00").objetoImp(ObjetoImp.NO_OBJETO))
                .build();
    }
}
