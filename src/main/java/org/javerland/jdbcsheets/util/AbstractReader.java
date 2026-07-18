/* Created on 22.12.2024 */
package org.javerland.jdbcsheets.util;

import org.javerland.jdbcsheets.enums.ReaderType;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base contract shared by spreadsheet query readers.
 *
 * @author juraj.pacolt
 */
public abstract class AbstractReader {

    /** Spreadsheet file read by this instance. */
    protected File file;
    List<Column> columns = new ArrayList<>();
    Integer offset = null;
    Integer limit = null;
    int index = 0;
    String tableName = null;

    /** Creates an empty reader state for a concrete format implementation. */
    protected AbstractReader() {
    }

    /**
     * Lists workbook worksheets.
     *
     * @return worksheet names in workbook order
     */
    public abstract List<String> getSheets();

    /**
     * Parses a supported read-only query and initializes iteration.
     *
     * @param query SQL query
     * @throws SQLException if the query or workbook cannot be read
     */
    public abstract void parseQuery(String query) throws SQLException;

    /** Releases the underlying workbook resources. */
    public abstract void close();

    /**
     * Reads the next matching row.
     *
     * @return next projected row, or {@code null} when iteration is complete
     */
    public abstract Object[] next();

    /**
     * Identifies the spreadsheet format.
     *
     * @return spreadsheet format handled by this reader
     */
    public abstract ReaderType getType();

    /**
     * Lists columns exposed by a worksheet.
     *
     * @param sheetName worksheet name
     * @return worksheet columns in ordinal order
     */
    public abstract List<Column> listColumnsBySheetName(String sheetName);

    /**
     * Reports the current reader position.
     *
     * @return current physical row index
     */
    public int getRowIndex() {
        return index;
    }

    /**
     * Lists projected columns.
     *
     * @return unmodifiable view of columns projected by the current query
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Reports the selected worksheet.
     *
     * @return current worksheet name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Resolves a projected column name or alias without regard to case.
     *
     * @param columnName name or alias to resolve
     * @return zero-based index, or {@code -1} if no column matches
     */
    public int getColumnIndexByName(String columnName) {
        if (columnName == null) {
            return -1;
        }
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getAlias() != null && columnName.equalsIgnoreCase(columns.get(i).getAlias())) {
                return i;
            } else if (columnName.equalsIgnoreCase(columns.get(i).getName())) {
                return i;
            }
        }
        return -1;
    }
}
