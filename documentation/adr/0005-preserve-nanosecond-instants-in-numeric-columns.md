# 0005 — Preserve nanosecond instants in numeric columns

## Status

Accepted on 3 October 2026 for workshop journal facts and their projections.

## Context

The workshop contract preserves Java `Instant` values at nanosecond precision. An integration test writing
`2042-01-06T08:00:00.123456789Z` observes `.123457Z` after the PostgreSQL timestamp round-trip. Two facts one
nanosecond apart must retain their business order in both the journal and downstream readers.

## Considered options

- A timestamp truncated to microseconds plus a remainder — exact, but every selection and ordering must compare
  both columns, including JPQL joins and activity overlap predicates.
- Separate epoch-second and nanosecond columns — exact, with the same two-column comparison cost.
- Decimal epoch seconds in a single numeric column — **kept**: the fractional nine digits represent the nanoseconds
  exactly, and SQL ordering and comparisons use the same canonical value.

## Decision

Store workshop engagement, closure, event and projected activity dates as `NUMERIC(30,9)` decimal seconds since
the Unix epoch. Explicit JPA `@Convert` annotations use the technical shared `ExactInstantConverter`, so the Java
fields and query parameters remain `Instant`; unrelated timestamp columns retain their existing representation.
Negative fractional seconds decode using mathematical floor. Each reader maps its own immutable entity to the
same converted columns, preserving context boundaries.

The Liquibase migration converts the existing UTC timestamp values using `extract(epoch ... AT TIME ZONE 'UTC')`.
It preserves the precision still present in those values, without reconstructing lost nanoseconds. New facts retain
all nine digits. The migration infers nothing from old dates.

## Consequences

- Journal reconstruction, projections, JPQL selection, and sorting share one exact representation.
- Native consumers of these columns receive numbers and must use the converter or a mapped JPQL projection.
- This column-type change requires coordinated deployment; an old server expecting timestamps cannot run against
  the migrated schema. Fresh-tenant bootstrap and data ownership rules remain applicable.
