# Driver jdbc-sheets
JDBC driver for XLSX and others sheets reading

# Example using

## Client example
```java
Class.forName("org.javerland.jdbcsheets.Driver");
// TODO ...
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

# How to build

By Maven standard

```shell
mvn clean package
```

Or assembly single jar, with all dependencies

```shell
mvn clean package assembly:single
```
