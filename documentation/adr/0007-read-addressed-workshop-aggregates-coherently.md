# 0007 — Read addressed workshop aggregates coherently

## Status

Accepted on 4 October 2026 for addressed Atelier reads and conflict previews.

## Context

A follow-up revision covers both its journal and closure. Under PostgreSQL `READ COMMITTED`, loading the
parent and then its lazy journal can combine revision and closure from before a committed cancellation with
journal facts from after it. A preview would then sign a revision that never described its displayed facts.
A fetch join alone still mixes versions when the parent was previously loaded in the caller's JPA context.

The tenant connection setup currently prevents changing transaction isolation after acquisition: Hibernate
selects the schema first. The Pupitre owner documents this constraint. This fix must preserve that tenant
selection chain and the existing write concurrency protocol.

## Considered options

- Add a parent read lock — rejected: read-only PostgreSQL transactions reject locking selects, and a simple
  read need not serialise writers.
- Switch to `REPEATABLE_READ` — rejected here: it requires a separate redesign of tenant connection setup.
- Fetch parent and journal together in a fresh persistence context — kept: a single PostgreSQL statement
  observes one committed version, without blocking an ordinary concurrent writer.

## Decision

`JpaSuiviDAtelierRepository.get` fetches the parent and its complete journal in one JPQL fetch join. Reconstruction
uses those fetched facts; derived projection collections do not participate in this aggregate read.

The addressed `SuivisDAtelierApplicationService.get` and `ApercusDeResolution.apercu` run in their own read-only
transaction (`REQUIRES_NEW`) at the existing default `READ COMMITTED` isolation. Suspending a caller transaction
also suspends its persistence context, so cached parent or journal entities cannot override the fetched version.
These two entry points read committed state: they deliberately do not expose a caller's uncommitted writes.

Write orchestration continues to use the existing domain service and repository inside its write transaction.
Confirmation and canonical receipt recovery retain their existing `getForUpdate` protocol. Neither the
transaction isolation of other reads nor the multitenant connection provider changes.

## Consequences

- A dossier or preview contains a journal, revision and closure from one committed aggregate version.
- A concurrent commit may make that version immediately old; confirmation still compares its revision under
  the write lock. No snapshot is promised across separate requests or between aggregates and display labels.
- A nested addressed read temporarily needs a second connection while the caller's connection remains held.
- PostgreSQL integration tests synchronise a writer between parent and journal acquisition and also pre-load
  a parent in the calling persistence context. They accept an entirely old or entirely new aggregate, never a
  mixture, including cancellation and closure. Preview tests exercise the same public boundary.
