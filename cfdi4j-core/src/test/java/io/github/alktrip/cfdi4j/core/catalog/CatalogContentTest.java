package io.github.alktrip.cfdi4j.core.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Spot checks of generated values against the official file {@code catCFDI_V_4_20261106.xls}
 * (values read by hand from the sheets named in each test).
 */
class CatalogContentTest {

    @Test
    void catalogVersionMatchesTheFileRecordedInSources() throws IOException {
        String sources = Files.readString(Path.of("..", "specs", "SOURCES.md"));

        assertThat(sources).contains("`" + CatalogVersion.SOURCE_FILE + "`").contains("`" + CatalogVersion.SHA256 + "`");
    }

    @Test
    void formaPago() {
        assertThat(FormaPago.of("03")).isSameAs(FormaPago.TRANSFERENCIA);
        assertThat(FormaPago.TRANSFERENCIA.descripcion()).contains("Transferencia electrónica de fondos");
        assertThat(FormaPago.TRANSFERENCIA.bancarizado()).contains(Requisito.SI);
        assertThat(FormaPago.EFECTIVO.bancarizado()).contains(Requisito.NO);
        assertThat(FormaPago.POR_DEFINIR.bancarizado()).contains(Requisito.OPCIONAL);
        assertThat(FormaPago.of("07").bancarizado()).isEmpty();
        assertThat(FormaPago.values()).hasSize(22);
    }

    @Test
    void regimenFiscalTipoPersona() {
        assertThat(RegimenFiscal.GENERAL_PM.clave()).isEqualTo("601");
        assertThat(RegimenFiscal.GENERAL_PM.personaMoral()).isTrue();
        assertThat(RegimenFiscal.GENERAL_PM.personaFisica()).isFalse();
        assertThat(RegimenFiscal.RESICO.personaFisica()).isTrue();
        assertThat(RegimenFiscal.RESICO.personaMoral()).isTrue();
        assertThat(RegimenFiscal.of("999").personaFisica()).isFalse();
        assertThat(RegimenFiscal.of("999").personaMoral()).isFalse();
    }

    @Test
    void usoCfdiRegimenesReceptor() {
        assertThat(UsoCFDI.NOMINA.clave()).isEqualTo("CN01");
        assertThat(UsoCFDI.NOMINA.regimenesFiscalesReceptor()).containsExactly(RegimenFiscal.SUELDOS_SALARIOS);
        assertThat(UsoCFDI.NOMINA.personaMoral()).isFalse();
        assertThat(UsoCFDI.SIN_EFECTOS_FISCALES.descripcion()).contains("Sin efectos fiscales.");
        assertThat(UsoCFDI.SIN_EFECTOS_FISCALES.regimenesFiscalesReceptor()).hasSize(19).contains(RegimenFiscal.SIN_OBLIGACIONES_FISCALES);
        assertThat(UsoCFDI.GASTOS_EN_GENERAL.regimenesFiscalesReceptor()).doesNotContain(RegimenFiscal.SIN_OBLIGACIONES_FISCALES);
        assertThat(UsoCFDI.of("X99").regimenesFiscalesReceptor()).isEmpty();
        assertThat(UsoCFDI.of("X99").personaFisica()).isFalse();
    }

    @Test
    void monedaDecimalesYVariacion() {
        assertThat(Moneda.MXN.decimales()).isEqualTo(OptionalInt.of(2));
        assertThat(Moneda.JPY.decimales()).isEqualTo(OptionalInt.of(0));
        assertThat(Moneda.OMR.decimales()).isEqualTo(OptionalInt.of(3));
        assertThat(Moneda.CLF.decimales()).isEqualTo(OptionalInt.of(4));
        assertThat(Moneda.USD.porcentajeVariacion()).contains(new BigDecimal("5.00"));
        assertThat(Moneda.XXX.porcentajeVariacion()).isEmpty();
        assertThat(Moneda.of("ZZZ").decimales()).isEmpty();
        assertThat(Moneda.values()).hasSize(183);
    }

    @Test
    void tipoDeComprobanteValorMaximo() {
        assertThat(TipoDeComprobante.INGRESO.valorMaximo()).contains(new BigDecimal("999999999999999999.999999"));
        assertThat(TipoDeComprobante.TRASLADO.valorMaximo()).hasValueSatisfying(v -> assertThat(v).isZero());
        assertThat(TipoDeComprobante.NOMINA.valorMaximo()).isEmpty();
    }

    @Test
    void paisFormatos() {
        assertThat(Pais.MEX.formatoCodigoPostal()).contains("[0-9]{5}");
        assertThat(Pais.MEX.validacionRegistroIdentidadTributaria()).contains("Lista del SAT");
        assertThat(Pais.MEX.agrupaciones()).containsExactly("TLCAN");
        assertThat(Pais.DEU.agrupaciones()).containsExactly("Unión Europea");
        assertThat(Pais.AFG.formatoCodigoPostal()).isEmpty();
        assertThat(Pais.MEX.vigencia()).isEmpty();
        assertThat(Pais.MEX.isVigente(LocalDate.of(2030, 1, 1))).isTrue();
        assertThat(Pais.of("XYZ").agrupaciones()).isEmpty();
        assertThat(Pais.of("XYZ").formatoRegistroIdentidadTributaria()).isEmpty();
    }

    @Test
    void impuesto() {
        assertThat(Impuesto.ISR.retencion()).isTrue();
        assertThat(Impuesto.ISR.traslado()).isFalse();
        assertThat(Impuesto.IVA.traslado()).isTrue();
        assertThat(Impuesto.IEPS.localOFederal()).contains("Federal");
        assertThat(Impuesto.of("004").retencion()).isFalse();
        assertThat(Impuesto.of("004").localOFederal()).isEmpty();
    }

    @Test
    void tipoFactorHasNoDescriptionColumn() {
        assertThat(TipoFactor.EXENTO.clave()).isEqualTo("Exento");
        assertThat(TipoFactor.EXENTO.descripcion()).isEmpty();
        assertThat(TipoFactor.EXENTO.isKnown()).isTrue();
    }

    @Test
    void objetoImpVigencias() {
        assertThat(ObjetoImp.SI_OBJETO_IVA_NO_TRASLADO.vigencia()).contains(Vigencia.desde(LocalDate.of(2024, 12, 13)));
        assertThat(ObjetoImp.SI_OBJETO_IVA_NO_TRASLADO.isVigente(LocalDate.of(2024, 12, 12))).isFalse();
        assertThat(ObjetoImp.SI_OBJETO_NO_CAUSA.vigencia()).contains(Vigencia.desde(LocalDate.of(2022, 10, 7)));
    }

    @Test
    void tasaOCuota() {
        assertThat(TasaOCuota.reglas()).hasSize(19);
        assertThat(TasaOCuota.reglas()).anySatisfy(r -> {
            assertThat(r.impuesto()).isEqualTo(Impuesto.IVA);
            assertThat(r.tipoFactor()).isEqualTo(TipoFactor.TASA);
            assertThat(r.traslado()).isTrue();
            assertThat(r.rango()).isFalse();
            assertThat(r.admite(new BigDecimal("0.16"))).isTrue();
        });
        assertThat(TasaOCuota.reglas()).anySatisfy(r -> {
            assertThat(r.impuestoPublicado()).isEqualTo("IVA Crédito aplicado del 50%");
            assertThat(r.impuesto()).isEqualTo(Impuesto.IVA);
            assertThat(r.maximo()).isEqualByComparingTo("0.08");
        });
        assertThat(TasaOCuota.reglas()).anySatisfy(r -> {
            assertThat(r.impuesto()).isEqualTo(Impuesto.ISR);
            assertThat(r.rango()).isTrue();
            assertThat(r.retencion()).isTrue();
            assertThat(r.minimo()).isEqualByComparingTo("0");
            assertThat(r.maximo()).isEqualByComparingTo("0.35");
        });
        assertThat(TasaOCuota.reglas()).anySatisfy(r -> {
            assertThat(r.impuesto()).isEqualTo(Impuesto.IEPS);
            assertThat(r.tipoFactor()).isEqualTo(TipoFactor.CUOTA);
            assertThat(r.maximo()).isEqualByComparingTo("72.1605");
            assertThat(r.vigencia()).isEqualTo(Vigencia.desde(LocalDate.of(2026, 1, 1)));
        });
    }
}
