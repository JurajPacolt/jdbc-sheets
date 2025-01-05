# Driver jdbc-sheets
JDBC driver for XLSX and others sheets reading

# Example using

```java
Class.forName("org.javerland.jdbcsheets.Driver");

try (Connection conn = DriverManager.getConnection("jdbc:sheets://?file=./test-data.xlsx")) {
    // TODO ...
}

try (Connection conn = DriverManager.getConnection("jdbc:sheets://?database=test-data.xlsx&directory=./")) {
    // TODO ...
}
```

## Query
```mysql
select 
    A as "Column 1", B, C, D, E 
from 
    Sheet1 
limit 10 
offset 5;
```
Where conditions not supported now.

# How to build

By Maven standard

```shell
mvn clean package
```

Or assembly single jar, with all dependencies

```shell
mvn clean package assembly:single
```
