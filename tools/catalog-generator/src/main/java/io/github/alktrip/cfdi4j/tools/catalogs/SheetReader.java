package io.github.alktrip.cfdi4j.tools.catalogs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Reads cells of a SAT catalog sheet and converts them strictly: any value that does not have the
 * expected shape stops the generator with a message that names the sheet, row and column.
 */
final class SheetReader {

    private final Sheet sheet;
    private final DataFormatter formatter;

    SheetReader(Sheet sheet) {
        this.sheet = sheet;
        this.formatter = new DataFormatter(Locale.US);
        this.formatter.setUseCachedValuesForFormulaCells(true);
    }

    String sheetName() {
        return sheet.getSheetName();
    }

    int lastRow() {
        return sheet.getLastRowNum();
    }

    /** Finds the header row: the first row whose cell {@code column} reads exactly {@code text}. */
    int findRow(int column, String text) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            if (text(r, column).equals(text)) {
                return r;
            }
        }
        throw fail(-1, column, "header '" + text + "' not found");
    }

    /** Fails unless the cell reads exactly {@code expected} (after whitespace normalization). */
    void expect(int row, int column, String expected) {
        String actual = text(row, column);
        if (!actual.equals(expected)) {
            throw fail(row, column, "expected header '" + expected + "' but found '" + actual + "'");
        }
    }

    /**
     * Returns the displayed text of a cell, trimmed and with internal whitespace collapsed. Numeric
     * keys stored with a format such as {@code 00} are returned as displayed ({@code "01"}).
     */
    String text(int row, int column) {
        Cell cell = cell(row, column);
        if (cell == null) {
            return "";
        }
        return normalize(formatter.formatCellValue(cell));
    }

    Optional<String> optionalText(int row, int column) {
        String value = text(row, column);
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    String requiredText(int row, int column) {
        return optionalText(row, column).orElseThrow(() -> fail(row, column, "value is required"));
    }

    /** Reads a date stored as an Excel date. */
    Optional<LocalDate> date(int row, int column) {
        Cell cell = cell(row, column);
        if (cell == null || cell.getCellType() == CellType.BLANK || text(row, column).isEmpty()) {
            return Optional.empty();
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return Optional.of(cell.getLocalDateTimeCellValue().toLocalDate());
        }
        throw fail(row, column, "expected a date cell but found '" + text(row, column) + "'");
    }

    /**
     * Reads a decimal number. Text cells are parsed exactly; numeric cells (binary doubles) are
     * converted to the given scale and rejected if that loses information.
     */
    Optional<BigDecimal> decimal(int row, int column, int numericScale) {
        Cell cell = cell(row, column);
        String text = text(row, column);
        if (cell == null || text.isEmpty()) {
            return Optional.empty();
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        try {
            if (type == CellType.NUMERIC) {
                return Optional.of(BigDecimal.valueOf(cell.getNumericCellValue()).setScale(numericScale, RoundingMode.UNNECESSARY));
            }
            return Optional.of(new BigDecimal(text));
        } catch (ArithmeticException | NumberFormatException e) {
            throw fail(row, column, "not an exact decimal: '" + text + "'");
        }
    }

    /** Reads a "Sí"/"Si"/"No" cell. */
    boolean yesNo(int row, int column) {
        String value = requiredText(row, column);
        return switch (value) {
            case "Sí", "Si" -> true;
            case "No" -> false;
            default -> throw fail(row, column, "expected Sí/No but found '" + value + "'");
        };
    }

    /** Reads a "Sí"/"Si"/"No"/"Opcional" cell into the name of a {@code Requisito} constant. */
    String requisito(int row, int column) {
        String value = requiredText(row, column);
        return switch (value) {
            case "Sí", "Si" -> "SI";
            case "No" -> "NO";
            case "Opcional" -> "OPCIONAL";
            default -> throw fail(row, column, "expected Sí/No/Opcional but found '" + value + "'");
        };
    }

    GeneratorException fail(int row, int column, String message) {
        String where = row < 0 ? "" : " row " + (row + 1);
        String col = column < 0 ? "" : " column " + (char) ('A' + column);
        return new GeneratorException("Sheet " + sheet.getSheetName() + where + col + ": " + message);
    }

    private Cell cell(int row, int column) {
        Row r = sheet.getRow(row);
        return r == null ? null : r.getCell(column);
    }

    static String normalize(String value) {
        return value.replace(' ', ' ').strip().replaceAll("\\s+", " ");
    }
}
