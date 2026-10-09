# 0003 — Bootstrap only empty tenant schemas

## Status

Accepted on 30 September 2026 for fresh installations of the activity model.

## Context

The activity model is installed on a fresh database. A cleaned changeset history alone would let a
schema carrying an earlier valid Liquibase prefix start as if it were new. Each tenant has its own history;
`TenantSchemasInitializer` uses a raw connection whose search path is not the tenant schema.

## Considered options

- Adopt or migrate historical schemas — rejected: old data is outside this installation contract.
- Reset histories, accept checksums or create obsolete tables before dropping them — rejected: these would conceal adoption.
- Record a first-execution guard before all business DDL — **kept**.

## Decision

Insert one technical changeset before the first business creation in `2026/08/001-element_de_fabrication.xml`.
Its precondition requires zero rows in the tenant's `databasechangelog`, qualified by
`${database.defaultSchemaName}`, and uses `HALT` on both failure and error. Record the changeset normally after
success; it does not run again on restart. Keep the identities, authors and paths of the activity changesets the model
still uses, such as `2026/09/009-activite_d_atelier.xml` and `2026/09/011-activite_d_atelier_operateur.xml`.

Amended on 9 October 2026, when conflict sequences were removed from the model: the changesets that only built what
the model dropped were deleted or edited in place rather than followed by a dropping changeset, because every database
is purged before the new model. Deleted: `2026/09/001` and `008` (event identity registry), `010` (resolvable-activity
flag), `012` (conflict sequence tables), `013` (latest-end column) and `2026/10/004` (replaced-event link). Edited:
`2026/08/002` (no cancellation columns), `2026/09/007` (no intention column; renamed `007-evenement_d_atelier_activites.xml`, changeset `evenement_d_atelier_activites`) and `2026/10/005` (no instant to convert
for a dropped column). `013` was the example of a useful changeset kept by this decision; it no longer is, so the rule
above names the changesets that survive instead.

The master installs only surviving business tables. Existing business tables without Liquibase history are not
adopted: normal creation fails. No automatic reset, checksum exception, compatibility surface or data migration
is provided.

## Consequences

### Positive

- Fresh tenants install independently and restart with unchanged data and history.
- A historical prefix is refused before new business DDL and remains intact.
- The test exercises the real application, tenant initializer, master and JPA on new, non-reused PostgreSQL containers.

### Negative

- Existing installations require a separate, explicit product decision; this bootstrap cannot upgrade them.
- The guard's position and tenant qualification are part of the installation contract and require integration proof.
