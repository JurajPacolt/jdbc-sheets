package org.javerland.jdbcsheets;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/** Read-only prepared statement with positional scalar parameters. */
final class JdbcSheetsPreparedStatement extends JdbcSheetsStatement implements PreparedStatement {

    private final String template;
    private final int parameterCount;
    private final Map<Integer, Object> parameters = new HashMap<>();
    private ResultSet resultSet;
    private ResultSetMetaData resultSetMetaData;

    JdbcSheetsPreparedStatement(JdbcSheetsConnection connection, File file, String sql) throws SQLException {
        super(connection, file);
        if (sql == null || sql.trim().isEmpty()) {
            throw new SQLException("Prepared SQL must not be empty.");
        }
        this.template = sql;
        this.parameterCount = countParameters(sql);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        resultSet = super.executeQuery(renderSql());
        resultSetMetaData = resultSet.getMetaData();
        return resultSet;
    }

    @Override
    public boolean execute() throws SQLException {
        executeQuery();
        return true;
    }

    @Override
    public int executeUpdate() throws SQLException {
        throw readOnly();
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        throw readOnly();
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        throw sqlArgumentNotAllowed();
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw sqlArgumentNotAllowed();
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        throw sqlArgumentNotAllowed();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw sqlArgumentNotAllowed();
    }

    @Override
    public void addBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException("Prepared batches are not supported.");
    }

    @Override
    public void clearParameters() throws SQLException {
        parameters.clear();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        set(parameterIndex, null);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setByte(int parameterIndex, byte value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setShort(int parameterIndex, short value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setInt(int parameterIndex, int value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setLong(int parameterIndex, long value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setFloat(int parameterIndex, float value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setDouble(int parameterIndex, double value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setString(int parameterIndex, String value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        setString(parameterIndex, value);
    }

    @Override
    public void setDate(int parameterIndex, Date value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setDate(int parameterIndex, Date value, Calendar calendar) throws SQLException {
        setDate(parameterIndex, value);
    }

    @Override
    public void setTime(int parameterIndex, Time value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setTime(int parameterIndex, Time value, Calendar calendar) throws SQLException {
        setTime(parameterIndex, value);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp value) throws SQLException {
        set(parameterIndex, value);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp value, Calendar calendar) throws SQLException {
        setTimestamp(parameterIndex, value);
    }

    @Override
    public void setURL(int parameterIndex, URL value) throws SQLException {
        set(parameterIndex, value == null ? null : value.toString());
    }

    @Override
    public void setObject(int parameterIndex, Object value) throws SQLException {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Date || value instanceof Time || value instanceof Timestamp
                || value instanceof URL) {
            set(parameterIndex, value instanceof URL ? value.toString() : value);
            return;
        }
        throw new SQLFeatureNotSupportedException("Unsupported prepared parameter type: "
                + value.getClass().getName());
    }

    @Override
    public void setObject(int parameterIndex, Object value, int targetSqlType) throws SQLException {
        setObject(parameterIndex, value);
    }

    @Override
    public void setObject(int parameterIndex, Object value, int targetSqlType, int scaleOrLength)
            throws SQLException {
        setObject(parameterIndex, value);
    }

    @Override
    public void setObject(int parameterIndex, Object value, SQLType targetSqlType) throws SQLException {
        setObject(parameterIndex, value);
    }

    @Override
    public void setObject(int parameterIndex, Object value, SQLType targetSqlType, int scaleOrLength)
            throws SQLException {
        setObject(parameterIndex, value);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return resultSetMetaData;
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return new JdbcSheetsParameterMetaData(parameterCount);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] value) throws SQLException {
        throw unsupportedBinding("byte arrays");
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream value, int length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream value, int length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream value, int length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setRef(int parameterIndex, Ref value) throws SQLException {
        throw unsupportedBinding("REF");
    }

    @Override
    public void setBlob(int parameterIndex, Blob value) throws SQLException {
        throw unsupportedBinding("BLOB");
    }

    @Override
    public void setClob(int parameterIndex, Clob value) throws SQLException {
        throw unsupportedBinding("CLOB");
    }

    @Override
    public void setArray(int parameterIndex, Array value) throws SQLException {
        throw unsupportedBinding("ARRAY");
    }

    @Override
    public void setRowId(int parameterIndex, RowId value) throws SQLException {
        throw unsupportedBinding("ROWID");
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw unsupportedBinding("NCLOB");
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML value) throws SQLException {
        throw unsupportedBinding("SQLXML");
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream value, long length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream value, long length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream value) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream value) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw unsupportedBinding("streams");
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw unsupportedBinding("streams");
    }

    private void set(int parameterIndex, Object value) throws SQLException {
        validateParameterIndex(parameterIndex);
        parameters.put(parameterIndex, value);
    }

    private void validateParameterIndex(int parameterIndex) throws SQLException {
        if (parameterIndex < 1 || parameterIndex > parameterCount) {
            throw new SQLException("Invalid parameter index: " + parameterIndex);
        }
    }

    private String renderSql() throws SQLException {
        for (int index = 1; index <= parameterCount; index++) {
            if (!parameters.containsKey(index)) {
                throw new SQLException("Parameter " + index + " is not set.");
            }
        }
        StringBuilder rendered = new StringBuilder();
        boolean inString = false;
        int parameterIndex = 1;
        for (int i = 0; i < template.length(); i++) {
            char ch = template.charAt(i);
            if (ch == '\'') {
                rendered.append(ch);
                if (inString && i + 1 < template.length() && template.charAt(i + 1) == '\'') {
                    rendered.append(template.charAt(++i));
                } else {
                    inString = !inString;
                }
            } else if (ch == '?' && !inString) {
                rendered.append(toLiteral(parameters.get(parameterIndex++)));
            } else {
                rendered.append(ch);
            }
        }
        return rendered.toString();
    }

    private String toLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        String text = value.toString();
        return "'" + text.replace("'", "''") + "'";
    }

    private int countParameters(String sql) throws SQLException {
        int count = 0;
        boolean inString = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                if (inString && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    inString = !inString;
                }
            } else if (ch == '?' && !inString) {
                count++;
            }
        }
        if (inString) {
            throw new SQLException("Unterminated string literal in prepared SQL.");
        }
        return count;
    }

    private SQLFeatureNotSupportedException unsupportedBinding(String type) {
        return new SQLFeatureNotSupportedException("Prepared " + type + " parameters are not supported.");
    }

    private SQLFeatureNotSupportedException readOnly() {
        return new SQLFeatureNotSupportedException("The XLSX prepared statement is read-only.");
    }

    private SQLFeatureNotSupportedException sqlArgumentNotAllowed() {
        return new SQLFeatureNotSupportedException("Use the no-argument PreparedStatement execution method.");
    }
}
