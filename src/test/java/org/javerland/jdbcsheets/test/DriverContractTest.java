package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.Driver;
import org.javerland.jdbcsheets.DriverInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.ServiceLoader;

class DriverContractTest {

    @Test
    void driverUsesJdbcUrlContract() throws Exception {
        Driver driver = new Driver();

        Assertions.assertFalse(driver.acceptsURL(null));
        Assertions.assertFalse(driver.acceptsURL("jdbc:other:test"));
        Assertions.assertTrue(driver.acceptsURL("jdbc:sheets://?database=file.xlsx&directory=."));
        Assertions.assertNull(driver.connect("jdbc:other:test", new Properties()));
        Assertions.assertFalse(driver.jdbcCompliant());
    }

    @Test
    void driverIsDiscoverableThroughServiceLoader() {
        boolean found = false;
        for (java.sql.Driver driver : ServiceLoader.load(java.sql.Driver.class)) {
            if (driver instanceof Driver) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "JDBC driver must be registered through META-INF/services");
    }

    @Test
    void preparedStatementsAreAvailableAndCallableStatementsFailExplicitly() throws Exception {
        Class.forName("org.javerland.jdbcsheets.Driver");
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sheets://?file=src/test/resources/test-data.xlsx")) {
            try (java.sql.PreparedStatement prepared = connection.prepareStatement(
                    "select A from Sheet1 where A = ?")) {
                prepared.setString(1, "Melissa");
                try (java.sql.ResultSet resultSet = prepared.executeQuery()) {
                    Assertions.assertTrue(resultSet.next());
                }
            }
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.prepareCall("call anything()"));
        }
    }

    @Test
    void versionAndConnectionValidationAreConsistent() throws Exception {
        Assertions.assertEquals("26.3.2", DriverInfo.DRIVER_VERSION);
        String pom = Files.readString(Path.of("pom.xml"));
        Assertions.assertTrue(pom.contains("<version>" + DriverInfo.DRIVER_VERSION + "</version>"));

        Assertions.assertThrows(SQLException.class,
                () -> DriverManager.getConnection("jdbc:sheets://?file=missing-workbook.xlsx"));
    }

    @Test
    void urlEncodedFilePathsAreSupported() throws Exception {
        Path directory = Files.createTempDirectory("jdbc sheets ");
        Path workbookPath = directory.resolve("book & one.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                FileOutputStream output = new FileOutputStream(workbookPath.toFile())) {
            workbook.createSheet("Sheet1").createRow(0).createCell(0).setCellValue("ok");
            workbook.write(output);
        }

        String encoded = URLEncoder.encode(
                workbookPath.toAbsolutePath().toString().replace('\\', '/'), StandardCharsets.UTF_8);
        try (Connection connection = DriverManager.getConnection("jdbc:sheets://?file=" + encoded)) {
            Assertions.assertTrue(connection.isValid(0));
        } finally {
            Files.deleteIfExists(workbookPath);
            Files.deleteIfExists(directory);
        }
    }
}
