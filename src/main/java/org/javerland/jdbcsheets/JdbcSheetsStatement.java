/* Created on 15.12.2024 */
package org.javerland.jdbcsheets;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author juraj.pacolt
 */
class JdbcSheetsStatement implements Statement {

    final JdbcSheetsConnection connection;
    final File file;
    final String id;
    Map<String, JdbcSheetsResultSet> resultSetMap = new LinkedHashMap<>();
    private JdbcSheetsResultSet lastResultSet;
    private volatile boolean closed = false;
    private volatile boolean closeOnCompletion = false;
    private volatile int maxRows = 0;
    private volatile int fetchSize = 0;
    private volatile int queryTimeout = 0;
    private volatile boolean cancelled = false;
    private volatile long deadlineNanos = 0L;

    public JdbcSheetsStatement(JdbcSheetsConnection connection, File file) {
        this.connection = connection;
        this.file = file;
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    protected File getFile() {
        return file;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        ensureOpen();
        if (lastResultSet != null && !lastResultSet.isClosed()) {
            lastResultSet.close();
            ensureOpen();
        }
        cancelled = false;
        deadlineNanos = queryTimeout == 0 ? 0L
                : System.nanoTime() + TimeUnit.SECONDS.toNanos(queryTimeout);
        JdbcSheetsResultSet rs = new JdbcSheetsResultSet(connection, this, sql);
        try {
            checkExecutionState();
        } catch (SQLException ex) {
            rs.close();
            throw ex;
        }
        resultSetMap.put(rs.getId(), rs);
        lastResultSet = rs;
        return rs;
    }

    protected void closeResultSet(String resultSetId) throws SQLException {
        resultSetMap.remove(resultSetId);
        if (lastResultSet != null && lastResultSet.getId().equals(resultSetId)) {
            lastResultSet = null;
        }
        if (closeOnCompletion && resultSetMap.isEmpty() && !closed) {
            close();
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        SQLException failure = null;
        for (JdbcSheetsResultSet rs : new ArrayList<>(resultSetMap.values())) {
            try {
                rs.close();
            } catch (SQLException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        resultSetMap.clear();
        lastResultSet = null;
        connection.closeStatement(id);
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        // ignore
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        if (max != 0) {
            throw new SQLFeatureNotSupportedException("Max field size is not supported.");
        }
    }

    @Override
    public int getMaxRows() throws SQLException {
        return maxRows;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        ensureOpen();
        if (max < 0) {
            throw new SQLException("Max rows must be >= 0.");
        }
        maxRows = max;
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        // ignore
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        ensureOpen();
        if (seconds < 0) {
            throw new SQLException("Query timeout must be >= 0.");
        }
        queryTimeout = seconds;
    }

    @Override
    public void cancel() throws SQLException {
        ensureOpen();
        cancelled = true;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        // ignore
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        // ignore
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException("Named cursors are not supported.");
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        executeQuery(sql);
        return true;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return lastResultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return -1;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        if (lastResultSet != null) {
            lastResultSet.close();
        }
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw new SQLFeatureNotSupportedException("Only FETCH_FORWARD is supported.");
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (rows < 0) {
            throw new SQLException("Fetch size must be >= 0.");
        }
        fetchSize = rows;
    }

    @Override
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException("Batch execution is not supported.");
    }

    @Override
    public void clearBatch() throws SQLException {
        // ignore
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException("Batch execution is not supported.");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        if (current == Statement.KEEP_CURRENT_RESULT) {
            throw new SQLFeatureNotSupportedException("Multiple open results are not supported.");
        }
        return getMoreResults();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return execute(sql);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        // ignore
    }

    @Override
    public boolean isPoolable() throws SQLException {
        // ignore
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        ensureOpen();
        closeOnCompletion = true;
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return closeOnCompletion;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    private void ensureOpen() throws SQLException {
        if (closed) {
            throw new SQLException("Statement is closed.");
        }
        connection.ensureOpen();
    }

    int getMaxRowsLimit() {
        return maxRows;
    }

    void checkExecutionState() throws SQLException {
        if (cancelled) {
            throw new SQLException("Query was cancelled.");
        }
        if (deadlineNanos != 0L && System.nanoTime() - deadlineNanos >= 0L) {
            throw new SQLTimeoutException("Query timeout exceeded.");
        }
    }
}
