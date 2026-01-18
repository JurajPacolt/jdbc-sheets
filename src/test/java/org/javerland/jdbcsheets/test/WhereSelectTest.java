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
import java.sql.Statement;

/**
 * @author juraj.pacolt
 */
public class WhereSelectTest {

    @Test
    public void whereSupportsBasicOperators() throws Exception {
        Class.forName(TestConstants.DRIVER_CLASS);
        Path tempFile = createTempWorkbook();
        try (Connection conn = DriverManager.getConnection(urlForFile(tempFile))) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 where A > 15")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals(20.0d, rs.getDouble(1), 0.0001d);
                    Assertions.assertFalse(rs.next());
                }
                try (ResultSet rs = stmt.executeQuery("select B from Sheet1 where B like 'Ali%'")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals("Alice", rs.getString(1));
                    Assertions.assertFalse(rs.next());
                }
                try (ResultSet rs = stmt.executeQuery("select B from Sheet1 where lower(B) = 'alice'")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals("Alice", rs.getString(1));
                    Assertions.assertFalse(rs.next());
                }
                try (ResultSet rs = stmt.executeQuery("select B from Sheet1 where upper(B) like 'ALI%'")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals("Alice", rs.getString(1));
                    Assertions.assertFalse(rs.next());
                }
                try (ResultSet rs = stmt.executeQuery("select C from Sheet1 where C = 5")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals(5.0d, rs.getDouble(1), 0.0001d);
                    Assertions.assertFalse(rs.next());
                }
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 where A < 15")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals(10.0d, rs.getDouble(1), 0.0001d);
                    Assertions.assertFalse(rs.next());
                }
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path createTempWorkbook() throws Exception {
        Path tempFile = Files.createTempFile("jdbc-sheets-where", ".xlsx");
        tempFile.toFile().deleteOnExit();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row1 = workbook.createSheet("Sheet1").createRow(0);
            row1.createCell(0).setCellValue(10.0d);
            row1.createCell(1).setCellValue("Alice");
            row1.createCell(2).setCellValue(5.0d);

            Row row2 = workbook.getSheet("Sheet1").createRow(1);
            row2.createCell(0).setCellValue(20.0d);
            row2.createCell(1).setCellValue("Bob");
            row2.createCell(2).setCellValue(15.0d);

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
