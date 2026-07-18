/* Created on 27.12.2024 */
package org.javerland.jdbcsheets.util;

import java.util.Objects;

/**
 * Column descriptor used by JDBC result-set metadata.
 *
 * @author juraj.pacolt
 */
public class Column {

    private String name;
    private String alias;
    private int sqlType;

    /**
     * Creates a column descriptor.
     *
     * @param name source column name
     * @param alias optional projected alias
     * @param sqlType type from {@link java.sql.Types}
     */
    public Column(String name, String alias, int sqlType) {
        this.name = name;
        this.alias = alias;
        this.sqlType = sqlType;
    }

    /**
     * Creates an unaliased column descriptor.
     *
     * @param name source column name
     * @param sqlType type from {@link java.sql.Types}
     */
    public Column(String name, int sqlType) {
        this.name = name;
        this.alias = null;
        this.sqlType = sqlType;
    }

    /** Creates an empty mutable descriptor. */
    public Column() {
    }

    /**
     * Returns the source name.
     *
     * @return source column name
     */
    public String getName() {
        return name;
    }

    /**
     * Changes the source name.
     *
     * @param name source column name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the projected alias.
     *
     * @return projected alias, or {@code null}
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Changes the projected alias.
     *
     * @param alias projected alias, or {@code null}
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Returns the inferred JDBC type.
     *
     * @return type from {@link java.sql.Types}
     */
    public int getSqlType() {
        return sqlType;
    }

    /**
     * Changes the JDBC type.
     *
     * @param sqlType type from {@link java.sql.Types}
     */
    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Column column = (Column) o;
        return Objects.equals(name, column.name) && Objects.equals(alias, column.alias) && sqlType == column.sqlType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias, sqlType);
    }
}
