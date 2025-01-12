/* Created on 16.12.2024 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.DriverInfo;
import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.*;

/**
 * @author juraj.pacolt
 */
public class BasicDriverTest {

    @Test
    public void basicParametersTest() throws ClassNotFoundException, SQLException {
        Class.forName("org.javerland.jdbcsheets.Driver");
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_PARAMS)) {
            String database = conn.getClientInfo(DriverInfo.PROP_DATABASE);
            String directory = conn.getClientInfo(DriverInfo.PROP_DIRECTORY);
            Assertions.assertTrue(database.equals(TestConstants.FILE_NAME) && directory.equals("."));
        }
    }

    @Test
    public void basicFilePathTest() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            String file = conn.getClientInfo(DriverInfo.PROP_FILE);
            Assertions.assertTrue(file.equals(TestConstants.FULL_FILE_PATH));
        }
    }

    @Test
    public void databaseMetadataTest() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData dmd = conn.getMetaData();
            boolean driverName = dmd.getDriverName().equals(DriverInfo.DRIVER_NAME);
            boolean driverVersion = dmd.getDriverVersion().equals(DriverInfo.DRIVER_VERSION);
            boolean url = dmd.getURL().equals(TestConstants.URL_WITH_FILE);

            String expected = "Sheet1,";
            StringBuilder found = new StringBuilder();
            try (ResultSet rs = dmd.getTables(null, null, null, null)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    found.append(tableName).append(",");
                }
            }

            String expectedColumns = "A,B,C,D,E,";
            StringBuilder foundColumns = new StringBuilder();
            try (ResultSet rs = dmd.getColumns(null, null, "Sheet1", null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    foundColumns.append(columnName).append(",");
                }
            }

            Assertions.assertTrue(driverName && driverVersion && url && expected.contentEquals(
                    found) && expectedColumns.contentEquals(foundColumns));
        }
    }
}
