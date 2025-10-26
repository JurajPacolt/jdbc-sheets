/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class ResultSetOperationsTest {

    @Test
    public void testGetStringByIndex() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A, B from Sheet1 limit 1")) {
                    assertTrue(rs.next());
                    assertNotNull(rs.getString(1));
                    assertNotNull(rs.getString(2));
                }
            }
        }
    }

    @Test
    public void testGetStringByColumnName() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A as Col1, B from Sheet1 limit 1")) {
                    assertTrue(rs.next());
                    assertNotNull(rs.getString("Col1"));
                    assertNotNull(rs.getString("B"));
                }
            }
        }
    }

    @Test
    public void testFindColumn() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A as FirstName, B as LastName from Sheet1 limit 1")) {
                    assertEquals(1, rs.findColumn("FirstName"));
                    assertEquals(2, rs.findColumn("LastName"));
                }
            }
        }
    }

    @Test
    public void testGetObject() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 1")) {
                    assertTrue(rs.next());
                    Object obj = rs.getObject(1);
                    assertNotNull(obj);
                    assertTrue(obj instanceof String);
                }
            }
        }
    }

    @Test
    public void testGetBoolean() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 1")) {
                    assertTrue(rs.next());
                    assertNotNull(rs.getBoolean(1));
                }
            }
        }
    }

    @Test
    public void testResultSetIteration() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 5")) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        assertNotNull(rs.getString(1));
                    }
                    assertEquals(5, count);
                }
            }
        }
    }

    @Test
    public void testGetRow() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 3")) {
                    int expectedRow = 0;
                    while (rs.next()) {
                        expectedRow++;
                        assertEquals(expectedRow, rs.getRow());
                    }
                }
            }
        }
    }

    @Test
    public void testGetMetadata() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A, B from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    assertNotNull(metadata);
                    assertEquals(2, metadata.getColumnCount());
                }
            }
        }
    }

    @Test
    public void testGetStatement() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 1")) {
                    Statement resultStmt = rs.getStatement();
                    assertNotNull(resultStmt);
                    assertEquals(stmt, resultStmt);
                }
            }
        }
    }

    @Test
    public void testEmptyResultSet() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 where A = 'NonExistentValue'")) {
                    assertFalse(rs.next());
                }
            }
        }
    }
}
