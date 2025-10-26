/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.util;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;

import java.util.List;

/**
 * Evaluates WHERE clause expressions against row data
 * @author juraj.pacolt
 */
public class WhereEvaluator {

    private final Object[] rowData;
    private final List<org.javerland.jdbcsheets.util.Column> columns;

    public WhereEvaluator(Object[] rowData, List<org.javerland.jdbcsheets.util.Column> columns) {
        this.rowData = rowData;
        this.columns = columns;
    }

    public boolean evaluate(Expression expression) {
        if (expression == null) {
            return true;
        }
        return evaluateExpression(expression);
    }

    private boolean evaluateExpression(Expression expression) {
        if (expression instanceof AndExpression) {
            AndExpression and = (AndExpression) expression;
            return evaluateExpression(and.getLeftExpression()) && evaluateExpression(and.getRightExpression());
        } else if (expression instanceof OrExpression) {
            OrExpression or = (OrExpression) expression;
            return evaluateExpression(or.getLeftExpression()) || evaluateExpression(or.getRightExpression());
        } else if (expression instanceof EqualsTo) {
            EqualsTo eq = (EqualsTo) expression;
            return compare(getValue(eq.getLeftExpression()), getValue(eq.getRightExpression())) == 0;
        } else if (expression instanceof NotEqualsTo) {
            NotEqualsTo neq = (NotEqualsTo) expression;
            return compare(getValue(neq.getLeftExpression()), getValue(neq.getRightExpression())) != 0;
        } else if (expression instanceof GreaterThan) {
            GreaterThan gt = (GreaterThan) expression;
            return compare(getValue(gt.getLeftExpression()), getValue(gt.getRightExpression())) > 0;
        } else if (expression instanceof GreaterThanEquals) {
            GreaterThanEquals gte = (GreaterThanEquals) expression;
            return compare(getValue(gte.getLeftExpression()), getValue(gte.getRightExpression())) >= 0;
        } else if (expression instanceof MinorThan) {
            MinorThan lt = (MinorThan) expression;
            return compare(getValue(lt.getLeftExpression()), getValue(lt.getRightExpression())) < 0;
        } else if (expression instanceof MinorThanEquals) {
            MinorThanEquals lte = (MinorThanEquals) expression;
            return compare(getValue(lte.getLeftExpression()), getValue(lte.getRightExpression())) <= 0;
        } else if (expression instanceof IsNullExpression) {
            IsNullExpression isNull = (IsNullExpression) expression;
            Object value = getValue(isNull.getLeftExpression());
            return isNull.isNot() ? (value != null) : (value == null);
        } else if (expression instanceof LikeExpression) {
            LikeExpression like = (LikeExpression) expression;
            Object left = getValue(like.getLeftExpression());
            Object right = getValue(like.getRightExpression());
            
            if (left == null || right == null) {
                return false;
            }
            
            String leftStr = String.valueOf(left);
            String pattern = String.valueOf(right);
            
            pattern = pattern.replace("%", ".*").replace("_", ".");
            boolean matches = leftStr.matches(pattern);
            return like.isNot() ? !matches : matches;
        } else if (expression instanceof Parenthesis) {
            Parenthesis paren = (Parenthesis) expression;
            return evaluateExpression(paren.getExpression());
        } else if (expression instanceof NotExpression) {
            NotExpression not = (NotExpression) expression;
            return !evaluateExpression(not.getExpression());
        }
        
        return false;
    }

    private Object getValue(Expression expression) {
        if (expression instanceof Column) {
            Column column = (Column) expression;
            String columnName = column.getColumnName().toUpperCase();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).getName().equals(columnName)) {
                    return rowData[i];
                }
            }
            return null;
        } else if (expression instanceof StringValue) {
            return ((StringValue) expression).getValue();
        } else if (expression instanceof LongValue) {
            return ((LongValue) expression).getValue();
        } else if (expression instanceof DoubleValue) {
            return ((DoubleValue) expression).getValue();
        } else if (expression instanceof NullValue) {
            return null;
        }
        return null;
    }

    private int compare(Object left, Object right) {
        if (left == null && right == null) return 0;
        if (left == null) return -1;
        if (right == null) return 1;

        String leftStr = String.valueOf(left);
        String rightStr = String.valueOf(right);

        try {
            Double leftNum = Double.parseDouble(leftStr);
            Double rightNum = Double.parseDouble(rightStr);
            return leftNum.compareTo(rightNum);
        } catch (NumberFormatException e) {
            return leftStr.compareTo(rightStr);
        }
    }
}

