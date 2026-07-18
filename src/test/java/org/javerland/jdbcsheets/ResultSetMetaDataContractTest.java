package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.util.AbstractReader;
import org.javerland.jdbcsheets.util.Column;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResultSetMetaDataContractTest {

    @Test
    void xlsxMetadataUsesReaderColumnsAndMockitoCollaborator() throws Exception {
        AbstractReader reader = mock(AbstractReader.class);
        List<Column> columns = List.of(
                new Column("A", "alias", Types.BOOLEAN),
                new Column("B", Types.DOUBLE),
                new Column("C", Types.TIMESTAMP),
                new Column("D", Types.DATE),
                new Column("E", Types.JAVA_OBJECT),
                new Column("F", Types.VARCHAR));
        when(reader.getColumns()).thenReturn(columns);
        when(reader.getTableName()).thenReturn("Sheet1");
        ResultSetMetaData metadata = new JdbcSheetsResultSetMetaData(reader);

        Assertions.assertAll(
                () -> Assertions.assertEquals(6, metadata.getColumnCount()),
                () -> Assertions.assertEquals("alias", metadata.getColumnLabel(1)),
                () -> Assertions.assertEquals("A", metadata.getColumnName(1)),
                () -> Assertions.assertEquals("B", metadata.getColumnLabel(2)),
                () -> Assertions.assertEquals("Sheet1", metadata.getTableName(2)),
                () -> Assertions.assertEquals("BOOLEAN", metadata.getColumnTypeName(1)),
                () -> Assertions.assertEquals(Boolean.class.getName(), metadata.getColumnClassName(1)),
                () -> Assertions.assertEquals(Double.class.getName(), metadata.getColumnClassName(2)),
                () -> Assertions.assertEquals(java.sql.Timestamp.class.getName(), metadata.getColumnClassName(3)),
                () -> Assertions.assertEquals(java.sql.Date.class.getName(), metadata.getColumnClassName(4)),
                () -> Assertions.assertEquals(Object.class.getName(), metadata.getColumnClassName(5)),
                () -> Assertions.assertEquals(String.class.getName(), metadata.getColumnClassName(6)),
                () -> Assertions.assertTrue(metadata.isSigned(2)),
                () -> Assertions.assertFalse(metadata.isSigned(1)),
                () -> Assertions.assertTrue(metadata.isReadOnly(1)),
                () -> Assertions.assertFalse(metadata.isWritable(1)),
                () -> Assertions.assertSame(metadata, metadata.unwrap(ResultSetMetaData.class)),
                () -> Assertions.assertTrue(metadata.isWrapperFor(ResultSetMetaData.class)));
        Assertions.assertThrows(SQLException.class, () -> metadata.unwrap(String.class));
        assertEveryColumnMethodValidatesItsIndex(metadata);
    }

    @Test
    void systemMetadataInfersRuntimeAndSqlClasses() throws Exception {
        List<Column> columns = List.of(
                new Column("TEXT", "label", Types.VARCHAR),
                new Column("INTEGER", Types.INTEGER),
                new Column("BINARY", Types.VARBINARY));
        SystemResultSet populated = new SystemResultSet(columns,
                List.<Object[]>of(new Object[] { new StringBuilder("runtime"), 4, new byte[] { 1 } }));
        ResultSetMetaData populatedMetadata = populated.getMetaData();
        Assertions.assertEquals(StringBuilder.class.getName(), populatedMetadata.getColumnClassName(1));
        Assertions.assertEquals("label", populatedMetadata.getColumnLabel(1));

        SystemResultSet empty = new SystemResultSet(columns, List.of());
        ResultSetMetaData emptyMetadata = empty.getMetaData();
        Assertions.assertEquals(String.class.getName(), emptyMetadata.getColumnClassName(1));
        Assertions.assertEquals(Integer.class.getName(), emptyMetadata.getColumnClassName(2));
        Assertions.assertEquals(byte[].class.getName(), emptyMetadata.getColumnClassName(3));
        assertEveryColumnMethodValidatesItsIndex(emptyMetadata);
    }

    private void assertEveryColumnMethodValidatesItsIndex(ResultSetMetaData metadata) {
        Arrays.stream(metadata.getClass().getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getParameterCount() > 0 && method.getParameterTypes()[0] == int.class)
                .forEach(method -> {
                    Object[] arguments = defaultArguments(method);
                    arguments[0] = 0;
                    InvocationTargetException thrown = Assertions.assertThrows(InvocationTargetException.class,
                            () -> method.invoke(metadata, arguments), method.getName());
                    Assertions.assertInstanceOf(SQLException.class, thrown.getCause(), method.getName());
                });
    }

    private Object[] defaultArguments(Method method) {
        Object[] arguments = new Object[method.getParameterCount()];
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i] == int.class) {
                arguments[i] = 0;
            } else if (types[i] == boolean.class) {
                arguments[i] = false;
            }
        }
        return arguments;
    }
}
