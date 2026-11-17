package io.github.alktrip.cfdi4j.pagos20;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alktrip.cfdi4j.core.catalog.FormaPago;
import io.github.alktrip.cfdi4j.core.catalog.Impuesto;
import io.github.alktrip.cfdi4j.core.catalog.Moneda;
import io.github.alktrip.cfdi4j.core.catalog.ObjetoImp;
import io.github.alktrip.cfdi4j.core.catalog.TipoFactor;
import io.github.alktrip.cfdi4j.core.validation.Cfdi40Validator;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Scale;

/** Whatever the builder derives must pass the rules of the matrix. */
class PagosProperties {

    private static final Cfdi40Validator VALIDATOR = PagosFixtures.validator();

    @Property(tries = 300)
    void paymentInPesosOfAnInvoiceWithIva(@ForAll @BigRange(min = "0.01", max = "10000000") @Scale(2) BigDecimal base,
            @ForAll @IntRange(min = 1, max = 100) int porcentajePagado) {
        BigDecimal total = base.add(base.multiply(new BigDecimal("0.16")).setScale(2, RoundingMode.HALF_UP));
        BigDecimal pagado = total.multiply(BigDecimal.valueOf(porcentajePagado)).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
        Pagos pagos = Pagos.builder().pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.MXN)
                .monto(pagado.toPlainString())
                .doctoRelacionado(d -> d.idDocumento(PagosFixtures.UUID).monedaDR(Moneda.MXN).numParcialidad(1).impSaldoAnt(total.toPlainString())
                        .impPagado(pagado.toPlainString()).objetoImpDR(ObjetoImp.SI_OBJETO)
                        .trasladoDR(base.toPlainString(), Impuesto.IVA, TipoFactor.TASA, "0.160000"))).build();

        assertThat(VALIDATOR.validate(PagosFixtures.con(pagos)).errors()).extracting(Violation::code).isEmpty();
    }

    @Property(tries = 300)
    void paymentInDollarsOfAnInvoiceInPesos(@ForAll @BigRange(min = "1", max = "5000000") @Scale(2) BigDecimal base,
            @ForAll @BigRange(min = "10", max = "30") @Scale(4) BigDecimal tipoCambio) {
        BigDecimal total = base.add(base.multiply(new BigDecimal("0.16")).setScale(2, RoundingMode.HALF_UP));
        BigDecimal monto = total.divide(tipoCambio, 2, RoundingMode.HALF_UP);
        Pagos pagos = Pagos.builder().pago(p -> p.fechaPago(PagosFixtures.FECHA_PAGO).formaDePagoP(FormaPago.TRANSFERENCIA).monedaP(Moneda.USD)
                .tipoCambioP(tipoCambio.toPlainString()).monto(monto.toPlainString())
                .doctoRelacionado(d -> d.idDocumento(PagosFixtures.UUID).monedaDR(Moneda.MXN).equivalenciaDR(tipoCambio.toPlainString()).numParcialidad(1)
                        .impSaldoAnt(total.toPlainString()).impPagado(total.toPlainString()).objetoImpDR(ObjetoImp.SI_OBJETO)
                        .trasladoDR(base.toPlainString(), Impuesto.IVA, TipoFactor.TASA, "0.160000"))).build();

        assertThat(VALIDATOR.validate(PagosFixtures.con(pagos)).errors()).extracting(Violation::code).isEmpty();
    }
}
