/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Juraj Pacolt
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
        return null;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return Stream.of(getPropertyInfo(url, null)).filter(i ->
                (i.name.equals(DriverInfo.PROP_DIRECTORY) && i.name.equals(DriverInfo.PROP_DATABASE))
                        || (i.name.equals(DriverInfo.PROP_FILE))).findFirst().isPresent();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        try {
            URL u = new URL(url);
            if (u.getProtocol().equalsIgnoreCase("jdbc")) {
                throw new SQLException("Unsupported jdbc protocol.");
            }

            Map<String, String> queryMap = new HashMap<>();
            if (StringUtils.isNotBlank(u.getQuery())) {
                queryMap = Arrays.asList(u.getQuery().split("&")).stream().map(i -> {
                    String[] item = i.split("=");
                    try {
                        return new AbstractMap.SimpleImmutableEntry<String, String>(
                                URLDecoder.decode(item[0], StandardCharsets.UTF_8.name()),
                                item.length > 1 ? URLDecoder.decode(item[1], StandardCharsets.UTF_8.name()) : null
                        );
                    } catch (UnsupportedEncodingException ex) {
                        throw new JdbcSheetsException(ex.getMessage(), ex);
                    }
                }).collect(
                        Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey,
                                AbstractMap.SimpleImmutableEntry::getValue));
            }

            if (info != null) {
                for (Map.Entry<Object, Object> e : info.entrySet()) {
                    queryMap.put(e.getKey().toString(), ObjectUtils.defaultIfNull(e.getValue(), "")
                            .toString());
                }
            }

            DriverPropertyInfo[] props = new DriverPropertyInfo[]{
                    new DriverPropertyInfo(DriverInfo.PROP_DIRECTORY, queryMap.get("directory")),
                    new DriverPropertyInfo(DriverInfo.PROP_DATABASE, queryMap.get("database")),
                    new DriverPropertyInfo(DriverInfo.PROP_FILE, queryMap.get("file"))
            };

            return props;
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
