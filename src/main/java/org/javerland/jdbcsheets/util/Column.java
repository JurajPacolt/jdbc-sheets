/* Created on 27.12.2024 */
package org.javerland.jdbcsheets.util;

import org.apache.calcite.avatica.SqlType;

import java.util.Objects;

/**
 * @author juraj.pacolt
 */
public class Column {

    private String name;
    private String alias;
    private SqlType sqlType;

    public Column(String name, String alias, SqlType sqlType) {
        this.name = name;
        this.alias = alias;
        this.sqlType = sqlType;
    }

    public Column() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public SqlType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Column column = (Column) o;
        return Objects.equals(name, column.name) && Objects.equals(alias, column.alias) && sqlType == column.sqlType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias, sqlType);
    }
}
