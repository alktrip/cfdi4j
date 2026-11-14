package io.github.alktrip.cfdi4j.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import io.github.alktrip.cfdi4j.xml.internal.SatResourceResolver;
import io.github.alktrip.cfdi4j.xml.internal.SatResources;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.xml.validation.Schema;
import org.junit.jupiter.api.Test;

/** ADR 0007: the schemas compile and validate with every network connection refused. */
class OfflineSchemaTest {

    @Test
    void compilesAndValidatesWithoutAnyNetworkAccess() {
        List<URI> attempts = new CopyOnWriteArrayList<>();
        ProxySelector original = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                attempts.add(uri);
                throw new IllegalStateException("network access attempted: " + uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                attempts.add(uri);
            }
        });
        try {
            Schema schema = CfdiSchema.compile();
            CfdiSchema validator = new CfdiSchema(schema, Locale.ENGLISH);

            // The instance declares SAT schema locations; they must not be followed either.
            assertThat(validator.validate(Fixtures.sealedXml(Fixtures.simple())).violations()).isEmpty();
            assertThat(validator.validateUnsealed(Fixtures.bytes(Fixtures.complete())).errors()).isEmpty();
        } finally {
            ProxySelector.setDefault(original);
        }
        assertThat(attempts).isEmpty();
    }

    @Test
    void satUrlsMapToBundledFilesAndNothingElseIsResolved() {
        assertThat(SatResources.resourcePath("http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd"))
                .contains("sat/xml/www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd");
        assertThat(SatResources.resourcePath("https://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd"))
                .contains("sat/xml/www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd");
        assertThat(SatResources.resourcePath("http://example.com/x.xsd")).isEmpty();
        assertThat(SatResources.resourcePath("http://www.sat.gob.mx/../etc/passwd")).isEmpty();
        assertThat(SatResources.resourcePath("file:///etc/passwd")).isEmpty();
        assertThat(SatResources.resourcePath(null)).isEmpty();

        assertThatThrownBy(() -> SatResources.open("http://www.sat.gob.mx/sitio_internet/cfd/no/existe.xsd"))
                .isInstanceOf(Cfdi4jException.class).hasMessageContaining("never downloads");
        assertThatThrownBy(() -> new SatResourceResolver().resolveResource(null, null, null, "http://example.com/x.xsd", null))
                .isInstanceOf(Cfdi4jException.class);
    }

    @Test
    void resolverReturnsTheBundledBytes() throws IOException {
        var input = new SatResourceResolver().resolveResource("http://www.w3.org/2001/XMLSchema", null, "pub",
                "http://www.sat.gob.mx/sitio_internet/cfd/tipoDatos/tdCFDI/tdCFDI.xsd", "base");

        try (var in = input.getByteStream()) {
            assertThat(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).contains("t_RFC");
        }
        assertThat(input.getSystemId()).endsWith("tdCFDI.xsd");
        assertThat(input.getPublicId()).isEqualTo("pub");
        assertThat(input.getBaseURI()).isEqualTo("base");
        assertThat(input.getCharacterStream()).isNull();
        assertThat(input.getStringData()).isNull();
        assertThat(input.getEncoding()).isNull();
        assertThat(input.getCertifiedText()).isFalse();
        input.setSystemId("s");
        input.setPublicId("p");
        input.setBaseURI("b");
        input.setByteStream(null);
        assertThat(input.getSystemId()).isEqualTo("s");
        assertThat(input.getByteStream()).isNull();
    }
}
