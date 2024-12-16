/* Created on 16.12.2024 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.DriverInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author juraj.pacolt
 */
public class BasicDriverTest {

    public final static String FILE_NAME = "test-data.xlsx";
    public final static String DIRECTORY = ".";
    public final static String FULL_FILE_PATH = String.format("%s/%s", DIRECTORY, FILE_NAME);
    public final static String URL_WITH_PARAMS = String.format("jdbc:sheets://?database=%s&directory=%s", FILE_NAME, DIRECTORY);
    public final static String URL_WITH_FILE = String.format("jdbc:sheets://?file=%s", FULL_FILE_PATH);
    public final static String DRIVER_CLASS = "org.javerland.jdbcsheets.Driver";

    @Test
    public void basicParametersTest() throws ClassNotFoundException, SQLException {
        Class.forName("org.javerland.jdbcsheets.Driver");
        try (Connection conn = DriverManager.getConnection(URL_WITH_PARAMS)) {
            String database = conn.getClientInfo("database");
            String directory = conn.getClientInfo("directory");
            Assertions.assertTrue(database.equals(FILE_NAME) && directory.equals("."));
        }
    }

    @Test
    public void basicFilePathTest() throws ClassNotFoundException, SQLException {
        Class.forName(DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(URL_WITH_FILE)) {
            String file = conn.getClientInfo("file");
            Assertions.assertTrue(file.equals(FULL_FILE_PATH));
        }
    }

    @Test
    public void databaseMetadataTest() throws ClassNotFoundException, SQLException {
        Class.forName(DRIVER_CLASS);
        try (Connection conn = DriverManager.getConnection(URL_WITH_FILE)) {
            DatabaseMetaData dmd = conn.getMetaData();
            boolean driverName = dmd.getDriverName().equals(DriverInfo.DRIVER_NAME);
            boolean driverVersion = dmd.getDriverVersion().equals(DriverInfo.DRIVER_VERSION);
            boolean url = dmd.getURL().equals(URL_WITH_FILE);
            Assertions.assertTrue(driverName && driverVersion && url);
        }
    }
}
