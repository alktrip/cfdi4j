package io.github.alktrip.cfdi4j.core.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Arithmetic for CFDI 4.0 amounts, as written in the Anexo 20 (DOF 2022-01-13), "Validar"
 * sections of {@code Concepto/@Importe} and {@code Traslado|Retencion/@Importe}, and in the Guía
 * de llenado del CFDI 4.0, preguntas frecuentes 37, 39 and 41.
 *
 * <p>Not part of the public API.
 */
public final class Importes {

    /** Maximum number of decimals of {@code t_Importe} and of the concept-level amounts. */
    public static final int MAX_DECIMALES = 6;

    private static final BigDecimal EPSILON = new BigDecimal(BigInteger.ONE, 12);

    private Importes() {}

    /**
     * Rounds an amount to the decimals the currency supports. The Anexo 20 says "redondeo"
     * without naming a method; the Guía (pregunta 41) contrasts it with rounding up and calls it
     * "redondeo aritmético", i.e. half up.
     *
     * @param valor amount
     * @param decimales decimals of the currency
     * @return the rounded amount, with exactly {@code decimales} decimals
     */
    public static BigDecimal redondear(BigDecimal valor, int decimales) {
        return valor.setScale(decimales, RoundingMode.HALF_UP);
    }

    /**
     * Multiplies two concept-level amounts (Cantidad × ValorUnitario, or Base × TasaOCuota).
     *
     * <p>The exact product is rounded half up to {@value #MAX_DECIMALES} decimals only when it has
     * more, then trailing zeros are removed while keeping at least the currency decimals. A value
     * rounded this way always lies between the lower and upper limits of the Anexo 20.
     *
     * @param a first factor
     * @param b second factor
     * @param decimalesMoneda minimum decimals to keep (decimals of the currency)
     * @return the product
     */
    public static BigDecimal producto(BigDecimal a, BigDecimal b, int decimalesMoneda) {
        BigDecimal exacto = a.multiply(b);
        if (exacto.scale() > MAX_DECIMALES) {
            exacto = exacto.setScale(MAX_DECIMALES, RoundingMode.HALF_UP);
        }
        return normalizar(exacto, decimalesMoneda);
    }

    /**
     * Removes trailing zeros but keeps at least {@code minimoDecimales} decimals. Never changes the
     * numeric value.
     *
     * @param valor amount
     * @param minimoDecimales minimum decimals
     * @return the amount
     */
    public static BigDecimal normalizar(BigDecimal valor, int minimoDecimales) {
        BigDecimal sinCeros = valor.stripTrailingZeros();
        return sinCeros.scale() < minimoDecimales ? sinCeros.setScale(minimoDecimales) : sinCeros;
    }

    /**
     * Returns the number of decimals as written ("NumDecimales" in the Anexo 20).
     *
     * @param valor amount
     * @return the decimals, never negative
     */
    public static int decimales(BigDecimal valor) {
        return Math.max(0, valor.scale());
    }

    /**
     * Limits of {@code Concepto/@Importe} (also {@code Parte/@Importe}), Anexo 20:
     *
     * <pre>
     * inferior = trunc((Cantidad - 10^-dC / 2) * (ValorUnitario - 10^-dV / 2))
     * superior = ceil((Cantidad + 10^-dC / 2 - 10^-12) * (ValorUnitario + 10^-dV / 2 - 10^-12))
     * </pre>
     *
     * <p>Both results use the number of decimals written in the Importe.
     *
     * @param cantidad Cantidad
     * @param valorUnitario ValorUnitario
     * @param decimalesImporte decimals written in the Importe
     * @return the limits
     */
    public static Limites limitesImporteConcepto(BigDecimal cantidad, BigDecimal valorUnitario, int decimalesImporte) {
        BigDecimal mc = mediaUnidad(cantidad);
        BigDecimal mv = mediaUnidad(valorUnitario);
        BigDecimal inferior = cantidad.subtract(mc).multiply(valorUnitario.subtract(mv));
        BigDecimal superior = cantidad.add(mc).subtract(EPSILON).multiply(valorUnitario.add(mv).subtract(EPSILON));
        return new Limites(
                inferior.setScale(decimalesImporte, RoundingMode.DOWN),
                superior.setScale(decimalesImporte, RoundingMode.CEILING));
    }

    /**
     * Limits of {@code Traslado/@Importe} and {@code Retencion/@Importe} of a concept, Anexo 20:
     *
     * <pre>
     * inferior = trunc((Base - 10^-dB / 2) * TasaOCuota)
     * superior = ceil((Base + 10^-dB / 2 - 10^-12) * TasaOCuota)
     * </pre>
     *
     * @param base Base
     * @param tasaOCuota TasaOCuota
     * @param decimalesImporte decimals written in the Importe
     * @return the limits
     */
    public static Limites limitesImporteImpuesto(BigDecimal base, BigDecimal tasaOCuota, int decimalesImporte) {
        BigDecimal mb = mediaUnidad(base);
        BigDecimal inferior = base.subtract(mb).multiply(tasaOCuota);
        BigDecimal superior = base.add(mb).subtract(EPSILON).multiply(tasaOCuota);
        return new Limites(
                inferior.setScale(decimalesImporte, RoundingMode.DOWN),
                superior.setScale(decimalesImporte, RoundingMode.CEILING));
    }

    /** {@code 10^-d / 2}, where d is the number of decimals written in the value. */
    private static BigDecimal mediaUnidad(BigDecimal valor) {
        return new BigDecimal(BigInteger.valueOf(5), decimales(valor) + 1);
    }

    /**
     * Inclusive limits for a computed amount.
     *
     * @param inferior lower limit
     * @param superior upper limit
     */
    public record Limites(BigDecimal inferior, BigDecimal superior) {

        /** Checks that no component is null. */
        public Limites {
            Objects.requireNonNull(inferior, "inferior");
            Objects.requireNonNull(superior, "superior");
        }

        /**
         * Tells whether a value lies within the limits, both inclusive.
         *
         * @param valor the value
         * @return {@code true} if {@code inferior <= valor <= superior}
         */
        public boolean contiene(BigDecimal valor) {
            return valor.compareTo(inferior) >= 0 && valor.compareTo(superior) <= 0;
        }
    }
}
