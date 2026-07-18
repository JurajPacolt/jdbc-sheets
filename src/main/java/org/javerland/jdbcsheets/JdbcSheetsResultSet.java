/* Created on 15.12.2024 */
package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.exception.JdbcSheetsException;
import org.javerland.jdbcsheets.util.AbstractReader;
import org.javerland.jdbcsheets.util.XlsxReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

/**
 * @author juraj.pacolt
 */
final class JdbcSheetsResultSet implements ResultSet {

    private JdbcSheetsStatement stmt;
    private final String id;
    private AbstractReader reader;
    private Object[] actualRow;
    private boolean closed = false;
    private boolean lastWasNull = false;
    private int currentRow = 0;
    private boolean afterLast = false;

    public JdbcSheetsResultSet(JdbcSheetsConnection connection, JdbcSheetsStatement stmt, String query) throws SQLException {
        this.id = UUID.randomUUID().toString();
        this.stmt = stmt;
        switch (connection.getReaderType()) {
            case XLSX:
                reader = new XlsxReader(stmt.getFile(), connection.usesHeaderRow());
                break;
            default:
                throw new SQLFeatureNotSupportedException("Unsupported reader type: " + connection.getReaderType());
        }
        reader.parseQuery(query);
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean next() throws SQLException {
        ensureOpen();
        stmt.checkExecutionState();
        int maxRows = stmt.getMaxRowsLimit();
        if (maxRows > 0 && currentRow >= maxRows) {
            actualRow = null;
            afterLast = currentRow > 0;
            currentRow = 0;
            return false;
        }
        actualRow = reader.next();
        stmt.checkExecutionState();
        if (actualRow == null) {
            afterLast = currentRow > 0;
            currentRow = 0;
            return false;
        }
        currentRow++;
        return true;
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            try {
                reader.close();
            } catch (JdbcSheetsException ex) {
                throw new SQLException("Failed to close XLSX workbook.", ex);
            }
        } finally {
            actualRow = null;
            stmt.closeResultSet(id);
        }
    }

    @Override
    public boolean wasNull() throws SQLException {
        ensureOpen();
        return lastWasNull;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        Object value = getValue(columnIndex);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        Object value = getValue(columnIndex);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String text = value.toString().trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        BigDecimal value = toBigDecimal(columnIndex);
        return value != null ? value.byteValue() : 0;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        BigDecimal value = toBigDecimal(columnIndex);
        return value != null ? value.shortValue() : 0;
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        BigDecimal value = toBigDecimal(columnIndex);
        return value != null ? value.intValue() : 0;
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        BigDecimal value = toBigDecimal(columnIndex);
        return value != null ? value.longValue() : 0;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        BigDecimal value = toBigDecimal(columnIndex);
        return value != null ? value.floatValue() : 0;
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        BigDecimal value = toBigDecimal(columnIndex);
        return value != null ? value.doubleValue() : 0;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        BigDecimal value = toBigDecimal(columnIndex);
        return value != null ? value.setScale(scale, RoundingMode.HALF_UP) : null;
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        Object value = getValue(columnIndex);
        if (value == null) {
            return null;
        }
        return value instanceof byte[] ? (byte[]) value : value.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        Object value = getValue(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof java.util.Date) {
            return new Date(((java.util.Date) value).getTime());
        }
        throw new SQLException("Cannot convert column " + columnIndex + " to Date.");
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        Object value = getValue(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Time) {
            return (Time) value;
        }
        if (value instanceof java.util.Date) {
            return new Time(((java.util.Date) value).getTime());
        }
        throw new SQLException("Cannot convert column " + columnIndex + " to Time.");
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        Object value = getValue(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        throw new SQLException("Cannot convert column " + columnIndex + " to Timestamp.");
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new ByteArrayInputStream(value.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return getBinaryStream(columnIndex);
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        byte[] value = getBytes(columnIndex);
        return value == null ? null : new ByteArrayInputStream(value);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Object value = getValue(columnLabel);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getBoolean(idx);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getByte(idx);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getShort(idx);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getInt(idx);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getLong(idx);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getFloat(idx);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getDouble(idx);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        int idx = findColumn(columnLabel);
        BigDecimal val = getBigDecimal(idx);
        return val != null ? val.setScale(scale, RoundingMode.HALF_UP) : null;
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(findColumn(columnLabel));
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(findColumn(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(findColumn(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return getAsciiStream(findColumn(columnLabel));
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return getUnicodeStream(findColumn(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return getBinaryStream(findColumn(columnLabel));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public String getCursorName() throws SQLException {
        return "";
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        ensureOpen();
        return new JdbcSheetsResultSetMetaData(reader);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return getValue(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getValue(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        ensureOpen();
        int idx = reader.getColumnIndexByName(columnLabel);
        if (idx < 0) {
            throw new SQLException("Column \"" + columnLabel + "\" not found.");
        }
        return idx + 1;
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new StringReader(value);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return getCharacterStream(findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return toBigDecimal(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        int idx = findColumn(columnLabel);
        return getBigDecimal(idx);
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        ensureOpen();
        return currentRow == 0 && !afterLast;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        ensureOpen();
        return afterLast;
    }

    @Override
    public boolean isFirst() throws SQLException {
        ensureOpen();
        return currentRow == 1;
    }

    @Override
    public boolean isLast() throws SQLException {
        return false;
    }

    @Override
    public void beforeFirst() throws SQLException {
        throw forwardOnly();
    }

    @Override
    public void afterLast() throws SQLException {
        throw forwardOnly();
    }

    @Override
    public boolean first() throws SQLException {
        throw forwardOnly();
    }

    @Override
    public boolean last() throws SQLException {
        throw forwardOnly();
    }

    @Override
    public int getRow() throws SQLException {
        ensureOpen();
        return currentRow;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        throw forwardOnly();
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        throw forwardOnly();
    }

    @Override
    public boolean previous() throws SQLException {
        throw forwardOnly();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw forwardOnly();
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    @Override
    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void insertRow() throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateRow() throws SQLException {
        throw readOnly();
    }

    @Override
    public void deleteRow() throws SQLException {
        throw readOnly();
    }

    @Override
    public void refreshRow() throws SQLException {
        throw forwardOnly();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw readOnly();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw readOnly();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw readOnly();
    }

    @Override
    public Statement getStatement() throws SQLException {
        return stmt;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnIndex);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("REF values are not supported.");
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("BLOB values are not supported.");
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("CLOB values are not supported.");
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("ARRAY values are not supported.");
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnLabel);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return getRef(findColumn(columnLabel));
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return getBlob(findColumn(columnLabel));
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return getClob(findColumn(columnLabel));
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return getArray(findColumn(columnLabel));
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDate(columnIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return getTime(columnIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(columnLabel);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (java.net.MalformedURLException ex) {
            throw new SQLException("Invalid URL value at column " + columnIndex, ex);
        }
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return getURL(findColumn(columnLabel));
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw readOnly();
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("ROWID values are not supported.");
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return getRowId(findColumn(columnLabel));
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw readOnly();
    }

    @Override
    public int getHoldability() throws SQLException {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw readOnly();
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("NCLOB values are not supported.");
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return getNClob(findColumn(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("SQLXML values are not supported.");
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return getSQLXML(findColumn(columnLabel));
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw readOnly();
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return getCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return getCharacterStream(columnLabel);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw readOnly();
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw readOnly();
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        if (type == null) {
            throw new SQLException("Target type must not be null.");
        }
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        Object converted = convertValue(value, type, columnIndex);
        if (converted != null) {
            return type.cast(converted);
        }
        throw new SQLException("Cannot convert column " + columnIndex + " to " + type.getName());
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        int idx = findColumn(columnLabel);
        return getObject(idx, type);
    }

    private Object getValue(int columnIndex) throws SQLException {
        ensureOpen();
        ensureOnRow();
        if (columnIndex < 1 || columnIndex > actualRow.length) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        Object value = actualRow[columnIndex - 1];
        lastWasNull = value == null;
        return value;
    }

    private Object getValue(String columnLabel) throws SQLException {
        int idx = reader.getColumnIndexByName(columnLabel);
        if (idx < 0) {
            throw new SQLException("Column \"" + columnLabel + "\" not found.");
        }
        return getValue(idx + 1);
    }

    private BigDecimal toBigDecimal(int columnIndex) throws SQLException {
        Object value = getValue(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof java.util.Date) {
            throw new SQLException("Cannot convert date to numeric value at column " + columnIndex);
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException ex) {
            throw new SQLException("Invalid numeric value at column " + columnIndex + ": " + value, ex);
        }
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
            throw new SQLException("ResultSet is closed.");
        }
    }

    private void ensureOnRow() throws SQLException {
        if (actualRow == null) {
            throw new SQLException("ResultSet cursor is not positioned on a row.");
        }
    }

    private SQLFeatureNotSupportedException forwardOnly() {
        return new SQLFeatureNotSupportedException("This ResultSet is forward-only.");
    }

    private SQLFeatureNotSupportedException readOnly() {
        return new SQLFeatureNotSupportedException("This ResultSet is read-only.");
    }

    private Object convertValue(Object value, Class<?> type, int columnIndex) throws SQLException {
        if (type == String.class) {
            return value.toString();
        }
        try {
            if (type == BigDecimal.class) {
                return new BigDecimal(value.toString());
            }
            if (type == Integer.class) {
                return new BigDecimal(value.toString()).intValue();
            }
            if (type == Long.class) {
                return new BigDecimal(value.toString()).longValue();
            }
            if (type == Double.class) {
                return new BigDecimal(value.toString()).doubleValue();
            }
            if (type == Float.class) {
                return new BigDecimal(value.toString()).floatValue();
            }
            if (type == Short.class) {
                return new BigDecimal(value.toString()).shortValue();
            }
            if (type == Byte.class) {
                return new BigDecimal(value.toString()).byteValue();
            }
        } catch (NumberFormatException ex) {
            throw new SQLException("Cannot convert column " + columnIndex + " to " + type.getName() + ".", ex);
        }
        if (type == Boolean.class) {
            return getBoolean(columnIndex);
        }
        if (type == Date.class) {
            return getDate(columnIndex);
        }
        if (type == Time.class) {
            return getTime(columnIndex);
        }
        if (type == Timestamp.class) {
            return getTimestamp(columnIndex);
        }
        return null;
    }
}
