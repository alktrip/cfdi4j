package io.github.alktrip.cfdi4j.core.internal.rules;

import io.github.alktrip.cfdi4j.core.catalog.CatalogKey;
import io.github.alktrip.cfdi4j.core.catalog.TipoDeComprobante;
import io.github.alktrip.cfdi4j.core.internal.Importes;
import io.github.alktrip.cfdi4j.core.model.Complemento;
import io.github.alktrip.cfdi4j.core.model.Comprobante;
import io.github.alktrip.cfdi4j.core.model.Concepto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

/** Facts about a comprobante shared by several rules. */
final class Fiscal {

    /** RFC genérico nacional (Anexo 20, Receptor/Rfc). */
    static final String RFC_GENERICO_NACIONAL = "XAXX010101000";
    /** RFC genérico extranjero (Anexo 20, Receptor/Rfc). */
    static final String RFC_GENERICO_EXTRANJERO = "XEXX010101000";
    /** RFC of a third party abroad (Anexo 20, ACuentaTerceros). */
    static final String RFC_TERCERO_EXTRANJERO = "EXT990101NI1";
    /** Name required with {@link #RFC_TERCERO_EXTRANJERO} (error matrix, CFDI40189). */
    static final String NOMBRE_TERCERO_EXTRANJERO = "EXPEDICIÓN DE CFDI POR RESIDENTES EN MÉXICO QUE PRESTAN SERVICIOS DE "
            + "INTERMEDIACIÓN ENTRE TERCEROS A OFERENTES DE BIENES Y SERVICIOS RESIDENTES EN EL EXTRANJERO";
    static final String PUBLICO_EN_GENERAL = "PUBLICO EN GENERAL";

    /** Namespaces of the complemento de comercio exterior (bundled ComercioExterior11/20 XSLT). */
    static final Set<String> COMERCIO_EXTERIOR = Set.of("http://www.sat.gob.mx/ComercioExterior11", "http://www.sat.gob.mx/ComercioExterior20");
    /** Namespace of the complemento de impuestos locales (bundled implocal.xslt). */
    static final String IMPUESTOS_LOCALES = "http://www.sat.gob.mx/implocal";
    /** Namespace of the complemento concepto de hidrocarburos y petrolíferos (bundled XSLT). */
    static final String HIDROCARBUROS = "http://www.sat.gob.mx/hidrocarburospetroliferos";

    static final String COMPROBANTE = "/cfdi:Comprobante";

    private Fiscal() {}

    static String at(String attribute) {
        return COMPROBANTE + "/@" + attribute;
    }

    static String concepto(int index) {
        return COMPROBANTE + "/cfdi:Conceptos/cfdi:Concepto[" + (index + 1) + "]";
    }

    static boolean ingresoEgresoNomina(Comprobante c) {
        TipoDeComprobante t = c.tipoDeComprobante();
        return t.equals(TipoDeComprobante.INGRESO) || t.equals(TipoDeComprobante.EGRESO) || t.equals(TipoDeComprobante.NOMINA);
    }

    static boolean trasladoOPago(Comprobante c) {
        return c.tipoDeComprobante().equals(TipoDeComprobante.TRASLADO) || c.tipoDeComprobante().equals(TipoDeComprobante.PAGO);
    }

    /** Decimals the currency supports, if the currency is in c_Moneda. */
    static OptionalInt decimalesMoneda(Comprobante c) {
        return c.moneda().decimales();
    }

    /** Whether the value has more decimals than the currency supports (false if the currency is unknown). */
    static boolean excedeDecimales(Comprobante c, BigDecimal valor) {
        OptionalInt d = decimalesMoneda(c);
        return d.isPresent() && Importes.decimales(valor) > d.getAsInt();
    }

    /** Whether a catalog key is in the catalog and valid on the date of the comprobante. */
    static boolean enCatalogo(Comprobante c, CatalogKey key) {
        return key.isVigente(c.fecha().toLocalDate());
    }

    static boolean personaMoral(String rfc) {
        return rfc.length() == 12;
    }

    static boolean personaFisica(String rfc) {
        return rfc.length() == 13;
    }

    static boolean generico(String rfc) {
        return RFC_GENERICO_NACIONAL.equals(rfc) || RFC_GENERICO_EXTRANJERO.equals(rfc);
    }

    static Stream<Complemento> complementos(Comprobante c) {
        return Stream.concat(c.complemento().stream(), c.conceptos().stream().flatMap(x -> x.complementoConcepto().stream()));
    }

    static boolean tieneComercioExterior(Comprobante c) {
        return c.complemento().stream().anyMatch(x -> COMERCIO_EXTERIOR.contains(x.namespace()));
    }

    static List<Concepto.Traslado> traslados(Concepto c) {
        return c.impuestos().map(Concepto.Impuestos::traslados).orElse(List.of());
    }

    static List<Concepto.Retencion> retenciones(Concepto c) {
        return c.impuestos().map(Concepto.Impuestos::retenciones).orElse(List.of());
    }

    static BigDecimal suma(Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static boolean iguales(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    /** Names compared the way people write them: case, accents kept, surrounding and repeated spaces ignored. */
    static boolean mismoNombre(String a, String b) {
        return normalizar(a).equalsIgnoreCase(normalizar(b));
    }

    static String normalizar(String s) {
        return s.strip().replaceAll("\\s+", " ");
    }

    static <T> Optional<T> opt(T value) {
        return Optional.ofNullable(value);
    }
}
