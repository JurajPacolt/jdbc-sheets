/* Created on 11.01.2025 */
package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.util.SqlTypeUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author juraj.pacolt
 */
class SystemResultSetMetaData implements ResultSetMetaData {

    private SystemResultSet resultSet;

    public SystemResultSetMetaData(SystemResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return resultSet.getColumns().size();
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        toIndex(column);
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        toIndex(column);
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        toIndex(column);
        return false;
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
        toIndex(column);
        return false;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        toIndex(column);
        return 128;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        int idx = toIndex(column);
        String alias = resultSet.getColumns().get(idx).getAlias();
        return alias == null ? resultSet.getColumns().get(idx).getName() : alias;
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return resultSet.getColumns().get(toIndex(column)).getName();
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
        return "";
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        toIndex(column);
        return "";
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return resultSet.getColumns().get(toIndex(column)).getSqlType();
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        int sqlType = resultSet.getColumns().get(toIndex(column)).getSqlType();
        return SqlTypeUtils.toSqlType(sqlType);
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
        int idx = toIndex(column);
        if (resultSet.getData().size() > 0 && resultSet.getData().get(0)[idx] != null) {
            return resultSet.getData().get(0)[idx].getClass().getName();
        }
        int sqlType = resultSet.getColumns().get(idx).getSqlType();
        return getClassNameForSqlType(sqlType);
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
        if (column < 1 || column > resultSet.getColumns().size()) {
            throw new SQLException("Invalid column index: " + column);
        }
        return column - 1;
    }

    private String getClassNameForSqlType(int sqlType) {
        switch (sqlType) {
            case java.sql.Types.BOOLEAN:
            case java.sql.Types.BIT:
                return Boolean.class.getName();
            case java.sql.Types.TINYINT:
                return Byte.class.getName();
            case java.sql.Types.SMALLINT:
                return Short.class.getName();
            case java.sql.Types.INTEGER:
                return Integer.class.getName();
            case java.sql.Types.BIGINT:
                return Long.class.getName();
            case java.sql.Types.FLOAT:
            case java.sql.Types.REAL:
                return Float.class.getName();
            case java.sql.Types.DOUBLE:
                return Double.class.getName();
            case java.sql.Types.DECIMAL:
            case java.sql.Types.NUMERIC:
                return java.math.BigDecimal.class.getName();
            case java.sql.Types.DATE:
                return java.sql.Date.class.getName();
            case java.sql.Types.TIME:
                return java.sql.Time.class.getName();
            case java.sql.Types.TIMESTAMP:
                return java.sql.Timestamp.class.getName();
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
                return byte[].class.getName();
            default:
                return String.class.getName();
        }
    }
}
