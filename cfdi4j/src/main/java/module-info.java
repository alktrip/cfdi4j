/** Entry point to validate, seal and verify CFDI 4.0 documents. */
module io.github.alktrip.cfdi4j {
    requires transitive io.github.alktrip.cfdi4j.core;
    requires transitive io.github.alktrip.cfdi4j.xml;
    requires transitive io.github.alktrip.cfdi4j.crypto;
}
