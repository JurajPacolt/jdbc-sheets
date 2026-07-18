package org.javerland.jdbcsheets;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

class PreparedStatementContractTest {

    @Test
    void scalarParametersAreBoundWithoutChangingQuotedQuestionMarks() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook));
                PreparedStatement prepared = connection.prepareStatement(
                        "select A,B from Sheet1 where A >= ? and B <> ? order by A desc")) {
            prepared.setInt(1, 15);
            prepared.setString(2, "skip");
            Assertions.assertEquals(List.of(30.0d, 20.0d), numbers(prepared.executeQuery()));
            Assertions.assertNotNull(prepared.getMetaData());

            prepared.clearParameters();
            Assertions.assertThrows(SQLException.class, prepared::executeQuery);
            Assertions.assertThrows(SQLException.class, () -> prepared.setInt(0, 1));
            Assertions.assertThrows(SQLException.class, () -> prepared.setInt(3, 1));
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void nullBooleanEscapingAndParameterMetadataFollowContract() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook));
                PreparedStatement quotedQuestion = connection.prepareStatement(
                        "select A from Sheet1 where B = '?' and A > ?")) {
            ParameterMetaData metadata = quotedQuestion.getParameterMetaData();
            Assertions.assertEquals(1, metadata.getParameterCount());
            Assertions.assertEquals(Types.JAVA_OBJECT, metadata.getParameterType(1));
            Assertions.assertEquals(ParameterMetaData.parameterModeIn, metadata.getParameterMode(1));
            Assertions.assertThrows(SQLException.class, () -> metadata.getParameterType(0));
            Assertions.assertSame(metadata, metadata.unwrap(ParameterMetaData.class));

            quotedQuestion.setObject(1, 15);
            Assertions.assertEquals(List.of(20.0d), numbers(quotedQuestion.executeQuery()));

            try (PreparedStatement nullCheck = connection.prepareStatement(
                    "select B from Sheet1 where C is ? and D = ?")) {
                nullCheck.setNull(1, Types.VARCHAR);
                nullCheck.setBoolean(2, true);
                try (ResultSet resultSet = nullCheck.executeQuery()) {
                    Assertions.assertTrue(resultSet.next());
                    Assertions.assertEquals("O'Brien", resultSet.getString(1));
                    Assertions.assertFalse(resultSet.next());
                }
            }
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void writeBatchStreamAndSqlArgumentOperationsRemainUnsupported() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook));
                PreparedStatement prepared = connection.prepareStatement("select A from Sheet1 where A = ?")) {
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, prepared::executeUpdate);
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, prepared::executeLargeUpdate);
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, prepared::addBatch);
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> prepared.setBinaryStream(1, null));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> prepared.setObject(1, new Object()));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> prepared.executeQuery("select A from Sheet1"));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.prepareStatement("select A from Sheet1", Statement.RETURN_GENERATED_KEYS));
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    private List<Double> numbers(ResultSet resultSet) throws Exception {
        try (ResultSet rows = resultSet) {
            List<Double> values = new ArrayList<>();
            while (rows.next()) {
                values.add(rows.getDouble(1));
            }
            return values;
        }
    }

    private Path createWorkbook() throws Exception {
        Path file = Files.createTempFile("jdbc-sheets-prepared", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            addRow(workbook.createSheet("Sheet1").createRow(0), 10, "skip", false, false);
            addRow(workbook.getSheet("Sheet1").createRow(1), 20, "?", false, false);
            addRow(workbook.getSheet("Sheet1").createRow(2), 30, "O'Brien", true, true);
            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                workbook.write(output);
            }
        }
        return file;
    }

    private void addRow(Row row, double number, String text, boolean leaveNull, boolean flag) {
        row.createCell(0).setCellValue(number);
        row.createCell(1).setCellValue(text);
        if (!leaveNull) {
            row.createCell(2).setCellValue("value");
        }
        row.createCell(3).setCellValue(flag);
    }

    private String url(Path workbook) {
        return "jdbc:sheets://?file=" + workbook.toAbsolutePath().toString().replace('\\', '/');
    }
}
