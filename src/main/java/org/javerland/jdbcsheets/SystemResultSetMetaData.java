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
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        return 0;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        return false;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        return 128;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        String alias = resultSet.getColumns().get(column).getAlias();
        return alias == null ? resultSet.getColumns().get(column).getName() : alias;
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return resultSet.getColumns().get(column).getName();
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return "";
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return 0;
    }

    @Override
    public int getScale(int column) throws SQLException {
        return 0;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return "";
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return "";
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return resultSet.getColumns().get(column).getSqlType();
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        int sqlType = resultSet.getColumns().get(column).getSqlType();
        return SqlTypeUtils.toSqlType(sqlType);
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        return false;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        if (resultSet.getData().size() > 0 && resultSet.getData().get(0)[column] != null) {
            return resultSet.getData().get(0)[column].getClass().getName();
        }
        int sqlType = resultSet.getColumns().get(column).getSqlType();
        return getColumnTypeName(sqlType);
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
