package io.github.alktrip.cfdi4j.xml;

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
import io.github.alktrip.cfdi4j.core.model.Addenda;
import io.github.alktrip.cfdi4j.core.model.ComplementoNoModelado;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import java.time.LocalDateTime;
import java.util.Optional;

/** Comprobantes for tests. RFCs and names are SAT public test data (Certificados_de_Prueba.zip). */
final class Fixtures {

    static final String TFD_NS = "http://www.sat.gob.mx/TimbreFiscalDigital";

    private Fixtures() {}

    /** The XML of a comprobante with seal attributes filled in (not a real seal; for schema tests). */
    static byte[] sealedXml(Comprobante cfdi) {
        String xml = CfdiXml.defaults().toXml(cfdi);
        String sealed = xml.replaceFirst(" Fecha=\"([^\"]+)\"",
                " Fecha=\"$1\" Sello=\"c2VsbG8=\" NoCertificado=\"30001000000500003416\" Certificado=\"Y2VydA==\"");
        return sealed.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    static byte[] bytes(Comprobante cfdi) {
        return CfdiXml.defaults().toBytes(cfdi);
    }

    static ComprobanteBuilder base() {
        return Comprobante.builder()
                .serie("A")
                .folio("123")
                .fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0))
                .formaPago(FormaPago.TRANSFERENCIA)
                .metodoPago(MetodoPago.PUE)
                .moneda(Moneda.MXN)
                .tipoDeComprobante(TipoDeComprobante.INGRESO)
                .exportacion(Exportacion.NO_APLICA)
                .lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XAXX010101000").nombre("PUBLICO EN GENERAL").domicilioFiscalReceptor("72000")
                        .regimenFiscalReceptor(RegimenFiscal.SIN_OBLIGACIONES_FISCALES).usoCFDI(UsoCFDI.SIN_EFECTOS_FISCALES));
    }

    static Comprobante simple() {
        return base()
                .concepto(c -> c.claveProdServ("01010101").cantidad("1").claveUnidad("ACT").descripcion("Servicio")
                        .valorUnitario("100.00").objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16())
                .build();
    }

    /** Uses every node and optional attribute of cfdv40.xsd. */
    static Comprobante complete() {
        return base()
                .condicionesDePago("Contado & <inmediato>")
                .moneda(Moneda.USD)
                .tipoCambio("17.123456")
                .confirmacion("ECVH1")
                .informacionGlobal(Periodicidad.MENSUAL, Meses.NOVIEMBRE, 2026)
                .cfdiRelacionados(TipoRelacion.SUSTITUCION, "5FB2822E-396D-4725-8521-CDC4BDD20CCF", "5FB2822E-396D-4725-8521-CDC4BDD20CC0")
                .cfdiRelacionados(TipoRelacion.NOTA_CREDITO, "5FB2822E-396D-4725-8521-CDC4BDD20CC1")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM)
                        .facAtrAdquirente("0123456789"))
                .receptor(r -> r.rfc("XEXX010101000").nombre("CLIENTE \"EXTRANJERO\"").domicilioFiscalReceptor("72000")
                        .residenciaFiscal(Pais.USA).numRegIdTrib("123456789")
                        .regimenFiscalReceptor(RegimenFiscal.SIN_OBLIGACIONES_FISCALES).usoCFDI(UsoCFDI.SIN_EFECTOS_FISCALES))
                .concepto(c -> c.claveProdServ("84111506").noIdentificacion("SKU-1").cantidad("1.500000").claveUnidad("E48")
                        .unidad("Servicio").descripcion("Honorarios").valorUnitario("1000.000000").descuento("10.00")
                        .objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16().trasladoExento(Impuesto.IEPS)
                        .retencion(Impuesto.ISR, TipoFactor.TASA, "0.100000")
                        .aCuentaTerceros("JUFA7608212V6", "ADRIANA JUAREZ FERNANDEZ", RegimenFiscal.ACTIVIDADES_EMPRESARIALES_PF, "29133")
                        .informacionAduanera("21  47  3807  8003832")
                        .cuentaPredial("15956011002")
                        .complementoConcepto(new ComplementoNoModelado("http://www.sat.gob.mx/iedu",
                                "<iedu:instEducativas xmlns:iedu=\"http://www.sat.gob.mx/iedu\" CURP=\"XEXX010101HNEXXXA4\" "
                                        + "nivelEducativo=\"Primaria\" nombreAlumno=\"ALUMNO\" autRVOE=\"1234\" version=\"1.0\"/>",
                                Optional.of("http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xsd")))
                        .parte(p -> p.claveProdServ("84111506").noIdentificacion("P-1").cantidad("2").unidad("Pieza")
                                .descripcion("Parte").valorUnitario("5.00").importe("10.00").informacionAduanera("21  47  3807  8003832")))
                .concepto(c -> c.claveProdServ("01010101").cantidad("3").claveUnidad("H87").descripcion("Pieza")
                        .valorUnitario("0.333333").objetoImp(ObjetoImp.NO_OBJETO))
                .complemento(new ComplementoNoModelado(TFD_NS,
                        "<tfd:TimbreFiscalDigital xmlns:tfd=\"" + TFD_NS + "\" Version=\"1.1\" "
                                + "UUID=\"5FB2822E-396D-4725-8521-CDC4BDD20CCF\" FechaTimbrado=\"2026-11-14T10:31:00\" "
                                + "RfcProvCertif=\"KIJ0906199R1\" SelloCFD=\"abc\" NoCertificadoSAT=\"30001000000500003456\" SelloSAT=\"def\"/>",
                        Optional.of("http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd")))
                .addenda(new Addenda("<ext:Pedido xmlns:ext=\"http://example.com/ext\" Numero=\"42\"><ext:Nota>texto</ext:Nota></ext:Pedido>"
                        + "<otra xmlns=\"http://example.com/otra\"/>"))
                .build();
    }
}
