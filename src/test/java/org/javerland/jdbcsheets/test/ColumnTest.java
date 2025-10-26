/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.util.Column;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class ColumnTest {

    @Test
    public void testColumnConstructorWithAlias() {
        Column column = new Column("A", "ColA", Types.VARCHAR);
        assertEquals("A", column.getName());
        assertEquals("ColA", column.getAlias());
        assertEquals(Types.VARCHAR, column.getSqlType());
    }

    @Test
    public void testColumnConstructorWithoutAlias() {
        Column column = new Column("B", Types.INTEGER);
        assertEquals("B", column.getName());
        assertNull(column.getAlias());
        assertEquals(Types.INTEGER, column.getSqlType());
    }

    @Test
    public void testDefaultConstructor() {
        Column column = new Column();
        assertNull(column.getName());
        assertNull(column.getAlias());
        assertEquals(0, column.getSqlType());
    }

    @Test
    public void testSettersAndGetters() {
        Column column = new Column();
        column.setName("TestColumn");
        column.setAlias("TC");
        column.setSqlType(Types.DOUBLE);

        assertEquals("TestColumn", column.getName());
        assertEquals("TC", column.getAlias());
        assertEquals(Types.DOUBLE, column.getSqlType());
    }

    @Test
    public void testEquals() {
        Column column1 = new Column("A", "ColA", Types.VARCHAR);
        Column column2 = new Column("A", "ColA", Types.VARCHAR);
        Column column3 = new Column("B", "ColB", Types.INTEGER);

        assertEquals(column1, column2);
        assertNotEquals(column1, column3);
        assertNotEquals(column1, null);
        assertNotEquals(column1, "String");
    }

    @Test
    public void testHashCode() {
        Column column1 = new Column("A", "ColA", Types.VARCHAR);
        Column column2 = new Column("A", "ColA", Types.VARCHAR);

        assertEquals(column1.hashCode(), column2.hashCode());
    }

    @Test
    public void testEqualsWithNullAlias() {
        Column column1 = new Column("A", null, Types.VARCHAR);
        Column column2 = new Column("A", null, Types.VARCHAR);
        
        assertEquals(column1, column2);
    }

    @Test
    public void testEqualsDifferentSqlType() {
        Column column1 = new Column("A", "ColA", Types.VARCHAR);
        Column column2 = new Column("A", "ColA", Types.INTEGER);
        
        assertNotEquals(column1, column2);
    }
}
