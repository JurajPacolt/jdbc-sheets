/* Created on 11.01.2025 */
package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.util.Column;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author juraj.pacolt
 */
class SystemResultSet implements ResultSet {

    private List<Object[]> data;
    private int row = -1;
    private List<Column> columns;
    private boolean closed = false;
    private boolean lastWasNull = false;
    private int fetchSize = 0;

    public SystemResultSet(List<Column> columns, List<Object[]> data) throws SQLException {
        this.columns = columns;
        this.data = data;
    }

    public List<Object[]> getData() {
        return data;
    }

    public List<Column> getColumns() {
        return columns;
    }

    @Override
    public boolean next() throws SQLException {
        ensureOpen();
        if (row < data.size()) {
            row++;
        }
        return row < data.size();
    }

    @Override
    public void close() throws SQLException {
        closed = true;
    }

    @Override
    public boolean wasNull() throws SQLException {
        ensureOpen();
        return lastWasNull;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? null : value.toString();
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
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
        Object value = getObject(columnIndex);
        return value == null ? 0 : (Byte.parseByte(value.toString()));
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? 0 : (Short.parseShort(value.toString()));
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? 0 : (Integer.parseInt(value.toString()));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? 0 : (Long.parseLong(value.toString()));
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? 0 : (Float.parseFloat(value.toString()));
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? 0 : (Double.parseDouble(value.toString()));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? null : new BigDecimal(value.toString()).setScale(scale, RoundingMode.HALF_UP);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        return value instanceof byte[] ? (byte[]) value : value.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? null : (Date) value;
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? null : (Time) value;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? null : (Timestamp) value;
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
        Object value = getObject(columnLabel);
        return value == null ? null : value.toString();
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(findColumn(columnLabel));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? 0 : (Byte.parseByte(value.toString()));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? 0 : (Short.parseShort(value.toString()));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? 0 : (Integer.parseInt(value.toString()));
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? 0 : (Long.parseLong(value.toString()));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? 0 : (Float.parseFloat(value.toString()));
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? 0 : (Double.parseDouble(value.toString()));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? null : new BigDecimal(value.toString()).setScale(scale, RoundingMode.HALF_UP);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? null : (Date) value;
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? null : (Time) value;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? null : (Timestamp) value;
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
    public boolean previous() throws SQLException {
        ensureOpen();
        if (row > 0) {
            row--;
            return true;
        }
        row = -1;
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD && direction != ResultSet.FETCH_REVERSE
                && direction != ResultSet.FETCH_UNKNOWN) {
            throw new SQLException("Invalid fetch direction: " + direction);
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_UNKNOWN;
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
    public int getRow() throws SQLException {
        ensureOpen();
        return row >= 0 && row < data.size() ? row + 1 : 0;
    }

    @Override
    public boolean absolute(int requestedRow) throws SQLException {
        ensureOpen();
        int target = requestedRow > 0 ? requestedRow - 1 : data.size() + requestedRow;
        if (requestedRow == 0) {
            row = -1;
            return false;
        }
        row = target;
        if (row < 0) {
            row = -1;
            return false;
        }
        if (row >= data.size()) {
            row = data.size();
            return false;
        }
        return true;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        ensureOpen();
        int base = row < 0 ? -1 : row;
        int target = base + rows;
        if (target < 0) {
            row = -1;
            return false;
        }
        if (target >= data.size()) {
            row = data.size();
            return false;
        }
        row = target;
        return true;
    }

    @Override
    public int getType() throws SQLException {
        return ResultSet.TYPE_SCROLL_INSENSITIVE;
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
        throw readOnly();
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
        return null;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnIndex);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw unsupported("REF");
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        throw unsupported("BLOB");
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw unsupported("CLOB");
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw unsupported("ARRAY");
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
        Object value = getObject(columnIndex);
        return value == null ? null : (Date) value;
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? null : (Date) value;
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? null : (Time) value;
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? null : (Time) value;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        Object value = getObject(columnIndex);
        return value == null ? null : (Timestamp) value;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        Object value = getObject(columnLabel);
        return value == null ? null : (Timestamp) value;
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException ex) {
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
        throw unsupported("ROWID");
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
        throw unsupported("NCLOB");
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return getNClob(findColumn(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw unsupported("SQLXML");
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
        Object val = getObject(columnIndex);
        return val == null ? null : val.toString();
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        return val == null ? null : val.toString();
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
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (type == String.class) {
            return type.cast(value.toString());
        }
        throw new SQLException("Cannot convert column " + columnIndex + " to " + type.getName());
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return getObject(findColumn(columnLabel), type);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new SystemResultSetMetaData(this);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        ensureOpen();
        ensureOnRow();
        if (columnIndex < 1 || columnIndex > columns.size()) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        Object value = data.get(row)[columnIndex - 1];
        lastWasNull = value == null;
        return value;
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(findColumn(columnLabel));
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        int idx = IntStream.range(0, columns.size()).filter(i -> columns.get(i).getName()
                .equalsIgnoreCase(columnLabel)).findFirst().orElse(-1);
        if (idx < 0) {
            throw new SQLException(String.format("Column \"%s\" not found", columnLabel));
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
        Object val = getObject(columnIndex);
        return val != null ? new BigDecimal(val.toString()) : null;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        Object val = getObject(columnLabel);
        return val != null ? new BigDecimal(val.toString()) : null;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        ensureOpen();
        return !data.isEmpty() && row == -1;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        ensureOpen();
        return !data.isEmpty() && row == data.size();
    }

    @Override
    public boolean isFirst() throws SQLException {
        ensureOpen();
        return row == 0;
    }

    @Override
    public boolean isLast() throws SQLException {
        ensureOpen();
        return row == data.size() - 1;
    }

    @Override
    public void beforeFirst() throws SQLException {
        ensureOpen();
        this.row = -1;
    }

    @Override
    public void afterLast() throws SQLException {
        ensureOpen();
        this.row = data.size();
    }

    @Override
    public boolean first() throws SQLException {
        ensureOpen();
        if (data.isEmpty()) {
            row = data.size();
            return false;
        }
        row = 0;
        return true;
    }

    @Override
    public boolean last() throws SQLException {
        ensureOpen();
        if (data.isEmpty()) {
            row = data.size();
            return false;
        }
        row = data.size() - 1;
        return true;
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
        if (row < 0 || row >= data.size()) {
            throw new SQLException("ResultSet cursor is not positioned on a row.");
        }
    }

    private SQLFeatureNotSupportedException readOnly() {
        return new SQLFeatureNotSupportedException("This ResultSet is read-only.");
    }

    private SQLFeatureNotSupportedException unsupported(String type) {
        return new SQLFeatureNotSupportedException(type + " values are not supported.");
    }
}
