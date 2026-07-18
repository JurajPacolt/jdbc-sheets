/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.exception.JdbcSheetsException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Read-only JDBC driver for XLSX workbooks.
 *
 * @author juraj.pacolt
 */
public class Driver implements java.sql.Driver {

    private static final Object REGISTRATION_LOCK = new Object();
    private static Driver instance;
    private static final String URL_PREFIX = "jdbc:sheets://";

    static {
        register();
    }

    /** Creates a driver instance. Registration is normally handled by the JDBC service provider. */
    public Driver() {
    }

    /**
     * Register driver.
     */
    public static void register() {
        synchronized (REGISTRATION_LOCK) {
            if (instance != null) {
                return;
            }
            try {
                instance = new Driver();
                DriverManager.registerDriver(instance);
            } catch (SQLException ex) {
                throw new JdbcSheetsException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Unregister driver.
     */
    public static void unregister() {
        synchronized (REGISTRATION_LOCK) {
            if (instance == null) {
                return;
            }
            try {
                DriverManager.deregisterDriver(instance);
                instance = null;
            } catch (SQLException ex) {
                throw new JdbcSheetsException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        Properties properties = new Properties();
        for (DriverPropertyInfo property : getPropertyInfo(url, info)) {
            if (property.value != null) {
                properties.setProperty(property.name, property.value);
            }
        }
        properties.setProperty("url", url);
        return new JdbcSheetsConnection(properties);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.regionMatches(true, 0, URL_PREFIX, 0, URL_PREFIX.length());
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            throw new SQLException("Unsupported JDBC URL: " + url);
        }
        try {
            Properties parsed = new Properties();
            int questionMark = url.indexOf('?', URL_PREFIX.length());
            if (questionMark >= 0 && questionMark + 1 < url.length()) {
                String query = url.substring(questionMark + 1);
                for (String parameter : query.split("&")) {
                    if (parameter.isEmpty()) {
                        continue;
                    }
                    String[] entry = parameter.split("=", 2);
                    String key = decode(entry[0]);
                    String value = entry.length == 2 ? decode(entry[1]) : "";
                    if (!key.isEmpty()) {
                        parsed.setProperty(key, value);
                    }
                }
            }

            if (info != null) {
                for (String name : info.stringPropertyNames()) {
                    parsed.setProperty(name, info.getProperty(name, ""));
                }
            }

            return new DriverPropertyInfo[]{
                    new DriverPropertyInfo(DriverInfo.PROP_DIRECTORY, parsed.getProperty(DriverInfo.PROP_DIRECTORY)),
                    new DriverPropertyInfo(DriverInfo.PROP_DATABASE, parsed.getProperty(DriverInfo.PROP_DATABASE)),
                    new DriverPropertyInfo(DriverInfo.PROP_FILE, parsed.getProperty(DriverInfo.PROP_FILE)),
                    new DriverPropertyInfo(DriverInfo.PROP_HEADER, parsed.getProperty(DriverInfo.PROP_HEADER, "false"))
            };
        } catch (IllegalArgumentException ex) {
            throw new SQLException("Invalid URL encoding in JDBC URL.", ex);
        }
    }

    @Override
    public int getMajorVersion() {
        return DriverInfo.MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return DriverInfo.MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("org.javerland.jdbcsheets");
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
