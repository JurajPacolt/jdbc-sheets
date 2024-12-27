/* Created on 28.12.2024 */
package org.javerland.jdbcsheets;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * @author juraj.pacolt
 */
public class JdbcSheetsCallableStatement extends JdbcSheetsPreparedStatement implements CallableStatement {

    String sql;
    ResultSet resultSet;

    public JdbcSheetsCallableStatement(JdbcSheetsConnection connection, File file, String sql) {
        super(connection, file, sql);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
    }

    @Override
    public boolean wasNull() throws SQLException {
        return resultSet.wasNull();
    }

    @Override
    public String getString(int parameterIndex) throws SQLException {
        return resultSet.getString(parameterIndex);
    }

    @Override
    public boolean getBoolean(int parameterIndex) throws SQLException {
        return resultSet.getBoolean(parameterIndex);
    }

    @Override
    public byte getByte(int parameterIndex) throws SQLException {
        return resultSet.getByte(parameterIndex);
    }

    @Override
    public short getShort(int parameterIndex) throws SQLException {
        return resultSet.getShort(parameterIndex);
    }

    @Override
    public int getInt(int parameterIndex) throws SQLException {
        return resultSet.getInt(parameterIndex);
    }

    @Override
    public long getLong(int parameterIndex) throws SQLException {
        return resultSet.getLong(parameterIndex);
    }

    @Override
    public float getFloat(int parameterIndex) throws SQLException {
        return resultSet.getFloat(parameterIndex);
    }

    @Override
    public double getDouble(int parameterIndex) throws SQLException {
        return resultSet.getDouble(parameterIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return resultSet.getBigDecimal(parameterIndex, scale);
    }

    @Override
    public byte[] getBytes(int parameterIndex) throws SQLException {
        return resultSet.getBytes(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex) throws SQLException {
        return resultSet.getDate(parameterIndex);
    }

    @Override
    public Time getTime(int parameterIndex) throws SQLException {
        return resultSet.getTime(parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return resultSet.getTimestamp(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex) throws SQLException {
        return resultSet.getObject(parameterIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return resultSet.getBigDecimal(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        return resultSet.getObject(parameterIndex, map);
    }

    @Override
    public Ref getRef(int parameterIndex) throws SQLException {
        return resultSet.getRef(parameterIndex);
    }

    @Override
    public Blob getBlob(int parameterIndex) throws SQLException {
        return resultSet.getBlob(parameterIndex);
    }

    @Override
    public Clob getClob(int parameterIndex) throws SQLException {
        return resultSet.getClob(parameterIndex);
    }

    @Override
    public Array getArray(int parameterIndex) throws SQLException {
        return resultSet.getArray(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return resultSet.getDate(parameterIndex, cal);
    }

    @Override
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return resultSet.getTime(parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return resultSet.getTimestamp(parameterIndex, cal);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
    }

    @Override
    public URL getURL(int parameterIndex) throws SQLException {
        return resultSet.getURL(parameterIndex);
    }

    @Override
    public void setURL(String parameterName, URL val) throws SQLException {
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws SQLException {
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws SQLException {
    }

    @Override
    public void setByte(String parameterName, byte x) throws SQLException {
    }

    @Override
    public void setShort(String parameterName, short x) throws SQLException {
    }

    @Override
    public void setInt(String parameterName, int x) throws SQLException {
    }

    @Override
    public void setLong(String parameterName, long x) throws SQLException {
    }

    @Override
    public void setFloat(String parameterName, float x) throws SQLException {
    }

    @Override
    public void setDouble(String parameterName, double x) throws SQLException {
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
    }

    @Override
    public void setString(String parameterName, String x) throws SQLException {
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws SQLException {
    }

    @Override
    public void setDate(String parameterName, Date x) throws SQLException {
    }

    @Override
    public void setTime(String parameterName, Time x) throws SQLException {
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
    }

    @Override
    public void setObject(String parameterName, Object x) throws SQLException {
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
    }

    @Override
    public String getString(String parameterName) throws SQLException {
        return resultSet.getString(parameterName);
    }

    @Override
    public boolean getBoolean(String parameterName) throws SQLException {
        return resultSet.getBoolean(parameterName);
    }

    @Override
    public byte getByte(String parameterName) throws SQLException {
        return resultSet.getByte(parameterName);
    }

    @Override
    public short getShort(String parameterName) throws SQLException {
        return resultSet.getShort(parameterName);
    }

    @Override
    public int getInt(String parameterName) throws SQLException {
        return resultSet.getInt(parameterName);
    }

    @Override
    public long getLong(String parameterName) throws SQLException {
        return resultSet.getLong(parameterName);
    }

    @Override
    public float getFloat(String parameterName) throws SQLException {
        return resultSet.getFloat(parameterName);
    }

    @Override
    public double getDouble(String parameterName) throws SQLException {
        return resultSet.getDouble(parameterName);
    }

    @Override
    public byte[] getBytes(String parameterName) throws SQLException {
        return resultSet.getBytes(parameterName);
    }

    @Override
    public Date getDate(String parameterName) throws SQLException {
        return resultSet.getDate(parameterName);
    }

    @Override
    public Time getTime(String parameterName) throws SQLException {
        return resultSet.getTime(parameterName);
    }

    @Override
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return resultSet.getTimestamp(parameterName);
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        return resultSet.getObject(parameterName);
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return resultSet.getBigDecimal(parameterName);
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return resultSet.getObject(parameterName, map);
    }

    @Override
    public Ref getRef(String parameterName) throws SQLException {
        return resultSet.getRef(parameterName);
    }

    @Override
    public Blob getBlob(String parameterName) throws SQLException {
        return resultSet.getBlob(parameterName);
    }

    @Override
    public Clob getClob(String parameterName) throws SQLException {
        return resultSet.getClob(parameterName);
    }

    @Override
    public Array getArray(String parameterName) throws SQLException {
        return resultSet.getArray(parameterName);
    }

    @Override
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return resultSet.getDate(parameterName, cal);
    }

    @Override
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return resultSet.getTime(parameterName, cal);
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return resultSet.getTimestamp(parameterName, cal);
    }

    @Override
    public URL getURL(String parameterName) throws SQLException {
        return resultSet.getURL(parameterName);
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        return resultSet.getRowId(parameterIndex);
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        return resultSet.getRowId(parameterName);
    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        return resultSet.getNClob(parameterIndex);
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        return resultSet.getNClob(parameterName);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return resultSet.getSQLXML(parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return resultSet.getSQLXML(parameterName);
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        return resultSet.getNString(parameterIndex);
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        return resultSet.getNString(parameterName);
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return resultSet.getNCharacterStream(parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return resultSet.getNCharacterStream(parameterName);
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return resultSet.getCharacterStream(parameterIndex);
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return resultSet.getCharacterStream(parameterName);
    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
    }

    @Override
    public void setNClob(String parameterName, Reader reader) throws SQLException {
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        return resultSet.getObject(parameterIndex, type);
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        return resultSet.getObject(parameterName, type);
    }
}
