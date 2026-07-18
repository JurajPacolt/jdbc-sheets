package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.util.Column;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

class SystemResultSetTest {

    @Test
    void gettersCursorNavigationAndWasNullFollowJdbcContract() throws Exception {
        SystemResultSet resultSet = sampleResultSet();

        Assertions.assertTrue(resultSet.isBeforeFirst());
        Assertions.assertEquals(0, resultSet.getRow());
        Assertions.assertThrows(SQLException.class, () -> resultSet.getObject(1));
        Assertions.assertTrue(resultSet.next());
        Assertions.assertTrue(resultSet.isFirst());
        Assertions.assertEquals(1, resultSet.getRow());
        Assertions.assertEquals("alpha", resultSet.getString("TEXT"));
        Assertions.assertEquals(42, resultSet.getInt("NUMBER"));
        Assertions.assertEquals(new BigDecimal("42.00"), resultSet.getBigDecimal(2, 2));
        Assertions.assertTrue(resultSet.getBoolean("FLAG"));
        Assertions.assertArrayEquals("alpha".getBytes(StandardCharsets.UTF_8), resultSet.getBytes(1));
        try (InputStream stream = resultSet.getAsciiStream(1)) {
            Assertions.assertArrayEquals("alpha".getBytes(StandardCharsets.US_ASCII), stream.readAllBytes());
        }
        Assertions.assertNull(resultSet.getObject("OPTIONAL"));
        Assertions.assertTrue(resultSet.wasNull());
        Assertions.assertEquals(1, resultSet.findColumn("text"));
        Assertions.assertThrows(SQLException.class, () -> resultSet.findColumn("missing"));

        Assertions.assertTrue(resultSet.next());
        Assertions.assertTrue(resultSet.isLast());
        Assertions.assertEquals(7, resultSet.getObject("NUMBER", Integer.class));
        Assertions.assertFalse(resultSet.next());
        Assertions.assertTrue(resultSet.isAfterLast());
        Assertions.assertTrue(resultSet.previous());
        Assertions.assertEquals(2, resultSet.getRow());
        Assertions.assertTrue(resultSet.absolute(1));
        Assertions.assertTrue(resultSet.relative(1));
        Assertions.assertTrue(resultSet.last());
        resultSet.beforeFirst();
        Assertions.assertTrue(resultSet.first());
        resultSet.afterLast();
        Assertions.assertTrue(resultSet.isAfterLast());
        Assertions.assertTrue(resultSet.absolute(-1));
        Assertions.assertEquals(2, resultSet.getRow());
        Assertions.assertFalse(resultSet.absolute(0));
        Assertions.assertTrue(resultSet.isBeforeFirst());
    }

    @Test
    void fetchWrapperUnsupportedTypesAndClosedStateAreChecked() throws Exception {
        SystemResultSet resultSet = sampleResultSet();
        resultSet.setFetchSize(8);
        resultSet.setFetchDirection(ResultSet.FETCH_REVERSE);
        Assertions.assertEquals(8, resultSet.getFetchSize());
        Assertions.assertEquals(ResultSet.FETCH_UNKNOWN, resultSet.getFetchDirection());
        Assertions.assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, resultSet.getType());
        Assertions.assertEquals(ResultSet.CONCUR_READ_ONLY, resultSet.getConcurrency());
        Assertions.assertSame(resultSet, resultSet.unwrap(ResultSet.class));
        Assertions.assertTrue(resultSet.isWrapperFor(ResultSet.class));
        Assertions.assertThrows(SQLException.class, () -> resultSet.unwrap(String.class));
        Assertions.assertThrows(SQLException.class, () -> resultSet.setFetchSize(-1));
        Assertions.assertThrows(SQLException.class, () -> resultSet.setFetchDirection(Integer.MIN_VALUE));

        resultSet.next();
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> resultSet.getBlob(1));
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> resultSet.getArray("TEXT"));
        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> resultSet.getSQLXML(1));

        resultSet.close();
        Assertions.assertTrue(resultSet.isClosed());
        Assertions.assertAll(
                () -> Assertions.assertThrows(SQLException.class, resultSet::next),
                () -> Assertions.assertThrows(SQLException.class, resultSet::isFirst),
                () -> Assertions.assertThrows(SQLException.class, resultSet::isLast),
                () -> Assertions.assertThrows(SQLException.class, resultSet::beforeFirst),
                () -> Assertions.assertThrows(SQLException.class, resultSet::afterLast),
                () -> Assertions.assertThrows(SQLException.class, resultSet::getRow),
                () -> Assertions.assertThrows(SQLException.class, resultSet::wasNull));
    }

    @Test
    void everyDeclaredMutationMethodRejectsWrites() throws Exception {
        assertEveryMutationIsReadOnly(sampleResultSet());
    }

    static void assertEveryMutationIsReadOnly(ResultSet resultSet) {
        Set<String> rowMutations = Set.of("insertRow", "updateRow", "deleteRow", "refreshRow",
                "cancelRowUpdates", "moveToInsertRow", "moveToCurrentRow");
        Arrays.stream(resultSet.getClass().getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getName().startsWith("update") || rowMutations.contains(method.getName()))
                .forEach(method -> {
                    InvocationTargetException thrown = Assertions.assertThrows(InvocationTargetException.class,
                            () -> method.invoke(resultSet, defaultArguments(method)), method.toString());
                    Assertions.assertInstanceOf(SQLFeatureNotSupportedException.class, thrown.getCause(),
                            method.toString());
                });
    }

    private static Object[] defaultArguments(Method method) {
        Object[] values = new Object[method.getParameterCount()];
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i] == int.class) {
                values[i] = 1;
            } else if (types[i] == long.class) {
                values[i] = 1L;
            } else if (types[i] == short.class) {
                values[i] = (short) 1;
            } else if (types[i] == byte.class) {
                values[i] = (byte) 1;
            } else if (types[i] == float.class) {
                values[i] = 1.0f;
            } else if (types[i] == double.class) {
                values[i] = 1.0d;
            } else if (types[i] == boolean.class) {
                values[i] = true;
            } else if (types[i] == String.class) {
                values[i] = "TEXT";
            }
        }
        return values;
    }

    private SystemResultSet sampleResultSet() throws SQLException {
        List<Column> columns = List.of(
                new Column("TEXT", Types.VARCHAR),
                new Column("NUMBER", Types.INTEGER),
                new Column("FLAG", Types.BOOLEAN),
                new Column("OPTIONAL", Types.VARCHAR));
        return new SystemResultSet(columns, List.of(
                new Object[] { "alpha", 42, 1, null },
                new Object[] { "beta", 7, false, "present" }));
    }
}
