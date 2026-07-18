package org.javerland.jdbcsheets.util;

import org.javerland.jdbcsheets.enums.ReaderType;
import org.javerland.jdbcsheets.exception.JdbcSheetsException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

class UtilityContractTest {

    @Test
    void columnValueObjectAndSqlTypeLookupAreCovered() {
        Column column = new Column();
        column.setName("A");
        column.setAlias("alias");
        column.setSqlType(Types.INTEGER);

        Column equal = new Column("A", "alias", Types.INTEGER);
        Assertions.assertEquals(equal, column);
        Assertions.assertEquals(equal.hashCode(), column.hashCode());
        Assertions.assertNotEquals(column, new Column("A", Types.INTEGER));
        Assertions.assertNotEquals(column, null);
        Assertions.assertEquals("INTEGER", SqlTypeUtils.toSqlType(Types.INTEGER));
        Assertions.assertNull(SqlTypeUtils.toSqlType(Integer.MIN_VALUE));
    }

    @Test
    void abstractReaderFindsNamesAndAliasesCaseInsensitively() {
        StubReader reader = new StubReader();
        reader.columns.add(new Column("A", "First", Types.VARCHAR));
        reader.columns.add(new Column("B", Types.INTEGER));

        Assertions.assertEquals(0, reader.getColumnIndexByName("first"));
        Assertions.assertEquals(0, reader.getColumnIndexByName("a"));
        Assertions.assertEquals(1, reader.getColumnIndexByName("b"));
        Assertions.assertEquals(-1, reader.getColumnIndexByName("missing"));
        Assertions.assertEquals(-1, reader.getColumnIndexByName(null));
        Assertions.assertEquals(reader.columns, reader.getColumns());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> reader.getColumns().add(new Column("B", Types.VARCHAR)));
    }

    @Test
    void xlsxColumnCoordinatesAreConvertedInBothDirections() {
        XslxReader reader = new XslxReader(Path.of("unused.xlsx").toFile());
        Assertions.assertEquals("A", reader.getColumnName(0));
        Assertions.assertEquals("Z", reader.getColumnName(25));
        Assertions.assertEquals("AA", reader.getColumnName(26));
        Assertions.assertEquals("ZZ", reader.getColumnName(701));
        Assertions.assertEquals(0, reader.getColumnIndexFromName("a"));
        Assertions.assertEquals(26, reader.getColumnIndexFromName("AA"));
        Assertions.assertEquals(-1, reader.getColumnIndexFromName(""));
        Assertions.assertEquals(-1, reader.getColumnIndexFromName("A1"));
        Assertions.assertEquals(ReaderType.XLSX, reader.getType());
    }

    @Test
    void customExceptionConstructorsPreserveMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        Assertions.assertNull(new JdbcSheetsException().getMessage());
        Assertions.assertEquals("message", new JdbcSheetsException("message").getMessage());
        Assertions.assertSame(cause, new JdbcSheetsException(cause).getCause());
        JdbcSheetsException withBoth = new JdbcSheetsException("message", cause);
        Assertions.assertEquals("message", withBoth.getMessage());
        Assertions.assertSame(cause, withBoth.getCause());
    }

    private static final class StubReader extends AbstractReader {
        @Override
        public List<String> getSheets() {
            return List.of();
        }

        @Override
        public void parseQuery(String query) throws SQLException {
        }

        @Override
        public void close() {
        }

        @Override
        public Object[] next() {
            return null;
        }

        @Override
        public ReaderType getType() {
            return ReaderType.XLSX;
        }

        @Override
        public List<Column> listColumnsBySheetName(String sheetName) {
            return List.of();
        }
    }
}
