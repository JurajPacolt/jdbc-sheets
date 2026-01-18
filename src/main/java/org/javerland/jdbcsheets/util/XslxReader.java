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
import java.math.BigDecimal;
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
    private RowFilter rowFilter = row -> true;
    private int matchedRows = 0;
    private int returnedRows = 0;

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
        if (limit != null && returnedRows >= limit) {
            return null;
        }
        while (true) {
            XSSFRow row = sheet.getRow(index);
            if (row == null) {
                return null;
            }
            index++;
            if (!rowFilter.matches(row)) {
                continue;
            }
            if (offset != null && matchedRows < offset) {
                matchedRows++;
                continue;
            }
            matchedRows++;
            if (limit != null && returnedRows >= limit) {
                return null;
            }
            List<Object> result = new ArrayList<>();
            selectExpressions.forEach(expr -> result.add(expr.evaluate(row)));
            returnedRows++;
            return result.toArray(new Object[] {});
        }
    }

    @Override
    public void parseQuery(String query) throws SQLException {
        columns.clear();
        selectExpressions.clear();
        rowFilter = row -> true;
        matchedRows = 0;
        returnedRows = 0;
        offset = null;
        limit = null;
        index = 0;
        tableName = null;

        ParsedQuery parsed = parseSimpleQuery(query);
        tableName = parsed.tableName;
        rowFilter = parsed.rowFilter != null ? parsed.rowFilter : row -> true;
        if (parsed.limit != null) {
            limit = parsed.limit;
        }
        if (parsed.offset != null) {
            offset = parsed.offset;
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
            AliasSplit split = splitImplicitAlias(item);
            if (split != null) {
                expression = split.expression;
                alias = split.alias;
            }
        }

        alias = stripQuotes(alias);
        expression = expression.trim();

        if (expression.contains("||")) {
            addConcatExpression(expression, alias, tableAlias);
            return;
        }

        FunctionCall functionCall = parseFunctionCall(expression, tableAlias, tableName);
        if (functionCall != null) {
            addFunctionExpression(functionCall, alias, expression);
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
        selectExpressions.add(new SelectExpression(column, createColumnProvider(columnName)));
    }

    private void addFunctionExpression(FunctionCall functionCall, String alias, String expression) {
        String name = alias != null ? alias : expression.trim();
        Column column = new Column(name, alias, Types.VARCHAR);
        columns.add(column);
        selectExpressions.add(new SelectExpression(column, row -> evaluateFunction(functionCall, row)));
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
                    sb.append(part.literal);
                    hasValue = true;
                } else {
                    Object val = part.provider.getValue(row);
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
        return extractColumnName(expression, tableAlias, tableName);
    }

    private String extractColumnName(String expression, String tableAlias, String tableNameValue) throws SQLException {
        String columnName = expression;
        int dotIdx = expression.lastIndexOf('.');
        if (dotIdx >= 0) {
            String prefix = stripQuotes(expression.substring(0, dotIdx).trim());
            columnName = expression.substring(dotIdx + 1).trim();
            if (!prefix.isEmpty() && !isTableMatch(prefix, tableAlias, tableNameValue)) {
                throw new SQLException("Only columns from \"" + tableNameValue + "\" are supported.");
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
            parts.add(ConcatPart.literal(literal));
            return;
        }

        FunctionCall functionCall = parseFunctionCall(token, tableAlias, tableName);
        if (functionCall != null) {
            parts.add(ConcatPart.provider(row -> evaluateFunction(functionCall, row)));
            return;
        }
        String columnName = extractColumnName(token, tableAlias);
        if (!isColumnName(columnName)) {
            throw new SQLException("Only column references or string literals are supported in concatenation.");
        }
        parts.add(ConcatPart.provider(createColumnProvider(columnName.toUpperCase(Locale.ROOT))));
    }

    private RowValueProvider createColumnProvider(String columnName) {
        return row -> {
            int idx = getColumnIndexFromName(columnName);
            if (idx < 0) {
                return null;
            }
            XSSFCell cell = row.getCell(idx);
            return getCellValue(cell);
        };
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

        parseWhereLimitOffset(remainder, parsed);
    }

    private void parseWhereLimitOffset(String remainder, ParsedQuery parsed) throws SQLException {
        if (remainder == null || remainder.isEmpty()) {
            return;
        }
        String lower = remainder.toLowerCase(Locale.ROOT);
        if (containsUnsupportedClauses(lower)) {
            throw new SQLException("Only SELECT, FROM, WHERE, LIMIT, OFFSET are supported.");
        }

        Clause whereClause = findClause(remainder, "where");
        Clause limitClause = findClause(remainder, "limit");
        Clause offsetClause = findClause(remainder, "offset");

        List<Clause> clauses = new ArrayList<>();
        if (whereClause != null) {
            clauses.add(whereClause);
        }
        if (limitClause != null) {
            clauses.add(limitClause);
        }
        if (offsetClause != null) {
            clauses.add(offsetClause);
        }
        clauses.sort((a, b) -> Integer.compare(a.start, b.start));

        if (clauses.isEmpty()) {
            throw new SQLException("Only SELECT, FROM, WHERE, LIMIT, OFFSET are supported.");
        }
        if (clauses.get(0).start > 0 && !remainder.substring(0, clauses.get(0).start).trim().isEmpty()) {
            throw new SQLException("Only SELECT, FROM, WHERE, LIMIT, OFFSET are supported.");
        }

        for (int i = 0; i < clauses.size(); i++) {
            Clause clause = clauses.get(i);
            int contentStart = clause.start + clause.keyword.length();
            int contentEnd = (i + 1 < clauses.size()) ? clauses.get(i + 1).start : remainder.length();
            String content = remainder.substring(contentStart, contentEnd).trim();
            if (content.isEmpty()) {
                throw new SQLException("Empty " + clause.keyword.toUpperCase(Locale.ROOT) + " clause.");
            }
            if ("where".equals(clause.keyword)) {
                parsed.rowFilter = parseWhereClause(content, parsed.tableAlias, parsed.tableName);
            } else if ("limit".equals(clause.keyword)) {
                parsed.limit = parsePositiveInt(content, "LIMIT");
            } else if ("offset".equals(clause.keyword)) {
                parsed.offset = parsePositiveInt(content, "OFFSET");
            }
        }
    }

    private boolean containsUnsupportedClauses(String lower) {
        return containsKeywordOutsideQuotes(lower, "group")
                || containsKeywordOutsideQuotes(lower, "order")
                || containsKeywordOutsideQuotes(lower, "having")
                || containsKeywordOutsideQuotes(lower, "join")
                || containsKeywordOutsideQuotes(lower, "union")
                || containsKeywordOutsideQuotes(lower, "intersect")
                || containsKeywordOutsideQuotes(lower, "except");
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

    private AliasSplit splitImplicitAlias(String item) {
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        boolean inBracket = false;
        int parenDepth = 0;
        int lastSpace = -1;
        for (int i = 0; i < item.length(); i++) {
            char ch = item.charAt(i);
            if (inSingle) {
                if (ch == '\'') {
                    if (i + 1 < item.length() && item.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        inSingle = false;
                    }
                }
                continue;
            }
            if (inDouble) {
                if (ch == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (inBacktick) {
                if (ch == '`') {
                    inBacktick = false;
                }
                continue;
            }
            if (inBracket) {
                if (ch == ']') {
                    inBracket = false;
                }
                continue;
            }
            if (ch == '\'') {
                inSingle = true;
                continue;
            }
            if (ch == '"') {
                inDouble = true;
                continue;
            }
            if (ch == '`') {
                inBacktick = true;
                continue;
            }
            if (ch == '[') {
                inBracket = true;
                continue;
            }
            if (ch == '(') {
                parenDepth++;
                continue;
            }
            if (ch == ')' && parenDepth > 0) {
                parenDepth--;
                continue;
            }
            if (parenDepth == 0 && Character.isWhitespace(ch)) {
                lastSpace = i;
            }
        }
        if (lastSpace < 0) {
            return null;
        }
        String expr = item.substring(0, lastSpace).trim();
        String alias = item.substring(lastSpace).trim();
        if (expr.isEmpty() || alias.isEmpty()) {
            return null;
        }
        if (expr.endsWith("||") || expr.endsWith("|")) {
            return null;
        }
        if (!isAliasToken(alias)) {
            return null;
        }
        return new AliasSplit(expr, alias);
    }

    private boolean isAliasToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        char first = token.charAt(0);
        char last = token.charAt(token.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'') || (first == '`' && last == '`')) {
            return token.length() > 1;
        }
        if (first == '[' && last == ']') {
            return token.length() > 1;
        }
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (i == 0) {
                if (!Character.isLetter(ch) && ch != '_') {
                    return false;
                }
            } else if (!Character.isLetterOrDigit(ch) && ch != '_') {
                return false;
            }
        }
        return true;
    }

    private FunctionCall parseFunctionCall(String expression, String tableAlias, String tableNameValue)
            throws SQLException {
        String trimmed = expression.trim();
        Matcher matcher = Pattern.compile("(?is)^(lower|upper)\\s*\\((.+)\\)$").matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }
        String function = matcher.group(1).trim().toLowerCase(Locale.ROOT);
        String argument = matcher.group(2).trim();
        if (argument.isEmpty()) {
            throw new SQLException("Function argument is empty.");
        }
        if (argument.startsWith("'") && argument.endsWith("'") && argument.length() >= 2) {
            String literal = argument.substring(1, argument.length() - 1).replace("''", "'");
            return new FunctionCall(FunctionType.fromName(function), literal, true);
        }
        String columnName = extractColumnName(argument, tableAlias, tableNameValue);
        if (!isColumnName(columnName)) {
            throw new SQLException("Only column references or string literals are supported in functions.");
        }
        return new FunctionCall(FunctionType.fromName(function), columnName.toUpperCase(Locale.ROOT), false);
    }

    private Object evaluateFunction(FunctionCall functionCall, XSSFRow row) {
        Object value;
        if (functionCall.isLiteral) {
            value = functionCall.argument;
        } else {
            value = createColumnProvider(functionCall.argument).getValue(row);
        }
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (functionCall.type == FunctionType.LOWER) {
            return text.toLowerCase(Locale.ROOT);
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private Clause findClause(String sql, String keyword) {
        int pos = findKeywordOutsideQuotes(sql, keyword);
        if (pos < 0) {
            return null;
        }
        return new Clause(keyword, pos);
    }

    private RowFilter parseWhereClause(String clause, String tableAlias, String tableNameValue) throws SQLException {
        if (containsKeywordOutsideQuotes(clause, "and") || containsKeywordOutsideQuotes(clause, "or")) {
            throw new SQLException("Only single conditions in WHERE are supported.");
        }
        if (containsUnsupportedOperators(clause)) {
            throw new SQLException("Only =, <, >, LIKE operators are supported in WHERE.");
        }

        int likePos = findKeywordOutsideQuotes(clause, "like");
        String operator;
        String left;
        String right;
        if (likePos >= 0) {
            operator = "like";
            left = clause.substring(0, likePos).trim();
            right = clause.substring(likePos + 4).trim();
        } else {
            OperatorPosition opPos = findComparisonOperator(clause);
            if (opPos == null) {
                throw new SQLException("Only =, <, >, LIKE operators are supported in WHERE.");
            }
            operator = String.valueOf(opPos.operator);
            left = clause.substring(0, opPos.position).trim();
            right = clause.substring(opPos.position + 1).trim();
        }

        RowValueProvider leftProvider = parseWhereLeftExpression(left, tableAlias, tableNameValue);
        Literal literal = parseLiteral(right, operator);
        return row -> {
            Object cellValue = leftProvider.getValue(row);
            return evaluateCondition(cellValue, operator, literal);
        };
    }

    private RowValueProvider parseWhereLeftExpression(String expression, String tableAlias, String tableNameValue)
            throws SQLException {
        FunctionCall functionCall = parseFunctionCall(expression, tableAlias, tableNameValue);
        if (functionCall != null) {
            return row -> evaluateFunction(functionCall, row);
        }
        String columnName = extractColumnName(expression, tableAlias, tableNameValue);
        if (!isColumnName(columnName)) {
            throw new SQLException("Only column references or lower/upper functions are supported in WHERE.");
        }
        int columnIndex = getColumnIndexFromName(columnName);
        if (columnIndex < 0) {
            throw new SQLException("Invalid column name in WHERE: " + columnName);
        }
        return createColumnProvider(columnName.toUpperCase(Locale.ROOT));
    }

    private Literal parseLiteral(String token, String operator) throws SQLException {
        if (token == null || token.isEmpty()) {
            throw new SQLException("Missing literal in WHERE.");
        }
        String trimmed = token.trim();
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            String value = trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
            return new Literal(LiteralType.STRING, value);
        }
        if ("like".equalsIgnoreCase(operator)) {
            throw new SQLException("LIKE requires a string literal.");
        }
        try {
            return new Literal(LiteralType.NUMBER, new BigDecimal(trimmed));
        } catch (NumberFormatException ex) {
            throw new SQLException("Only string or numeric literals are supported in WHERE.");
        }
    }

    private boolean evaluateCondition(Object cellValue, String operator, Literal literal) {
        if (cellValue == null) {
            return false;
        }
        if ("like".equalsIgnoreCase(operator)) {
            String text = cellValue.toString();
            return matchesLike(text, (String) literal.value);
        }
        if ("=".equals(operator)) {
            return compareEquals(cellValue, literal);
        }
        BigDecimal cellNumber = toBigDecimal(cellValue);
        if (cellNumber == null || literal.type != LiteralType.NUMBER) {
            return false;
        }
        BigDecimal literalNumber = (BigDecimal) literal.value;
        int cmp = cellNumber.compareTo(literalNumber);
        if (">".equals(operator)) {
            return cmp > 0;
        }
        if ("<".equals(operator)) {
            return cmp < 0;
        }
        return false;
    }

    private boolean compareEquals(Object cellValue, Literal literal) {
        if (literal.type == LiteralType.STRING) {
            return cellValue.toString().equals(literal.value);
        }
        BigDecimal cellNumber = toBigDecimal(cellValue);
        if (cellNumber == null) {
            return false;
        }
        return cellNumber.compareTo((BigDecimal) literal.value) == 0;
    }

    private BigDecimal toBigDecimal(Object cellValue) {
        if (cellValue instanceof Number) {
            return new BigDecimal(cellValue.toString());
        }
        if (cellValue instanceof String) {
            try {
                return new BigDecimal(((String) cellValue).trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private boolean matchesLike(String value, String pattern) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            switch (ch) {
                case '%':
                    regex.append(".*");
                    break;
                case '_':
                    regex.append('.');
                    break;
                default:
                    if ("\\.[]{}()*+-?^$|".indexOf(ch) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(ch);
                    break;
            }
        }
        return Pattern.compile(regex.toString()).matcher(value).matches();
    }

    private OperatorPosition findComparisonOperator(String clause) {
        boolean inString = false;
        OperatorPosition result = null;
        for (int i = 0; i < clause.length(); i++) {
            char ch = clause.charAt(i);
            if (inString) {
                if (ch == '\'') {
                    if (i + 1 < clause.length() && clause.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }
            if (ch == '\'') {
                inString = true;
                continue;
            }
            if (ch == '=' || ch == '<' || ch == '>') {
                if (result != null) {
                    return null;
                }
                result = new OperatorPosition(i, ch);
            }
        }
        return result;
    }

    private boolean containsUnsupportedOperators(String clause) {
        return findSequenceOutsideQuotes(clause, ">=") >= 0
                || findSequenceOutsideQuotes(clause, "<=") >= 0
                || findSequenceOutsideQuotes(clause, "<>") >= 0
                || findSequenceOutsideQuotes(clause, "!=") >= 0;
    }

    private int findSequenceOutsideQuotes(String sql, String needle) {
        String lowerSql = sql.toLowerCase(Locale.ROOT);
        String lowerNeedle = needle.toLowerCase(Locale.ROOT);
        boolean inString = false;
        for (int i = 0; i <= lowerSql.length() - lowerNeedle.length(); i++) {
            char ch = lowerSql.charAt(i);
            if (inString) {
                if (ch == '\'') {
                    if (i + 1 < lowerSql.length() && lowerSql.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }
            if (ch == '\'') {
                inString = true;
                continue;
            }
            if (lowerSql.startsWith(lowerNeedle, i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean containsKeywordOutsideQuotes(String sql, String keyword) {
        return findKeywordOutsideQuotes(sql, keyword) >= 0;
    }

    private int findKeywordOutsideQuotes(String sql, String keyword) {
        String lowerSql = sql.toLowerCase(Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        int keywordLength = lowerKeyword.length();
        boolean inString = false;
        for (int i = 0; i <= lowerSql.length() - keywordLength; i++) {
            char ch = lowerSql.charAt(i);
            if (inString) {
                if (ch == '\'') {
                    if (i + 1 < lowerSql.length() && lowerSql.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }
            if (ch == '\'') {
                inString = true;
                continue;
            }
            if (lowerSql.startsWith(lowerKeyword, i)
                    && isKeywordBoundary(lowerSql, i - 1)
                    && isKeywordBoundary(lowerSql, i + keywordLength)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isKeywordBoundary(String value, int index) {
        if (index < 0 || index >= value.length()) {
            return true;
        }
        char ch = value.charAt(index);
        return !Character.isLetterOrDigit(ch) && ch != '_';
    }

    private Integer parsePositiveInt(String token, String clause) throws SQLException {
        try {
            int value = Integer.parseInt(token.trim());
            if (value < 0) {
                throw new SQLException(clause + " must be >= 0.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new SQLException("Invalid " + clause + " value.");
        }
    }

    private static class ParsedQuery {
        private String tableName;
        private String tableAlias;
        private Integer limit;
        private Integer offset;
        private List<String> selectItems;
        private RowFilter rowFilter;
    }

    private static class AliasSplit {
        private final String expression;
        private final String alias;

        private AliasSplit(String expression, String alias) {
            this.expression = expression;
            this.alias = alias;
        }
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

    private interface RowFilter {
        boolean matches(XSSFRow row);
    }

    private static class OperatorPosition {
        private final int position;
        private final char operator;

        private OperatorPosition(int position, char operator) {
            this.position = position;
            this.operator = operator;
        }
    }

    private static class Clause {
        private final String keyword;
        private final int start;

        private Clause(String keyword, int start) {
            this.keyword = keyword;
            this.start = start;
        }
    }

    private enum LiteralType {
        STRING,
        NUMBER
    }

    private static class Literal {
        private final LiteralType type;
        private final Object value;

        private Literal(LiteralType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private enum FunctionType {
        LOWER,
        UPPER;

        private static FunctionType fromName(String name) {
            if ("lower".equalsIgnoreCase(name)) {
                return LOWER;
            }
            return UPPER;
        }
    }

    private static class FunctionCall {
        private final FunctionType type;
        private final String argument;
        private final boolean isLiteral;

        private FunctionCall(FunctionType type, String argument, boolean isLiteral) {
            this.type = type;
            this.argument = argument;
            this.isLiteral = isLiteral;
        }
    }

    private static class ConcatPart {
        private final boolean isLiteral;
        private final String literal;
        private final RowValueProvider provider;

        private ConcatPart(boolean isLiteral, String literal, RowValueProvider provider) {
            this.isLiteral = isLiteral;
            this.literal = literal;
            this.provider = provider;
        }

        private static ConcatPart literal(String value) {
            return new ConcatPart(true, value, null);
        }

        private static ConcatPart provider(RowValueProvider provider) {
            return new ConcatPart(false, null, provider);
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
