/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.util.SqlTypeUtils;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class SqlTypeUtilsTest {

    @Test
    public void testVarcharType() {
        String typeName = SqlTypeUtils.toSqlType(Types.VARCHAR);
        assertEquals("VARCHAR", typeName);
    }

    @Test
    public void testIntegerType() {
        String typeName = SqlTypeUtils.toSqlType(Types.INTEGER);
        assertEquals("INTEGER", typeName);
    }

    @Test
    public void testDoubleType() {
        String typeName = SqlTypeUtils.toSqlType(Types.DOUBLE);
        assertEquals("DOUBLE", typeName);
    }

    @Test
    public void testDateType() {
        String typeName = SqlTypeUtils.toSqlType(Types.DATE);
        assertEquals("DATE", typeName);
    }

    @Test
    public void testTimestampType() {
        String typeName = SqlTypeUtils.toSqlType(Types.TIMESTAMP);
        assertEquals("TIMESTAMP", typeName);
    }

    @Test
    public void testBooleanType() {
        String typeName = SqlTypeUtils.toSqlType(Types.BOOLEAN);
        assertEquals("BOOLEAN", typeName);
    }

    @Test
    public void testBigintType() {
        String typeName = SqlTypeUtils.toSqlType(Types.BIGINT);
        assertEquals("BIGINT", typeName);
    }

    @Test
    public void testInvalidType() {
        String typeName = SqlTypeUtils.toSqlType(9999);
        assertNull(typeName);
    }

    @Test
    public void testNullType() {
        String typeName = SqlTypeUtils.toSqlType(Types.NULL);
        assertEquals("NULL", typeName);
    }
}
