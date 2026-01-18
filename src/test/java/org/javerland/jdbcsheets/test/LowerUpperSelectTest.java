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
public class LowerUpperSelectTest {

    @Test
    public void selectLowerUpperFunctions() throws Exception {
        Class.forName(TestConstants.DRIVER_CLASS);
        Path tempFile = createTempWorkbook();
        try (Connection conn = DriverManager.getConnection(urlForFile(tempFile))) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(
                        "select lower(A) as LowerName, upper(B) as UpperName from Sheet1 limit 1 offset 0")) {
                    Assertions.assertTrue(rs.next());
                    Assertions.assertEquals("john", rs.getString("LowerName"));
                    Assertions.assertEquals("DOE", rs.getString("UpperName"));
                }
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path createTempWorkbook() throws Exception {
        Path tempFile = Files.createTempFile("jdbc-sheets-lower-upper", ".xlsx");
        tempFile.toFile().deleteOnExit();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet("Sheet1").createRow(0);
            row.createCell(0).setCellValue("John");
            row.createCell(1).setCellValue("doe");
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
