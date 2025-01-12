/* Created on 22.12.2024 */
package org.javerland.jdbcsheets.util;

import org.javerland.jdbcsheets.enums.ReaderType;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author juraj.pacolt
 */
public abstract class AbstractReader {

    protected File file;
    List<Column> columns = new ArrayList<>();
    Integer offset = null;
    Integer limit = null;
    int index = 0;
    String tableName = null;

    public abstract List<String> getSheets();

    public abstract void parseQuery(String query) throws SQLException;

    public abstract void close();

    public abstract Object[] next();

    public abstract ReaderType getType();

    public abstract List<Column> listColumnsBySheetName(String sheetName);

    public int getRowIndex() {
        return index;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }

    public int getColumnIndexByName(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getAlias() != null && columnName.toUpperCase()
                    .equals(columns.get(i).getAlias().toUpperCase())) {
                return i;
            } else if (columnName.equals(columns.get(i).getName())) {
                return i;
            }
        }
        return -1;
    }
}
