# 0009 — Evolve the API without breaking its clients

## Status

Accepted on 8 October 2026, first applied to the product categories (glm-back#100: #103, glm-front#249, #107).

## Context

glm-front consumes the API through types generated from `documentation/openapi.json`. The back and the front are
two repositories, built and deployed separately: neither is ever guaranteed to run the same version as the other.
The front runs as two applications, gestion and the offline-first pupitre, and a pupitre can keep an older bundle
in cache for hours.

Product categories replaced the element `type` (`ORDRE_DE_FABRICATION | PRODUIT`) by `categorie`, a code that each
company declares. The first version of #103 renamed the field in the requests and in every response at once. A back
deployed before the front would then have refused every element creation sent by the deployed front, and that front
would have read `undefined` where it expected a type. Resetting the databases, which was acceptable because no
client was deployed yet, does not help: the break is in the contract, not in the data.

## Considered options

- Rename in one step and deploy back and front together — rejected: it relies on a synchronised deployment that
  nothing enforces, and on a pupitre that has already reloaded.
- Version the routes (`/api/v2/...`) — rejected: it doubles controllers, DTOs, OpenAPI and tests for a change
  that only affects a field, and gives no date for removing `v1`.
- Expand, migrate, contract — retained.

## Decision

A change to the API that an existing client would notice is delivered in three steps, each with its own ticket,
and each step leaves every deployed combination of back and front working.

1. **Expand (back).** Add the new shape next to the old one, without removing or changing anything.
   - **Response:** return both fields. The old one is computed from the new one, and marked
     `@Schema(deprecated = true)`, with its former `allowableValues` and its former `requiredMode`, so that types
     generated before the change stay valid.
   - **Request:** accept both fields, with the new one taking precedence when both are present. The old one is
     optional and `deprecated`. A `@JsonIgnore @Schema(hidden = true) @AssertTrue` method keeps the 400 when
     neither field is sent.
   - **Translation:** the mapping between old and new values lives in `infrastructure/primary`, never in the
     domain. The domain only knows the new concept. When several contexts expose the old field, the mapping is
     written once in `shared/` and named as legacy (`shared/elementtype/LegacyElementType`).
   - **Scope of the old field:** it never has to represent what did not exist before. When a new value has no
     faithful old counterpart, the old field takes a default documented in its `@Schema` description, for example
     `PRODUIT` for any category other than `OF`.
   - **Tests:** the expand step keeps tests that use the old field (scenarios, `OpenApiConfigurationIT`
     required fields), next to tests of the new one.
2. **Migrate (front).** The front regenerates its types and switches reads and writes to the new field. Nothing is
   removed from the back.
3. **Contract (back).** Remove the old field, its translation and its tests. The ticket is created during the
   expand step and is blocked by the front ticket. It is merged only once that front is deployed everywhere,
   including on the pupitres.

Not concerned: what no client depends on yet (a new route, a new optional response field), and the database
schema, which follows its own migration rules. Even when a database reset is accepted, the API still evolves in
these three steps.

## Consequences

- Back and front are deployed in any order. A version of either side always works with the previous and the next
  version of the other.
- For one release, the OpenAPI document carries two fields for one concept. The `deprecated` flag and the
  description say which one to read. The front sees the deprecation in its generated types.
- A change costs three tickets instead of one. The contract ticket is the place where removal is decided, so the
  old shape does not stay by default.
- An error code is part of the API, but a response carries only one `type`. Renaming one therefore inverts the
  first two steps: the front first recognises both codes, then the back returns the new one, then the front forgets
  the old one.
