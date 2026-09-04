# 0001 — Generate event identities in the offline-first pupitre

## Status

Accepted.

## Context

The pupitre records arrival, presence and workshop gestures while it can be disconnected. A retry must retain the
real gesture time and must not append a second journal event. Event identities must remain isolated by tenant and
must survive any local cache eviction.

## Considered options

- Generate the UUID in the pupitre and keep a relational tenant-local registry — **kept**.
- Generate a new UUID on the back for every request — rejected: an offline retry cannot identify its original event.
- Keep idempotency in a front cache or with a TTL — rejected: neither is durable enough for delayed replay.
- Store an opaque hash or serialized JSON fingerprint — rejected: comparison would not expose the published gesture fields.

## Decision

Generate one UUID for each offline-first gesture in the pupitre. Persist it as the journal event identity and reserve
it atomically in a PostgreSQL table in the current tenant schema. Store the replayable fingerprint in explicit
columns, then associate it with the resulting day or workshop follow-up in the same transaction. Keep server-generated
UUIDs for non-pupitre corrections and reserve them as non-replayable identities.

## Consequences

### Positive

- A strict retry returns the original aggregate without replaying mutable business checks.
- The primary key arbitrates concurrent submissions without a shared cache.
- Tenant schemas keep identity spaces independent.

### Negative

- The registry grows with the journals because it has no TTL.
- Every new workshop event path must reserve its identity in the same transaction.
- The front must retain an UUID for each queued gesture.
