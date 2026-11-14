package io.github.alktrip.cfdi4j.core.catalog;

/** Values used by SAT catalogs to state whether something applies: "Sí", "No" or "Opcional". */
public enum Requisito {
    /** "Sí": required or applicable. */
    SI,
    /** "No": not allowed or not applicable. */
    NO,
    /** "Opcional": allowed but not required. */
    OPCIONAL
}
