# Driver jdbc-sheets
JDBC driver for XLSX reading

# Example

For example file `test-data.xlsx` with `Sheet1` and those data.

_The data are generated purely randomly using AI. It does not contain real personal data._

<img src="./src/test/resources/data-image.png">

## Potencional query
```mysql
select 
    A as "Col1", B, C, D, E 
from 
    Sheet1 
limit 5 
offset 0;
```
Supported now:
- column aliases (shown in results)
- table aliasing (e.g. `Sheet1 tbl`)
- `*` and `tbl.*`
- `limit` / `offset`
- simple `where` with `=`, `<`, `>`, `like`
- concatenation with `||`
- `lower()` and `upper()` in SELECT and WHERE

Not supported yet: ORDER BY, GROUP BY, JOIN, AND/OR in WHERE, `>=`, `<=`, `!=`, `<>`, and others.

### Examples
```mysql
select A as Col1, B from Sheet1;
```

```mysql
select tbl.A, tbl.B from Sheet1 tbl;
```

```mysql
select A || ' ' || B as FullName from Sheet1 limit 5;
```

```mysql
select lower(A) as lower_name, upper(B) as upper_name from Sheet1;
```

```mysql
select A, B from Sheet1 where A > 10;
select B from Sheet1 where B like 'Ali%';
```

```mysql
select A || ' ' || B as FullName from Sheet1 where lower(A) like '%ste%';
```

### Notes
- `where` supports only a single condition (no AND/OR).
- `like` supports `%` and `_`, and is case-sensitive.
- Numeric comparisons work only when the cell value is numeric.
- Formula cells are read from cached results when formula evaluation is not available.

## Example how to read

```java
Class.forName("org.javerland.jdbcsheets.Driver");

try (Connection conn = DriverManager.getConnection("jdbc:sheets://?file=./test-data.xlsx")) {
    try (Statement stmt = conn.createStatement()) {
        try (ResultSet rs = stmt.executeQuery("select A as Col1, B, C, D, E from Sheet1 limit 5 offset 0")) {
            while (rs.next()) {
                String name = rs.getString("Col1");
                String surname = rs.getString(2); // Or "B"
                System.out.println(String.format("%s %s", name, surname));
            }
        }
    }
}

try (Connection conn = DriverManager.getConnection("jdbc:sheets://?database=test-data.xlsx&directory=./")) {
    // ... .. .
}
```

Output
```
Melissa Perry
Valerie Grant
Steve Stone
Jessica Rosales
Tyler Sandoval
```

# How to build

By Maven standard

```shell
mvn clean package
```

Or assembly single jar, with all dependencies

```shell
mvn clean package assembly:single
```
