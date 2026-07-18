package org.javerland.jdbcsheets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JdbcSheetsStatementTest {

    @Test
    void configurationAndReadOnlyContractAreReportedCorrectly() throws Exception {
        JdbcSheetsConnection connection = mock(JdbcSheetsConnection.class);
        JdbcSheetsStatement statement = new JdbcSheetsStatement(connection, workbook());

        statement.setMaxRows(12);
        statement.setFetchSize(7);
        statement.setQueryTimeout(3);
        statement.setFetchDirection(ResultSet.FETCH_FORWARD);
        statement.setMaxFieldSize(0);

        Assertions.assertAll(
                () -> Assertions.assertEquals(12, statement.getMaxRows()),
                () -> Assertions.assertEquals(7, statement.getFetchSize()),
                () -> Assertions.assertEquals(3, statement.getQueryTimeout()),
                () -> Assertions.assertEquals(ResultSet.FETCH_FORWARD, statement.getFetchDirection()),
                () -> Assertions.assertEquals(ResultSet.TYPE_FORWARD_ONLY, statement.getResultSetType()),
                () -> Assertions.assertEquals(ResultSet.CONCUR_READ_ONLY, statement.getResultSetConcurrency()),
                () -> Assertions.assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT,
                        statement.getResultSetHoldability()),
                () -> Assertions.assertEquals(-1, statement.getUpdateCount()),
                () -> Assertions.assertSame(connection, statement.getConnection()),
                () -> Assertions.assertSame(statement, statement.unwrap(Statement.class)),
                () -> Assertions.assertTrue(statement.isWrapperFor(Statement.class)));

        Assertions.assertThrows(SQLException.class, () -> statement.setMaxRows(-1));
        Assertions.assertThrows(SQLException.class, () -> statement.setFetchSize(-1));
        Assertions.assertThrows(SQLException.class, () -> statement.setQueryTimeout(-1));
        Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                () -> statement.setFetchDirection(ResultSet.FETCH_REVERSE));
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> statement.setMaxFieldSize(1));
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> statement.executeUpdate("update x"));
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> statement.addBatch("select A"));
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, statement::executeBatch);
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, statement::getGeneratedKeys);
        Assertions.assertThrows(SQLFeatureNotSupportedException.class,
                () -> statement.getMoreResults(Statement.KEEP_CURRENT_RESULT));
        Assertions.assertThrows(SQLException.class, () -> statement.unwrap(String.class));
    }

    @Test
    void closeUsesMockitoToCloseResultsAndAggregateFailures() throws Exception {
        JdbcSheetsConnection connection = mock(JdbcSheetsConnection.class);
        JdbcSheetsStatement statement = new JdbcSheetsStatement(connection, workbook());
        JdbcSheetsResultSet first = mock(JdbcSheetsResultSet.class);
        JdbcSheetsResultSet second = mock(JdbcSheetsResultSet.class);
        SQLException firstFailure = new SQLException("first result failed");
        SQLException secondFailure = new SQLException("second result failed");
        doThrow(firstFailure).when(first).close();
        doThrow(secondFailure).when(second).close();
        statement.resultSetMap.put("first", first);
        statement.resultSetMap.put("second", second);

        SQLException thrown = Assertions.assertThrows(SQLException.class, statement::close);

        Assertions.assertSame(firstFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[] { secondFailure }, thrown.getSuppressed());
        Assertions.assertTrue(statement.isClosed());
        Assertions.assertTrue(statement.resultSetMap.isEmpty());
        verify(first).close();
        verify(second).close();
        verify(connection).closeStatement(statement.getId());

        statement.close();
        verify(connection, times(1)).closeStatement(statement.getId());
    }

    @Test
    void closeOnCompletionClosesStatementWhenLastResultIsRemoved() throws Exception {
        JdbcSheetsConnection connection = mock(JdbcSheetsConnection.class);
        JdbcSheetsStatement statement = new JdbcSheetsStatement(connection, workbook());
        JdbcSheetsResultSet resultSet = mock(JdbcSheetsResultSet.class);
        statement.resultSetMap.put("result", resultSet);

        statement.closeOnCompletion();
        statement.closeResultSet("result");

        Assertions.assertTrue(statement.isCloseOnCompletion());
        Assertions.assertTrue(statement.isClosed());
        verify(connection).closeStatement(statement.getId());
    }

    @Test
    void closedOrDisconnectedStatementRejectsFurtherConfiguration() throws Exception {
        JdbcSheetsConnection connection = mock(JdbcSheetsConnection.class);
        JdbcSheetsStatement statement = new JdbcSheetsStatement(connection, workbook());
        statement.close();
        Assertions.assertThrows(SQLException.class, () -> statement.setMaxRows(1));

        JdbcSheetsConnection failedConnection = mock(JdbcSheetsConnection.class);
        doThrow(new SQLException("connection closed")).when(failedConnection).ensureOpen();
        JdbcSheetsStatement disconnected = new JdbcSheetsStatement(failedConnection, workbook());
        Assertions.assertThrows(SQLException.class, () -> disconnected.setQueryTimeout(1));
    }

    @Test
    void cancelIsObservedByTheCurrentResultSet() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(DriverInfo.PROP_FILE, workbook().getAbsolutePath());
        try (JdbcSheetsConnection connection = new JdbcSheetsConnection(properties);
                Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from Sheet1");
            statement.cancel();
            Assertions.assertThrows(SQLException.class, resultSet::next);
            resultSet.close();
        }
    }

    private File workbook() {
        return new File("src/test/resources/test-data.xlsx");
    }
}
