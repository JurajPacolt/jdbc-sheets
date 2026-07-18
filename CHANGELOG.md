# Changelog

All notable changes to this project are documented here.

## 26.3.1

- Added scalar `PreparedStatement` support with parameter metadata and safe literal binding.
- Added optional `header=true` column names, including JDBC metadata integration.
- Added `AND`/`OR`, parentheses, extended comparisons, null predicates, and single-column ordering.
- Corrected JDBC lifecycle, read-only, metadata-result schema, timeout, and cancellation contracts.
- Open XLSX packages read-only and infer all column types in a single worksheet pass.
- Renamed `XslxReader` to `XlsxReader`; the old class remains as a deprecated compatibility alias.
- Added JDBC service-provider registration and Java 11/17/21/25 CI coverage.
- Expanded JUnit 5 and Mockito tests, JaCoCo gates, SpotBugs, dependency analysis, and optional PIT.
- Added Maven Wrapper, reproducible archive timestamps, and automatic-module manifest metadata.
