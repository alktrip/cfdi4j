package io.github.alktrip.cfdi4j.catalogs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.core.catalog.CatalogVersion;
import io.github.alktrip.cfdi4j.core.catalog.ClaveProdServInfo;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.RegimenFiscal;
import io.github.alktrip.cfdi4j.core.catalog.Requisito;
import io.github.alktrip.cfdi4j.core.catalog.UsoCFDI;
import io.github.alktrip.cfdi4j.core.catalog.Vigencia;
import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.MetodoPago;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.catalog.Exportacion;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.ComprobanteBuilder;
import io.github.alktrip.cfdi4j.core.validation.Cfdi40Validator;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SatCatalogsTest {

    private static final SatCatalogs CATALOGOS = SatCatalogs.provider();

    @Test
    void findsProductKeys() {
        assertThat(CATALOGOS.claveProdServ("84111506")).contains(new ClaveProdServInfo("84111506", "Servicios de facturación",
                Requisito.OPCIONAL, Requisito.OPCIONAL, Optional.empty(), Vigencia.desde(LocalDate.of(2022, 1, 1))));
        assertThat(CATALOGOS.claveProdServ("01010101")).hasValueSatisfying(i -> assertThat(i.descripcion()).isEqualTo("No existe en el catálogo"));
        assertThat(CATALOGOS.claveProdServ("99999999")).isEmpty();
    }

    @Test
    void findsUnitKeys() {
        assertThat(CATALOGOS.claveUnidad("H87")).hasValueSatisfying(u -> assertThat(u.nombre()).isEqualTo("Pieza"));
        assertThat(CATALOGOS.claveUnidad("ACT")).hasValueSatisfying(u -> assertThat(u.nombre()).isEqualTo("Actividad"));
        assertThat(CATALOGOS.claveUnidad("Dx")).isPresent();
        assertThat(CATALOGOS.claveUnidad("dx")).isEmpty();
    }

    @Test
    void findsPostalCodesWithTheirTimeZones() {
        assertThat(CATALOGOS.codigoPostal("72000")).hasValueSatisfying(cp -> {
            assertThat(cp.estado()).isEqualTo("PUE");
            assertThat(cp.diferenciaHorariaVerano()).isEqualTo(ZoneOffset.ofHours(-6));
            assertThat(cp.diferenciaHorariaInvierno()).isEqualTo(ZoneOffset.ofHours(-6));
        });
        assertThat(CATALOGOS.codigoPostal("22000")).hasValueSatisfying(cp -> {
            assertThat(cp.estado()).isEqualTo("BCN");
            assertThat(cp.diferenciaHorariaVerano()).isEqualTo(ZoneOffset.ofHours(-7));
            assertThat(cp.diferenciaHorariaInvierno()).isEqualTo(ZoneOffset.ofHours(-8));
        });
        assertThat(CATALOGOS.codigoPostal("00000")).isEmpty();
    }

    @Test
    void loadsEveryRowOfTheOfficialFile() {
        // Rows counted in catCFDI_V_4_20261106.xls; postal codes come from its two sheets.
        assertThat(CATALOGOS.sizes()).containsEntry("c_ClaveProdServ", 52513).containsEntry("c_ClaveUnidad", 2418)
                .containsEntry("c_CodigoPostal", 95748);
    }

    @ParameterizedTest
    @ValueSource(strings = {"c_ClaveProdServ.tsv", "c_ClaveUnidad.tsv", "c_CodigoPostal.tsv"})
    void dataComesFromTheSameFileAsTheCoreCatalogs(String resource) throws IOException {
        try (InputStream in = SatCatalogs.class.getResourceAsStream(resource)) {
            String header = new String(in.readNBytes(300), StandardCharsets.UTF_8).lines().findFirst().orElseThrow();

            assertThat(header).contains(CatalogVersion.SOURCE_FILE).contains(CatalogVersion.SHA256);
        }
        assertThat(SatCatalogs.SOURCE_FILE).isEqualTo(CatalogVersion.SOURCE_FILE);
    }

    @Test
    void readsValidityWithAndWithoutEnd() {
        assertThat(SatCatalogs.vigencia("2022-01-01", "")).isEqualTo(Vigencia.desde(LocalDate.of(2022, 1, 1)));
        assertThat(SatCatalogs.vigencia("2022-01-01", "2023-12-31")).isEqualTo(Vigencia.entre(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 12, 31)));
    }

    @Test
    void rejectsMissingOrMalformedResources() {
        assertThatThrownBy(() -> SatCatalogs.load("no-existe.tsv", 2, f -> f)).isInstanceOf(IllegalStateException.class).hasMessageContaining("no-existe.tsv");
        assertThatThrownBy(() -> SatCatalogs.load("mal-formado.tsv", 3, f -> f)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("line 2: expected 3 fields but found 2");
    }

    // --- with the validator ---

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneOffset.UTC);
    private static final Set<String> CODIGOS_DE_CATALOGO = Set.of("CFDI40126", "CFDI40162", "CFDI40163", "CFDI40164", "CFDI40165");

    private static Comprobante factura(Consumer<ComprobanteBuilder.ConceptoBuilder> concepto) {
        return Comprobante.builder().serie("A").folio("1").fecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0))
                .formaPago(FormaPago.TRANSFERENCIA).metodoPago(MetodoPago.PUE).moneda(Moneda.MXN)
                .tipoDeComprobante(TipoDeComprobante.INGRESO).exportacion(Exportacion.NO_APLICA).lugarExpedicion("72000")
                .emisor(e -> e.rfc("EKU9003173C9").nombre("ESCUELA KEMPER URGATE").regimenFiscal(RegimenFiscal.GENERAL_PM))
                .receptor(r -> r.rfc("XIA190128J61").nombre("XENON INDUSTRIAL ARTICLES").domicilioFiscalReceptor("76343")
                        .regimenFiscalReceptor(RegimenFiscal.GENERAL_PM).usoCFDI(UsoCFDI.GASTOS_EN_GENERAL))
                .concepto(c -> {
                    c.claveProdServ("84111506").cantidad("1").claveUnidad("E48").descripcion("Servicio").valorUnitario("100.00")
                            .objetoImp(ObjetoImp.SI_OBJETO).trasladoIva16();
                    concepto.accept(c);
                })
                .build();
    }

    @Test
    void validatorFindsTheProviderAndVerifiesTheCatalogRules() {
        ValidationResult result = Cfdi40Validator.builder().clock(CLOCK).build().validate(factura(c -> { }));

        assertThat(result.errors()).isEmpty();
        assertThat(result.violations()).extracting(Violation::code).doesNotContainAnyElementsOf(CODIGOS_DE_CATALOGO);
    }

    @Test
    void unknownKeysAreErrors() {
        ValidationResult result = Cfdi40Validator.builder().clock(CLOCK).build()
                .validate(factura(c -> c.claveProdServ("99999999").claveUnidad("ZZ0")));

        assertThat(result.errors()).as("%s", result.violations()).extracting(Violation::code).contains("CFDI40162", "CFDI40165");
    }

    @Test
    void anExplicitNullProviderDisablesTheCatalogRules() {
        ValidationResult result = Cfdi40Validator.builder().clock(CLOCK).catalogProvider(null).build().validate(factura(c -> { }));

        assertThat(result.warnings()).extracting(Violation::code).contains("CFDI40162", "CFDI40165");
    }
}
