# Official sources

Every official artifact used by cfdi4j, with its origin, publication date and SHA-256.

- **Retrieved:** 2026-11-14 (all entries below).
- **Published** is the HTTP `Last-Modified` date reported by the SAT server at retrieval time;
  the SAT does not version most of these files in any other way.
- Paths are relative to `specs/`. The files under `sat/xml/` mirror their URL path, which is what the
  offline resolvers use to map an absolute SAT URL to a bundled resource (ADR 0007).
- Files are stored **byte-for-byte as downloaded** (CRLF line endings and BOMs included; see
  `.gitattributes`). A test recomputes every hash in the "Bundled" tables.

Index pages:

- Anexo 20: <http://omawww.sat.gob.mx/tramitesyservicios/Paginas/anexo_20.htm>
- Certificado de sello digital (test certificates): <http://omawww.sat.gob.mx/tramitesyservicios/Paginas/certificado_sello_digital.htm>

## Reference documents (not committed)

These files are large or not needed at runtime. `specs/fetch-sources.sh` downloads them into
`specs/downloads/` (git-ignored) and verifies the hashes below.

| File | Description | Published | SHA-256 |
|------|-------------|-----------|---------|
| [`Anexo20_2022.pdf`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Anexo20_2022.pdf) | Anexo 20, CFDI 4.0 (standard) | 2022-09-25 | `b3b6d63a97c859a31d7aacf0fe17b2d58207280df6f06caff3ceaf79954cf4bd` |
| [`Anexo_20_Guia_de_llenado_CFDI.pdf`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Anexo_20_Guia_de_llenado_CFDI.pdf) | Guía de llenado del CFDI 4.0 | 2026-01-17 | `70e0c92df44b4f36975e4a0a5b1f0609f83c62deb029eda440fe4df6e2265a25` |
| [`Guia_llenado_CFDI_global.pdf`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Guia_llenado_CFDI_global.pdf) | Guía de llenado del CFDI global | 2024-02-09 | `9a85b87d1416f7e61e5726d8103b3b467092033de3b86b3c99fd009441f75042` |
| [`PregFrecCFDIVer4_0.pdf`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/PregFrecCFDIVer4_0.pdf) | Preguntas frecuentes CFDI 4.0 | 2024-05-30 | `44267da00068c89cd3e8bc31a08b0f87862821b75f47ac05b08823e9fcdc4bcd` |
| [`catCFDI_V_4_20261106.xls`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/catCFDI_V_4_20261106.xls) | Catálogos CFDI 4.0, version 2026-11-06 | 2026-11-06 | `6249e37275ce164a1c4ac44e726dcd60858dac5c5604ff6c9ab06005a9199e1a` |
| [`MatrizDeErrores_CFDI_v40_20260325.xls`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/MatrizDeErrores_CFDI_v40_20260325.xls) | Matriz de errores CFDI 4.0, version 2026-03-25 | 2026-03-24 | `bdc350035c7ed720af5be15509e01d0bda7c12cec4ad9797b0c34f889c1140a7` |
| [`Certificados_de_Prueba.zip`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Certificados_de_Prueba.zip) | CSD/FIEL de prueba (test certificates) | 2025-05-09 | `23563077528c1fefcf5d57ad5e2eed9fa51956fe821c97a5d4c54f32f2ebeb8a` |
| [`Complemento_de_Pagos20_RevB.pdf`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Complemento_de_Pagos20_RevB.pdf) | Estándar del complemento para recepción de pagos 2.0, revisión B | 2023-12-15 | `2580c5335a95315c8f2f7c2e741db3a441bbbe994635897757f514a690b38a3f` |
| [`Guia_llenado_pagos.pdf`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/Guia_llenado_pagos.pdf) | Guía de llenado del complemento para recepción de pagos 2.0 | 2026-01-17 | `d76bcaddc343fd23f6a03a0d735815f848655a01953c1884e6ca71f8c58c4700` |
| [`MatrizDeErrores_CRP_V20_RevB.xls`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/MatrizDeErrores_CRP_V20_RevB.xls) | Matriz de errores del complemento de pagos 2.0, revisión B | 2024-12-14 | `84fef6323770dadd89f4f6fa74fbafaa06a4692d577604363938b31e0d6e4268` |
| [`catPagos.xls`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/catPagos.xls) | Catálogos del complemento de pagos | 2022-09-25 | `5492da027a2edec9a7680e6a43c426ca3c7e3554a596f89ee0248cce576cb200` |
| [`PregFrec_RP.pdf`](http://omawww.sat.gob.mx/tramitesyservicios/Paginas/documentos/PregFrec_RP.pdf) | Preguntas frecuentes del complemento de pagos | 2022-09-25 | `bc1a8a5dd1f5f61439012aa4648a22db35fc243fa97cba6c87a13d6fb12d272e` |

## Bundled: XML schemas and stylesheets

Bundled unmodified in `cfdi4j-xml` (ADR 0007). `cadenaoriginal_4_0.xslt` is XSLT 2.0 and includes
`utilerias.xslt` and 33 complement stylesheets; all of them are needed for it to compile.

| Path | Published | SHA-256 |
|------|-----------|---------|
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/2/cadenaoriginal_2_0/utilerias.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/2/cadenaoriginal_2_0/utilerias.xslt) | 2020-08-08 | `1f7e9c9c04b2239cdf0928e6471d4f48165535c4d4d3ea445ec310b65cdf6c2c` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/4/cadenaoriginal_4_0/cadenaoriginal_4_0.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/4/cadenaoriginal_4_0/cadenaoriginal_4_0.xslt) | 2026-04-17 | `b0559b380e73b850ca8a3da53b077a6051a09d87068d8d98e82dfd4acfba7565` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd`](http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd) | 2024-06-04 | `2489b5b535f5cbc6a6c2db6132620de1833f85372512d01deea62880e700e276` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte20.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte20.xslt) | 2021-12-01 | `0f8fa95e65eaf1fbfaba34ee7c0b78396dd04c920b1b54facddd178fad55add5` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte30.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte30.xslt) | 2023-09-25 | `3da384b7797ab037c0fc37d79716e0f4eb7704c6a585353aa2d73c6f76892e35` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte31.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/CartaPorte/CartaPorte31.xslt) | 2024-06-17 | `6ef3db6a004a7b27014bf8f03b9762f337479f0694525b193eaa9e3ff058fa0f` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/ComercioExterior11/ComercioExterior11.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/ComercioExterior11/ComercioExterior11.xslt) | 2020-08-08 | `c96afd3f14e8c1802a653ffe71d7147f2f74eb4f5daf9e0b21afca0b2c80c7f7` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/ComercioExterior20/ComercioExterior20.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/ComercioExterior20/ComercioExterior20.xslt) | 2024-01-16 | `b7540d6878c8d1e14b3d7f8a8bf3889dc03b0dfb6dfbfc6d6b30e27a094b8446` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/EstadoDeCuentaCombustible/ecc12.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/EstadoDeCuentaCombustible/ecc12.xslt) | 2020-08-08 | `ad76f5a5c726df61fe82c1db5e3a189b1026fa0cd87518ea225498e24f4014fb` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/GastosHidrocarburos10/GastosHidrocarburos10.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/GastosHidrocarburos10/GastosHidrocarburos10.xslt) | 2020-08-08 | `dc295079d9884a2afec09d63819eb874b4bede35ca8d4fe713625d0973e78a6b` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/HidrocarburosPetro/hidrocarburospetroliferos.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/HidrocarburosPetro/hidrocarburospetroliferos.xslt) | 2026-04-27 | `b212b9df8119de4278a24d5258b27ac295746b7476cc6aef969a3ad1d6c55a4b` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/IngresosHidrocarburos10/IngresosHidrocarburos.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/IngresosHidrocarburos10/IngresosHidrocarburos.xslt) | 2020-08-08 | `8dc9fb24cb47162da3d6fb831a1aa57a12571986eb123adfad2471370d56c10f` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xsd`](http://www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xsd) | 2023-01-07 | `f37dd303a490ecd1b53eff3b6498aa1be9c084afaf903908ca30e8d55c23d14a` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/Pagos/Pagos20.xslt) | 2022-01-05 | `9277a0a5049fbd56e3123e6489ffb16f6ccfbca30943aa7dbd0f969f3b70bd0f` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd`](http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd) | 2020-08-08 | `fb3d061fe9914500bef8beb3ea2e8722ee8a8224fa3af63643877a64c0ea05f7` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/cadenaoriginal_TFD_1_1.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/cadenaoriginal_TFD_1_1.xslt) | 2020-08-08 | `d7807e936d5a768cec5dca7a35a8b6dd515287083ca9dd7424c1b722c4b0d5a6` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/TuristaPasajeroExtranjero/TuristaPasajeroExtranjero.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/TuristaPasajeroExtranjero/TuristaPasajeroExtranjero.xslt) | 2020-08-08 | `5a65dd5bd8d340fdc547b92ff2797199a97674bf2c78feacb69186c91338a68b` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/aerolineas/aerolineas.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/aerolineas/aerolineas.xslt) | 2020-08-08 | `b197afab9535802b04d5e614a2320c10b8b67180e97f120b51d7bb60e61e028d` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/arteantiguedades/obrasarteantiguedades.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/arteantiguedades/obrasarteantiguedades.xslt) | 2020-08-08 | `0823ab4e0ad86ffc6b674680981f4471a60c8ab72431b8dedacd08b1e61ba8e8` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/catalogos/Pagos/catPagos.xsd`](http://www.sat.gob.mx/sitio_internet/cfd/catalogos/Pagos/catPagos.xsd) | 2020-08-08 | `72dbaa6e3d6518ac26b1d6a03f44bf4280a4bf1e0361c926bdc714cd3db1dbd6` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/catalogos/catCFDI.xsd`](http://www.sat.gob.mx/sitio_internet/cfd/catalogos/catCFDI.xsd) | 2024-12-13 | `6c58936cb77576f839a4d7915953ceaf252b9eb9319f9458fe5bb67ae2bb0bb1` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/certificadodestruccion/certificadodedestruccion.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/certificadodestruccion/certificadodedestruccion.xslt) | 2020-08-08 | `e4b09099c1453b47bb286a69b05e60d5a5bccaaac466a4a2aebf788601f69349` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/cfdiregistrofiscal/cfdiregistrofiscal.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/cfdiregistrofiscal/cfdiregistrofiscal.xslt) | 2020-08-08 | `6f47783bfeafa3920cbb452a3adee0239efd2250c13b9d91d242930416d1dee2` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/consumodecombustibles/consumodeCombustibles11.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/consumodecombustibles/consumodeCombustibles11.xslt) | 2021-12-29 | `f43a77f25aba6ea7ce48769a5c700d2edce78e75914fbe866eafdb816ac2a9c1` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/detallista/detallista.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/detallista/detallista.xslt) | 2020-08-08 | `e09906ef78afc077aa3318ca87bf2469e8570f97299d2f0237a2bc81da1e5e12` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/divisas/divisas.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/divisas/divisas.xslt) | 2020-08-08 | `64e24ea80b61f6a48ab242875e66877a49449286140187a668aa88241464640e` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/donat/donat11.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/donat/donat11.xslt) | 2020-08-08 | `608a65b07c5841857562026c244c81f9cf3ddd32ff1d28bebab347bb4230fee3` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xslt) | 2020-08-08 | `ef5e7e6cc0666cfc67d0c41ad865c2a044947289013ca2add0f89dea7fde667f` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/implocal/implocal.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/implocal/implocal.xslt) | 2020-08-08 | `f99e91c983b7a43b72dc41129afca7342a30e6e45de71b34f7d4e5b6154439d4` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/ine/ine11.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/ine/ine11.xslt) | 2020-08-08 | `4098bf77bfee89e870604b7bc96ba4bdb19df78341f5e0cbffdd094eb916bcad` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/leyendasFiscales/leyendasFisc.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/leyendasFiscales/leyendasFisc.xslt) | 2020-08-08 | `1588e059a3fdfc29bcca4de27f99dcf52abbaa7d815f428f2353a3105f52328c` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/nomina/nomina12.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/nomina/nomina12.xslt) | 2020-08-08 | `4130bdb7f62d0987caa5af754ca0704c9e2693e3b454adfff02ba6146d673a08` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/notariospublicos/notariospublicos.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/notariospublicos/notariospublicos.xslt) | 2020-08-08 | `7d2609b4b2977ace634d022e83f49bb20534bf137d7bdba014a40ee4b85e1d21` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/pagoenespecie/pagoenespecie.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/pagoenespecie/pagoenespecie.xslt) | 2020-08-08 | `685854e25f091ebcb95a4865a7df80630949b2f400e47676ff8dfa2f3019a404` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/pfic/pfic.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/pfic/pfic.xslt) | 2020-08-08 | `5f18f835cf6007e92b156ad4cc65ab9167becdcfb1742f6cd655fd2f173e8b1b` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/renovacionysustitucionvehiculos/renovacionysustitucionvehiculos.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/renovacionysustitucionvehiculos/renovacionysustitucionvehiculos.xslt) | 2020-08-08 | `1cda351ecbe8c4ddb4c6bb6508e7ea9c36b11627317870f04939765274979aba` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/servicioparcialconstruccion/servicioparcialconstruccion.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/servicioparcialconstruccion/servicioparcialconstruccion.xslt) | 2020-08-08 | `692f0470dd0fd57e4c6ea87012296b78d67c31b00c3120eaba446d29219b29e4` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/tipoDatos/tdCFDI/tdCFDI.xsd`](http://www.sat.gob.mx/sitio_internet/cfd/tipoDatos/tdCFDI/tdCFDI.xsd) | 2024-06-04 | `b3b81fe4017b95d5477f23a32f47e8b0571683cfddfff1330508c75e02b504cd` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/valesdedespensa/valesdedespensa.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/valesdedespensa/valesdedespensa.xslt) | 2020-08-08 | `dff69b6c188c1332b6908936767050121571952a09b99f881c39e41773392b92` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/vehiculousado/vehiculousado.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/vehiculousado/vehiculousado.xslt) | 2020-08-08 | `683b2a66cfd07d67f3c19c48499692e648079d663475f2f04f14d37131a29235` |
| [`sat/xml/www.sat.gob.mx/sitio_internet/cfd/ventavehiculos/ventavehiculos11.xslt`](http://www.sat.gob.mx/sitio_internet/cfd/ventavehiculos/ventavehiculos11.xslt) | 2020-08-08 | `77a103915ea66d6ade5a23e5db7e3c66467abf744acef5f9ac8cafdc16362c7d` |

## Bundled: test certificates

Public test material from `Certificados_de_Prueba.zip` (see above), persona moral
`EKU9003173C9` (ESCUELA KEMPER URGATE SA DE CV). Password for every key in the archive, as stated in
its `Contraseña.txt`: `12345678a`. These are **not** real taxpayer credentials.

| Path | Original name in the archive | SHA-256 |
|------|------------------------------|---------|
| `sat/csd-pruebas/EKU9003173C9/csd.cer` | `Personas Morales/EKU9003173C9_20230517223532/CSD_EKU9003173C9_20230517223903/CSD_Sucursal_1_EKU9003173C9_20230517_223850.cer` | `92be3ef448e0fd01467f3b0cbd8e77b568d84ea58f22f5afa400b2ec99bbbb53` |
| `sat/csd-pruebas/EKU9003173C9/csd.key` | `Personas Morales/EKU9003173C9_20230517223532/CSD_EKU9003173C9_20230517223903/CSD_Sucursal_1_EKU9003173C9_20230517_223850.key` | `2d6308aeb5bc6ded4930e2cafd6861f8f7f138e7dd0c27fa25d9138ccbe8b049` |
| `sat/csd-pruebas/EKU9003173C9/fiel.cer` | `Personas Morales/EKU9003173C9_20230517223532/eku9003173c9.cer` | `256432344c27b1e0d7884913abec66441871ad297faf5819999a68b18a6556b0` |

Facts read from the certificates with `openssl x509` on 2026-11-14:

| File | Serial (hex of ASCII) → NoCertificado | Valid | Key usage |
|------|----------------------------------------|-------|-----------|
| `csd.cer` | `3330303031303030303030353030303033343136` → `30001000000500003416` | 2023-05-18 → 2027-05-18 | Digital Signature, Non Repudiation |
| `fiel.cer` | `3330303031303030303030353030303033343135` → `30001000000500003415` | 2023-05-18 → 2027-05-17 | Digital Signature, Non Repudiation, Data Encipherment, Key Agreement; EKU e-mail protection, TLS client |

`csd.key` is an encrypted PKCS#8 `EncryptedPrivateKeyInfo` using PBES2 (PBKDF2, 2048 iterations,
`des-ede3-cbc`).

Complemento de pagos index page (2026-11-17):
<http://omawww.sat.gob.mx/tramitesyservicios/Paginas/recepcion_de_pagos.htm>. It also links
`Pagos20_.xsd`, an earlier variant of `Pagos20.xsd` (`EquivalenciaDR` with 6 instead of 10
decimals, published 2023-01-07); it is not used, because `cadenaoriginal_4_0.xslt` and the
standard reference `Pagos20.xsd`.
