package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.Driver;
import org.javerland.jdbcsheets.DriverInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

class DriverBehaviorTest {

    @Test
    void urlPropertiesAreDecodedAndExplicitPropertiesTakePrecedence() throws Exception {
        Driver driver = new Driver();
        Properties explicit = new Properties();
        explicit.setProperty(DriverInfo.PROP_DATABASE, "override.xlsx");
        explicit.setProperty(DriverInfo.PROP_DIRECTORY, "explicit directory");

        DriverPropertyInfo[] info = driver.getPropertyInfo(
                "JDBC:SHEETS://?database=url.xlsx&directory=encoded+directory&file=a%3Db.xlsx", explicit);
        Map<String, DriverPropertyInfo> byName = Arrays.stream(info)
                .collect(Collectors.toMap(property -> property.name, Function.identity()));

        Assertions.assertEquals("override.xlsx", byName.get(DriverInfo.PROP_DATABASE).value);
        Assertions.assertEquals("explicit directory", byName.get(DriverInfo.PROP_DIRECTORY).value);
        Assertions.assertEquals("a=b.xlsx", byName.get(DriverInfo.PROP_FILE).value);
        Assertions.assertEquals("false", byName.get(DriverInfo.PROP_HEADER).value);
        Assertions.assertTrue(driver.acceptsURL("JDBC:SHEETS://"));
        Assertions.assertThrows(SQLException.class, () -> driver.getPropertyInfo("jdbc:other:", null));
        Assertions.assertThrows(SQLException.class,
                () -> driver.getPropertyInfo("jdbc:sheets://?file=%ZZ", null));
    }

    @Test
    void versionLoggerAndRegistrationLifecycleAreStable() throws Exception {
        Driver driver = new Driver();
        Assertions.assertEquals(26, driver.getMajorVersion());
        Assertions.assertEquals(3, driver.getMinorVersion());
        Assertions.assertEquals("26.3.2", DriverInfo.DRIVER_VERSION);
        Assertions.assertNotNull(driver.getParentLogger());

        try {
            Driver.unregister();
            Driver.unregister();
        } finally {
            Driver.register();
            Driver.register();
        }
    }
}
