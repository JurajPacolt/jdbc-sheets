package org.javerland.jdbcsheets.test;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

class AdvancedSqlTest {

    @Test
    void comparisonsLogicalExpressionsNullChecksAndOrderingWorkTogether() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook));
                Statement statement = connection.createStatement()) {
            Assertions.assertEquals(List.of("gamma", "beta", "beta"), strings(statement,
                    "select B from Sheet1 where (A >= 20 and B = 'beta') or A is null order by A desc"));
            Assertions.assertEquals(List.of(20.0d), numbers(statement,
                    "select A from Sheet1 where A <= 20 and A != 10 order by A"));
            Assertions.assertEquals(List.of("alpha"), strings(statement,
                    "select B from Sheet1 where A is not null and (B = 'alpha' or B = 'gamma')"));
            Assertions.assertEquals(List.of(10.0d, 30.0d), numbers(statement,
                    "select A from Sheet1 where A <> 20 order by A asc"));
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    private List<String> strings(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            List<String> values = new ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
            return values;
        }
    }

    private List<Double> numbers(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            List<Double> values = new ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getDouble(1));
            }
            return values;
        }
    }

    private Path createWorkbook() throws Exception {
        Path file = Files.createTempFile("jdbc-sheets-advanced-sql", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            addRow(workbook.createSheet("Sheet1").createRow(0), 10, "alpha");
            addRow(workbook.getSheet("Sheet1").createRow(1), 20, "beta");
            addRow(workbook.getSheet("Sheet1").createRow(2), 30, "beta");
            workbook.getSheet("Sheet1").createRow(3).createCell(1).setCellValue("gamma");
            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                workbook.write(output);
            }
        }
        return file;
    }

    private void addRow(Row row, double number, String text) {
        row.createCell(0).setCellValue(number);
        row.createCell(1).setCellValue(text);
    }

    private String url(Path workbook) {
        return "jdbc:sheets://?file=" + workbook.toAbsolutePath().toString().replace('\\', '/');
    }
}
