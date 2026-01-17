/* Created on 21.12.2024 */
package org.javerland.jdbcsheets.util;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
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
    private final DataFormatter dataFormatter = new DataFormatter();
    private FormulaEvaluator formulaEvaluator = null;

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
        if (limit != null && offset != null && index >= (offset + Math.abs(limit))) {
            return null;
        }
        List<Object> result = new ArrayList<>();
        XSSFRow row = sheet.getRow(index);
        if (row == null) {
            return null;
        }
        columns.forEach(column -> {
            int idx = getColumnIndexFromName(column.getName());
            if (idx < 0) {
                result.add(null);
                return;
            }
            XSSFCell cell = row.getCell(idx);
            result.add(getCellValue(cell));
        });
        index++;
        return result.toArray(new Object[] {});
    }

    @Override
    public void parseQuery(String query) throws SQLException {
        try {
            columns.clear();
            offset = null;
            limit = null;
            index = 0;
            tableName = null;

            Select selectStatement = (Select) CCJSqlParserUtil.parse(query);
            PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

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
                offset = limitClause.getOffset() != null ? Integer.valueOf(limitClause.getOffset().toString()) : 0;
                index = offset;
                limit = limitClause.getRowCount() != null ?
                        Integer.valueOf(limitClause.getRowCount().toString()) :
                        null;
            }

            try {
                workbook = new XSSFWorkbook(Files.newInputStream(file.toPath()), false);
                formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                sheet = workbook.getSheet(tableName);
                if (sheet == null) {
                    throw new SQLException(String.format("Sheet with name \"%s\" doesn't exists.", tableName));
                }
            } catch (IOException ex) {
                throw new SQLException(ex.getMessage(), ex);
            }

            // Columns from sheet, if * all columns returned.
            for (SelectItem selectItem : plainSelect.getSelectItems()) {
                Expression expression = selectItem.getExpression();
                if (expression instanceof AllColumns) {
                    addAllColumns();
                } else if (expression instanceof AllTableColumns) {
                    AllTableColumns allTableColumns = (AllTableColumns) expression;
                    if (allTableColumns.getTable() != null &&
                            !allTableColumns.getTable().getName().equalsIgnoreCase(tableName)) {
                        throw new SQLException("Only columns from \"" + tableName + "\" are supported.");
                    }
                    addAllColumns();
                } else if (expression instanceof net.sf.jsqlparser.schema.Column) {
                    net.sf.jsqlparser.schema.Column column = (net.sf.jsqlparser.schema.Column) expression;
                    String columnName = column.getColumnName();
                    String alias = selectItem.getAlias() != null ? selectItem.getAlias().getName() : null;
                    columns.add(new Column(columnName.toUpperCase(), alias, Types.VARCHAR));
                } else {
                    throw new SQLException("Only column references or * are supported in SELECT.");
                }
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
    public List<Column> listColumnsBySheetName(String sheetName) {
        List<Column> result = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheet(sheetName);
            Row headerRow = getFirstNonEmptyRow(sheet);
            if (headerRow != null) {
                short lastCell = headerRow.getLastCellNum();
                for (int i = 0; i < lastCell; i++) {
                    result.add(new Column(getColumnName(i), null, Types.VARCHAR));
                }
            }
        } catch (IOException | InvalidFormatException ex) {
            throw new JdbcSheetsException(ex.getMessage(), ex);
        }
        return result;
    }

    String getColumnName(int columnIndex) {
        StringBuilder columnName = new StringBuilder();
        while (columnIndex >= 0) {
            int remainder = columnIndex % 26;
            columnName.insert(0, (char) (remainder + 'A'));
            columnIndex = (columnIndex / 26) - 1;
        }
        return columnName.toString();
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

    private void addAllColumns() {
        int columnCount = getMaxColumnCount();
        for (int i = 0; i < columnCount; i++) {
            columns.add(new Column(getColumnName(i), null, Types.VARCHAR));
        }
    }

    private int getMaxColumnCount() {
        Row firstRow = getFirstNonEmptyRow(sheet);
        if (firstRow == null) {
            return 0;
        }
        short lastCell = firstRow.getLastCellNum();
        return Math.max(0, lastCell);
    }

    private Row getFirstNonEmptyRow(Sheet sheet) {
        if (sheet == null) {
            return null;
        }
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 0; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getLastCellNum() > 0) {
                return row;
            }
        }
        return null;
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA && formulaEvaluator != null) {
            CellValue evaluated = formulaEvaluator.evaluate(cell);
            if (evaluated == null) {
                return null;
            }
            cellType = evaluated.getCellType();
            switch (cellType) {
                case BOOLEAN:
                    return evaluated.getBooleanValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue();
                    }
                    return evaluated.getNumberValue();
                case STRING:
                    return evaluated.getStringValue();
                case BLANK:
                    return null;
                case ERROR:
                default:
                    return null;
            }
        }
        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case BLANK:
                return null;
            case ERROR:
                return null;
            case FORMULA:
            default:
                return dataFormatter.formatCellValue(cell, formulaEvaluator);
        }
    }
}
