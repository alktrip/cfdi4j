# Cobertura de la matriz de errores del complemento de pagos 2.0

<!-- Generado por RulesCoverageReportTest (cfdi4j-pagos20). No editar a mano. -->

Fuente: `MatrizDeErrores_CRP_V20_RevB.xls` (SHA-256 `84fef6323770dadd89f4f6fa74fbafaa06a4692d577604363938b31e0d6e4268`), ver `specs/SOURCES.md`.

Las reglas se aplican solo a comprobantes con el complemento `pago20:Pagos`. Las que requieren un servicio del SAT se reportan como **no verificadas** (advertencia) sin el puerto correspondiente (ADR 0004).

| Estado | Códigos |
|---|---|
| implementada | 99 |
| parcial | 1 |
| requiere servicio externo | 2 |
| no aplica | 1 |
| **total** | 103 |

## Códigos oficiales

| Código | Elemento | Atributo | Estado | Nota |
|---|---|---|---|---|
| CRP20101 | Comprobante | TipoDeComprobante | implementada |  |
| CRP20102 | Comprobante | Exportacion | implementada |  |
| CRP20103 | Comprobante | SubTotal | implementada |  |
| CRP20104 | Comprobante | Moneda | implementada |  |
| CRP20105 | Comprobante | FormaPago | implementada |  |
| CRP20106 | Comprobante | MetodoPago | implementada |  |
| CRP20107 | Comprobante | CondicionesDePago | implementada |  |
| CRP20108 | Comprobante | Descuento | implementada |  |
| CRP20109 | Comprobante | TipoCambio | implementada |  |
| CRP20110 | Comprobante | Total | implementada |  |
| CRP20111 | cfdi:Conceptos/Concepto |  | implementada |  |
| CRP20112 | cfdi:Conceptos/Concepto |  | implementada |  |
| CRP20113 | cfdi:Conceptos/Concepto | ClaveProdServ | implementada |  |
| CRP20114 | cfdi:Conceptos/Concepto | NoIdentificacion | implementada |  |
| CRP20115 | cfdi:Conceptos/Concepto | Cantidad | implementada |  |
| CRP20116 | cfdi:Conceptos/Concepto | ClaveUnidad | implementada |  |
| CRP20117 | cfdi:Conceptos/Concepto | Unidad | implementada |  |
| CRP20118 | cfdi:Conceptos/Concepto | Descripcion | implementada |  |
| CRP20119 | cfdi:Conceptos/Concepto | ValorUnitario | implementada |  |
| CRP20120 | cfdi:Conceptos/Concepto | Importe | implementada |  |
| CRP20121 | cfdi:Conceptos/Concepto | Descuento | implementada |  |
| CRP20122 | cfdi:Conceptos/Concepto | ObjetoImp | implementada |  |
| CRP20123 | cfdi:Conceptos/Concepto:Impuestos |  | implementada |  |
| CRP20201 | Pagos | TotalRetencionesIVA | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20202 | Pagos | TotalRetencionesISR | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20203 | Pagos | TotalRetencionesIEPS | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20204 | Pagos | TotalTrasladosBaseIVA16 | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20205 | Pagos | TotalTrasladosImpuestoIVA16 | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20206 | Pagos | TotalTrasladosBaseIVA8 | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20207 | Pagos | TotalTrasladosImpuestoIVA8 | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20208 | Pagos | TotalTrasladosBaseIVA0 | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20209 | Pagos | TotalTrasladosImpuestoIVA0 | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20210 | Pagos | TotalTrasladosBaseExento | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20211 | Pagos | MontoTotalPagos | implementada | Suma de los valores de cada Pago por su TipoCambioP. Como el ejemplo de la Guía de llenado (página 77) calcula los totales con los importes antes de convertir, se acepta la diferencia que produce el redondeo de cada valor a los decimales de su moneda, multiplicada por TipoCambioP, más el redondeo final. |
| CRP20212 | Pagos/pago10:Pago | FormaDePagoP | implementada |  |
| CRP20213 | Pagos/pago10:Pago | MonedaP | implementada |  |
| CRP20214 | Pagos/pago10:Pago | MonedaP | implementada |  |
| CRP20215 | Pagos/pago10:Pago | MonedaP | implementada |  |
| CRP20216 | Pagos/pago10:Pago | TipoCambioP | requiere servicio externo | Requiere el tipo de cambio publicado para la fecha y la clave de confirmación del PAC. |
| CRP20217 | Pagos/pago10:Pago | Monto | implementada | Cuando todos los documentos están en la moneda del pago. |
| CRP20218 | Pagos/pago10:Pago | Monto | implementada |  |
| CRP20219 | Pagos/pago10:Pago | Monto | implementada |  |
| CRP20220 | Pagos/pago10:Pago | Monto | parcial | Se compara el equivalente en MXN con el valor máximo de c_TipoDeComprobante para "P"; los límites personalizados de l_RFC requieren un servicio del SAT. |
| CRP20221 | Pagos/pago10:Pago | RfcEmisorCtaOrd | implementada |  |
| CRP20222 | Pagos/pago10:Pago | RfcEmisorCtaOrd | requiere servicio externo | Si el RFC no es XEXX010101000 se valida con un TaxpayerRegistry; sin él se reporta como no verificado. |
| CRP20223 | Pagos/pago10:Pago | NomBancoOrdExt | implementada |  |
| CRP20224 | Pagos/pago10:Pago | CtaOrdenante | implementada |  |
| CRP20225 | Pagos/pago10:Pago | CtaOrdenante | implementada |  |
| CRP20226 | Pagos/pago10:Pago | CtaBeneficiario | implementada |  |
| CRP20227 | Pagos/pago10:Pago | RfcEmisorCtaBen | implementada |  |
| CRP20228 | Pagos/pago10:Pago | CtaBeneficiario | implementada |  |
| CRP20229 | Pagos/pago10:Pago | TipoCadPago | implementada |  |
| CRP20230 | Pagos/pago10:Pago | CertPago | implementada |  |
| CRP20231 | Pagos/pago10:Pago | CertPago | implementada |  |
| CRP20232 | Pagos/pago10:Pago | CadPago | implementada |  |
| CRP20233 | Pagos/pago10:Pago | CadPago | implementada |  |
| CRP20234 | Pagos/pago10:Pago | SelloPago | implementada |  |
| CRP20235 | Pagos/pago10:Pago | SelloPago | implementada |  |
| CRP20236 | Pagos/pago10:Pago/pago10:DoctoRelacionado | MonedaDR | implementada |  |
| CRP20237 | Pagos/pago10:Pago/pago10:DoctoRelacionado | EquivalenciaDR | implementada |  |
| CRP20238 | Pagos/pago10:Pago/pago10:DoctoRelacionado | EquivalenciaDR | implementada |  |
| CRP20239 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ImpSaldoAnt | implementada |  |
| CRP20240 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ImpSaldoAnt | implementada |  |
| CRP20241 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ImpPagado | implementada |  |
| CRP20242 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ImpPagado | implementada |  |
| CRP20243 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ImpSaldoInsoluto | implementada |  |
| CRP20244 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ImpSaldoInsoluto | implementada |  |
| CRP20245 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ObjetoImpDR | implementada |  |
| CRP20246 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ObjetoImpDR | implementada |  |
| CRP20247 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ObjetoImpDR | implementada |  |
| CRP20248 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR |  | implementada |  |
| CRP20249 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:RetencionesDR/RetencionDR | BaseDR | implementada |  |
| CRP20250 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:RetencionesDR/RetencionDR | ImpuestoDR | implementada |  |
| CRP20251 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:RetencionesDR/RetencionDR | TipoFactorDR | implementada |  |
| CRP20252 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:RetencionesDR/RetencionDR | TipoFactorDR | implementada |  |
| CRP20253 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:RetencionesDR/RetencionDR | TasaOCuotaDR | implementada |  |
| CRP20254 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:RetencionesDR/RetencionDR | ImporteDR | implementada |  |
| CRP20255 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:TrasladosDR/TrasladoDR | BaseDR | implementada |  |
| CRP20256 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:TrasladosDR/TrasladoDR | ImpuestoDR | implementada |  |
| CRP20257 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:TrasladosDR/TrasladoDR | TipoFactorDR | implementada |  |
| CRP20258 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:TrasladosDR/TrasladoDR | TipoFactorDR | implementada |  |
| CRP20259 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:TrasladosDR/TrasladoDR | TipoFactorDR | implementada |  |
| CRP20260 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:TrasladosDR/TrasladoDR | TasaOCuotaDR | implementada |  |
| CRP20261 | Pagos/pago10:Pago/pago10:DoctoRelacionado/ImpuestosDR:TrasladosDR/TrasladoDR | ImporteDR | implementada |  |
| CRP20262 | Pagos/pago10:Pago/pago10:ImpuestosP:RetencionesP/RetencionP | ImpuestoP | implementada |  |
| CRP20263 | Pagos/pago10:Pago/pago10:ImpuestosP:RetencionesP/RetencionP | ImpuestoP | implementada |  |
| CRP20264 | Pagos/pago10:Pago/pago10:ImpuestosP:RetencionesP/RetencionP | ImporteP | implementada |  |
| CRP20265 | Pagos/pago10:Pago/pago10:ImpuestosP:RetencionesP/RetencionP | ImporteP | implementada | Se compara con la suma de los valores de los documentos divididos entre su EquivalenciaDR (estándar, revisión B). El estándar no fija el redondeo; como en el ejemplo de la Guía de llenado (página 77), el valor se acepta redondeado a los decimales de la moneda del pago. |
| CRP20266 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | TrasladoP | implementada |  |
| CRP20267 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | BaseP | implementada |  |
| CRP20268 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | BaseP | implementada | Se compara con la suma de los valores de los documentos divididos entre su EquivalenciaDR (estándar, revisión B). El estándar no fija el redondeo; como en el ejemplo de la Guía de llenado (página 77), el valor se acepta redondeado a los decimales de la moneda del pago. |
| CRP20269 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | BaseP | implementada | Se compara con la suma de los valores de los documentos divididos entre su EquivalenciaDR (estándar, revisión B). El estándar no fija el redondeo; como en el ejemplo de la Guía de llenado (página 77), el valor se acepta redondeado a los decimales de la moneda del pago. |
| CRP20270 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | ImpuestoP | implementada |  |
| CRP20271 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | ImpuestoP | implementada |  |
| CRP20272 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | TasaOCuotaP | implementada |  |
| CRP20273 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | ImporteP | implementada |  |
| CRP20274 | Pagos/pago10:Pago/pago10:ImpuestosP:TrasladosP/TrasladoP | ImporteP | implementada | Se compara con la suma de los valores de los documentos divididos entre su EquivalenciaDR (estándar, revisión B). El estándar no fija el redondeo; como en el ejemplo de la Guía de llenado (página 77), el valor se acepta redondeado a los decimales de la moneda del pago. |
| CRP20275 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ImpPagado | implementada |  |
| CRP20276 | Pagos/pago10:Pago | Monto | implementada | Cuando algún documento está en otra moneda: si la suma de ImpPagado / EquivalenciaDR (truncada) excede Monto, Monto debe estar entre la suma de los límites inferiores y la de los superiores (Guía de llenado, página 33; el límite superior se redondea al medio). |
| CRP20277 | Pagos/pago10:Pago/pago10:DoctoRelacionado | EquivalenciaDR | implementada | Se aplica dentro del cálculo de CRP20275 y CRP20276: una EquivalenciaDR de 1 cuenta con 10 decimales. |
| CRP20278 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ObjetoImpDR | implementada |  |
| CRP20279 | Pagos/pago10:Pago/pago10:DoctoRelacionado | ObjetoImpDR | implementada |  |
| CRP20999 | Error no clasificado | No clasificado | no aplica | Código del PAC para errores no clasificados. |
