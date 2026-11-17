package io.github.alktrip.cfdi4j.tools.catalogs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;

/** Reads synthetic sheets laid out like the official error matrix. */
class ErrorMatrixTest {

    @Test
    void readsCodesIncludingTheSixDigitOnes() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb);
            row(sheet, 2, "1", "CFDI40101", "Validación uno", "Descripción uno");
            row(sheet, 3, "2", "CFDI140223", "Validación dos", "");

            List<ErrorMatrix.Row> rows = ErrorMatrix.read(sheet);

            assertThat(rows).extracting(ErrorMatrix.Row::codigo).containsExactly("CFDI40101", "CFDI140223");
            String source = ErrorMatrix.emit(rows, "// header\n", "matriz.xls", "abc");
            assertThat(source).contains("public final class MatrizErrores").contains("new Entrada(\"CFDI140223\"")
                    .contains("SOURCE_FILE = \"matriz.xls\"");
        }
    }

    @Test
    void unexpectedOrDuplicatedCodesStopTheGenerator() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb);
            row(sheet, 2, "1", "CFDI3301", "V", "D");
            assertThatThrownBy(() -> ErrorMatrix.read(sheet)).hasMessageContaining("unexpected error code 'CFDI3301'");
        }
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb);
            row(sheet, 2, "1", "CFDI40101", "V", "D");
            row(sheet, 3, "2", "CFDI40101", "V", "D");
            assertThatThrownBy(() -> ErrorMatrix.read(sheet)).hasMessageContaining("duplicated error code CFDI40101");
        }
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb);
            row(sheet, 2, "1", "", "V", "D");
            assertThatThrownBy(() -> ErrorMatrix.read(sheet)).hasMessageContaining("row without error code");
        }
    }

    private static Sheet sheet(HSSFWorkbook wb) {
        Sheet sheet = wb.createSheet(ErrorMatrix.SHEET);
        sheet.createRow(0).createCell(0).setCellValue("Matriz de códigos de error");
        Row h = sheet.createRow(1);
        String[] headers = {"NÚM", "ELEMENTO", "ATRIBUTO", "VALIDACIÓN", "CÓDIGO ERROR", "DESCRIPCIÓN DE CÓDIGO DE ERROR"};
        for (int i = 0; i < headers.length; i++) {
            h.createCell(i).setCellValue(headers[i]);
        }
        return sheet;
    }

    private static void row(Sheet sheet, int index, String num, String code, String validacion, String descripcion) {
        Row r = sheet.createRow(index);
        r.createCell(0).setCellValue(num);
        r.createCell(1).setCellValue("Comprobante");
        r.createCell(2).setCellValue("Fecha");
        r.createCell(3).setCellValue(validacion);
        r.createCell(4).setCellValue(code);
        r.createCell(5).setCellValue(descripcion);
    }
}
