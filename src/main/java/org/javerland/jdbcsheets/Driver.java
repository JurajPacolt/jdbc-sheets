/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author juraj.pacolt
 */
public class Driver implements java.sql.Driver {

    private static Driver instance;
    private static final Logger PARENT_LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final Logger LOGGER = Logger.getLogger(Driver.class.getName());

    static {
        register();
    }

    /**
     * Register driver.
     */
    public static void register() {
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

    /**
     * Unregister driver.
     */
    public static void unregister() {
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

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        Properties properties = Arrays.stream(getPropertyInfo(url, info))
                .filter(i -> i != null && i.name != null)
                .collect(Collectors.toMap(
                        i -> i.name,
                        i -> i.value,
                        (existing, replacement) -> existing,
                        Properties::new
                ));
        return new JdbcSheetsConnection(properties);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return Stream.of(getPropertyInfo(url, null)).anyMatch(i ->
                i.name.equals(DriverInfo.PROP_FILE));
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (!url.trim().toLowerCase().startsWith("jdbc:sheets://")) {
            throw new JdbcSheetsException("Illegal connection string url.");
        }
        try {
            URL u = new URL("file://localhost" + url.substring(14));

            Map<String, String> queryMap = new HashMap<>();
            if (StringUtils.isNotBlank(u.getQuery())) {
                queryMap = Arrays.stream(u.getQuery().split("&"))
                        .map(param -> param.split("="))
                        .collect(Collectors.toMap(
                                entry -> entry[0],
                                entry -> entry.length > 1 ? entry[1] : ""
                        ));
            }

            if (info != null) {
                for (Map.Entry<Object, Object> e : info.entrySet()) {
                    queryMap.put(e.getKey().toString(), ObjectUtils.defaultIfNull(e.getValue(), "")
                            .toString());
                }
            }

            return new DriverPropertyInfo[]{
                    new DriverPropertyInfo(DriverInfo.PROP_DIRECTORY, queryMap.get("directory")),
                    new DriverPropertyInfo(DriverInfo.PROP_DATABASE, queryMap.get("database")),
                    new DriverPropertyInfo(DriverInfo.PROP_FILE, queryMap.get("file"))
            };
        } catch (MalformedURLException ex) {
            throw new JdbcSheetsException(ex.getMessage(), ex);
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
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return PARENT_LOGGER;
    }
}
