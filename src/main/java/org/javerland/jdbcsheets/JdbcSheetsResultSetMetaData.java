/* Created on 15.12.2024 */
package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.util.AbstractReader;
import org.javerland.jdbcsheets.util.SqlTypeUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @@author juraj.pacolt
 */
class JdbcSheetsResultSetMetaData implements ResultSetMetaData {

    private final AbstractReader reader;

    public JdbcSheetsResultSetMetaData(AbstractReader reader) {
        this.reader = reader;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return reader.getColumns().size();
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        toIndex(column);
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return getColumnType(column) == Types.VARCHAR;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        toIndex(column);
        return true;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        toIndex(column);
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        toIndex(column);
        return ResultSetMetaData.columnNullableUnknown;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        int type = getColumnType(column);
        return type == Types.TINYINT || type == Types.SMALLINT || type == Types.INTEGER
                || type == Types.BIGINT || type == Types.REAL || type == Types.FLOAT
                || type == Types.DOUBLE || type == Types.NUMERIC || type == Types.DECIMAL;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        toIndex(column);
        return Integer.MAX_VALUE;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        int idx = toIndex(column);
        String alias = reader.getColumns().get(idx).getAlias();
        return alias == null ? reader.getColumns().get(idx).getName() : alias;
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return reader.getColumns().get(toIndex(column)).getName();
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        toIndex(column);
        return "";
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        toIndex(column);
        return 0;
    }

    @Override
    public int getScale(int column) throws SQLException {
        toIndex(column);
        return 0;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        toIndex(column);
        return reader.getTableName();
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        toIndex(column);
        return "";
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return reader.getColumns().get(toIndex(column)).getSqlType();
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        return SqlTypeUtils.toSqlType(getColumnType(column));
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        toIndex(column);
        return true;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        toIndex(column);
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        toIndex(column);
        return false;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        switch (getColumnType(column)) {
            case Types.BOOLEAN:
            case Types.BIT:
                return Boolean.class.getName();
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                return Double.class.getName();
            case Types.TIMESTAMP:
                return java.sql.Timestamp.class.getName();
            case Types.DATE:
                return java.sql.Date.class.getName();
            case Types.JAVA_OBJECT:
                return Object.class.getName();
            default:
                return String.class.getName();
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

    private int toIndex(int column) throws SQLException {
        if (column < 1 || column > reader.getColumns().size()) {
            throw new SQLException("Invalid column index: " + column);
        }
        return column - 1;
    }
}
