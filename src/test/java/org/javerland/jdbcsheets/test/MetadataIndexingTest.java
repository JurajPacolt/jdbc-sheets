/* Created on 20.03.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.*;

/**
 * @author juraj.pacolt
 */
public class MetadataIndexingTest {

    @Test
    public void resultSetMetaDataUsesOneBasedIndex() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A as Col1, B from Sheet1 limit 1 offset 0")) {
                    ResultSetMetaData meta = rs.getMetaData();
                    Assertions.assertEquals(2, meta.getColumnCount());
                    Assertions.assertEquals("Col1", meta.getColumnLabel(1));
                    Assertions.assertEquals("A", meta.getColumnName(1));
                    Assertions.assertEquals("B", meta.getColumnLabel(2));
                    Assertions.assertEquals("B", meta.getColumnName(2));
                }
            }
        }
    }

    @Test
    public void systemMetaDataUsesOneBasedIndex() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            DatabaseMetaData dmd = conn.getMetaData();
            try (ResultSet rs = dmd.getTables(null, null, null, null)) {
                ResultSetMetaData meta = rs.getMetaData();
                Assertions.assertEquals("TABLE_CAT", meta.getColumnName(1));
                Assertions.assertEquals("TABLE_SCHEM", meta.getColumnName(2));
                Assertions.assertEquals("TABLE_NAME", meta.getColumnName(3));
            }
        }
    }
}
