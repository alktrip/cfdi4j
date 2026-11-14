package io.github.alktrip.cfdi4j.tools.catalogs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.alktrip.cfdi4j.tools.catalogs.KeyedCatalog.Header;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;

/** Reads small synthetic sheets laid out like the official catalog file. */
class KeyedCatalogTest {

    private static final KeyedCatalog CATALOG = new KeyedCatalog("c_Test", "Test", 1, 2, 3,
            List.of(new Header(0, 1, "Descripción"), new Header(0, 2, "Fecha inicio de vigencia"),
                    new Header(0, 3, "Fecha fin de vigencia")),
            1, List.of());

    @Test
    void readsKeysDescriptionsAndDates() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb, "Descripción");
            dataRow(wb, sheet, 2, "A1", "Primera  clave ", LocalDate.of(2022, 1, 1));
            sheet.createRow(3);
            dataRow(wb, sheet, 4, "B2", "Segunda", LocalDate.of(2024, 12, 13));

            List<KeyedCatalog.Entry> entries = CATALOG.read(new SheetReader(sheet), (c, k) -> k);

            assertThat(entries).extracting(KeyedCatalog.Entry::clave).containsExactly("A1", "B2");
            assertThat(entries.get(0).descripcion()).isEqualTo("Primera clave");
            assertThat(entries.get(1).inicio()).isEqualTo(LocalDate.of(2024, 12, 13));
            assertThat(entries.get(1).fin()).isNull();

            String source = CATALOG.emit(entries, Map.of("A1", "PRIMERA", "B2", "SEGUNDA"), "// header\n");
            assertThat(source)
                    .contains("public final class Test extends CatalogKey")
                    .contains("public static final Test PRIMERA =")
                    .contains("new Test(\"A1\", \"Primera clave\", Vigencia.desde(LocalDate.of(2022, 1, 1)))")
                    .contains("public static Test of(String clave)");
        }
    }

    @Test
    void changedHeaderStopsTheGenerator() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb, "Descripcion corregida");

            assertThatThrownBy(() -> CATALOG.read(new SheetReader(sheet), (c, k) -> k))
                    .isInstanceOf(GeneratorException.class)
                    .hasMessageContaining("expected header 'Descripción' but found 'Descripcion corregida'");
        }
    }

    @Test
    void duplicatedKeyStopsTheGenerator() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb, "Descripción");
            dataRow(wb, sheet, 2, "A1", "Uno", LocalDate.of(2022, 1, 1));
            dataRow(wb, sheet, 3, "A1", "Otra vez", LocalDate.of(2022, 1, 1));

            assertThatThrownBy(() -> CATALOG.read(new SheetReader(sheet), (c, k) -> k))
                    .hasMessageContaining("duplicated key 'A1'");
        }
    }

    @Test
    void textWhereADateIsExpectedStopsTheGenerator() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            Sheet sheet = sheet(wb, "Descripción");
            Row row = sheet.createRow(2);
            row.createCell(0).setCellValue("A1");
            row.createCell(1).setCellValue("Uno");
            row.createCell(2).setCellValue("1 de enero");

            assertThatThrownBy(() -> CATALOG.read(new SheetReader(sheet), (c, k) -> k))
                    .hasMessageContaining("expected a date cell");
        }
    }

    private static Sheet sheet(HSSFWorkbook wb, String descripcionHeader) {
        Sheet sheet = wb.createSheet("c_Test");
        sheet.createRow(0).createCell(0).setCellValue("Catálogo de prueba.");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("c_Test");
        header.createCell(1).setCellValue(descripcionHeader);
        header.createCell(2).setCellValue("Fecha inicio de vigencia");
        header.createCell(3).setCellValue("Fecha fin de vigencia");
        return sheet;
    }

    private static void dataRow(HSSFWorkbook wb, Sheet sheet, int index, String clave, String descripcion, LocalDate inicio) {
        CellStyle date = wb.createCellStyle();
        date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("m/d/yy"));
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(clave);
        row.createCell(1).setCellValue(descripcion);
        var cell = row.createCell(2);
        cell.setCellValue(inicio);
        cell.setCellStyle(date);
    }
}
