# Cobertura de la matriz de errores CFDI 4.0

<!-- Generado por RulesCoverageReportTest a partir de Cfdi40Rules. No editar a mano. -->

Fuente: `MatrizDeErrores_CFDI_v40_20260325.xls` (SHA-256 `bdc350035c7ed720af5be15509e01d0bda7c12cec4ad9797b0c34f889c1140a7`), ver `specs/SOURCES.md`.

Las reglas que requieren un catálogo grande, la lista de RFC del SAT o el estado de un certificado se validan con los puertos `CatalogProvider`, `TaxpayerRegistry` y `CertificateStatusProvider`; sin ellos se reportan como **no verificadas** (advertencia), nunca como aprobadas (ADR 0004).

| Estado | Códigos |
|---|---|
| implementada | 101 |
| parcial | 10 |
| requiere servicio externo | 15 |
| no aplica | 3 |
| **total** | 129 |

## Códigos oficiales

| Código | Elemento | Atributo | Estado | Nota |
|---|---|---|---|---|
| CFDI40101 | Comprobante | Fecha | parcial | Se valida el patrón (años 2010 a 2099). No se valida que corresponda con la hora local donde se expide, que el PAC comprueba al timbrar. |
| CFDI40102 | Comprobante | Sello | implementada | Se valida al verificar el sello (Cfdi4j.verify), sobre el documento tal como se recibió. |
| CFDI40103 | Comprobante | FormaPago | implementada |  |
| CFDI40104 | Comprobante | FormaPago | implementada |  |
| CFDI40105 | Comprobante | FormaPago | implementada |  |
| CFDI40106 | Comprobante | Certificado | parcial | Con el certificado que sella: el RFC del certificado es el del emisor, la Fecha cae en su vigencia, es un CSD y no una e.firma, y no está revocado (con un CertificateStatusProvider). No se valida que lo haya emitido el SAT ni las excepciones de los proveedores del sector primario o de expedición a través del adquirente. Al verificar (Cfdi4j.verify) se valida además que el atributo contenga un certificado legible. |
| CFDI40107 | Comprobante | SubTotal | implementada |  |
| CFDI40108 | Comprobante | SubTotal | implementada |  |
| CFDI40109 | Comprobante | SubTotal | implementada |  |
| CFDI40110 | Comprobante | Descuento | implementada |  |
| CFDI40111 | Comprobante | Descuento | implementada |  |
| CFDI40112 | Comprobante | Descuento | implementada |  |
| CFDI40113 | Comprobante | Moneda | implementada |  |
| CFDI40114 | Comprobante | TipoCambio | implementada |  |
| CFDI40115 | Comprobante | TipoCambio | implementada |  |
| CFDI40116 | Comprobante | TipoCambio | implementada |  |
| CFDI40117 | Comprobante | TipoCambio | implementada |  |
| CFDI40118 | Comprobante | TipoCambio | requiere servicio externo | Requiere el tipo de cambio FIX publicado y la clave de confirmación del PAC. La Guía de llenado indica que esta validación aplica cuando el SAT publique el procedimiento. |
| CFDI40119 | Comprobante | Total | parcial | Se valida contra SubTotal, Descuento y los totales de impuestos federales. Con el complemento de impuestos locales se reporta como no verificado. |
| CFDI40120 | Comprobante | Total | parcial | Se valida contra el valor máximo de c_TipoDeComprobante. Los límites personalizados de l_RFC requieren un servicio del SAT. |
| CFDI40121 | Comprobante | TipoDeComprobante | implementada |  |
| CFDI40122 | Comprobante | Exportacion | implementada | El modelo, el lector XML y el XSD exigen el atributo; su valor se valida con CFDI40123. |
| CFDI40123 | Comprobante | Exportacion | implementada |  |
| CFDI40124 | Comprobante | MetodoPago | implementada |  |
| CFDI40125 | Comprobante | MetodoPago | implementada |  |
| CFDI40126 | Comprobante | LugarExpedicion | implementada | Con un CatalogProvider. Sin él se reporta como no verificado; la validación XSD también comprueba la clave contra la enumeración de catCFDI.xsd. |
| CFDI40127 | Comprobante | Confirmacion | requiere servicio externo | Depende de los rangos de CFDI40118 y CFDI40120, que requieren datos del SAT. |
| CFDI40128 | Comprobante | Confirmacion | no aplica | Validación que realiza el PAC. |
| CFDI40129 | Comprobante | Confirmacion | no aplica | Validación que realiza el PAC. |
| CFDI40130 | cfdi:InformacionGlobal |  | implementada |  |
| CFDI40131 | cfdi:InformacionGlobal | Periodicidad | implementada |  |
| CFDI40132 | cfdi:InformacionGlobal | Periodicidad | implementada |  |
| CFDI40133 | cfdi:InformacionGlobal | Meses | implementada |  |
| CFDI40134 | cfdi:InformacionGlobal | Meses | implementada |  |
| CFDI40135 | cfdi:InformacionGlobal | Meses | implementada |  |
| CFDI40136 | cfdi:InformacionGlobal | Año | implementada | El año en curso se toma de la Fecha del comprobante: se aceptan ese año y hasta 5 ejercicios anteriores. |
| CFDI40137 | cfdi:CfdiRelacionados | TipoRelacion | implementada |  |
| CFDI40138 | cfdi:Emisor | Nombre | requiere servicio externo | Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado. |
| CFDI40139 | cfdi:Emisor | Nombre | requiere servicio externo | Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado. |
| CFDI40140 | cfdi:Emisor | RegimenFiscal | implementada |  |
| CFDI40141 | cfdi:Emisor | RegimenFiscal | implementada |  |
| CFDI40142 | cfdi:Emisor | FacAtrAdquirente | requiere servicio externo | Requiere el servicio del Validador de RFC para emitir facturas a través del adquirente; se reporta como no verificado cuando el atributo existe. |
| CFDI40143 | cfdi:Receptor | Rfc | requiere servicio externo | Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado. |
| CFDI40144 | cfdi:Receptor | Nombre | requiere servicio externo | Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado. |
| CFDI40145 | cfdi:Receptor | Nombre | requiere servicio externo | Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado. |
| CFDI40146 | cfdi:Receptor | Nombre | implementada |  |
| CFDI40147 | cfdi:Receptor | DomicilioFiscalReceptor | requiere servicio externo | Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado. |
| CFDI40148 | cfdi:Receptor | DomicilioFiscalReceptor | requiere servicio externo | Se valida con un TaxpayerRegistry (lista de RFC inscritos no cancelados); sin él se reporta como no verificado. |
| CFDI40149 | cfdi:Receptor | DomicilioFiscalReceptor | implementada |  |
| CFDI40150 | cfdi:Receptor | ResidenciaFiscal | implementada |  |
| CFDI40151 | cfdi:Receptor | ResidenciaFiscal | implementada | Un RFC que no es el genérico extranjero se trata como RFC del SAT o genérico nacional. |
| CFDI40152 | cfdi:Receptor | ResidenciaFiscal | implementada |  |
| CFDI40153 | cfdi:Receptor | ResidenciaFiscal | implementada |  |
| CFDI40154 | cfdi:Receptor | NumRegIdTrib | implementada | Un RFC que no es el genérico extranjero se trata como RFC del SAT o genérico nacional. |
| CFDI40155 | cfdi:Receptor | NumRegIdTrib | implementada |  |
| CFDI40156 | cfdi:Receptor | NumRegIdTrib | parcial | Se valida el patrón de la columna "Formato de Registro de Identidad Tributaria" de c_Pais. La validación contra listas del país (columna "Validación del Registro de Identidad Tributaria") requiere un servicio externo. |
| CFDI40157 | cfdi:Receptor | RegimenFiscalReceptor | implementada |  |
| CFDI40158 | cfdi:Receptor | RegimenFiscalReceptor | implementada |  |
| CFDI40159 | cfdi:Receptor | RegimenFiscalReceptor | implementada |  |
| CFDI40160 | cfdi:Receptor | UsoCFDI | implementada |  |
| CFDI40161 | cfdi:Receptor | UsoCFDI | implementada |  |
| CFDI40162 | cfdi:Conceptos/Concepto | ClaveProdServ | implementada | Se valida con un CatalogProvider; sin él se reporta como no verificado (una vez por código). La validación XSD también comprueba la clave. |
| CFDI40163 | cfdi:Conceptos/Concepto | ClaveProdServ | parcial | Con un CatalogProvider se identifica la clave que exige un complemento; como el catálogo nombra el complemento en texto, se reporta como no verificado. |
| CFDI40164 | cfdi:Conceptos/Concepto | ClaveProdServ | implementada | Se valida con un CatalogProvider; sin él se reporta como no verificado (una vez por código). Cuando el catálogo indica "Sí" en Incluir IVA trasladado o Incluir IEPS trasladado, el concepto debe trasladar ese impuesto. |
| CFDI40165 | cfdi:Conceptos/Concepto | ClaveUnidad | implementada | Se valida con un CatalogProvider; sin él se reporta como no verificado (una vez por código). La validación XSD también comprueba la clave. |
| CFDI40166 | cfdi:Conceptos/Concepto | ValorUnitario | implementada |  |
| CFDI40167 | cfdi:Conceptos/Concepto | Importe | implementada |  |
| CFDI40168 | cfdi:Conceptos/Concepto | Descuento | implementada |  |
| CFDI40169 | cfdi:Conceptos/Concepto | Descuento | implementada |  |
| CFDI40170 | cfdi:Conceptos/Concepto | ObjetoImp | implementada |  |
| CFDI40171 | cfdi:Conceptos/Concepto | ObjetoImp | implementada |  |
| CFDI40172 | cfdi:Conceptos/Concepto | ObjetoImp | implementada |  |
| CFDI40173 | cfdi:Conceptos/Concepto:Impuestos | Impuestos | implementada |  |
| CFDI40174 | cfdi:Conceptos/Concepto:Impuestos:Traslados/Traslado | Base | implementada |  |
| CFDI40175 | cfdi:Conceptos/Concepto:Impuestos:Traslados/Traslado | Impuesto | implementada |  |
| CFDI40176 | cfdi:Conceptos/Concepto:Impuestos:Traslados/Traslado | TipoFactor | implementada |  |
| CFDI40177 | cfdi:Conceptos/Concepto:Impuestos:Traslados/Traslado | TipoFactor | implementada |  |
| CFDI40178 | cfdi:Conceptos/Concepto:Impuestos:Traslados/Traslado | TipoFactor | implementada |  |
| CFDI40179 | cfdi:Conceptos/Concepto:Impuestos:Traslados/Traslado | TasaOCuota | implementada |  |
| CFDI40180 | cfdi:Conceptos/Concepto:Impuestos:Traslados/Traslado | Importe | implementada |  |
| CFDI40181 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | Base | implementada |  |
| CFDI40182 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | Impuesto | implementada |  |
| CFDI40183 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | TipoFactor | implementada |  |
| CFDI40184 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | TipoFactor | implementada |  |
| CFDI40185 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | TasaOCuota | implementada |  |
| CFDI40186 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | Importe | implementada |  |
| CFDI40187 | cfdi:Conceptos/Concepto:ACuentaTerceros | RfcACuentaTerceros | requiere servicio externo | Si el RFC no es EXT990101NI1 se valida con un TaxpayerRegistry; sin él se reporta como no verificado. |
| CFDI40188 | cfdi:Conceptos/Concepto:ACuentaTerceros | RfcACuentaTerceros | implementada |  |
| CFDI40189 | cfdi:Conceptos/Concepto:ACuentaTerceros | NombreACuentaTerceros | parcial | Con EXT990101NI1 se valida la descripción obligatoria; en otro caso se requiere un TaxpayerRegistry. |
| CFDI40190 | cfdi:Conceptos/Concepto:ACuentaTerceros | NombreACuentaTerceros | requiere servicio externo | Se valida con un TaxpayerRegistry; sin él se reporta como no verificado. |
| CFDI40191 | cfdi:Conceptos/Concepto:ACuentaTerceros | RegimenFiscalACuentaTerceros | implementada |  |
| CFDI40192 | cfdi:Conceptos/Concepto:ACuentaTerceros | DomicilioFiscalACuentaTerceros | requiere servicio externo | Se valida con un TaxpayerRegistry; sin él se reporta como no verificado. |
| CFDI40193 | cfdi:Conceptos/Concepto:ACuentaTerceros | DomicilioFiscalACuentaTerceros | parcial | Con EXT990101NI1 se compara con LugarExpedicion; en otro caso se requiere un TaxpayerRegistry. |
| CFDI40194 | cfdi:Conceptos/Concepto:InformacionAduanera | NumeroPedimento | parcial | Se valida la estructura del número de pedimento. La validación contra c_Aduana, c_PatenteAduanal y c_NumPedimentoAduana no está implementada. |
| CFDI40195 | cfdi:Conceptos/Concepto:InformacionAduanera | NumeroPedimento | implementada |  |
| CFDI40196 | cfdi:Conceptos/Concepto:Parte | ClaveProdServ | implementada | Se valida con un CatalogProvider; sin él se reporta como no verificado (una vez por código). |
| CFDI40197 | cfdi:Conceptos/Concepto:Parte | ValorUnitario | implementada |  |
| CFDI40198 | cfdi:Conceptos/Concepto:Parte | Importe | implementada | Se valida cuando la Parte registra ValorUnitario e Importe. |
| CFDI40199 | cfdi:Conceptos/Concepto:Parte:InformacionAduanera | NumeroPedimento | parcial | Igual que CFDI40194. |
| CFDI40200 | cfdi:Conceptos/Concepto:Parte:InformacionAduanera | NumeroPedimento | implementada |  |
| CFDI40201 | cfdi:Impuestos | Impuestos | implementada |  |
| CFDI40202 | cfdi:Impuestos | TotalImpuestosRetenidos | implementada |  |
| CFDI40203 | cfdi:Impuestos | TotalImpuestosRetenidos | implementada |  |
| CFDI40204 | cfdi:Impuestos | TotalImpuestosTrasladados | implementada |  |
| CFDI40205 | cfdi:Impuestos | TotalImpuestosTrasladados | implementada |  |
| CFDI40206 | cfdi:Impuestos:Retenciones | Retenciones | implementada |  |
| CFDI40207 | cfdi:Impuestos:Retenciones/Retencion | Impuesto | implementada |  |
| CFDI40208 | cfdi:Impuestos:Retenciones/Retencion | Impuesto | implementada |  |
| CFDI40209 | cfdi:Impuestos:Retenciones/Retencion | Importe | implementada | Misma condición que CFDI40206, que es el código que se reporta. |
| CFDI40210 | cfdi:Impuestos:Retenciones/Retencion | Importe | implementada |  |
| CFDI40211 | cfdi:Impuestos:Retenciones/Retencion | Importe | implementada |  |
| CFDI40212 | cfdi:Impuestos:Traslados | Traslados | implementada |  |
| CFDI40213 | cfdi:Impuestos:Traslados/Traslado | Traslado | implementada |  |
| CFDI40214 | cfdi:Impuestos:Traslados/Traslado | Base | implementada |  |
| CFDI40215 | cfdi:Impuestos:Traslados/Traslado | Base | implementada |  |
| CFDI40216 | cfdi:Impuestos:Traslados/Traslado | Base | implementada |  |
| CFDI40217 | cfdi:Impuestos:Traslados/Traslado | Impuesto | implementada |  |
| CFDI40218 | cfdi:Impuestos:Traslados/Traslado | Impuesto | implementada |  |
| CFDI40219 | cfdi:Impuestos:Traslados/Traslado | TasaOCuota | implementada |  |
| CFDI40220 | cfdi:Impuestos:Traslados/Traslado | Importe | implementada |  |
| CFDI40221 | cfdi:Impuestos:Traslados/Traslado | Importe | implementada |  |
| CFDI40222 | Comprobante | Exportacion | implementada |  |
| CFDI140223 | cfdi:Conceptos/Concepto:ACuentaTerceros | RegimenFiscalACuentaTerceros | implementada |  |
| CFDI140224 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | Impuesto | requiere servicio externo | Requiere la marca "Retención" de la lista de RFC inscritos (TaxpayerRegistry); sin ella se reporta como no verificado cuando el emisor es persona física en el régimen 626 y el receptor es persona moral. |
| CFDI140225 | cfdi:Conceptos/Concepto:Impuestos:Retenciones/Retencion | Impuesto | requiere servicio externo | Requiere la marca "Retención" de la lista de RFC inscritos (TaxpayerRegistry); sin ella se reporta como no verificado cuando el emisor es persona física en el régimen 626 y el receptor es persona moral. |
| CFDI140226 | cfdi:Conceptos/Concepto | ObjetoImp | implementada |  |
| CFDI140227 | cfdi:Conceptos/Concepto | ObjetoImp | implementada |  |
| CFDI140228 | cfdi:Conceptos/Concepto | ClaveProdServ | implementada |  |
| CFDI40999 | Error no clasificado | No clasificado | no aplica | Código del PAC para errores no clasificados. |

## Códigos de cfdi4j

| Código | Módulo | Descripción |
|---|---|---|
| CFDI4J-001 | cfdi4j-xml | El XML no cumple con el esquema XSD del SAT. |
| CFDI4J-002 | cfdi4j-xml | Complemento sin esquema incluido: no verificado contra XSD (advertencia). |
| CFDI4J-003 | cfdi4j-xml | El documento no es XML bien formado o declara DOCTYPE. |
| CFDI4J-101 | cfdi4j | El documento no se puede leer como CFDI 4.0 (verificación). |
| CFDI4J-102 | cfdi4j | Falta Sello, NoCertificado o Certificado (verificación). |
| CFDI4J-103 | cfdi4j-core, cfdi4j | NoCertificado no es el número del certificado. |
| CFDI4J-104 | cfdi4j | SelloCFD del TimbreFiscalDigital no es el Sello (verificación). |
| CFDI4J-105 | cfdi4j | SelloSAT no verificado sin el certificado del SAT (advertencia). |
| CFDI4J-106 | cfdi4j | SelloSAT no corresponde al timbre (verificación). |
| CFDI4J-107 | cfdi4j-core | Una regla no se pudo evaluar con el documento (error; nunca se da por aprobada). |
| CFDI4J-108 | cfdi4j | El certificado no lo emitió una autoridad del SAT configurada; sin autoridades, advertencia (verificación). |
| CFDI4J-109 | cfdi4j | El documento excede el tamaño máximo configurado; no se lee (validación y verificación). |
