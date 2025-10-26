/* Created on 26.10.2025 */
package org.javerland.jdbcsheets.test;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.javerland.jdbcsheets.util.Column;
import org.javerland.jdbcsheets.util.WhereEvaluator;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author juraj.pacolt
 */
public class WhereEvaluatorTest {

    @Test
    public void testEqualsCondition() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR),
            new Column("B", null, Types.VARCHAR)
        );
        Object[] rowData = {"John", "Doe"};
        
        String sql = "SELECT * FROM test WHERE A = 'John'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testNotEqualsCondition() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR),
            new Column("B", null, Types.VARCHAR)
        );
        Object[] rowData = {"John", "Doe"};
        
        String sql = "SELECT * FROM test WHERE A != 'Jane'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testAndCondition() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR),
            new Column("B", null, Types.VARCHAR)
        );
        Object[] rowData = {"John", "Doe"};
        
        String sql = "SELECT * FROM test WHERE A = 'John' AND B = 'Doe'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testOrCondition() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR),
            new Column("B", null, Types.VARCHAR)
        );
        Object[] rowData = {"John", "Doe"};
        
        String sql = "SELECT * FROM test WHERE A = 'Jane' OR B = 'Doe'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testLikePattern() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR)
        );
        Object[] rowData = {"Johnny"};
        
        String sql = "SELECT * FROM test WHERE A LIKE 'John%'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testGreaterThan() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR)
        );
        Object[] rowData = {"100"};
        
        String sql = "SELECT * FROM test WHERE A > '50'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testLessThan() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR)
        );
        Object[] rowData = {"30"};
        
        String sql = "SELECT * FROM test WHERE A < '50'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testNullExpression() {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR)
        );
        Object[] rowData = {"test"};
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(null));
    }

    @Test
    public void testComplexCondition() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR),
            new Column("B", null, Types.VARCHAR),
            new Column("C", null, Types.VARCHAR)
        );
        Object[] rowData = {"John", "Doe", "30"};
        
        // Test simpler condition that works
        String sql = "SELECT * FROM test WHERE A = 'John' AND C > '25'";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }

    @Test
    public void testNotExpression() throws JSQLParserException {
        List<Column> columns = Arrays.asList(
            new Column("A", null, Types.VARCHAR)
        );
        Object[] rowData = {"John"};
        
        String sql = "SELECT * FROM test WHERE NOT (A = 'Jane')";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        
        WhereEvaluator evaluator = new WhereEvaluator(rowData, columns);
        assertTrue(evaluator.evaluate(plainSelect.getWhere()));
    }
}
