# 0001 — Generate event identities in the offline-first pupitre

## Status

Accepted. Retained scope: activity gestures and their durable replay in the tenant schema.

## Context

The pupitre records activity gestures while disconnected. A retry must retain the real gesture time,
identity, intention and target, and must not append another journal event. The identity space belongs
to each tenant and must survive local cache eviction and delayed replay.

## Considered options

- Generate the UUID at the gesture and keep a relational tenant-local registry — **kept**.
- Generate a new UUID on the back for every request — rejected: a retry cannot identify its original event.
- Keep idempotency in a front cache or with a TTL — rejected: eviction or expiration loses delayed replay.
- Store an opaque hash or serialized JSON fingerprint — rejected: comparison would hide the published fields.

## Decision

Generate one UUID at each pupitre gesture and preserve its replayable content. Reserve that identity
atomically in PostgreSQL in the current tenant schema, compare the fingerprint in explicit columns,
and associate it with the resulting workshop follow-up in the same transaction. The association to
the follow-up and the target activity remain distinct. An omitted date remains distinct from a supplied date.
Server-generated identities for corrections and other non-pupitre events are reserved as non-replayable.

A strict retry returns the original follow-up before mutable business rules are checked again.
Authentication, role and tenant authorization are checked on every request; the registry bypasses only
the repeated business act. Reusing a reserved identity with different content remains a refusal.

## Consequences

### Positive

- A delayed retry returns its original result even when mutable references or rules have changed.
- The primary key arbitrates concurrent submissions, and rollback leaves no partial reservation or association.
- Tenant schemas keep identity spaces independent, without a shared cache.

### Negative

- The registry grows with the journals because it has no TTL.
- Every event path must reserve its identity and associate its result in the same transaction.
- The pupitre must durably retain the UUID, dates, intention and target of every queued gesture.
