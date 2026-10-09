# 0007 — Read addressed workshop aggregates coherently

## Status

Accepted on 4 October 2026 for addressed Atelier reads and conflict previews.

Amended on 9 October 2026 (glm-front#254): the previews and the confirmations are gone
([ADR 0011](0011-regularise-an-automatic-end-directly.md)). The decision stands for the addressed read that remains,
`SuivisDAtelierApplicationService.get`, which serves the follow-up detail and the dossier of an automatic end.

## Context

A follow-up revision covers both its journal and closure. Under PostgreSQL `READ COMMITTED`, loading the
parent and then its lazy journal can combine revision and closure from before a committed write with
journal facts from after it. A dossier would then show a revision that never described its displayed facts.
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

The addressed `SuivisDAtelierApplicationService.get` runs in its own read-only
transaction (`REQUIRES_NEW`) at the existing default `READ COMMITTED` isolation. Suspending a caller transaction
also suspends its persistence context, so cached parent or journal entities cannot override the fetched version.
This entry point reads committed state: it deliberately does not expose a caller's uncommitted writes.

Write orchestration continues to use the existing domain service and repository inside its write transaction.
Judging a punch takes the follow-up lock with `getForUpdate`, and every update compares the revision under the write
lock. Neither the transaction isolation of other reads nor the multitenant connection provider changes.

## Consequences

- A dossier contains a journal, revision and closure from one committed aggregate version.
- A concurrent commit may make that version immediately old; a regularisation compares the revision it read under the
  write lock and answers `saisie-concurrente`, and the manager rereads the dossier. No snapshot is promised across
  separate requests or between aggregates and display labels.
- A nested addressed read temporarily needs a second connection while the caller's connection remains held.
- PostgreSQL integration tests synchronise a writer between parent and journal acquisition and also pre-load
  a parent in the calling persistence context. They accept an entirely old or entirely new aggregate, never a
  mixture, including a punch and the closure.
