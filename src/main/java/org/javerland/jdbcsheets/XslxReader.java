/* Created on 21.12.2024 */
package org.javerland.jdbcsheets;

import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author juraj.pacolt
 */
class XslxReader extends AbstractReader {

    SqlParser.Config parserConfig = SqlParser.config()
            .withCaseSensitive(false);
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
        List<Object> result = new ArrayList<>();
        XSSFRow row = sheet.getRow(index);
        columns.forEach(column -> {
            int idx = getColumnIndexFromName(column);
            String value = row.getCell(idx).getStringCellValue();
            result.add(value);
        });
        index++;
        return result.toArray(new Object[]{});
    }

    @Override
    public void parseQuery(String query) {
        try {
            SqlParser parser = SqlParser.create(query, parserConfig);

            SqlSelect select = (SqlSelect) parser.parseQuery();

            // Columns from sheet, if * all columns returned.
            SqlNodeList selectList = select.getSelectList();
            for (SqlNode node : selectList) {
                if (node instanceof SqlIdentifier) {
                    SqlIdentifier ident = (SqlIdentifier) node;
                    String columnName = ident.names.get(0);
                    columns.add(columnName.toUpperCase());
                }
            }

            // Table is sheet ...
            SqlNode from = select.getFrom();
            SqlIdentifier tableName;
            if (from instanceof SqlIdentifier) {
                tableName = (SqlIdentifier) from;
            } else {
                throw new JdbcSheetsException("Others identifiers not supported now.");
            }

            // Where condition, simple filter for data from sheet.
            SqlNode where = select.getWhere();
            if (where != null) {
                // TODO For now we don't need, bud it's needed to finish to the future ...
            }

            offset = select.getOffset() != null ? Integer.valueOf(select.getOffset().toString()) : null;
            if (offset != null) {
                index = offset;
            }
            limit = select.getFetch() != null ? Integer.valueOf(select.getFetch().toString()) : null;

            try {
                workbook = new XSSFWorkbook(Files.newInputStream(file.toPath()), false);
                sheet = workbook.getSheet(tableName.getSimple());
            } catch (IOException ex) {
                throw new JdbcSheetsException(ex.getMessage(), ex);
            }

        } catch (SqlParseException ex) {
            throw new JdbcSheetsException(ex.getMessage(), ex);
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
