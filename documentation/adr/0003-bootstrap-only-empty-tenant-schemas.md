# 0003 — Bootstrap only empty tenant schemas

## Status

Accepted on 30 September 2026 for the retirement of workshop presence.

## Context

The activity model is installed on a fresh database. Removing obsolete presence changesets alone would let a
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
success; it does not run again on restart. Keep the identities, authors and paths of useful activity changesets,
including `2026/09/013-activite_d_atelier_fin_au_plus_tard.xml`.

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
