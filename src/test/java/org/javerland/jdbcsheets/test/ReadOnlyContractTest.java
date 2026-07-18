package org.javerland.jdbcsheets.test;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;

class ReadOnlyContractTest {

    @Test
    void nullsBlankRowsColumnWidthAndLifecycleFollowJdbcContract() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(urlForFile(workbook));
                Statement statement = connection.createStatement()) {
            ResultSet first = statement.executeQuery("select * from Sheet1");
            ResultSetMetaData metadata = first.getMetaData();
            Assertions.assertEquals(3, metadata.getColumnCount());
            Assertions.assertEquals(Types.DOUBLE, metadata.getColumnType(1));
            Assertions.assertEquals(Types.VARCHAR, metadata.getColumnType(2));

            Assertions.assertTrue(first.next());
            Assertions.assertEquals(0, first.getInt(1));
            Assertions.assertTrue(first.wasNull());
            Assertions.assertEquals("first", first.getString(2));
            Assertions.assertFalse(first.wasNull());
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> first.updateString(2, "changed"));

            Assertions.assertTrue(first.next(), "A missing physical row must not terminate the result set");
            Assertions.assertEquals(5.0d, first.getDouble(1));
            Assertions.assertEquals("later-column", first.getString(3));
            Assertions.assertFalse(first.next());

            ResultSet second = statement.executeQuery("select A from Sheet1");
            Assertions.assertTrue(first.isClosed(), "Executing a new query must close the previous result set");
            statement.close();
            Assertions.assertTrue(second.isClosed());
            Assertions.assertThrows(java.sql.SQLException.class, second::next);
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void metadataNeverRewritesTheWorkbook() throws Exception {
        Path workbook = createWorkbook();
        byte[] before = Files.readAllBytes(workbook);
        try (Connection connection = DriverManager.getConnection(urlForFile(workbook))) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(null, null, "Sheet%", new String[] { "TABLE" })) {
                Assertions.assertTrue(tables.next());
            }
            try (ResultSet columns = metadata.getColumns(null, null, "Sheet%", "_")) {
                int count = 0;
                while (columns.next()) {
                    count++;
                }
                Assertions.assertEquals(3, count);
            }
        }
        Assertions.assertArrayEquals(before, Files.readAllBytes(workbook));
        Files.deleteIfExists(workbook);
    }

    private Path createWorkbook() throws Exception {
        Path file = Files.createTempFile("jdbc-sheets-contract", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row first = workbook.createSheet("Sheet1").createRow(0);
            first.createCell(0, CellType.BLANK);
            first.createCell(1).setCellValue("first");

            Row third = workbook.getSheet("Sheet1").createRow(2);
            third.createCell(0).setCellValue(5.0d);
            third.createCell(1).setCellValue("third");
            third.createCell(2).setCellValue("later-column");
            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                workbook.write(out);
            }
        }
        return file;
    }

    private String urlForFile(Path file) {
        return "jdbc:sheets://?file=" + file.toAbsolutePath().toString().replace('\\', '/');
    }
}
