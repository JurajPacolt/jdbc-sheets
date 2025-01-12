/* Created on 21.12.2024 */
package org.javerland.jdbcsheets.util;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javerland.jdbcsheets.enums.ReaderType;
import org.javerland.jdbcsheets.exception.JdbcSheetsException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author juraj.pacolt
 */
public class XslxReader extends AbstractReader {

    XSSFSheet sheet = null;
    XSSFWorkbook workbook = null;

    public XslxReader(File xlsxFile) {
        super();
        this.file = xlsxFile;
    }

    @Override
    public List<String> getSheets() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            Iterator<Sheet> sheetIterator = workbook.sheetIterator();
            List<String> sheetNames = new ArrayList<>();
            while (sheetIterator.hasNext()) {
                sheetNames.add(sheetIterator.next().getSheetName());
            }
            return sheetNames;
        } catch (IOException | InvalidFormatException ex) {
            throw new JdbcSheetsException(ex.getMessage(), ex);
        }
    }

    @Override
    public Object[] next() {
        if (offset != null && limit != null && index > (offset + Math.abs(limit - 1))) {
            return null;
        }
        List<Object> result = new ArrayList<>();
        XSSFRow row = sheet.getRow(index);
        if (row == null) {
            return null;
        }
        columns.forEach(column -> {
            int idx = getColumnIndexFromName(column.getName());
            XSSFCell cell = row.getCell(idx);
            String value = cell != null ? cell.getStringCellValue() : null;
            result.add(value);
        });
        index++;
        return result.toArray(new Object[] {});
    }

    @Override
    public void parseQuery(String query) throws SQLException {
        try {
            Select selectStatement = (Select) CCJSqlParserUtil.parse(query);
            PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

            // Columns from sheet, if * all columns returned.
            for (SelectItem selectItem : plainSelect.getSelectItems()) {
                if (selectItem.getExpression() instanceof Column) {
                    Column column = (Column) selectItem.getExpression();
                    String columnName = column.getName();
                    String alias = selectItem.getAlias() != null ? selectItem.getAlias().getName() : null;
                    columns.add(new Column(columnName.toUpperCase(), alias, Types.VARCHAR));
                } else {
                    String alias = selectItem.getAlias() != null ? selectItem.getAlias().getName() : null;
                    columns.add(new Column(alias.toUpperCase(), null, Types.VARCHAR));
                }
            }

            // Table is sheet ...
            FromItem from = plainSelect.getFromItem();
            Table table = null;
            if (from instanceof Table) {
                table = (Table) from;
                this.tableName = table.getName();
            } else {
                throw new JdbcSheetsException("Others identifiers not supported now.");
            }

            // Where condition, simple filter for data from sheet.
            Expression whereClause = plainSelect.getWhere();
            if (whereClause != null) {
                // TODO For now we don't need, bud it's needed to finish to the future ...
            }

            Limit limitClause = plainSelect.getLimit();
            if (limitClause != null) {
                offset = limitClause.getOffset() != null ? Integer.valueOf(limitClause.getOffset().toString()) : null;
                if (offset != null) {
                    index = offset;
                }
                limit = limitClause.getRowCount() != null ?
                        Integer.valueOf(limitClause.getRowCount().toString()) :
                        null;
            }

            try {
                workbook = new XSSFWorkbook(Files.newInputStream(file.toPath()), false);
                sheet = workbook.getSheet(tableName);
                if (sheet == null) {
                    throw new SQLException(String.format("Sheet with name \"%s\" doesn't exists.", tableName));
                }
            } catch (IOException ex) {
                throw new SQLException(ex.getMessage(), ex);
            }

        } catch (JSQLParserException ex) {
            throw new SQLException(ex.getMessage(), ex);
        }
    }

    @Override
    public void close() {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException ex) {
                throw new JdbcSheetsException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public ReaderType getType() {
        return ReaderType.XLSX;
    }

    int getColumnIndexFromName(String cn) {
        String columnName = cn.toUpperCase();
        if (columnName.isEmpty()) {
            return -1;
        }
        int index = 0;
        for (int i = 0; i < columnName.length(); i++) {
            char ch = columnName.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                return -1;
            }
            index = index * 26 + (ch - 'A' + 1);
        }
        return index - 1;
    }
}
