# 0001 — Generate event identities in the offline-first pupitre

## Status

Accepted. Retained scope: activity gestures and their durable replay in the tenant schema.

Amended on 9 October 2026 (glm-front#254): the identity registry and the fingerprint comparison are gone. A replay is
now recognised by the event table and by the audit of ignored punches, as part of the reception rule of
[ADR 0010](0010-judge-each-punch-when-it-is-received.md). The decision to generate the UUID at the gesture stands.

## Context

The pupitre records activity gestures while disconnected. A retry must retain the real gesture time and
identity, and must not append another journal event. The identity space belongs to each tenant and must
survive local cache eviction and delayed replay.

## Considered options

- Generate the UUID at the gesture and recognise a retry by that UUID in the tenant schema — **kept**.
- Generate a new UUID on the back for every request — rejected: a retry cannot identify its original event.
- Keep idempotency in a front cache or with a TTL — rejected: eviction or expiration loses delayed replay.
- Reserve the UUID in a registry and compare a fingerprint of the content — **replaced** on 9 October 2026: the
  registry grew with the journals, every write path had to reserve and associate its identity, and a refusal for
  "same identity, other content" protected nothing once the server judges each punch itself.

## Decision

Generate one UUID at each pupitre gesture and preserve its replayable content: type, operator, workstation and
real gesture time. Before any business rule, the server looks the UUID up:

1. present in `evenement_d_atelier` (the whole table, whichever follow-up carries it): the punch is a replay. The
   answer is 200 and nothing is written, whatever content is resent;
2. otherwise present in the audit table of ignored punches: the same refusal, `pointage-ignore` (409), without a new
   audit row;
3. otherwise the punch is judged by the reception rule.

Concurrent punches of one follow-up are judged one after the other under its lock, so the second finds the first's
event; the primary key of `evenement_d_atelier` stays the backstop. The audit table has no key and no constraint, so
two rows for one retry are accepted. The manager's regularisation applies the first step only.

Reusing an identity with different content is no longer a refusal: the first arrival wins. Authentication, role and
tenant authorization are checked on every request.

## Consequences

### Positive

- A delayed retry returns its original result even when mutable references or rules have changed.
- There is no registry to grow, reserve in or keep in the same transaction as the write.
- Tenant schemas keep identity spaces independent, without a shared cache.

### Negative

- The audit table grows with the ignored punches, because it has no TTL.
- A client that reuses an identity for another gesture gets the original answer and loses its gesture silently.
- The pupitre must durably retain the UUID, type and gesture time of every queued gesture.
