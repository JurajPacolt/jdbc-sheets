/* Created on 16.12.2024 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Test;

import java.sql.*;

/**
 * @author juraj.pacolt
 */
public class XlsxQueringTest {

    @Test
    public void simpleQuery() throws ClassNotFoundException, SQLException {
        Class.forName(TestConstants.DRIVER_CLASS);
        final String expected = "Melissa,Perry,Valerie,Grant,Steven,Stone,Jessica,Rosales,Tyler,Sandoval,";
        try (Connection conn = DriverManager.getConnection(TestConstants.URL_WITH_FILE)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select A as One1,B,C,D,E from Sheet1 limit 5 offset 0")) {
                    StringBuilder sb = new StringBuilder();
                    while (rs.next()) {
                        String name = rs.getString("One1");
                        String surname = rs.getString(2);
                        sb.append(String.format("%s,%s,", name, surname));
                    }
                    assert sb.toString().equals(expected);
                }
            }
        }
    }

}
