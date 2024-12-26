/* Created on 22.12.2024 */
package org.javerland.jdbcsheets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author juraj.pacolt
 */
public abstract class AbstractReader {

    protected File file;
    List<String> columns = new ArrayList<>();
    Integer offset = null;
    Integer limit = null;
    int index = 0;

    public abstract List<String> getSheets();

    public abstract void parseQuery(String query);

    public abstract void close();

    public abstract Object[] next();

    public abstract ReaderType getType();
}
