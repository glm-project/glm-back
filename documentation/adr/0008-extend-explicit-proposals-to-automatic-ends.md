# 0008 — Extend explicit proposals to automatic ends

## Status

Superseded on 9 October 2026 by [ADR 0011](0011-regularise-an-automatic-end-directly.md) (glm-front#254). Accepted on
5 October 2026 for Atelier dossiers of automatic ends.

The guided proposals, the address states, `enConflit` and the continuations described below were removed with the
conflicts. The dossier of an automatic end survives, reduced to the expired activity, the punches of its key and the
bound of the end. The text is kept as the record of that protocol.

## Context

An activity that no real end terminated stops by itself at its deadline, thirteen elapsed hours after its
start, and carries an anomaly. The journal records nothing: the end is derived at the evaluation instant.
It is not a conflict, because a target that is only overdue contradicts nothing. The manager nevertheless has
to be able to settle it, and the only write that existed, `POST /api/atelier/suivis/{suivi}/regularisations`,
carries no command identity: retrying after a lost response would write a second end.

[ADR 0006](0006-confirm-explicit-resolution-proposals.md) already answers that risk for conflicts: preview, then
confirmation of the exact act, then a receipt that survives a lost response and a restart. The dossier route
and the protocol only needed to accept a second reason to open a dossier.

## Considered options

- Reuse `POST …/regularisations` from the back office — rejected: no command identity, no preview of the
  consequences, no receipt.
- Store the automatic end when it falls due — rejected: the deadline is judged at the reader's instant and a
  stored end would have to be undone by every correction of a start time, which breaks "the journal is the only
  truth".
- Open the dossier from the activity identity — rejected: an activity identity is the original opener and
  survives its correction, whereas the address of a dossier must name a fact that is active now.

## Decision

The dossier of an anchor adds the address state `FIN_AUTOMATIQUE`. The order of `kind()` stays: `INTROUVABLE`,
`ANCRE_ANNULEE`, `EN_CONFLIT`, then `FIN_AUTOMATIQUE` when the anchor opens an activity that `Activite.a` reads as
terminated automatically at the evaluation instant, otherwise `SANS_ANOMALIE`. The evaluation is exact to the
nanosecond and never stored: list, dossier, preview and confirmation each judge it at their own instant.

Two identities stay apart. The address of the dossier is the event id of the active opener
(`activite_d_atelier.ouverture_id`); the activity an act targets is the original `ActiviteId`
(`activites[].activite`). A client sends the second one in `activiteVisee` and the first one in the route.

Without a conflicting sequence, the activities of the dossier are the activity of the anchor when it is
terminated automatically. The perimeter lists the facts that open or target them, late gestures included. The
dossier adds `finAutomatique`, computed on that perimeter like `enConflit`: true while a concerned activity is
still terminated automatically, even when the address became `ANCRE_ANNULEE`. A client may therefore say
"anomaly settled" when neither `enConflit` nor `finAutomatique` is true, whatever the state of the address.

That set is stable before and after an act: it is the activity of the anchor, identified by its original
`ActiviteId`, which the correction of the opener keeps. Only the dossier of a conflicting sequence widens to the
activities that the facts of an act add. A late transition that targets the activity also opens another one,
with its own address, its own row in the list and its own deadline: once the transition is corrected, that
activity is not part of this dossier, so `finAutomatique` judges the anomaly of the dossier and nothing else. The
perimeter lists facts: it may still name the activity that such a gesture opens, which `activites` does not
contain.

The guided proposals follow the late gestures that target the activity:

| Late gestures that target the activity | Proposal                      | Act                                                                                    |
| -------------------------------------- | ----------------------------- | -------------------------------------------------------------------------------------- |
| none                                   | `REGULARISER_FIN`             | `REGULARISATION` of an end: target, operator and workstation of the opener, no instant |
| a transition                           | `CORRIGER_TRANSITION_TARDIVE` | `CORRECTION` of that transition, instant taken from it                                 |
| one or several ends                    | `CORRIGER_FIN_TARDIVE`        | `CORRECTION` of the latest end, instant taken from it                                  |

No instant is invented. The instant of a regularised end is typed by the manager; a correction takes the
instant of the real gesture it replaces. `REGULARISER_FIN` is never proposed when a late gesture targets the
activity: a transition makes every regularised end contradict it, whether the end is before or after the
transition, and with several late ends correcting any but the latest leaves the next one targeting an
activity that is already terminated. A transition is preferred to an end when both exist, because correcting
the end would contradict the transition. The replacement of a correction is a regularisation
(`OrigineDuPointage.REGULARISATION`) and may end the activity beyond its deadline.

Preview and confirmation accept the states `EN_CONFLIT` and `FIN_AUTOMATIQUE`; any other state is
`apercu-obsolete`. Roles do not change: reading is `USER` or `GESTIONNAIRE`, preview and confirmation
`GESTIONNAIRE`. An act that leaves or creates a contradiction is an accepted result: the dossier is then
`EN_CONFLIT` and the continuations name the active anchors.

`RestChoixDeResolution` still carries a `fait` only for an act that has one. For `REGULARISER_FIN` the `fait`
has no `instant` and is described by its own schema, `RestFaitARegulariser`, because the input schema
`RestFaitDeResolution` requires an instant and is not relaxed. The property is a `oneOf` of the two schemas.

Receipts already persisted keep their shape: `FormatDePropositionConfirmee` is untouched, and the three act
kinds it records are the existing ones. The new codes live only in the dossier answer.

## Consequences

- Gestion can settle an automatic end with the same preview, confirmation and receipt as a conflict, and a
  retry after a lost response returns the same receipt.
- A dossier of an automatic end has no `sequence`; clients read it from `perimetre`.
- The activity of an anchor that is still running is not part of its dossier: `SANS_ANOMALIE` has no activity.
- A mixed case (a late end and a late transition) offers the correction of the transition; the result may
  leave the end targeting a terminated activity, which is accepted and shown as a conflict.
- A guided proposal can therefore lead to a conflict, and the proposals are never simulated. With a restart at
  21:30 on the same key and a late end at 22:00 that targets the activity due at 21:00, the only proposal is
  `CORRIGER_FIN_TARDIVE`; the regularised end at 22:00 prolongs the activity past the restart, which ends it at
  21:30, and the end then targets an activity that is already terminated. The preview shows that `EN_CONFLIT`
  result, it is accepted (the contradiction is never refused), and the manager may prefer to cancel the late end
  instead.
- The scenarios of `atelier_anomalies_fin_automatique.feature` prove the deadline to the nanosecond, the three
  proposals, the accepted contradictions, the closed follow-up, the withdrawn permission, the missing
  workstation, the future instant and the concurrent write.
