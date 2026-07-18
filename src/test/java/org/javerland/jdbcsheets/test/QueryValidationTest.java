package org.javerland.jdbcsheets.test;

import org.javerland.jdbcsheets.test.constants.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

class QueryValidationTest {

    @Test
    void unsupportedOrMalformedSqlIsRejectedExplicitly() throws Exception {
        try (Connection connection = DriverManager.getConnection(TestConstants.URL_WITH_FILE);
                Statement statement = connection.createStatement()) {
            Assertions.assertThrows(SQLException.class, () -> statement.executeQuery(null));

            List<String> invalidQueries = List.of(
                    "update Sheet1 set A = 'x'",
                    "select distinct A from Sheet1",
                    "select all A from Sheet1",
                    "select from Sheet1",
                    "select A from MissingSheet",
                    "select A1 from Sheet1",
                    "select Unknown from Sheet1",
                    "select wrong.A from Sheet1 sheet",
                    "select A from Sheet1 group by A",
                    "select A from Sheet1 where A like 1",
                    "select A from Sheet1 where",
                    "select A from Sheet1 where (A = 1",
                    "select A from Sheet1 order A",
                    "select A from Sheet1 order by A, B",
                    "select A from Sheet1 limit -1",
                    "select A from Sheet1 limit nope",
                    "select A from Sheet1 offset nope");

            for (String query : invalidQueries) {
                Assertions.assertThrows(SQLException.class, () -> statement.executeQuery(query), query);
            }
        }
    }

    @Test
    void qualifiersImplicitAliasesLiteralFunctionsLimitAndOffsetAreAccepted() throws Exception {
        try (Connection connection = DriverManager.getConnection(TestConstants.URL_WITH_FILE);
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "select sheet.A Name, upper('hello') Greeting from Sheet1 sheet limit 1 offset 1")) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals("Valerie", resultSet.getString("Name"));
                Assertions.assertEquals("HELLO", resultSet.getString("Greeting"));
                Assertions.assertFalse(resultSet.next());
            }

            try (ResultSet resultSet = statement.executeQuery("select sheet.* from Sheet1 sheet limit 1")) {
                Assertions.assertEquals(5, resultSet.getMetaData().getColumnCount());
                Assertions.assertTrue(resultSet.next());
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "select A from Sheet1 where (A = 'Melissa' or A = 'Valerie') and B <> 'Perry' order by A desc")) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals("Valerie", resultSet.getString(1));
                Assertions.assertFalse(resultSet.next());
            }
        }
    }
}
