# 0011 — Regularise an automatic end directly

## Status

Accepted on 9 October 2026 (glm-front#254). It supersedes
[ADR 0006](0006-confirm-explicit-resolution-proposals.md) and
[ADR 0008](0008-extend-explicit-proposals-to-automatic-ends.md), and amends
[ADR 0007](0007-read-addressed-workshop-aggregates-coherently.md).

## Context

With the reception rule of [ADR 0010](0010-judge-each-punch-when-it-is-received.md), inconsistent punches no longer
create an anomaly. The manager has one case left: an activity that no end closed before its deadline, the automatic
end. The earlier protocol (preview, confirmation of the exact act, receipt, guided proposals, corrections and
cancellations) existed to settle conflicts, whose consequences could not be foreseen. Placing the end of one expired
activity has no such consequence: the end can only fall between the start of the activity and a bound.

## Considered options

- Keep the preview, the confirmation and the receipt for this single act — rejected: they guard against consequences
  that this act does not have, and they cost two routes, a table, a fingerprint and a signed proposal.
- Send the end through the pointage route — rejected: the reception rule would ignore it (`APRES_ECHEANCE`), whereas
  the manager's end may pass the deadline.
- A direct write whose identifier the client generates once per entry — **kept**.

## Decision

**The dossier** (`GET /api/atelier/suivis/{id}/anomalies/{pointage}`) answers 200 for an automatic end that is not yet
regularised. An unknown follow-up answers 404 `suivi-d-atelier-introuvable`, any other punch 404
`fin-automatique-introuvable`; the front goes back to the list. It holds the element
(`elementId`, `designation`), the expired activity, the punches of its key, and `borneDeFin`: the earlier of the next
start on the key and the closure, or nothing.

**The list** is a `Page` of `RestFinAutomatiqueEnListe`, with no discriminator and no `nature` parameter.

**The regularisation** is `POST /api/atelier/suivis/{id}/regularisations` with `{id, activite, dateDeSurvenue}`. The
client generates `id` once per entry and keeps it across resends. The operator, the workstation and the type are
deduced from the activity. The write does not go through the reception rule, may pass the deadline, and has the origin
`REGULARISATION`. It answers 201, or 200 when the identifier is already in `evenement_d_atelier`, before any rule.

**The refusals**, in the order they are checked: `activite-visee-introuvable` (404), `activite-deja-regularisee`,
`activite-non-echue` (not an automatic end: the deadline is not reached, or a real end, a punch or the closure, ended the activity),
`date-de-survenue-future` (400), `fin-avant-debut` (the time is not later than the start), `fin-apres-borne` (the time
passes the earlier of the next start on the key and the closure) and `saisie-concurrente`, after which the front
rereads the dossier. Only a `FIN` of a regularisation carries a target (`activiteVisee`).

The addressed read of [ADR 0007](0007-read-addressed-workshop-aggregates-coherently.md) still serves the dossier.

## Consequences

### Positive

- One request, one view: no preview, no confirmation, no receipt table, no fingerprint.
- A resend after a lost response is harmless, because the identifier is in the event table.
- The dossier and the refusals carry everything the front needs to bound the entry.

### Negative

- The manager cannot see the consequences before writing; the bound stands in for the preview.
- A regularised end is final. Nothing corrects or cancels it, and a second attempt answers
  `activite-deja-regularisee`.
- The acts of the manager are not audited, and inconsistent ones are not refused beyond the bound. Both stay out of
  scope for now.
