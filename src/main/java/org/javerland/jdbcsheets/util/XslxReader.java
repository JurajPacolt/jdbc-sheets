/* Created on 21.12.2024 */
package org.javerland.jdbcsheets.util;

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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author juraj.pacolt
 */
public class XslxReader extends AbstractReader {

    XSSFSheet sheet = null;
    XSSFWorkbook workbook = null;
    private final DataFormatter dataFormatter = new DataFormatter();
    private FormulaEvaluator formulaEvaluator = null;
    private static volatile boolean formulaEvalAvailable = true;
    private final List<SelectExpression> selectExpressions = new ArrayList<>();

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
        selectExpressions.forEach(expr -> result.add(expr.evaluate(row)));
        index++;
        return result.toArray(new Object[] {});
    }

    @Override
    public void parseQuery(String query) throws SQLException {
        columns.clear();
        selectExpressions.clear();
        offset = null;
        limit = null;
        index = 0;
        tableName = null;

        ParsedQuery parsed = parseSimpleQuery(query);
        tableName = parsed.tableName;
        if (parsed.limit != null) {
            limit = parsed.limit;
            offset = parsed.offset != null ? parsed.offset : 0;
            index = offset;
        } else if (parsed.offset != null) {
            offset = parsed.offset;
            index = offset;
        }

        try {
            workbook = new XSSFWorkbook(Files.newInputStream(file.toPath()), false);
            formulaEvaluator = null;
            if (formulaEvalAvailable) {
                try {
                    formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                } catch (Throwable ex) {
                    formulaEvalAvailable = false;
                    formulaEvaluator = null;
                }
            }
            sheet = workbook.getSheet(tableName);
            if (sheet == null) {
                throw new SQLException(String.format("Sheet with name \"%s\" doesn't exists.", tableName));
            }
        } catch (IOException ex) {
            throw new SQLException(ex.getMessage(), ex);
        }

        for (String item : parsed.selectItems) {
            applySelectItem(item, parsed.tableAlias);
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
            addSimpleColumn(getColumnName(i), null);
        }
    }

    private void applySelectItem(String rawItem, String tableAlias) throws SQLException {
        String item = rawItem.trim();
        if (item.isEmpty()) {
            throw new SQLException("Empty column in SELECT.");
        }
        if ("*".equals(item)) {
            addAllColumns();
            return;
        }
        if (item.endsWith(".*")) {
            String prefix = stripQuotes(item.substring(0, item.length() - 2).trim());
            if (!prefix.isEmpty() && !isTableMatch(prefix, tableAlias, tableName)) {
                throw new SQLException("Only columns from \"" + tableName + "\" are supported.");
            }
            addAllColumns();
            return;
        }

        String expression = item;
        String alias = null;
        Matcher asMatcher = Pattern.compile("(?is)^(.+?)\\s+as\\s+(.+)$").matcher(item);
        if (asMatcher.matches()) {
            expression = asMatcher.group(1).trim();
            alias = asMatcher.group(2).trim();
        } else {
            String[] parts = item.trim().split("\\s+");
            if (parts.length == 2) {
                expression = parts[0].trim();
                alias = parts[1].trim();
            } else if (parts.length > 2) {
                throw new SQLException("Only simple column references are supported in SELECT.");
            }
        }

        alias = stripQuotes(alias);
        expression = expression.trim();

        if (expression.contains("||")) {
            addConcatExpression(expression, alias, tableAlias);
            return;
        }

        String columnName = extractColumnName(expression, tableAlias);
        if (!isColumnName(columnName)) {
            throw new SQLException("Only column references or * are supported in SELECT.");
        }
        addSimpleColumn(columnName.toUpperCase(Locale.ROOT), alias);
    }

    private void addSimpleColumn(String columnName, String alias) {
        Column column = new Column(columnName, alias, Types.VARCHAR);
        columns.add(column);
        selectExpressions.add(new SelectExpression(column, row -> {
            int idx = getColumnIndexFromName(columnName);
            if (idx < 0) {
                return null;
            }
            XSSFCell cell = row.getCell(idx);
            return getCellValue(cell);
        }));
    }

    private void addConcatExpression(String expression, String alias, String tableAlias) throws SQLException {
        List<ConcatPart> parts = splitConcatParts(expression, tableAlias);
        String name = alias != null ? alias : expression.trim();
        Column column = new Column(name, alias, Types.VARCHAR);
        columns.add(column);
        selectExpressions.add(new SelectExpression(column, row -> {
            StringBuilder sb = new StringBuilder();
            boolean hasValue = false;
            for (ConcatPart part : parts) {
                if (part.isLiteral) {
                    sb.append(part.value);
                    hasValue = true;
                } else {
                    int idx = getColumnIndexFromName(part.value);
                    if (idx < 0) {
                        continue;
                    }
                    XSSFCell cell = row.getCell(idx);
                    Object val = getCellValue(cell);
                    if (val != null) {
                        sb.append(val.toString());
                        hasValue = true;
                    }
                }
            }
            return hasValue ? sb.toString() : null;
        }));
    }

    private String extractColumnName(String expression, String tableAlias) throws SQLException {
        String columnName = expression;
        int dotIdx = expression.lastIndexOf('.');
        if (dotIdx >= 0) {
            String prefix = stripQuotes(expression.substring(0, dotIdx).trim());
            columnName = expression.substring(dotIdx + 1).trim();
            if (!prefix.isEmpty() && !isTableMatch(prefix, tableAlias, tableName)) {
                throw new SQLException("Only columns from \"" + tableName + "\" are supported.");
            }
        }
        return stripQuotes(columnName);
    }

    private List<ConcatPart> splitConcatParts(String expression, String tableAlias) throws SQLException {
        List<ConcatPart> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (inString) {
                if (ch == '\'') {
                    if (i + 1 < expression.length() && expression.charAt(i + 1) == '\'') {
                        current.append('\'');
                        i++;
                    } else {
                        inString = false;
                        current.append(ch);
                    }
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (ch == '\'') {
                inString = true;
                current.append(ch);
                continue;
            }
            if (ch == '|' && i + 1 < expression.length() && expression.charAt(i + 1) == '|') {
                addConcatPart(parts, current.toString().trim(), tableAlias);
                current.setLength(0);
                i++;
                continue;
            }
            current.append(ch);
        }
        if (inString) {
            throw new SQLException("Unterminated string literal in SELECT.");
        }
        addConcatPart(parts, current.toString().trim(), tableAlias);
        if (parts.isEmpty()) {
            throw new SQLException("Empty expression in SELECT.");
        }
        return parts;
    }

    private void addConcatPart(List<ConcatPart> parts, String token, String tableAlias) throws SQLException {
        if (token.isEmpty()) {
            throw new SQLException("Invalid concatenation expression.");
        }
        if (token.startsWith("'") && token.endsWith("'") && token.length() >= 2) {
            String literal = token.substring(1, token.length() - 1).replace("''", "'");
            parts.add(new ConcatPart(true, literal));
            return;
        }
        String columnName = extractColumnName(token, tableAlias);
        if (!isColumnName(columnName)) {
            throw new SQLException("Only column references or string literals are supported in concatenation.");
        }
        parts.add(new ConcatPart(false, columnName.toUpperCase(Locale.ROOT)));
    }

    private boolean isTableMatch(String prefix, String tableAlias, String tableName) {
        if (tableAlias != null && prefix.equalsIgnoreCase(tableAlias)) {
            return true;
        }
        return tableName != null && prefix.equalsIgnoreCase(tableName);
    }

    private boolean isColumnName(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                if (ch < 'a' || ch > 'z') {
                    return false;
                }
            }
        }
        return true;
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2) {
            return trimmed;
        }
        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'') || (first == '`' && last == '`')) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (first == '[' && last == ']') {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private ParsedQuery parseSimpleQuery(String query) throws SQLException {
        if (query == null) {
            throw new SQLException("Query is null.");
        }
        String sql = query.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        Matcher selectMatcher = Pattern.compile("(?is)^select\\s+(.+?)\\s+from\\s+(.+)$").matcher(sql);
        if (!selectMatcher.find()) {
            throw new SQLException("Only SELECT queries are supported.");
        }

        String selectPart = selectMatcher.group(1).trim();
        if (selectPart.toLowerCase(Locale.ROOT).startsWith("distinct ")) {
            throw new SQLException("DISTINCT is not supported.");
        }
        if (selectPart.toLowerCase(Locale.ROOT).startsWith("all ")) {
            throw new SQLException("ALL is not supported.");
        }

        ParsedQuery parsed = new ParsedQuery();
        String fromPart = selectMatcher.group(2).trim();
        parseFromPart(fromPart, parsed);

        parsed.selectItems = splitSelectItems(selectPart);
        if (parsed.selectItems.isEmpty()) {
            throw new SQLException("No columns in SELECT.");
        }
        return parsed;
    }

    private void parseFromPart(String fromPart, ParsedQuery parsed) throws SQLException {
        if (fromPart.isEmpty()) {
            throw new SQLException("Missing FROM table name.");
        }

        String remainder;
        String tableToken;
        if (fromPart.startsWith("\"") || fromPart.startsWith("`") || fromPart.startsWith("'") || fromPart.startsWith("[")) {
            char quote = fromPart.charAt(0);
            char endQuote = quote == '[' ? ']' : quote;
            int endIdx = fromPart.indexOf(endQuote, 1);
            if (endIdx < 0) {
                throw new SQLException("Unterminated quoted table name.");
            }
            tableToken = fromPart.substring(1, endIdx);
            remainder = fromPart.substring(endIdx + 1).trim();
        } else {
            int spaceIdx = indexOfWhitespace(fromPart);
            if (spaceIdx < 0) {
                tableToken = fromPart;
                remainder = "";
            } else {
                tableToken = fromPart.substring(0, spaceIdx);
                remainder = fromPart.substring(spaceIdx).trim();
            }
        }

        parsed.tableName = stripQuotes(tableToken);
        if (parsed.tableName == null || parsed.tableName.isEmpty()) {
            throw new SQLException("Missing FROM table name.");
        }

        if (!remainder.isEmpty()) {
            String[] tokens = remainder.split("\\s+", 2);
            String first = tokens[0];
            String rest = tokens.length > 1 ? tokens[1].trim() : "";
            if (!isKeyword(first)) {
                parsed.tableAlias = stripQuotes(first);
                remainder = rest;
            }
        }

        parseLimitOffset(remainder, parsed);
    }

    private void parseLimitOffset(String remainder, ParsedQuery parsed) throws SQLException {
        if (remainder == null || remainder.isEmpty()) {
            return;
        }
        String lower = remainder.toLowerCase(Locale.ROOT);
        if (containsUnsupportedClauses(lower)) {
            throw new SQLException("Only SELECT, FROM, LIMIT, OFFSET are supported.");
        }

        Matcher limitMatcher = Pattern.compile("(?is)\\blimit\\s+(\\d+)").matcher(lower);
        if (limitMatcher.find()) {
            parsed.limit = Integer.valueOf(limitMatcher.group(1));
        }
        Matcher offsetMatcher = Pattern.compile("(?is)\\boffset\\s+(\\d+)").matcher(lower);
        if (offsetMatcher.find()) {
            parsed.offset = Integer.valueOf(offsetMatcher.group(1));
        }

        String stripped = lower.replaceAll("(?is)\\blimit\\s+\\d+", "")
                .replaceAll("(?is)\\boffset\\s+\\d+", "").trim();
        if (!stripped.isEmpty()) {
            throw new SQLException("Only SELECT, FROM, LIMIT, OFFSET are supported.");
        }
    }

    private boolean containsUnsupportedClauses(String lower) {
        return lower.matches("(?is).*\\b(where|group\\s+by|order\\s+by|having|join|union|intersect|except)\\b.*");
    }

    private boolean isKeyword(String token) {
        if (token == null) {
            return false;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.equals("limit") || lower.equals("offset") || lower.equals("where")
                || lower.equals("group") || lower.equals("order") || lower.equals("having")
                || lower.equals("join") || lower.equals("union") || lower.equals("intersect")
                || lower.equals("except");
    }

    private int indexOfWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private List<String> splitSelectItems(String selectPart) throws SQLException {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = 0; i < selectPart.length(); i++) {
            char ch = selectPart.charAt(i);
            if (inQuote) {
                if (ch == quoteChar) {
                    inQuote = false;
                }
                current.append(ch);
                continue;
            }
            if (ch == '"' || ch == '\'' || ch == '`') {
                inQuote = true;
                quoteChar = ch;
                current.append(ch);
                continue;
            }
            if (ch == ',') {
                String item = current.toString().trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (inQuote) {
            throw new SQLException("Unterminated quote in SELECT.");
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) {
            items.add(last);
        }
        return items;
    }

    private static class ParsedQuery {
        private String tableName;
        private String tableAlias;
        private Integer limit;
        private Integer offset;
        private List<String> selectItems;
    }

    private static class SelectExpression {
        private final Column column;
        private final RowValueProvider provider;

        private SelectExpression(Column column, RowValueProvider provider) {
            this.column = column;
            this.provider = provider;
        }

        private Object evaluate(XSSFRow row) {
            return provider.getValue(row);
        }
    }

    private interface RowValueProvider {
        Object getValue(XSSFRow row);
    }

    private static class ConcatPart {
        private final boolean isLiteral;
        private final String value;

        private ConcatPart(boolean isLiteral, String value) {
            this.isLiteral = isLiteral;
            this.value = value;
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
                return getCachedFormulaValue(cell);
            default:
                return dataFormatter.formatCellValue(cell, formulaEvaluator);
        }
    }

    private Object getCachedFormulaValue(Cell cell) {
        CellType cachedType = cell.getCachedFormulaResultType();
        switch (cachedType) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case STRING:
                return cell.getStringCellValue();
            case BLANK:
            case ERROR:
            default:
                return null;
        }
    }
}
