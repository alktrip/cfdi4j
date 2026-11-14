#!/usr/bin/env bash
# Regenerates the layer-1 golden files (ADR 0005) in cfdi4j-xml/src/test/resources/golden/own.
#
# 1. Inputs: written by GoldenInputs.java with the cfdi4j serializer (they are test documents).
# 2. Expected cadena original: produced WITHOUT cfdi4j, by the Saxon-HE command line running the
#    official stylesheet fetched from the SAT web site (not the bundled copy, not the cfdi4j
#    resolver). Needs network access.
# 3. SHA-256 of each cadena with sha256sum.
# 4. Expected sello: the cadena signed by OpenSSL (openssl dgst -sha256 -sign) with the SAT test CSD
#    key of EKU9003173C9, decrypted by OpenSSL (password from the SAT archive). RSASSA-PKCS1-v1_5 is
#    deterministic, so cfdi4j must produce exactly the same Base64.
#
# Run from the repository root after: ./mvnw -B -pl cfdi4j-xml -am test-compile
set -euo pipefail

OUT=cfdi4j-xml/src/test/resources/golden/own
M2=${M2_REPO:-$HOME/.m2/repository}
SAXON="$M2/net/sf/saxon/Saxon-HE/13.1/Saxon-HE-13.1.jar:$M2/org/xmlresolver/xmlresolver/6.1.0/xmlresolver-6.1.0.jar:$M2/org/xmlresolver/xmlresolver/6.1.0/xmlresolver-6.1.0-data.jar"
XSLT=http://www.sat.gob.mx/sitio_internet/cfd/4/cadenaoriginal_4_0/cadenaoriginal_4_0.xslt

CSD=specs/sat/csd-pruebas/EKU9003173C9
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
openssl pkcs8 -inform DER -in "$CSD/csd.key" -passin pass:12345678a -out "$TMP/key.pem"

mkdir -p "$OUT"
java -cp "cfdi4j-core/target/classes:cfdi4j-xml/target/classes:$SAXON" tools/golden/GoldenInputs.java "$OUT"

for xml in "$OUT"/*.xml; do
  name=${xml%.xml}
  java -cp "$SAXON" net.sf.saxon.Transform -s:"$xml" -xsl:"$XSLT" -o:"$name.cadena.txt"
  sha256sum "$name.cadena.txt" | cut -d' ' -f1 > "$name.cadena.sha256"
  openssl dgst -sha256 -sign "$TMP/key.pem" "$name.cadena.txt" | base64 -w0 > "$name.sello"
  echo "$(basename "$name"): $(cat "$name.cadena.sha256")"
done
