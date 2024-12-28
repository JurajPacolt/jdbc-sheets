/* Created on 14.12.2024 */
package org.javerland.jdbcsheets;

import org.apache.commons.lang3.StringUtils;
import org.javerland.jdbcsheets.enums.ReaderType;
import org.javerland.jdbcsheets.exception.JdbcSheetsException;

import java.io.File;
import java.sql.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author juraj.pacolt
 */
class JdbcSheetsConnection implements Connection {

    Properties props;
    File file;
    Map<String, Statement> statements = new LinkedHashMap<>();

    public JdbcSheetsConnection(Properties props) {
        this.props = props;
        this.file = getSourceFile();
    }

    protected ReaderType getReaderType() {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        String extension = fileName.substring(lastDotIndex + 1);
        return ReaderType.valueOf(extension.toUpperCase());
    }

    protected File getSourceFile() {
        try {
            String file = getClientInfo(DriverInfo.PROP_FILE);
            if (StringUtils.isBlank(file)) {
                file = getClientInfo(DriverInfo.PROP_DIRECTORY) + File.separator + getClientInfo(DriverInfo.PROP_DATABASE);
            }
            return new File(file);
        } catch (SQLException ex) {
            throw new JdbcSheetsException(ex.getMessage(), ex);
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        JdbcSheetsStatement stmt = new JdbcSheetsStatement(this, file);
        statements.put(stmt.getId(), stmt);
        return stmt;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        JdbcSheetsPreparedStatement ps = new JdbcSheetsPreparedStatement(this, file, sql);
        statements.put(ps.getId(), ps);
        return ps;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        JdbcSheetsCallableStatement cs = new JdbcSheetsCallableStatement(this, file, sql);
        statements.put(cs.getId(), cs);
        return cs;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return "";
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        // ignore
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        // ignore
        return false;
    }

    @Override
    public void commit() throws SQLException {
        // ignore
    }

    @Override
    public void rollback() throws SQLException {
        // ignore
    }

    @Override
    public void close() throws SQLException {
        for (Statement stmt : statements.values()) {
            stmt.close();
        }
        statements.clear();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return new JdbcSheetsDatabaseMetadata(this);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        // ignore
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        // it's only readonly ;)
        return true;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {

    }

    @Override
    public String getCatalog() throws SQLException {
        return "";
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        // ignore
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        // ignore
        return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return Collections.emptyMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        // ignore
    }

    @Override
    public int getHoldability() throws SQLException {
        // ignore
        return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        // ignore
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        // ignore
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        // ignore
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        // ignore
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        // ignore
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        // ignore
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        // ignore
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        // ignore
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return false;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        this.props.setProperty(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        this.props = properties;
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return props.getProperty(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return props;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        // ignore
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        // ignore
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
    }

    @Override
    public String getSchema() throws SQLException {
        return "";
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        // ignore
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        // ignore
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        // ignore
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
