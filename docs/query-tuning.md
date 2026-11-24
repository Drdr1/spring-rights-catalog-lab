# Query tuning: the two slow queries

Both fixes follow the same rule: keep the indexed column bare in the `WHERE` clause, and give
the planner an index whose leading columns match the equality filters.

`api/src/test/java/dev/derops/rights/QueryPlanIT.java` checks both on real PostgreSQL 16 with
20,000 titles and 80,000 license windows. It runs `EXPLAIN` on the exact SQL constants the
application uses and asserts which index the plan picks. The old versions are tested too,
and must still show a sequential scan, so the test proves the change made the difference.

## 1. Title search

Before:

```sql
SELECT id, name FROM title
WHERE lower(name) LIKE lower('%' || :term || '%')
ORDER BY name LIMIT 25;
```

`lower(name)` hides the column from any plain index, and a leading `%` defeats a btree index
anyway. The result is a sequential scan of every title on each keystroke.

After (`TitleQueries.SEARCH_SQL`):

```sql
SELECT id, name, kind, release_year FROM title
WHERE name ILIKE '%' || :term || '%'
ORDER BY name LIMIT :limit;
```

```sql
CREATE INDEX ix_title_name_trgm ON title USING gin (name gin_trgm_ops);
```

A trigram GIN index serves case-insensitive substring matches for terms of three or more
characters. User input is escaped (`%`, `_`, `\`) so a search for `100%` means the literal
text.

## 2. Availability

Before:

```sql
... WHERE upper(l.territory) = :territory AND lower(l.platform) = :platform
      AND :day BETWEEN l.start_date AND l.end_date
```

The functions on `territory` and `platform` force a full scan of `license_window` for every
availability check.

After (`AvailabilityQueries.AVAILABLE_SQL`): the values are normalised once in the
controller (`territory.toUpperCase()`, platform as an enum), so the columns stay bare:

```sql
WHERE l.territory = :territory AND l.platform = :platform
  AND l.start_date <= :day AND l.end_date >= :day
```

```sql
CREATE INDEX ix_license_lookup ON license_window (territory, platform, start_date, end_date);
```

Equality columns come first, then the range column. `end_date` is in the index too, so the
second date condition is checked without visiting the table rows.

## Not an index, but related: overlapping exclusive windows

```sql
CONSTRAINT ex_exclusive_overlap EXCLUDE USING gist (
  title_id WITH =, territory WITH =, platform WITH =,
  daterange(start_date, end_date, '[]') WITH &&
) WHERE (exclusive)
```

The service checks overlaps first to return a clear 409. This constraint is what still holds
when two requests pass that check at the same moment. `ApiIT` inserts two overlapping
exclusive windows straight into the database to prove it.
