# 0006 — Authenticate stateless resolution previews

## Status

Accepted on 3 October 2026 for the manual Atelier conflict-resolution contract.
The codec, preview and durable-confirmation endpoints are implemented in the generated OpenAPI contract.

## Context

A manager previews one correction, cancellation or regularisation against the revision of a workshop
follow-up. Previewing must not reserve an event identity, persist a projection or store a preview.
Confirmation must apply the exact proposed act, including its prospective event identity, after a
server restart. A browser-provided act cannot establish this guarantee.

## Considered options

- Persist previews — rejected: previewing would write and require cleanup of unused previews.
- Keep previews in memory — rejected: restart or another instance would lose confirmation evidence.
- Authenticate a self-contained reference — kept: the server can recover and verify its proposal.

## Decision

Atelier exposes a narrow application port for issuing and reading preview references. Its secondary
adapter uses the Java cryptography API; cryptographic bytes and key configuration remain outside the
domain. The domain's interpretation and transitions remain the sole authority on consequences.

The first format is `v1.<key-id>.<base64url(nonce || ciphertext || authentication-tag)>`, without
Base64 padding. AES-256-GCM encrypts and authenticates the payload with a freshly generated 96-bit
nonce and a 128-bit tag. The version and key identifier are authenticated additional data. Parsing
rejects unknown versions, unknown keys, malformed encodings and authentication failures as an invalid
reference, without revealing which payload component failed.

The authenticated payload binds the current tenant key and stable authenticated identity (JWT issuer
and subject), separately from the display author recorded with the act, follow-up identity,
pointage anchor, follow-up revision, command UUID, exact act, prospective event UUID when needed,
evaluation instant, expiry instant, and the material inputs and consequences displayed by the preview.
The act preserves the requested representation of its business instant as well as its exact
nanosecond value. The client submits the command UUID and reference at confirmation; it cannot
replace the payload's act, identities, tariffs or author.

Keys come from deployment secrets, with one active key identifier and a verification key ring shared
by all instances. Production keys are neither committed nor generated on process startup. Local and
test profiles use distinct explicit test keys. During rotation, the former key remains readable for
at least the preview validity window. Removing a compromised key immediately invalidates its
unconfirmed previews; it does not remove durable receipts.

Deployment configuration uses `atelier.resolution.apercus.cle-active` and
`atelier.resolution.apercus.cles.<key-id>`; each value is a Base64-encoded 32-byte AES key. Key
identifiers contain 1–32 ASCII letters, digits, `_` or `-`. No production fallback exists.
`atelier.resolution.apercus.validite` is a positive ISO duration, defaulting to `PT15M`. The adapter
rejects opaque input longer than 16,384 characters before decoding.

The preview validity window is configured, initially fifteen minutes. Expiry is exclusive: an act
cannot be first confirmed at or after its expiry. Confirmation checks tenant, issuer and subject, address,
revision, current reference existence, authorisation and values actually copied from the referential.
It reevaluates the proposed result at the current instant and refuses a material change, including
crossing an activity's expiry. Display labels and the actual technical recording or cancellation
instant may change. A refused or expired preview requires a new preview and a new command UUID.
Withdrawing a previously validated operator, workstation or authorisation invalidates that preview.
Confirmation translates only these known referential refusals to `apercu-obsolete`; preparing a new
preview retains its business refusals, and technical failures retain their technical result.

Confirmation first looks for its durable receipt in the current tenant and matches the authenticated
identity (tenant, issuer and subject), follow-up, command and exact opaque reference stored with it. A matching committed receipt
remains recoverable after preview expiry or key rotation; it needs no renewed referential or preview
validity check. For a new command, the reference is authenticated and decoded before application.
A command reused with another reference is rejected. On first
application, event identity reservation, guarded aggregate update, projections and receipt insertion
belong to one transaction. A simultaneous confirmation of the same command waits for that transaction
and returns its receipt. No receipt observed by a read proves only `NON_ATTESTEE`; it never proves
that an in-flight transaction rolled back.

## Consequences

- Previewing remains read-only and references survive restarts and requests routed to another instance.
- Stable deployment keys and their rotation become an operational requirement.
- The authenticated payload is bounded input; the adapter enforces a size limit before decoding.
- Codec tests prove round-trip, tampering, context binding and rotation. Application and real-database
  tests prove expiry, material drift, concurrency and atomic durable receipt recovery.
- The reference contains no monetary calculation and introduces no new bounded context.
