package org.javerland.jdbcsheets;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

/** Parameter metadata for dynamically typed spreadsheet queries. */
final class JdbcSheetsParameterMetaData implements ParameterMetaData {

    private final int parameterCount;

    JdbcSheetsParameterMetaData(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    @Override
    public int getParameterCount() {
        return parameterCount;
    }

    @Override
    public int isNullable(int parameter) throws SQLException {
        validate(parameter);
        return ParameterMetaData.parameterNullableUnknown;
    }

    @Override
    public boolean isSigned(int parameter) throws SQLException {
        validate(parameter);
        return true;
    }

    @Override
    public int getPrecision(int parameter) throws SQLException {
        validate(parameter);
        return 0;
    }

    @Override
    public int getScale(int parameter) throws SQLException {
        validate(parameter);
        return 0;
    }

    @Override
    public int getParameterType(int parameter) throws SQLException {
        validate(parameter);
        return Types.JAVA_OBJECT;
    }

    @Override
    public String getParameterTypeName(int parameter) throws SQLException {
        validate(parameter);
        return "JAVA_OBJECT";
    }

    @Override
    public String getParameterClassName(int parameter) throws SQLException {
        validate(parameter);
        return Object.class.getName();
    }

    @Override
    public int getParameterMode(int parameter) throws SQLException {
        validate(parameter);
        return ParameterMetaData.parameterModeIn;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    private void validate(int parameter) throws SQLException {
        if (parameter < 1 || parameter > parameterCount) {
            throw new SQLException("Invalid parameter index: " + parameter);
        }
    }
}
