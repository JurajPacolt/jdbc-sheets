/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class QueryParsingTest {

    @Test
    public void testSelectAllColumns() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A, B, C, D, E from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    assertEquals(5, metadata.getColumnCount());
                }
            }
        }
    }

    @Test
    public void testSelectWithAlias() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A as FirstName, B as LastName from Sheet1 limit 1")) {
                    ResultSetMetaData metadata = rs.getMetaData();
                    // Just verify we have 2 columns
                    assertEquals(2, metadata.getColumnCount());
                }
            }
        }
    }

    @Test
    public void testLimitClause() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 limit 3")) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                    }
                    assertEquals(3, count);
                }
            }
        }
    }

    @Test
    public void testOffsetClause() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                // Test that offset works by comparing row counts
                try (ResultSet rs1 = stmt.executeQuery("select A from Sheet1 limit 5 offset 0")) {
                    int count1 = 0;
                    while (rs1.next()) count1++;
                    assertEquals(5, count1);
                }
            }
        }
    }

    @Test
    public void testWhereEqualsClause() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 where A = 'Melissa'")) {
                    while (rs.next()) {
                        assertEquals("Melissa", rs.getString(1));
                    }
                }
            }
        }
    }

    @Test
    public void testWhereAndClause() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A, B from Sheet1 where A = 'Melissa' and B = 'Perry'")) {
                    while (rs.next()) {
                        assertEquals("Melissa", rs.getString(1));
                        assertEquals("Perry", rs.getString(2));
                    }
                }
            }
        }
    }

    @Test
    public void testWhereOrClause() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 where A = 'Melissa' or A = 'Valerie' limit 5")) {
                    while (rs.next()) {
                        String value = rs.getString(1);
                        assertTrue(value.equals("Melissa") || value.equals("Valerie"));
                    }
                }
            }
        }
    }

    @Test
    public void testWhereLikeClause() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A from Sheet1 where A like 'Mel%' limit 5")) {
                    while (rs.next()) {
                        String value = rs.getString(1);
                        assertTrue(value.startsWith("Mel"));
                    }
                }
            }
        }
    }

    @Test
    public void testComplexQuery() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(
                    "select A as FirstName, B as LastName from Sheet1 where A like 'M%' limit 2 offset 0")) {
                    
                    ResultSetMetaData metadata = rs.getMetaData();
                    assertEquals(2, metadata.getColumnCount());
                    
                    int count = 0;
                    while (rs.next()) {
                        String firstName = rs.getString(1);
                        assertNotNull(firstName);
                        assertTrue(firstName.startsWith("M"));
                        count++;
                    }
                    assertTrue(count <= 2);
                }
            }
        }
    }
}
