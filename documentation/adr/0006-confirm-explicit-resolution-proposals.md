# 0006 — Confirm explicit resolution proposals

## Status

Superseded on 9 October 2026 by [ADR 0011](0011-regularise-an-automatic-end-directly.md) (glm-front#254). Accepted on
4 October 2026 for Atelier conflict resolution.

The preview, the confirmation, the receipt and the `apercu-obsolete` refusal described below were removed with the
conflicts and the acts of correction. The text is kept as the record of that protocol.

## Context

A manager previews a correction, cancellation or regularisation against a workshop follow-up revision.
Previewing must remain a read: no event identity reservation, persisted receipt, server cache or browser
restoration. A committed act must remain recoverable after a lost response and server restart.

## Decision

The preview returns its command UUID, dossier address, follow-up revision, exact act, evaluation instant,
before/after dossiers, SHA-256 material-consequence fingerprint and prospective event UUID when the act
creates an event. The browser keeps this explicit proposal only in page memory. Closing or reloading
abandons an unconfirmed proposal; it does not cancel a request already sent.

Confirmation sends `commande`, `adresse`, `revision`, `acte`, `empreinteConsequences` and optional
`evenement`. Correction and regularisation require that event identity; cancellation excludes it.
Command and event identities remain distinct. The server generates the prospective identity during
preview and reserves it only during writing. Neither author, tenant nor tariffs are client authority.

There is no signature of preview origin and no technical expiry. The fingerprint detects material drift
in the normal browser journey; it does not authenticate a ticket. The server still applies current
roles and domain validations to every new confirmation.

Confirmation first looks for a receipt in the current enterprise. It checks the stable authenticated
identity (enterprise, issuer, subject) separately from display author, and compares the recorded request:
command, address, revision, business fields of the act, prospective identity and expected fingerprint.
An identical retry returns the receipt and canonical current dossier without revalidating the old act.
A changed display author, current clock or current consequences cannot invalidate an already committed
request; the historical author of the write is preserved.

For a new command the server locks the follow-up, rereads the receipt after waiting, then validates its
revision and current dossier address. `PreparationDesActes` prepares the same act as the preview using
current resources, permissions and evaluation instant, keeping its prospective event UUID. A changed
material consequence, withdrawn permission or disappeared resource produces `apercu-obsolete`, without
reservation, event or receipt. Preparing a new preview keeps ordinary domain refusals; technical failures
retain their technical result. Display labels and technical recording metadata do not affect the hash.
The lock protects the follow-up and writing transaction, not a global snapshot of all referentials.

Identity reservation, guarded aggregate update, projections and receipt insertion belong to one
transaction. Concurrent identical confirmations wait and return one receipt. A command reused for a
different proposal is refused. `NON_ATTESTEE` means only that no committed receipt is currently visible;
an in-flight confirmation may still succeed.

Receipts store only the confirmed proposal, authenticated identity and durable result. Their JSON format
is an implementation detail with one current representation. Liquibase removes ticket-only columns
through a new changeset and requires an empty receipt table for that format replacement. Applied
changelogs remain immutable. Existing development receipts require a separate owner-authorised cleanup;
no journal data or global database reset is part of this change.

## Consequences

- Backend startup and receipt recovery require no preview key, configuration or in-memory preview.
- A stale proposal requires a new preview and explicit review before another confirmation.
- Edits invalidate the previous preview; no state is restored on browser reload.
- Application and PostgreSQL tests prove material drift, permissions, concurrency, atomic receipts,
  exact nanosecond instants and stable authenticated retries.
- Atelier owns the preparation and consequences; no bounded context or monetary calculation is added.
