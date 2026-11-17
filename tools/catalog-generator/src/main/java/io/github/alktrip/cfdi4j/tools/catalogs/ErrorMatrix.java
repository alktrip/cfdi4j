package io.github.alktrip.cfdi4j.tools.catalogs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Reads the official error matrix ({@code MatrizDeErrores_CFDI_v40_<date>.xls}, sheet
 * {@code Códigos_Error_CFDI40}) and emits {@code MatrizErrores}, the inventory of official codes
 * (ADR 0004).
 */
final class ErrorMatrix {

    /**
     * A matrix and where its inventory goes.
     *
     * @param code pattern of its codes
     * @param pkg package of the generated class
     * @param className generated class name
     * @param title description in the Javadoc
     */
    record Kind(Pattern code, String pkg, String className, String title) {}

    /**
     * Official codes are {@code CFDI40xxx}; the 2026-03-25 matrix numbers its last rules
     * {@code CFDI140223} to {@code CFDI140228}. They are kept exactly as published.
     */
    static final Kind CFDI40 = new Kind(Pattern.compile("CFDI1?40[0-9]{3}"), "io.github.alktrip.cfdi4j.core.validation",
            "MatrizErrores", "The official error matrix of CFDI 4.0");
    static final Kind PAGOS20 = new Kind(Pattern.compile("CRP20[0-9]{3}"), "io.github.alktrip.cfdi4j.pagos20",
            "MatrizErroresPagos20", "The official error matrix of the complemento para recepción de pagos 2.0");

    record Row(String codigo, String elemento, String atributo, String validacion, String descripcion) {}

    static List<Row> read(Sheet sheet) {
        return read(sheet, CFDI40);
    }

    static List<Row> read(Sheet sheet, Kind kind) {
        SheetReader r = new SheetReader(sheet);
        int h = r.findRow(4, "CÓDIGO ERROR");
        r.expect(h, 0, "NÚM");
        r.expect(h, 1, "ELEMENTO");
        r.expect(h, 2, "ATRIBUTO");
        r.expect(h, 3, "VALIDACIÓN");
        r.expect(h, 5, "DESCRIPCIÓN DE CÓDIGO DE ERROR");
        List<Row> rows = new ArrayList<>();
        Set<String> codes = new LinkedHashSet<>();
        for (int row = h + 1; row <= r.lastRow(); row++) {
            String code = r.text(row, 4);
            if (code.isEmpty()) {
                boolean others = false;
                for (int c = 1; c <= 5; c++) {
                    others |= !r.text(row, c).isEmpty();
                }
                // Blank rows and section titles (text in the first column only) carry no rule.
                if (others) {
                    throw r.fail(row, 4, "row without error code");
                }
                continue;
            }
            if (!kind.code().matcher(code).matches()) {
                throw r.fail(row, 4, "unexpected error code '" + code + "'");
            }
            if (!codes.add(code)) {
                throw r.fail(row, 4, "duplicated error code " + code);
            }
            rows.add(new Row(code, r.text(row, 1), r.text(row, 2), r.requiredText(row, 3), r.text(row, 5)));
        }
        if (rows.isEmpty()) {
            throw new GeneratorException(sheet.getSheetName() + ": no codes");
        }
        return rows;
    }

    static String emit(List<Row> rows, Kind kind, String header, String sourceFile, String sha256) {
        if (kind.equals(CFDI40)) {
            return emit(rows, header, sourceFile, sha256);
        }
        StringBuilder out = new StringBuilder(header);
        out.append("package ").append(kind.pkg()).append(";\n\n")
                .append("import io.github.alktrip.cfdi4j.core.validation.MatrizErrores.Entrada;\nimport java.util.List;\n\n")
                .append("/**\n * ").append(kind.title()).append(": every code with its element, attribute, validation\n")
                .append(" * and description as published by the SAT (ADR 0004).\n *\n")
                .append(" * <p>Generated from {@code ").append(sourceFile).append("}; see {@code specs/SOURCES.md}.\n */\n")
                .append("public final class ").append(kind.className()).append(" {\n\n")
                .append("    /** File name as published by the SAT. */\n")
                .append("    public static final String SOURCE_FILE = ").append(JavaText.string(sourceFile)).append(";\n\n")
                .append("    /** SHA-256 of the file, as recorded in {@code specs/SOURCES.md}. */\n")
                .append("    public static final String SHA256 = ").append(JavaText.string(sha256)).append(";\n\n")
                .append("    private static final List<Entrada> ENTRADAS = List.of(\n");
        entries(out, rows);
        out.append("\n    private ").append(kind.className()).append("() {}\n\n")
                .append("    /**\n     * Returns every row, in matrix order.\n     *\n     * @return the rows\n     */\n")
                .append("    public static List<Entrada> entradas() {\n        return ENTRADAS;\n    }\n}\n");
        return out.toString();
    }

    private static void entries(StringBuilder out, List<Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            out.append("            new Entrada(").append(JavaText.string(r.codigo())).append(", ").append(JavaText.string(r.elemento()))
                    .append(", ").append(JavaText.string(r.atributo())).append(",\n                    ")
                    .append(JavaText.string(r.validacion())).append(",\n                    ")
                    .append(JavaText.string(r.descripcion())).append(i + 1 < rows.size() ? ")," : "));").append('\n');
        }
    }

    static String emit(List<Row> rows, String header, String sourceFile, String sha256) {
        StringBuilder out = new StringBuilder(header);
        out.append("package io.github.alktrip.cfdi4j.core.validation;\n\n")
                .append("import java.util.List;\nimport java.util.Map;\nimport java.util.Optional;\nimport java.util.function.Function;\n")
                .append("import java.util.stream.Collectors;\n\n")
                .append("/**\n * The official error matrix of CFDI 4.0: every {@code CFDI40xxx} code with its element,\n")
                .append(" * attribute, validation and description as published by the SAT (ADR 0004). Codes are kept\n")
                .append(" * exactly as published, including {@code CFDI140223} to {@code CFDI140228}.\n *\n")
                .append(" * <p>Generated from {@code ").append(sourceFile).append("}; see {@code specs/SOURCES.md}.\n */\n")
                .append("public final class MatrizErrores {\n\n")
                .append("    /**\n     * One row of the matrix.\n     *\n")
                .append("     * @param codigo the code, for example {@code CFDI40101}\n")
                .append("     * @param elemento column ELEMENTO\n     * @param atributo column ATRIBUTO\n")
                .append("     * @param validacion column VALIDACIÓN\n")
                .append("     * @param descripcion column DESCRIPCIÓN DE CÓDIGO DE ERROR; empty when the SAT left it blank\n     */\n")
                .append("    public record Entrada(String codigo, String elemento, String atributo, String validacion, String descripcion) {\n\n")
                .append("        /**\n         * Returns the description, or the validation text when the description is blank.\n")
                .append("         *\n         * @return the text\n         */\n")
                .append("        public String mensaje() {\n            return descripcion.isEmpty() ? validacion : descripcion;\n        }\n    }\n\n")
                .append("    /** File name as published by the SAT. */\n")
                .append("    public static final String SOURCE_FILE = ").append(JavaText.string(sourceFile)).append(";\n\n")
                .append("    /** SHA-256 of the file, as recorded in {@code specs/SOURCES.md}. */\n")
                .append("    public static final String SHA256 = ").append(JavaText.string(sha256)).append(";\n\n")
                .append("    private static final List<Entrada> ENTRADAS = List.of(\n");
        entries(out, rows);
        out.append("\n    private static final Map<String, Entrada> BY_CODIGO =\n")
                .append("            ENTRADAS.stream().collect(Collectors.toUnmodifiableMap(Entrada::codigo, Function.identity()));\n\n")
                .append("    private MatrizErrores() {}\n\n")
                .append("    /**\n     * Returns every row, in matrix order.\n     *\n     * @return the rows\n     */\n")
                .append("    public static List<Entrada> entradas() {\n        return ENTRADAS;\n    }\n\n")
                .append("    /**\n     * Returns the row of a code.\n     *\n     * @param codigo the code\n     * @return the row, if the code is in the matrix\n     */\n")
                .append("    public static Optional<Entrada> of(String codigo) {\n        return Optional.ofNullable(BY_CODIGO.get(codigo));\n    }\n}\n");
        return out.toString();
    }
}
