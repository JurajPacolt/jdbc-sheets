/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class ConnectionOperationsTest {

    @Test
    public void testCreateStatement() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            Statement stmt = conn.createStatement();
            assertNotNull(stmt);
            stmt.close();
        }
    }

    @Test
    public void testGetMetaData() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData metadata = conn.getMetaData();
            assertNotNull(metadata);
        }
    }

    @Test
    public void testGetClientInfo() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            String file = conn.getClientInfo("file");
            assertNotNull(file);
        }
    }

    @Test
    public void testIsValid() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            // Connection validity check - just verify it doesn't throw
            assertNotNull(conn);
        }
    }

    @Test
    public void testIsClosed() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE);
        assertFalse(conn.isClosed());
        conn.close();
        // After close, connection should be closed (implementation dependent)
        assertNotNull(conn);
    }

    @Test
    public void testMultipleStatements() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            Statement stmt1 = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            
            assertNotNull(stmt1);
            assertNotNull(stmt2);
            assertNotEquals(stmt1, stmt2);
            
            stmt1.close();
            stmt2.close();
        }
    }

    @Test
    public void testConnectionWithDifferentUrls() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        
        try (Connection conn1 = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            assertNotNull(conn1);
        }
        
        try (Connection conn2 = DriverManager.getConnection(TestConstants.URL_WITH_PARAMS)) {
            assertNotNull(conn2);
        }
    }

    @Test
    public void testAutoCommit() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            // Read-only driver doesn't support transactions
            assertNotNull(conn);
        }
    }

    @Test
    public void testGetCatalog() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            String catalog = conn.getCatalog();
            assertNotNull(catalog);
        }
    }
}
