package org.javerland.jdbcsheets.test;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

class WorkbookBoundaryTest {

    @Test
    void sparseWideSheetsAndMixedColumnTypesAreHandled() throws Exception {
        Path workbook = Files.createTempFile("jdbc-sheets-wide", ".xlsx");
        try {
            try (XSSFWorkbook source = new XSSFWorkbook()) {
                org.apache.poi.ss.usermodel.Sheet sheet = source.createSheet("Data");
                Row first = sheet.createRow(0);
                first.createCell(0).setCellValue(1.0d);
                first.createCell(26).setCellValue("last");
                Row third = sheet.createRow(2);
                third.createCell(0).setCellValue("mixed");
                third.createCell(26).setCellValue("other");
                try (FileOutputStream output = new FileOutputStream(workbook.toFile())) {
                    source.write(output);
                }
            }

            try (Connection connection = DriverManager.getConnection(url(workbook));
                    Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("select AA from Data order by AA")) {
                List<String> values = new ArrayList<>();
                while (rows.next()) {
                    values.add(rows.getString(1));
                }
                Assertions.assertEquals(List.of("last", "other"), values);

                try (ResultSet columns = connection.getMetaData().getColumns(null, null, "Data", "%")) {
                    int columnCount = 0;
                    while (columns.next()) {
                        columnCount++;
                        if ("A".equals(columns.getString("COLUMN_NAME"))) {
                            Assertions.assertEquals(Types.JAVA_OBJECT, columns.getInt("DATA_TYPE"));
                        }
                        if ("AA".equals(columns.getString("COLUMN_NAME"))) {
                            Assertions.assertEquals(Types.VARCHAR, columns.getInt("DATA_TYPE"));
                        }
                    }
                    Assertions.assertEquals(27, columnCount);
                }
            }
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void emptyAndCorruptWorkbooksFailPredictably() throws Exception {
        Path empty = Files.createTempFile("jdbc-sheets-empty", ".xlsx");
        Path corrupt = Files.createTempFile("jdbc-sheets-corrupt", ".xlsx");
        try {
            try (XSSFWorkbook source = new XSSFWorkbook()) {
                source.createSheet("Empty");
                try (FileOutputStream output = new FileOutputStream(empty.toFile())) {
                    source.write(output);
                }
            }
            Files.write(corrupt, "not an XLSX archive".getBytes(StandardCharsets.UTF_8));

            try (Connection connection = DriverManager.getConnection(url(empty));
                    Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("select * from Empty")) {
                Assertions.assertEquals(0, rows.getMetaData().getColumnCount());
                Assertions.assertFalse(rows.next());
            }

            try (Connection connection = DriverManager.getConnection(url(corrupt));
                    Statement statement = connection.createStatement()) {
                Assertions.assertThrows(SQLException.class, () -> statement.executeQuery("select * from Sheet1"));
            }
        } finally {
            Files.deleteIfExists(empty);
            Files.deleteIfExists(corrupt);
        }
    }

    @Test
    void mediumWorkbookQueryRemainsWithinAReasonableRegressionBudget() throws Exception {
        Path workbook = createMediumWorkbook();
        try {
            Assertions.assertTimeout(Duration.ofSeconds(15), () -> {
                try (Connection connection = DriverManager.getConnection(url(workbook));
                        Statement statement = connection.createStatement();
                        ResultSet rows = statement.executeQuery("select A,J from Data where J >= 2490 order by J")) {
                    int count = 0;
                    while (rows.next()) {
                        count++;
                    }
                    Assertions.assertEquals(10, count);
                }
            });
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    private Path createMediumWorkbook() throws Exception {
        Path file = Files.createTempFile("jdbc-sheets-medium", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Data");
            for (int rowIndex = 0; rowIndex < 2500; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                for (int columnIndex = 0; columnIndex < 10; columnIndex++) {
                    row.createCell(columnIndex).setCellValue(rowIndex + columnIndex / 10.0d);
                }
            }
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
