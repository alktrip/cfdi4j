package io.github.alktrip.cfdi4j.core.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Behaviour shared by every generated open-enum catalog (ADR 0003). */
class CatalogKeyTest {

    static Stream<Arguments> catalogs() {
        return Stream.of(
                catalog("c_FormaPago", FormaPago.values(), FormaPago::of),
                catalog("c_MetodoPago", MetodoPago.values(), MetodoPago::of),
                catalog("c_TipoDeComprobante", TipoDeComprobante.values(), TipoDeComprobante::of),
                catalog("c_Exportacion", Exportacion.values(), Exportacion::of),
                catalog("c_Moneda", Moneda.values(), Moneda::of),
                catalog("c_Pais", Pais.values(), Pais::of),
                catalog("c_RegimenFiscal", RegimenFiscal.values(), RegimenFiscal::of),
                catalog("c_UsoCFDI", UsoCFDI.values(), UsoCFDI::of),
                catalog("c_ObjetoImp", ObjetoImp.values(), ObjetoImp::of),
                catalog("c_Impuesto", Impuesto.values(), Impuesto::of),
                catalog("c_TipoFactor", TipoFactor.values(), TipoFactor::of),
                catalog("c_TipoRelacion", TipoRelacion.values(), TipoRelacion::of),
                catalog("c_Periodicidad", Periodicidad.values(), Periodicidad::of),
                catalog("c_Meses", Meses.values(), Meses::of));
    }

    private static <T extends CatalogKey> Arguments catalog(String name, List<T> values, Function<String, T> of) {
        return Arguments.of(name, values, of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogs")
    void ofReturnsTheConstantForEveryKnownKey(String name, List<CatalogKey> values, Function<String, CatalogKey> of) {
        assertThat(values).isNotEmpty();
        for (CatalogKey value : values) {
            assertThat(of.apply(value.clave())).isSameAs(value);
            assertThat(value.isKnown()).isTrue();
            assertThat(value).hasToString(value.clave());
        }
        assertThat(values).extracting(CatalogKey::clave).doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogs")
    void ofNeverFailsForAnUnknownKey(String name, List<CatalogKey> values, Function<String, CatalogKey> of) {
        CatalogKey unknown = of.apply("NOT-IN-CATALOG");

        assertThat(unknown.isKnown()).isFalse();
        assertThat(unknown.clave()).isEqualTo("NOT-IN-CATALOG");
        assertThat(unknown.descripcion()).isEmpty();
        assertThat(unknown.vigencia()).isEmpty();
        assertThat(unknown.isVigente(LocalDate.of(2026, 1, 1))).isFalse();
        assertThat(unknown).isEqualTo(of.apply("NOT-IN-CATALOG")).hasSameHashCodeAs(of.apply("NOT-IN-CATALOG"));
        assertThat(values).doesNotContain(unknown);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogs")
    void ofRejectsNull(String name, List<CatalogKey> values, Function<String, CatalogKey> of) {
        assertThatNullPointerException().isThrownBy(() -> of.apply(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogs")
    void keysOfDifferentCatalogsAreNeverEqual(String name, List<CatalogKey> values, Function<String, CatalogKey> of) {
        CatalogKey first = values.get(0);
        Object other = first instanceof Meses ? Periodicidad.of(first.clave()) : Meses.of(first.clave());

        assertThat(first).isNotEqualTo(other);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogs")
    void everyKnownKeyIsVigenteOnItsFirstDayUnlessTheCatalogHasNoDates(
            String name, List<CatalogKey> values, Function<String, CatalogKey> of) {
        for (CatalogKey value : values) {
            LocalDate day = value.vigencia().map(Vigencia::inicio).orElse(LocalDate.of(2022, 1, 1));
            assertThat(value.isVigente(day)).as("%s %s", name, value).isTrue();
        }
    }
}
