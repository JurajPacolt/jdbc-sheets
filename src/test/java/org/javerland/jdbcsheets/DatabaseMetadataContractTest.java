package org.javerland.jdbcsheets;

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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

class DatabaseMetadataContractTest {

    @Test
    void tableAndColumnPatternsTypesAndOrdinalsAreCorrect() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook))) {
            DatabaseMetaData metadata = connection.getMetaData();

            Assertions.assertEquals(List.of("Sales_2026"), tableNames(
                    metadata.getTables(null, null, "Sales\\_2026", new String[] { "TABLE" })));
            Assertions.assertEquals(List.of("Sales_2026", "SalesA2026"), tableNames(
                    metadata.getTables(null, null, "Sales%2026", null)));
            Assertions.assertTrue(tableNames(
                    metadata.getTables(null, null, "%", new String[] { "VIEW" })).isEmpty());

            try (ResultSet columns = metadata.getColumns(null, null, "Sales\\_2026", "_")) {
                Assertions.assertTrue(columns.next());
                Assertions.assertEquals("A", columns.getString("COLUMN_NAME"));
                Assertions.assertEquals(Types.VARCHAR, columns.getInt("DATA_TYPE"));
                Assertions.assertEquals(1, columns.getInt("ORDINAL_POSITION"));
                Assertions.assertTrue(columns.next());
                Assertions.assertEquals("B", columns.getString("COLUMN_NAME"));
                Assertions.assertEquals(Types.DOUBLE, columns.getInt("DATA_TYPE"));
                Assertions.assertEquals(2, columns.getInt("ORDINAL_POSITION"));
                Assertions.assertFalse(columns.next());
            }

            try (ResultSet types = metadata.getTableTypes()) {
                Assertions.assertTrue(types.next());
                Assertions.assertEquals("TABLE", types.getString(1));
                Assertions.assertFalse(types.next());
            }
            try (ResultSet primaryKeys = metadata.getPrimaryKeys(null, null, "Sales_2026")) {
                Assertions.assertEquals(6, primaryKeys.getMetaData().getColumnCount());
                Assertions.assertFalse(primaryKeys.next());
            }
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void advertisedCapabilitiesMatchTheReadOnlyForwardOnlyDriver() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook))) {
            DatabaseMetaData metadata = connection.getMetaData();
            Assertions.assertAll(
                    () -> Assertions.assertTrue(metadata.isReadOnly()),
                    () -> Assertions.assertTrue(metadata.allTablesAreSelectable()),
                    () -> Assertions.assertTrue(metadata.usesLocalFiles()),
                    () -> Assertions.assertTrue(metadata.supportsColumnAliasing()),
                    () -> Assertions.assertTrue(metadata.supportsTableCorrelationNames()),
                    () -> Assertions.assertTrue(metadata.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY)),
                    () -> Assertions.assertFalse(metadata.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)),
                    () -> Assertions.assertTrue(metadata.supportsResultSetConcurrency(
                            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)),
                    () -> Assertions.assertFalse(metadata.supportsResultSetConcurrency(
                            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)),
                    () -> Assertions.assertTrue(metadata.supportsResultSetHoldability(
                            ResultSet.HOLD_CURSORS_OVER_COMMIT)),
                    () -> Assertions.assertFalse(metadata.supportsBatchUpdates()),
                    () -> Assertions.assertFalse(metadata.supportsTransactions()),
                    () -> Assertions.assertFalse(metadata.supportsSavepoints()),
                    () -> Assertions.assertFalse(metadata.supportsGetGeneratedKeys()),
                    () -> Assertions.assertEquals(DatabaseMetaData.sqlStateSQL, metadata.getSQLStateType()),
                    () -> Assertions.assertEquals(java.sql.RowIdLifetime.ROWID_UNSUPPORTED,
                            metadata.getRowIdLifetime()),
                    () -> Assertions.assertSame(connection, metadata.getConnection()),
                    () -> Assertions.assertSame(metadata, metadata.unwrap(DatabaseMetaData.class)),
                    () -> Assertions.assertTrue(metadata.isWrapperFor(DatabaseMetaData.class)));
            Assertions.assertThrows(java.sql.SQLException.class, () -> metadata.unwrap(String.class));
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    @Test
    void emptyMetadataResultsStillExposeJdbcSpecifiedSchemas() throws Exception {
        Path workbook = createWorkbook();
        try (Connection connection = DriverManager.getConnection(url(workbook))) {
            DatabaseMetaData metadata = connection.getMetaData();
            assertEmptySchema(metadata.getSchemas(), "TABLE_SCHEM", "TABLE_CATALOG");
            assertEmptySchema(metadata.getCatalogs(), "TABLE_CAT", "TABLE_CAT");
            assertEmptySchema(metadata.getProcedures(null, null, null), "PROCEDURE_CAT", "SPECIFIC_NAME");
            assertEmptySchema(metadata.getProcedureColumns(null, null, null, null),
                    "PROCEDURE_CAT", "SPECIFIC_NAME");
            assertEmptySchema(metadata.getIndexInfo(null, null, "Sales_2026", false, false),
                    "TABLE_CAT", "FILTER_CONDITION");
            assertEmptySchema(metadata.getFunctions(null, null, null), "FUNCTION_CAT", "SPECIFIC_NAME");
            assertEmptySchema(metadata.getPseudoColumns(null, null, null, null), "TABLE_CAT", "IS_NULLABLE");

            try (ResultSet typeInfo = metadata.getTypeInfo()) {
                Assertions.assertEquals("TYPE_NAME", typeInfo.getMetaData().getColumnName(1));
                int count = 0;
                while (typeInfo.next()) {
                    count++;
                }
                Assertions.assertEquals(5, count);
            }
        } finally {
            Files.deleteIfExists(workbook);
        }
    }

    private List<String> tableNames(ResultSet resultSet) throws Exception {
        try (ResultSet tables = resultSet) {
            List<String> names = new ArrayList<>();
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME"));
            }
            return names;
        }
    }

    private void assertEmptySchema(ResultSet resultSet, String firstColumn, String lastColumn) throws Exception {
        try (ResultSet metadataResult = resultSet) {
            Assertions.assertFalse(metadataResult.next());
            Assertions.assertEquals(firstColumn, metadataResult.getMetaData().getColumnName(1));
            Assertions.assertEquals(lastColumn, metadataResult.getMetaData()
                    .getColumnName(metadataResult.getMetaData().getColumnCount()));
        }
    }

    private Path createWorkbook() throws Exception {
        Path file = Files.createTempFile("jdbc-sheets-metadata", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row first = workbook.createSheet("Sales_2026").createRow(0);
            first.createCell(0).setCellValue("name");
            first.createCell(1).setCellValue(2.0d);
            workbook.createSheet("SalesA2026").createRow(0).createCell(0).setCellValue("other");
            workbook.createSheet("Archive");
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
