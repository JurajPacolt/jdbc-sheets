/* Created on 18.01.2026 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * @author juraj.pacolt
 */
public class ConcatSelectTest {

    @Test
    public void selectConcatSupportsAlias() throws Exception {
        Class.forName(TestConstants.DRIVER_CLASS);
        Path tempFile = createTempWorkbook();
        try (Connection conn = DriverManager.getConnection(urlForFile(tempFile))) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A || ' ' || B as FullName from Sheet1 limit 1 offset 0")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals("John Doe", rs.getString("FullName"));
                    ResultSetMetaData meta = rs.getMetaData();
                    Assertions.assertEquals("FullName", meta.getColumnLabel(1));
                }
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path createTempWorkbook() throws Exception {
        Path tempFile = Files.createTempFile("jdbc-sheets-concat", ".xlsx");
        tempFile.toFile().deleteOnExit();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet("Sheet1").createRow(0);
            row.createCell(0).setCellValue("John");
            row.createCell(1).setCellValue("Doe");
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
