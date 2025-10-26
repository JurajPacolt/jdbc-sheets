/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.DriverInfo;
import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class DatabaseMetadataTest {

    @Test
    public void testGetDriverName() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            assertEquals(DriverInfo.DRIVER_NAME, metadata.getDriverName());
        }
    }

    @Test
    public void testGetDriverVersion() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            assertEquals(DriverInfo.DRIVER_VERSION, metadata.getDriverVersion());
        }
    }

    @Test
    public void testGetURL() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            assertEquals(TestConstants.URL_WITH_FILE, metadata.getURL());
        }
    }

    @Test
    public void testGetTables() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            try (ResultSet rs = metadata.getTables(null, null, null, null)) {
                assertTrue(rs.next());
                String tableName = rs.getString("TABLE_NAME");
                assertEquals("Sheet1", tableName);
            }
        }
    }

    @Test
    public void testGetColumns() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            try (ResultSet rs = metadata.getColumns(null, null, "Sheet1", null)) {
                int columnCount = 0;
                while (rs.next()) {
                    columnCount++;
                    String columnName = rs.getString("COLUMN_NAME");
                    assertNotNull(columnName);
                }
                assertTrue(columnCount >= 5); // A, B, C, D, E
            }
        }
    }

    @Test
    public void testGetSpecificColumn() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            try (ResultSet rs = metadata.getColumns(null, null, "Sheet1", "A")) {
                assertTrue(rs.next());
                assertEquals("A", rs.getString("COLUMN_NAME"));
            }
        }
    }

    @Test
    public void testSupportsTransactions() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            assertFalse(metadata.supportsTransactions());
        }
    }

    @Test
    public void testIsReadOnly() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            assertTrue(metadata.isReadOnly());
        }
    }

    @Test
    public void testGetConnection() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            Connection metadataConn = metadata.getConnection();
            assertNotNull(metadataConn);
            assertEquals(conn, metadataConn);
        }
    }

    @Test
    public void testMultipleTablesList() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            try (ResultSet rs = metadata.getTables(null, null, null, null)) {
                int tableCount = 0;
                while (rs.next()) {
                    tableCount++;
                    assertNotNull(rs.getString("TABLE_NAME"));
                }
                assertTrue(tableCount >= 1);
            }
        }
    }
}
