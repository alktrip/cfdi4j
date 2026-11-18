package io.github.alktrip.cfdi4j.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.Cfdi4j;
import io.github.alktrip.cfdi4j.CfdiEngine;
import io.github.alktrip.cfdi4j.SealedCfdi;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.crypto.Csd;
import io.github.alktrip.cfdi4j.xml.CfdiXml;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * One engine and one CSD shared by many threads: every document sealed in parallel must be
 * identical to the one sealed alone, and must validate and verify.
 */
class ConcurrencyTest {

    private static final Path CSD_DIR = Path.of("..", "specs", "sat", "csd-pruebas", "EKU9003173C9");

    @Test
    void validateSealAndVerifyInParallelGiveTheSameResultsAsSequentially() throws Exception {
        Csd csd = Csd.load(CSD_DIR.resolve("csd.cer"), CSD_DIR.resolve("csd.key"), "12345678a".toCharArray());
        CfdiEngine engine = Cfdi4j.engine();
        Comprobante factura = CfdiXml.defaults().parse(QuickStart.run(CSD_DIR));
        Comprobante recibo = ReciboDePago.run(CSD_DIR).comprobante();
        List<Comprobante> documentos = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            Comprobante base = i % 2 == 0 ? factura : recibo;
            documentos.add(rebuildWithFolio(base, Integer.toString(i)));
        }
        List<String> esperados = new ArrayList<>();
        for (Comprobante c : documentos) {
            esperados.add(engine.seal(c, csd).toXml());
        }

        List<Callable<String>> tareas = new ArrayList<>();
        for (Comprobante c : documentos) {
            tareas.add(() -> {
                assertThat(engine.validate(c).errors()).isEmpty();
                SealedCfdi sealed = engine.seal(c, csd);
                assertThat(engine.verify(sealed.toBytes()).isValid()).isTrue();
                assertThat(engine.validate(sealed.toBytes()).errors()).isEmpty();
                return sealed.toXml();
            });
        }
        List<String> resultados = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (Future<String> f : pool.invokeAll(tareas)) {
                resultados.add(f.get());
            }
        }

        assertThat(resultados).containsExactlyElementsOf(esperados);
    }

    /** The same document with another folio, so every task seals different content. */
    private static Comprobante rebuildWithFolio(Comprobante c, String folio) {
        String xml = CfdiXml.defaults().toXml(c);
        return CfdiXml.defaults().parse(xml.replaceFirst("<cfdi:Comprobante ", "<cfdi:Comprobante Folio=\"" + folio + "\" "));
    }
}
