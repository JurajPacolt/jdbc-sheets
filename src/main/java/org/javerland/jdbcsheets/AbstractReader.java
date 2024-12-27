/* Created on 22.12.2024 */
package org.javerland.jdbcsheets;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public int getRowIndex() {
        return index;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }
}
