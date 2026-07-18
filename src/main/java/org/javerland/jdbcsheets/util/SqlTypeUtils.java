/* Created on 12.01.2025 */
package org.javerland.jdbcsheets.util;

import java.lang.reflect.Field;
import java.sql.Types;

/**
 * Utilities for displaying constants from {@link Types}.
 *
 * @author juraj.pacolt
 */
public final class SqlTypeUtils {

    private SqlTypeUtils() {
    }

    /**
     * Resolves a JDBC type constant to its field name.
     *
     * @param sqlType value from {@link Types}
     * @return constant name, or {@code null} when unknown
     */
    public static String toSqlType(final int sqlType) {
        for (Field field : Types.class.getFields()) {
            if (field.getType() == int.class) {
                try {
                    int typeValue = field.getInt(null);
                    if (typeValue == sqlType) {
                        return field.getName();
                    }
                } catch (IllegalAccessException ex) {
                    // Ignore this exception, as we are just trying to find a matching type
                }
            }
        }
        return null;
    }

}
