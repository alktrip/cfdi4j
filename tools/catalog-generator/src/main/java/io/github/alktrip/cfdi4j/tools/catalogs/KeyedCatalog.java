package io.github.alktrip.cfdi4j.tools.catalogs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Describes a keyed catalog sheet and emits its open-enum class (ADR 0003).
 *
 * @param sheet sheet name, which is also the catalog name ({@code c_FormaPago})
 * @param className generated class name
 * @param descripcionColumn column of the description, or -1 when the catalog has none
 * @param inicioColumn column of "Fecha inicio de vigencia", or -1 when the catalog has none
 * @param finColumn column of "Fecha fin de vigencia", or -1
 * @param headers expected header cells, relative to the row that holds the catalog name
 * @param dataOffset first data row, relative to that row
 * @param fields catalog-specific columns
 */
record KeyedCatalog(
        String sheet,
        String className,
        int descripcionColumn,
        int inicioColumn,
        int finColumn,
        List<Header> headers,
        int dataOffset,
        List<Field> fields) {

    /** An expected header cell. */
    record Header(int rowOffset, int column, String text) {}

    /** Kinds of catalog-specific columns. */
    enum Kind {
        BOOLEAN("boolean"),
        OPTIONAL_INT("Integer"),
        OPTIONAL_DECIMAL("BigDecimal"),
        OPTIONAL_TEXT("String"),
        OPTIONAL_REQUISITO("Requisito"),
        REGIMEN_SET("Set<RegimenFiscal>"),
        TEXT_SET("Set<String>");

        final String fieldType;

        Kind(String fieldType) {
            this.fieldType = fieldType;
        }
    }

    /** Extracts the Java literal of a field from a data row. */
    @FunctionalInterface
    interface Extractor {
        String literal(SheetReader reader, int row, String clave, Context context);
    }

    /** A catalog-specific column. */
    record Field(String name, Kind kind, String javadoc, Extractor extractor) {}

    /** Names already resolved for other catalogs (for cross-catalog references). */
    interface Context {
        String constantName(String catalog, String clave);
    }

    /** A parsed row. */
    record Entry(String clave, String descripcion, LocalDate inicio, LocalDate fin, List<String> literals) {}

    List<Entry> read(SheetReader reader, Context context) {
        int headerRow = reader.findRow(0, sheet);
        for (Header h : headers) {
            reader.expect(headerRow + h.rowOffset(), h.column(), h.text());
        }
        List<Entry> entries = new ArrayList<>();
        Set<String> claves = new LinkedHashSet<>();
        for (int row = headerRow + dataOffset; row <= reader.lastRow(); row++) {
            String clave = reader.text(row, 0);
            if (clave.isEmpty()) {
                continue;
            }
            if (!claves.add(clave)) {
                throw reader.fail(row, 0, "duplicated key '" + clave + "'");
            }
            String descripcion = descripcionColumn < 0 ? null : reader.requiredText(row, descripcionColumn);
            LocalDate inicio = inicioColumn < 0 ? null : reader.date(row, inicioColumn)
                    .orElseThrow(() -> new GeneratorException(sheet + " key " + clave + ": no validity start date"));
            LocalDate fin = finColumn < 0 ? null : reader.date(row, finColumn).orElse(null);
            List<String> literals = new ArrayList<>();
            for (Field field : fields) {
                literals.add(field.extractor().literal(reader, row, clave, context));
            }
            entries.add(new Entry(clave, descripcion, inicio, fin, literals));
        }
        if (entries.isEmpty()) {
            throw new GeneratorException(sheet + ": no entries");
        }
        return entries;
    }

    static Map<String, String> keysWithDescriptions(List<Entry> entries) {
        Map<String, String> keys = new LinkedHashMap<>();
        entries.forEach(e -> keys.put(e.clave(), e.descripcion() == null ? e.clave() : e.descripcion()));
        return keys;
    }

    String emit(List<Entry> entries, Map<String, String> names, String header) {
        Set<String> imports = new TreeSet<>(List.of("java.util.List", "java.util.Map", "java.util.Objects", "java.util.function.Function", "java.util.stream.Collectors"));
        if (inicioColumn >= 0) {
            imports.add("java.time.LocalDate");
        }
        for (Field f : fields) {
            switch (f.kind()) {
                case OPTIONAL_INT -> imports.add("java.util.OptionalInt");
                case OPTIONAL_DECIMAL -> {
                    imports.add("java.math.BigDecimal");
                    imports.add("java.util.Optional");
                }
                case OPTIONAL_TEXT, OPTIONAL_REQUISITO -> imports.add("java.util.Optional");
                case REGIMEN_SET, TEXT_SET -> imports.add("java.util.Set");
                case BOOLEAN -> { }
            }
        }

        StringBuilder out = new StringBuilder(header);
        out.append("package ").append(CatalogGenerator.PACKAGE).append(";\n\n");
        imports.forEach(i -> out.append("import ").append(i).append(";\n"));
        out.append("\n/**\n * Catálogo {@code ").append(sheet).append("}.\n *\n")
                .append(" * <p>Generated from the official SAT catalog file (see {@link CatalogVersion}). Constants\n")
                .append(" * are named after the key or a curated short name (ADR 0008); {@link #of(String)} accepts any key.\n")
                .append(" */\n");
        out.append("public final class ").append(className).append(" extends CatalogKey {\n\n");

        for (Entry e : entries) {
            out.append("    /** {@code ").append(JavaText.javadoc(e.clave())).append("}");
            if (e.descripcion() != null) {
                out.append(": ").append(JavaText.javadoc(JavaText.sentence(e.descripcion())));
            }
            out.append(" */\n");
            out.append("    public static final ").append(className).append(' ').append(names.get(e.clave()))
                    .append(" =\n            new ").append(className).append('(')
                    .append(JavaText.string(e.clave())).append(", ")
                    .append(JavaText.string(e.descripcion())).append(", ")
                    .append(JavaText.vigencia(e.inicio(), e.fin()));
            for (String literal : e.literals()) {
                out.append(", ").append(literal);
            }
            out.append(");\n\n");
        }

        out.append("    private static final List<").append(className).append("> VALUES = List.of(\n");
        for (int i = 0; i < entries.size(); i++) {
            out.append("            ").append(names.get(entries.get(i).clave())).append(i + 1 < entries.size() ? ",\n" : ");\n\n");
        }
        out.append("    private static final Map<String, ").append(className).append("> BY_CLAVE =\n")
                .append("            VALUES.stream().collect(Collectors.toUnmodifiableMap(CatalogKey::clave, Function.identity()));\n\n");

        for (Field f : fields) {
            out.append("    private final ").append(f.kind().fieldType).append(' ').append(f.name()).append(";\n");
        }
        if (!fields.isEmpty()) {
            out.append('\n');
        }

        // Known-key constructor
        out.append("    private ").append(className).append("(String clave, String descripcion, Vigencia vigencia");
        for (Field f : fields) {
            out.append(", ").append(f.kind().fieldType).append(' ').append(f.name());
        }
        out.append(") {\n        super(clave, descripcion, vigencia, true);\n");
        for (Field f : fields) {
            out.append("        this.").append(f.name()).append(" = ").append(f.name()).append(";\n");
        }
        out.append("    }\n\n");

        // Unknown-key constructor
        out.append("    private ").append(className).append("(String clave) {\n        super(clave, null, null, false);\n");
        for (Field f : fields) {
            String unknown = switch (f.kind()) {
                case BOOLEAN -> "false";
                case REGIMEN_SET, TEXT_SET -> "Set.of()";
                default -> "null";
            };
            out.append("        this.").append(f.name()).append(" = ").append(unknown).append(";\n");
        }
        out.append("    }\n\n");

        out.append("    /**\n     * Returns the catalog entry for a key. Never fails for a non-null key: a key that is not in\n")
                .append("     * the catalog yields an instance whose {@link #isKnown()} is {@code false}.\n     *\n")
                .append("     * @param clave the key as written in the XML\n     * @return the entry\n     */\n")
                .append("    public static ").append(className).append(" of(String clave) {\n")
                .append("        Objects.requireNonNull(clave, \"clave\");\n")
                .append("        ").append(className).append(" known = BY_CLAVE.get(clave);\n")
                .append("        return known != null ? known : new ").append(className).append("(clave);\n    }\n\n");

        out.append("    /**\n     * Returns every key of the catalog, in catalog order.\n     *\n     * @return the known keys\n     */\n")
                .append("    public static List<").append(className).append("> values() {\n        return VALUES;\n    }\n\n");

        for (Field f : fields) {
            out.append("    /**\n     * ").append(f.javadoc()).append("\n     *\n     * @return the value");
            out.append(switch (f.kind()) {
                case BOOLEAN -> "; {@code false} for an unknown key";
                case REGIMEN_SET, TEXT_SET -> "; empty for an unknown key";
                default -> "; empty for an unknown key or when the catalog leaves it blank";
            });
            out.append("\n     */\n");
            switch (f.kind()) {
                case BOOLEAN -> out.append("    public boolean ").append(f.name()).append("() {\n        return ").append(f.name()).append(";\n    }\n\n");
                case OPTIONAL_INT -> out.append("    public OptionalInt ").append(f.name()).append("() {\n        return ")
                        .append(f.name()).append(" == null ? OptionalInt.empty() : OptionalInt.of(").append(f.name()).append(");\n    }\n\n");
                case REGIMEN_SET, TEXT_SET -> out.append("    public ").append(f.kind().fieldType).append(' ').append(f.name())
                        .append("() {\n        return ").append(f.name()).append(";\n    }\n\n");
                default -> out.append("    public Optional<").append(f.kind().fieldType).append("> ").append(f.name())
                        .append("() {\n        return Optional.ofNullable(").append(f.name()).append(");\n    }\n\n");
            }
        }

        out.append("    @Override\n    public boolean equals(Object other) {\n        return other instanceof ")
                .append(className).append(" that && clave().equals(that.clave());\n    }\n\n");
        out.append("    @Override\n    public int hashCode() {\n        return clave().hashCode();\n    }\n}\n");
        return out.toString();
    }
}
