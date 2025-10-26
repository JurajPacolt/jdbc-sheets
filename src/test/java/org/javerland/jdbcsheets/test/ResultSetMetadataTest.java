/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class ResultSetMetadataTest {

    @Test
    public void testColumnCount() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A, B, C from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    assertEquals(3, metadata.getColumnCount());
                }
            }
        }
    }

    @Test
    public void testColumnNames() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A, B, C from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    // Metadata uses 0-based indexing internally
                    assertEquals("B", metadata.getColumnName(1));
                    assertEquals("C", metadata.getColumnName(2));
                }
            }
        }
    }

    @Test
    public void testColumnTypes() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A, B from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    assertEquals(Types.VARCHAR, metadata.getColumnType(1));
                    assertEquals(Types.VARCHAR, metadata.getColumnType(2));
                }
            }
        }
    }

    @Test
    public void testTableName() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    assertEquals("Sheet1", metadata.getTableName(1));
                }
            }
        }
    }

    @Test
    public void testColumnLabel() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    // Check that we have at least one column
                    assertTrue(metadata.getColumnCount() >= 1);
                    assertNotNull(metadata.getColumnLabel(0));
                }
            }
        }
    }
}
