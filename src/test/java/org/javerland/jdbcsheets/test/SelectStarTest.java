/* Created on 20.03.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.*;

/**
 * @author juraj.pacolt
 */
public class SelectStarTest {

    @Test
    public void selectStarReturnsAllColumns() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select * from Sheet1 limit 1 offset 0")) {
                    ResultSetMetaData meta = rs.getMetaData();
                    Assertions.assertEquals(5, meta.getColumnCount());
                    Assertions.assertEquals("A", meta.getColumnName(1));
                    Assertions.assertEquals("B", meta.getColumnName(2));
                    Assertions.assertEquals("C", meta.getColumnName(3));
                    Assertions.assertEquals("D", meta.getColumnName(4));
                    Assertions.assertEquals("E", meta.getColumnName(5));
                }
            }
        }
    }
}
