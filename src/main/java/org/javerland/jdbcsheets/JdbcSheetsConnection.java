/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

import org.apache.commons.lang3.StringUtils;
import org.javerland.jdbcsheets.enums.ReaderType;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author juraj.pacolt
 */
final class JdbcSheetsConnection implements Connection {

    Properties props;
    File file;
    Map<String, Statement> statements = new LinkedHashMap<>();
    private boolean closed = false;
    private final boolean headerRow;

    public JdbcSheetsConnection(Properties props) throws SQLException {
        this.props = new Properties();
        this.props.putAll(props);
        this.file = getSourceFile();
        this.headerRow = parseHeaderRow(this.props.getProperty(DriverInfo.PROP_HEADER, "false"));
        getReaderType();
    }

    protected ReaderType getReaderType() throws SQLException {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0) {
            throw new SQLException("File has no extension: " + fileName);
        }
        String extension = fileName.substring(lastDotIndex + 1);
        try {
            return ReaderType.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new SQLFeatureNotSupportedException(
                    "Unsupported file type: " + extension + ". Supported: XLSX", ex);
        }
    }

    protected File getSourceFile() throws SQLException {
        try {
            String fileValue = props.getProperty(DriverInfo.PROP_FILE);
            Path sourcePath;
            if (StringUtils.isBlank(fileValue)) {
                String database = props.getProperty(DriverInfo.PROP_DATABASE);
                if (StringUtils.isBlank(database)) {
                    throw new SQLNonTransientConnectionException(
                            "Connection requires either 'file' or 'database' property.");
                }
                String directory = props.getProperty(DriverInfo.PROP_DIRECTORY, ".");
                sourcePath = Paths.get(directory).resolve(database).normalize();
            } else {
                sourcePath = Paths.get(fileValue).normalize();
            }
            File sourceFile = sourcePath.toFile();
            if (!sourceFile.isFile()) {
                throw new SQLNonTransientConnectionException("Spreadsheet file does not exist: " + sourcePath);
            }
            if (!sourceFile.canRead()) {
                throw new SQLNonTransientConnectionException("Spreadsheet file is not readable: " + sourcePath);
            }
            return sourceFile;
        } catch (InvalidPathException ex) {
            throw new SQLNonTransientConnectionException("Invalid spreadsheet path.", ex);
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        ensureOpen();
        JdbcSheetsStatement stmt = new JdbcSheetsStatement(this, file);
        statements.put(stmt.getId(), stmt);
        return stmt;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        ensureOpen();
        JdbcSheetsPreparedStatement statement = new JdbcSheetsPreparedStatement(this, file, sql);
        statements.put(statement.getId(), statement);
        return statement;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        ensureOpen();
        throw new SQLFeatureNotSupportedException("Callable statements are not supported.");
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        ensureOpen();
        return sql;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        ensureOpen();
        if (!autoCommit) {
            throw new SQLFeatureNotSupportedException("Transactions are not supported.");
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        ensureOpen();
        return true;
    }

    @Override
    public void commit() throws SQLException {
        ensureOpen();
    }

    @Override
    public void rollback() throws SQLException {
        ensureOpen();
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        SQLException failure = null;
        for (Statement stmt : new ArrayList<>(statements.values())) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        statements.clear();
        closed = true;
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        ensureOpen();
        return new JdbcSheetsDatabaseMetadata(this);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        ensureOpen();
        if (!readOnly) {
            throw new SQLFeatureNotSupportedException("The XLSX connection is read-only.");
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        ensureOpen();
        return true;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        ensureOpen();
        if (catalog != null && !catalog.isEmpty()) {
            throw new SQLFeatureNotSupportedException("Catalogs are not supported.");
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        ensureOpen();
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        ensureOpen();
        if (level != Connection.TRANSACTION_NONE) {
            throw new SQLFeatureNotSupportedException("Transactions are not supported.");
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        ensureOpen();
        return Connection.TRANSACTION_NONE;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        ensureOpen();
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        ensureOpen();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        validateResultSetOptions(resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        return createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        validateResultSetOptions(resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        return prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        validateResultSetOptions(resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        return prepareCall(sql);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        ensureOpen();
        return Collections.emptyMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        ensureOpen();
        if (map != null && !map.isEmpty()) {
            throw new SQLFeatureNotSupportedException("Custom SQL type maps are not supported.");
        }
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        ensureOpen();
        if (holdability != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
            throw new SQLFeatureNotSupportedException("Unsupported result-set holdability: " + holdability);
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        ensureOpen();
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException("Savepoints are not supported.");
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException("Savepoints are not supported.");
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        validateResultSetOptions(resultSetType, resultSetConcurrency, resultSetHoldability);
        return createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        validateResultSetOptions(resultSetType, resultSetConcurrency, resultSetHoldability);
        return prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        validateResultSetOptions(resultSetType, resultSetConcurrency, resultSetHoldability);
        return prepareCall(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        if (autoGeneratedKeys != Statement.NO_GENERATED_KEYS) {
            throw new SQLFeatureNotSupportedException("Generated keys are not supported.");
        }
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        if (columnIndexes != null && columnIndexes.length > 0) {
            throw new SQLFeatureNotSupportedException("Generated keys are not supported.");
        }
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        if (columnNames != null && columnNames.length > 0) {
            throw new SQLFeatureNotSupportedException("Generated keys are not supported.");
        }
        return prepareStatement(sql);
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (timeout < 0) {
            throw new SQLException("Timeout must be >= 0.");
        }
        return !closed && file != null && file.isFile() && file.canRead();
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (closed) {
            throw new SQLClientInfoException();
        }
        if (value == null) {
            this.props.remove(name);
        } else {
            this.props.setProperty(name, value);
        }
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if (closed) {
            throw new SQLClientInfoException();
        }
        if (properties != null) {
            this.props.putAll(properties);
        }
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        ensureOpen();
        return props.getProperty(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        ensureOpen();
        Properties copy = new Properties();
        copy.putAll(props);
        return copy;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        ensureOpen();
        if (schema != null && !schema.isEmpty()) {
            throw new SQLFeatureNotSupportedException("Schemas are not supported.");
        }
    }

    @Override
    public String getSchema() throws SQLException {
        ensureOpen();
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        close();
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        ensureOpen();
        if (milliseconds < 0) {
            throw new SQLException("Network timeout must be >= 0.");
        }
        if (milliseconds != 0) {
            throw new SQLFeatureNotSupportedException("Network timeouts do not apply to local XLSX files.");
        }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        ensureOpen();
        return 0;
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

    void ensureOpen() throws SQLException {
        if (closed) {
            throw new SQLException("Connection is closed.");
        }
    }

    void closeStatement(String statementId) {
        statements.remove(statementId);
    }

    File sourceFile() {
        return file;
    }

    boolean usesHeaderRow() {
        return headerRow;
    }

    private boolean parseHeaderRow(String value) throws SQLException {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new SQLException("The 'header' property must be true or false.");
    }

    private void validateResultSetOptions(int type, int concurrency, int holdability) throws SQLException {
        if (type != ResultSet.TYPE_FORWARD_ONLY) {
            throw new SQLFeatureNotSupportedException("Only TYPE_FORWARD_ONLY result sets are supported.");
        }
        if (concurrency != ResultSet.CONCUR_READ_ONLY) {
            throw new SQLFeatureNotSupportedException("Only CONCUR_READ_ONLY result sets are supported.");
        }
        if (holdability != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
            throw new SQLFeatureNotSupportedException("Unsupported result-set holdability: " + holdability);
        }
    }
}
