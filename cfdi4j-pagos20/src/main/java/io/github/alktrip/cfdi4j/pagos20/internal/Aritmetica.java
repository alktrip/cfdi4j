package io.github.alktrip.cfdi4j.pagos20.internal;

import io.github.alktrip.cfdi4j.core.internal.Importes;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Arithmetic of the complemento de pagos 2.0, as written in the standard (revisión B, "Validar"
 * of Totales, Monto, BaseP and ImporteP) and the Guía de llenado (page 33, margin formula).
 * Not part of the public API.
 */
public final class Aritmetica {

    /** Decimals of the Totales attributes (t_ImporteMXN). */
    public static final int DECIMALES_MXN = 2;

    /** Constant of the margin formula of ImpPagado (Guía de llenado del complemento de pagos, page 33). */
    private static final BigDecimal EPSILON = new BigDecimal(BigInteger.ONE, 16);

    /** Precision of divisions. */
    public static final MathContext PRECISION = MathContext.DECIMAL128;

    private Aritmetica() {}

    /** An amount of a DoctoRelacionado expressed in the currency of the Pago: {@code valor / EquivalenciaDR}. */
    public static BigDecimal aMonedaPago(BigDecimal valor, BigDecimal equivalenciaDR) {
        return valor.divide(equivalenciaDR, PRECISION);
    }

    /**
     * A converted sum written as BaseP or ImporteP: rounded half up to the decimals of the payment
     * currency, as the worked example of the Guía de llenado does (page 77: 300,000.00 MXN /
     * 17.7331 is registered as 16,917.520000 USD).
     */
    public static BigDecimal importe(BigDecimal suma, int decimalesMonedaP) {
        return suma.setScale(decimalesMonedaP, RoundingMode.HALF_UP);
    }

    /**
     * Precision of an amount expressed in the payment currency: the decimals written, but no more
     * than the currency supports (the SAT pads such amounts with zeros, see {@link #importe}).
     */
    public static int precision(BigDecimal registrado, int decimalesMonedaP) {
        return Math.min(Importes.decimales(registrado), decimalesMonedaP);
    }

    /** Half a unit of the given decimal position. */
    public static BigDecimal mediaUnidad(int decimales) {
        return new BigDecimal(BigInteger.valueOf(5), decimales + 1);
    }

    /**
     * Whether a registered amount in the payment currency equals an exact computed value: the
     * difference is at most half a unit of its {@link #precision}.
     */
    public static boolean igualConPrecision(BigDecimal registrado, BigDecimal exacto, int decimalesMonedaP) {
        return registrado.subtract(exacto).abs().compareTo(mediaUnidad(precision(registrado, decimalesMonedaP))) <= 0;
    }

    /** A total of Totales: the sum in MXN rounded to 2 decimals. */
    public static BigDecimal totalMxn(BigDecimal suma) {
        return Importes.redondear(suma, DECIMALES_MXN);
    }

    /**
     * Limits of one ImpPagado converted to the payment currency (Guía, page 33):
     *
     * <pre>
     * inferior = (ImpPagado - 10^-dI / 2) / (EquivalenciaDR + 10^-dE / 2 - 10^-16), truncated
     * superior = (ImpPagado + 10^-dI / 2 - 10^-16) / (EquivalenciaDR - 10^-dE / 2), rounded
     * </pre>
     *
     * <p>{@code dI} and {@code dE} are the decimals written in ImpPagado and EquivalenciaDR; an
     * EquivalenciaDR of 1 counts 10 decimals when the payment mixes currencies (CRP20277). Results
     * use the decimals of the payment currency; "redondear" is taken as half up.
     */
    public static Limites limitesImpPagado(BigDecimal impPagado, BigDecimal equivalenciaDR, int decimalesEquivalencia, int decimalesMonedaP) {
        BigDecimal mi = media(Importes.decimales(impPagado));
        BigDecimal me = media(decimalesEquivalencia);
        BigDecimal inferior = impPagado.subtract(mi).divide(equivalenciaDR.add(me).subtract(EPSILON), PRECISION);
        BigDecimal superior = impPagado.add(mi).subtract(EPSILON).divide(equivalenciaDR.subtract(me), PRECISION);
        return new Limites(inferior.setScale(decimalesMonedaP, RoundingMode.DOWN), superior.setScale(decimalesMonedaP, RoundingMode.HALF_UP));
    }

    private static BigDecimal media(int decimales) {
        return new BigDecimal(BigInteger.valueOf(5), decimales + 1);
    }

    /**
     * Inclusive limits.
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

        /** Whether the value is within both limits. */
        public boolean contiene(BigDecimal valor) {
            return valor.compareTo(inferior) >= 0 && valor.compareTo(superior) <= 0;
        }
    }
}
