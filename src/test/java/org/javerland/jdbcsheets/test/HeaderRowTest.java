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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

class HeaderRowTest {

    @Test
    void headerNamesAreQueryableAndHeaderIsExcludedFromDataAndInference() throws Exception {
        Path workbook = createWorkbook(false);
        try (Connection connection = DriverManager.getConnection(url(workbook, true));
                Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery(
                    "select \"Full Name\", Age from People where Age >= 18 order by Age desc")) {
                Assertions.assertEquals("Full Name", rows.getMetaData().getColumnName(1));
                Assertions.assertTrue(rows.next());
                Assertions.assertEquals("Ada", rows.getString("Full Name"));
                Assertions.assertEquals(37.0d, rows.getDouble("Age"));
                Assertions.assertTrue(rows.next());
                Assertions.assertEquals("Bob", rows.getString(1));
                Assertions.assertFalse(rows.next());
            }

            try (ResultSet allRows = statement.executeQuery("select * from People")) {
                int rowCount = 0;
                while (allRows.next()) {
                    rowCount++;
                }
                Assertions.assertEquals(3, rowCount);
            }

            List<String> names = new ArrayList<>();
            List<Integer> types = new ArrayList<>();
            try (ResultSet columns = connection.getMetaData().getColumns(null, null, "People", "%")) {
                while (columns.next()) {
                    names.add(columns.getString("COLUMN_NAME"));
                    types.add(columns.getInt("DATA_TYPE"));
                }
            }
            Assertions.assertEquals(List.of("Full Name", "Age", "Active"), names);
            Assertions.assertEquals(List.of(Types.VARCHAR, Types.DOUBLE, Types.BOOLEAN), types);

            try (PreparedStatement prepared = connection.prepareStatement(
                    "select \"Full Name\" from People where Age >= ? order by Age")) {
                prepared.setInt(1, 18);
                try (ResultSet rows = prepared.executeQuery()) {
                    Assertions.assertTrue(rows.next());
                    Assertions.assertEquals("Bob", rows.getString(1));
                    Assertions.assertTrue(rows.next());
                    Assertions.assertEquals("Ada", rows.getString(1));
                    Assertions.assertFalse(rows.next());
                }
            }
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void invalidHeaderSettingAndDuplicateNamesAreRejected() throws Exception {
        Path regular = createWorkbook(false);
        Path duplicate = createWorkbook(true);
        try {
            Assertions.assertThrows(SQLException.class,
                    () -> DriverManager.getConnection(url(regular, false) + "&header=maybe"));

            try (Connection connection = DriverManager.getConnection(url(duplicate, true));
                    Statement statement = connection.createStatement()) {
                Assertions.assertThrows(SQLException.class, () -> statement.executeQuery("select * from People"));
            }
        } finally {
            Files.deleteIfExists(regular);
            Files.deleteIfExists(duplicate);
        }
    }

    private Path createWorkbook(boolean duplicateHeader) throws Exception {
        Path file = Files.createTempFile("jdbc-sheets-header", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("People");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Full Name");
            header.createCell(1).setCellValue(duplicateHeader ? "Full Name" : "Age");
            header.createCell(2).setCellValue("Active");
            addPerson(sheet.createRow(1), "Ada", 37, true);
            addPerson(sheet.createRow(2), "Bob", 18, false);
            addPerson(sheet.createRow(3), "Cara", 16, true);
            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                workbook.write(output);
            }
        }
        return file;
    }

    private void addPerson(Row row, String name, double age, boolean active) {
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(age);
        row.createCell(2).setCellValue(active);
    }

    private String url(Path workbook, boolean header) {
        return "jdbc:sheets://?file=" + workbook.toAbsolutePath().toString().replace('\\', '/')
                + "&header=" + header;
    }
}
