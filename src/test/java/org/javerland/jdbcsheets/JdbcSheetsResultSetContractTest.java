package org.javerland.jdbcsheets;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;

class JdbcSheetsResultSetContractTest {

    @Test
    void conversionsStreamsLabelsAndCursorStateAreCovered() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook));
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select A,B,C,D,E,F from Sheet1")) {
            Assertions.assertTrue(resultSet.isBeforeFirst());
            Assertions.assertEquals(0, resultSet.getRow());
            Assertions.assertThrows(SQLException.class, () -> resultSet.getObject(1));
            Assertions.assertTrue(resultSet.next());
            Assertions.assertTrue(resultSet.isFirst());
            Assertions.assertEquals(1, resultSet.getRow());

            Assertions.assertEquals(12, resultSet.getByte(1));
            Assertions.assertEquals(12, resultSet.getShort("A"));
            Assertions.assertEquals(12, resultSet.getInt(1));
            Assertions.assertEquals(12L, resultSet.getLong(1));
            Assertions.assertEquals(12.75f, resultSet.getFloat(1), 0.001f);
            Assertions.assertEquals(12.75d, resultSet.getDouble(1), 0.001d);
            Assertions.assertEquals(new BigDecimal("12.75"), resultSet.getBigDecimal(1));
            Assertions.assertEquals(new BigDecimal("12.8"), resultSet.getBigDecimal(1, 1));
            Assertions.assertTrue(resultSet.getBoolean("B"));
            Assertions.assertEquals(42, resultSet.getObject("D", Integer.class));
            Assertions.assertEquals("42", resultSet.getObject(4, String.class));
            Assertions.assertEquals(42L, resultSet.getObject(4, Long.class));
            Assertions.assertEquals(42.0d, resultSet.getObject(4, Double.class));

            Timestamp timestamp = resultSet.getTimestamp("C");
            Assertions.assertEquals(LocalDate.of(2026, 3, 1), timestamp.toLocalDateTime().toLocalDate());
            Assertions.assertEquals(Date.class, resultSet.getDate(3).getClass());
            Assertions.assertEquals(Time.class, resultSet.getTime(3).getClass());
            Assertions.assertEquals(Timestamp.class, resultSet.getObject(3, Timestamp.class).getClass());

            try (InputStream ascii = resultSet.getAsciiStream("E");
                    InputStream binary = resultSet.getBinaryStream(5);
                    Reader characters = resultSet.getCharacterStream("E")) {
                byte[] expected = "https://example.test/path".getBytes(StandardCharsets.UTF_8);
                Assertions.assertArrayEquals(expected, binary.readAllBytes());
                Assertions.assertEquals("https://example.test/path",
                        new String(ascii.readAllBytes(), StandardCharsets.US_ASCII));
                char[] chars = new char[expected.length];
                int count = characters.read(chars);
                Assertions.assertEquals("https://example.test/path", new String(chars, 0, count));
            }
            Assertions.assertEquals("https://example.test/path", resultSet.getURL(5).toString());
            Assertions.assertNull(resultSet.getString("F"));
            Assertions.assertTrue(resultSet.wasNull());

            Assertions.assertEquals(1, resultSet.findColumn("a"));
            Assertions.assertThrows(SQLException.class, () -> resultSet.findColumn("missing"));
            Assertions.assertThrows(SQLException.class, () -> resultSet.getObject(0));
            Assertions.assertThrows(SQLException.class, () -> resultSet.getObject(7));
            Assertions.assertThrows(SQLException.class, () -> resultSet.getObject(1, (Class<?>) null));
            Assertions.assertThrows(SQLException.class, () -> resultSet.getObject(5, Integer.class));
            Assertions.assertThrows(SQLException.class, () -> resultSet.getDate(5));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, resultSet::previous);
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, resultSet::first);
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> resultSet.setFetchDirection(ResultSet.FETCH_REVERSE));
            Assertions.assertSame(statement, resultSet.getStatement());
            Assertions.assertSame(resultSet, resultSet.unwrap(ResultSet.class));
            Assertions.assertTrue(resultSet.isWrapperFor(ResultSet.class));
            SystemResultSetTest.assertEveryMutationIsReadOnly(resultSet);

            Assertions.assertTrue(resultSet.next());
            Assertions.assertFalse(resultSet.next());
            Assertions.assertTrue(resultSet.isAfterLast());
            Assertions.assertEquals(0, resultSet.getRow());
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void maxRowsAndClosedStateAreEnforced() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook));
                Statement statement = connection.createStatement()) {
            statement.setMaxRows(1);
            ResultSet resultSet = statement.executeQuery("select A from Sheet1");
            Assertions.assertTrue(resultSet.next());
            Assertions.assertFalse(resultSet.next());
            resultSet.close();
            Assertions.assertTrue(resultSet.isClosed());
            Assertions.assertThrows(SQLException.class, resultSet::next);
            Assertions.assertThrows(SQLException.class, resultSet::wasNull);
            resultSet.close();
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    private Path createWorkbook() throws Exception {
        Path file = Files.createTempFile("jdbc-sheets-result-set", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet("Sheet1").createRow(0);
            row.createCell(0).setCellValue(12.75d);
            row.createCell(1).setCellValue(true);
            Cell date = row.createCell(2);
            date.setCellValue(Date.valueOf(LocalDate.of(2026, 3, 1)));
            CellStyle style = workbook.createCellStyle();
            style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            date.setCellStyle(style);
            row.createCell(3).setCellValue("42");
            row.createCell(4).setCellValue("https://example.test/path");
            row.createCell(5).setBlank();

            Row second = workbook.getSheet("Sheet1").createRow(1);
            second.createCell(0).setCellValue(99d);
            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                workbook.write(output);
            }
        }
        return file;
    }

    private String url(Path workbook) {
        return "jdbc:sheets://?file=" + workbook.toAbsolutePath().toString().replace('\\', '/');
    }
}
