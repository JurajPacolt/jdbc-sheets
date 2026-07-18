package org.javerland.jdbcsheets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLClientInfoException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JdbcSheetsConnectionTest {

    @Test
    void propertiesAreDefensivelyCopiedAndClientInfoIsManaged() throws Exception {
        Properties source = validProperties();
        JdbcSheetsConnection connection = new JdbcSheetsConnection(source);
        try {
            source.setProperty("custom", "changed-outside");
            connection.setClientInfo("custom", "inside");

            Properties returned = connection.getClientInfo();
            returned.setProperty("custom", "changed-copy");

            Assertions.assertEquals("inside", connection.getClientInfo("custom"));
            connection.setClientInfo("custom", null);
            Assertions.assertNull(connection.getClientInfo("custom"));

            Properties additional = new Properties();
            additional.setProperty("one", "1");
            connection.setClientInfo(additional);
            Assertions.assertEquals("1", connection.getClientInfo("one"));
            Assertions.assertTrue(connection.isReadOnly());
            Assertions.assertTrue(connection.isValid(0));
            Assertions.assertSame(connection, connection.unwrap(Connection.class));
            Assertions.assertTrue(connection.isWrapperFor(Connection.class));
            Assertions.assertFalse(connection.isWrapperFor(Statement.class));
            Assertions.assertThrows(SQLException.class, () -> connection.unwrap(Statement.class));
        } finally {
            connection.close();
        }

        Assertions.assertThrows(SQLClientInfoException.class,
                () -> connection.setClientInfo("after-close", "value"));
        Assertions.assertFalse(connection.isValid(0));
    }

    @Test
    void sourceAndConnectionOptionsAreValidated() throws Exception {
        Assertions.assertThrows(SQLNonTransientConnectionException.class,
                () -> new JdbcSheetsConnection(new Properties()));

        Properties missing = new Properties();
        missing.setProperty(DriverInfo.PROP_FILE, "missing-file.xlsx");
        Assertions.assertThrows(SQLNonTransientConnectionException.class,
                () -> new JdbcSheetsConnection(missing));

        Properties unsupported = new Properties();
        unsupported.setProperty(DriverInfo.PROP_FILE, "src/test/resources/test-data.csv");
        Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                () -> new JdbcSheetsConnection(unsupported));

        Path temporaryDirectory = Files.createTempDirectory("jdbc-sheets-no-extension");
        Path extensionless = Files.createFile(temporaryDirectory.resolve("workbook"));
        try {
            Properties noExtension = new Properties();
            noExtension.setProperty(DriverInfo.PROP_FILE, extensionless.toString());
            Assertions.assertThrows(SQLException.class, () -> new JdbcSheetsConnection(noExtension));
        } finally {
            Files.deleteIfExists(extensionless);
            Files.deleteIfExists(temporaryDirectory);
        }

        try (JdbcSheetsConnection connection = new JdbcSheetsConnection(validProperties())) {
            Assertions.assertThrows(SQLException.class, () -> connection.isValid(-1));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connection.setAutoCommit(false));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connection.setReadOnly(false));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.setCatalog("catalog"));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.setSchema("schema"));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.setTypeMap(Map.of("custom", String.class)));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, connection::createBlob);
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, connection::createClob);
            Assertions.assertThrows(SQLFeatureNotSupportedException.class, connection::createSQLXML);
            Assertions.assertThrows(SQLException.class,
                    () -> connection.setNetworkTimeout(Runnable::run, -1));
            Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                    () -> connection.setNetworkTimeout(Runnable::run, 1));
            connection.setNetworkTimeout(Runnable::run, 0);
            connection.setCatalog(null);
            connection.setSchema("");
            connection.setTypeMap(Map.of());
            Assertions.assertNull(connection.getCatalog());
            Assertions.assertNull(connection.getSchema());
        }
    }

    @Test
    void closeUsesMockitoToCloseEverythingAndAggregateFailures() throws Exception {
        JdbcSheetsConnection connection = new JdbcSheetsConnection(validProperties());
        Statement first = mock(Statement.class);
        Statement second = mock(Statement.class);
        SQLException firstFailure = new SQLException("first close failed");
        SQLException secondFailure = new SQLException("second close failed");
        doThrow(firstFailure).when(first).close();
        doThrow(secondFailure).when(second).close();
        connection.statements.put("first", first);
        connection.statements.put("second", second);

        SQLException thrown = Assertions.assertThrows(SQLException.class, connection::close);

        Assertions.assertSame(firstFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[] { secondFailure }, thrown.getSuppressed());
        Assertions.assertTrue(connection.isClosed());
        Assertions.assertTrue(connection.statements.isEmpty());
        verify(first).close();
        verify(second).close();

        connection.close();
        verify(first, times(1)).close();
        verify(second, times(1)).close();
    }

    @Test
    void abortClosesConnectionAndOpenChecksAreEnforced() throws Exception {
        JdbcSheetsConnection connection = new JdbcSheetsConnection(validProperties());
        connection.abort(Runnable::run);

        Assertions.assertTrue(connection.isClosed());
        Assertions.assertThrows(SQLException.class, connection::createStatement);
        Assertions.assertThrows(SQLException.class, connection::getMetaData);
        Assertions.assertThrows(SQLException.class, () -> connection.setAutoCommit(true));
        Assertions.assertThrows(SQLException.class, connection::getAutoCommit);
        Assertions.assertThrows(SQLException.class, connection::getClientInfo);
    }

    private Properties validProperties() {
        Properties properties = new Properties();
        properties.setProperty(DriverInfo.PROP_FILE,
                Path.of("src/test/resources/test-data.xlsx").toAbsolutePath().toString());
        return properties;
    }
}
