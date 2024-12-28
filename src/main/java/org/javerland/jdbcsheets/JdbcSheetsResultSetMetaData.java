/* Created on 15.12.2024 */
package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.util.AbstractReader;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

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
        return Integer.MAX_VALUE;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        String alias = reader.getColumns().get(column).getAlias();
        return alias == null ? reader.getColumns().get(column).getName() : alias;
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return reader.getColumns().get(column).getName();
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return "";
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return -1;
    }

    @Override
    public int getScale(int column) throws SQLException {
        return -1;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return reader.getTableName();
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return "";
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return reader.getColumns().get(0).getSqlType().id;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        return "VARCHAR";
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
        return String.class.getName();
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
