package org.javerland.jdbcsheets;

import org.javerland.jdbcsheets.util.Column;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Creates schema-correct result sets returned by {@link DatabaseMetaData}. */
final class JdbcMetadataResultSets {

    private JdbcMetadataResultSets() {
    }

    static ResultSet procedures() throws SQLException {
        return empty(names("PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "RESERVED1", "RESERVED2",
                "RESERVED3", "REMARKS", "PROCEDURE_TYPE", "SPECIFIC_NAME"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.SMALLINT, Types.VARCHAR));
    }

    static ResultSet procedureColumns() throws SQLException {
        return empty(names("PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "COLUMN_NAME", "COLUMN_TYPE",
                "DATA_TYPE", "TYPE_NAME", "PRECISION", "LENGTH", "SCALE", "RADIX", "NULLABLE", "REMARKS",
                "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION",
                "IS_NULLABLE", "SPECIFIC_NAME"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.INTEGER,
                        Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.SMALLINT, Types.SMALLINT, Types.SMALLINT,
                        Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER,
                        Types.VARCHAR, Types.VARCHAR));
    }

    static ResultSet schemas() throws SQLException {
        return empty(names("TABLE_SCHEM", "TABLE_CATALOG"), types(Types.VARCHAR, Types.VARCHAR));
    }

    static ResultSet catalogs() throws SQLException {
        return empty(names("TABLE_CAT"), types(Types.VARCHAR));
    }

    static ResultSet columnPrivileges() throws SQLException {
        return empty(names("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "GRANTOR", "GRANTEE",
                "PRIVILEGE", "IS_GRANTABLE"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR));
    }

    static ResultSet tablePrivileges() throws SQLException {
        return empty(names("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE",
                "IS_GRANTABLE"), types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR));
    }

    static ResultSet rowIdentifiers() throws SQLException {
        return empty(names("SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH",
                "DECIMAL_DIGITS", "PSEUDO_COLUMN"),
                types(Types.SMALLINT, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
                        Types.SMALLINT, Types.SMALLINT));
    }

    static ResultSet keyReferences() throws SQLException {
        return empty(names("PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT",
                "FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE",
                "FK_NAME", "PK_NAME", "DEFERRABILITY"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.SMALLINT, Types.SMALLINT, Types.VARCHAR,
                        Types.VARCHAR, Types.SMALLINT));
    }

    static ResultSet typeInfo() throws SQLException {
        List<Column> columns = columns(
                names("TYPE_NAME", "DATA_TYPE", "PRECISION", "LITERAL_PREFIX", "LITERAL_SUFFIX", "CREATE_PARAMS",
                        "NULLABLE", "CASE_SENSITIVE", "SEARCHABLE", "UNSIGNED_ATTRIBUTE", "FIXED_PREC_SCALE",
                        "AUTO_INCREMENT", "LOCAL_TYPE_NAME", "MINIMUM_SCALE", "MAXIMUM_SCALE", "SQL_DATA_TYPE",
                        "SQL_DATETIME_SUB", "NUM_PREC_RADIX"),
                types(Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.SMALLINT, Types.BOOLEAN, Types.SMALLINT, Types.BOOLEAN, Types.BOOLEAN, Types.BOOLEAN,
                        Types.VARCHAR, Types.SMALLINT, Types.SMALLINT, Types.INTEGER, Types.INTEGER, Types.INTEGER));
        List<Object[]> data = new ArrayList<>();
        data.add(type("BOOLEAN", Types.BOOLEAN, 1, false, false, 2));
        data.add(type("DOUBLE", Types.DOUBLE, 15, false, true, 10));
        data.add(type("TIMESTAMP", Types.TIMESTAMP, 29, false, false, 10));
        data.add(type("VARCHAR", Types.VARCHAR, Integer.MAX_VALUE, true, false, 10));
        data.add(type("JAVA_OBJECT", Types.JAVA_OBJECT, 0, false, false, 10));
        return new SystemResultSet(columns, data);
    }

    static ResultSet indexInfo() throws SQLException {
        return empty(names("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE", "INDEX_QUALIFIER",
                "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC", "CARDINALITY", "PAGES",
                "FILTER_CONDITION"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR, Types.VARCHAR,
                        Types.SMALLINT, Types.SMALLINT, Types.VARCHAR, Types.VARCHAR, Types.BIGINT, Types.BIGINT,
                        Types.VARCHAR));
    }

    static ResultSet udts() throws SQLException {
        return empty(names("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "CLASS_NAME", "DATA_TYPE", "REMARKS",
                "BASE_TYPE"), types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                        Types.VARCHAR, Types.SMALLINT));
    }

    static ResultSet superTypes() throws SQLException {
        return empty(names("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SUPERTYPE_CAT", "SUPERTYPE_SCHEM",
                "SUPERTYPE_NAME"), types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR));
    }

    static ResultSet superTables() throws SQLException {
        return empty(names("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR));
    }

    static ResultSet attributes() throws SQLException {
        return empty(names("TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME", "DATA_TYPE", "ATTR_TYPE_NAME",
                "ATTR_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS", "ATTR_DEF",
                "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE",
                "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR,
                        Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.SMALLINT));
    }

    static ResultSet clientInfoProperties() throws SQLException {
        List<Column> columns = columns(names("NAME", "MAX_LEN", "DEFAULT_VALUE", "DESCRIPTION"),
                types(Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR));
        List<Object[]> data = List.of(
                new Object[] { DriverInfo.PROP_FILE, Integer.MAX_VALUE, null, "Path to an XLSX workbook" },
                new Object[] { DriverInfo.PROP_DIRECTORY, Integer.MAX_VALUE, ".", "Directory containing workbook" },
                new Object[] { DriverInfo.PROP_DATABASE, Integer.MAX_VALUE, null, "Workbook file name" },
                new Object[] { DriverInfo.PROP_HEADER, 5, "false", "Use first worksheet row as column names" });
        return new SystemResultSet(columns, data);
    }

    static ResultSet functions() throws SQLException {
        return empty(names("FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "REMARKS", "FUNCTION_TYPE",
                "SPECIFIC_NAME"), types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT,
                        Types.VARCHAR));
    }

    static ResultSet functionColumns() throws SQLException {
        return empty(names("FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "COLUMN_NAME", "COLUMN_TYPE",
                "DATA_TYPE", "TYPE_NAME", "PRECISION", "LENGTH", "SCALE", "RADIX", "NULLABLE", "REMARKS",
                "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SPECIFIC_NAME"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.INTEGER,
                        Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.SMALLINT, Types.SMALLINT, Types.SMALLINT,
                        Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR));
    }

    static ResultSet pseudoColumns() throws SQLException {
        return empty(names("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "COLUMN_SIZE",
                "DECIMAL_DIGITS", "NUM_PREC_RADIX", "COLUMN_USAGE", "REMARKS", "CHAR_OCTET_LENGTH", "IS_NULLABLE"),
                types(Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR));
    }

    private static ResultSet empty(String[] names, int[] types) throws SQLException {
        return new SystemResultSet(columns(names, types), List.of());
    }

    private static List<Column> columns(String[] names, int[] types) {
        if (names.length != types.length) {
            throw new IllegalArgumentException("Metadata names and types must have equal length.");
        }
        List<Column> columns = new ArrayList<>(names.length);
        for (int i = 0; i < names.length; i++) {
            columns.add(new Column(names[i], types[i]));
        }
        return columns;
    }

    private static Object[] type(String name, int type, int precision, boolean caseSensitive, boolean unsigned,
            int radix) {
        return new Object[] { name, type, precision, null, null, null, DatabaseMetaData.typeNullable,
                caseSensitive, DatabaseMetaData.typeSearchable, unsigned, false, false, null, 0, 0, 0, 0, radix };
    }

    private static String[] names(String... values) {
        return values;
    }

    private static int[] types(int... values) {
        return values;
    }
}
