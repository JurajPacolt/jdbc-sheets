/* Created on 20.03.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author juraj.pacolt
 */
public class CellTypesTest {

    @Test
    public void readsBasicCellTypes() throws Exception {
        Class.forName(TestConstants.DRIVER_CLASS);
        Path tempFile = createTempWorkbook();
        try (Connection conn = DriverManager.getConnection(urlForFile(tempFile))) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A,B,C,D,E from Sheet1 limit 1 offset 0")) {
                    Assertions.assertTrue(rs.next());

                    Assertions.assertEquals(12.5d, rs.getDouble(1), 0.0001d);
                    Assertions.assertTrue(rs.getObject(1) instanceof Number);

                    Assertions.assertTrue(rs.getBoolean(2));
                    Assertions.assertTrue(rs.getObject(2) instanceof Boolean);

                    Object dateObj = rs.getObject(3);
                    Assertions.assertNotNull(dateObj);
                    Assertions.assertTrue(dateObj instanceof Date);
                    LocalDate actualDate = Instant.ofEpochMilli(((Date) dateObj).getTime())
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    Assertions.assertEquals(LocalDate.of(2024, 1, 2), actualDate);

                    Assertions.assertEquals(25.0d, rs.getDouble(4), 0.0001d);
                    Assertions.assertEquals("hello", rs.getString(5));
                }
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path createTempWorkbook() throws Exception {
        Path tempFile = Files.createTempFile("jdbc-sheets-types", ".xlsx");
        tempFile.toFile().deleteOnExit();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet("Sheet1").createRow(0);
            row.createCell(0).setCellValue(12.5d);
            row.createCell(1).setCellValue(true);

            Cell dateCell = row.createCell(2);
            dateCell.setCellValue(java.sql.Date.valueOf(LocalDate.of(2024, 1, 2)));
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            dateCell.setCellStyle(dateStyle);

            Cell formulaCell = row.createCell(3);
            formulaCell.setCellFormula("A1*2");

            row.createCell(4).setCellValue("hello");

            try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                workbook.write(out);
            }
        }
        return tempFile;
    }

    private String urlForFile(Path file) {
        String path = file.toAbsolutePath().toString().replace('\\', '/');
        return "jdbc:sheets://?file=" + path;
    }
}
