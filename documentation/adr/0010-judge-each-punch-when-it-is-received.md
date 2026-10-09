# 0010 — Judge each punch when it is received

## Status

Accepted on 9 October 2026 (glm-front#254). It replaces the reading of contradictory punches as conflict sequences
that the manager resolves, and amends [ADR 0001](0001-generate-event-identities-in-the-offline-first-pupitre.md) and
[ADR 0002](0002-let-the-pupitre-turn-a-pause-into-activity-stops.md).

## Context

Pupitres punch offline, several pupitres can serve one operator, and a gesture reaches the server hours after it
happened. The earlier model accepted every punch, kept contradictory ones in the journal and marked their sequence as
"in conflict" for the manager to resolve: seven reasons, an intention and a target on every punch, previews,
confirmations, corrections and cancellations. Most of those conflicts were a double tap or a late replay, and the
engine that read them was the largest part of the contexts that depend on the workshop.

## Considered options

- Keep the conflicts and make them rarer — rejected: the sequences, the intention, the target and the acts of
  correction all stay.
- Refuse the inconsistent punch to the operator — rejected: a replay arrives hours later, to an operator who has moved
  on, and only the closed follow-up is worth showing.
- Ignore the punch without a trace — rejected: nothing would explain a missing activity.
- Judge each punch at its arrival, ignore the inconsistent ones and audit them — **kept**.

## Decision

**A punch is one of three types:** `DEBUT`, `NON_CONFORMITE` and `FIN`. It carries no intention and no target. The
key is the operator, the follow-up (the OF) and the workstation, and a key has at most one running activity. A `FIN`
closes the running activity of its key.

**The server judges the punch at its arrival**, first come first served. It first looks the identifier up
([ADR 0001](0001-generate-event-identities-in-the-offline-first-pupitre.md)): a replay answers 200 even if the follow-up
has been closed since. Then come the existing controls (body, closed follow-up for a `DEBUT` or a `NON_CONFORMITE`,
future gesture time, operator or workstation unknown, habilitation), and only then:

1. `ANTERIEUR`: the gesture time is strictly older than the latest accepted time of the key, regularisations included.
   An equal time passes. A `FIN` that is not later than the start of the activity it would close is also `ANTERIEUR`:
   no activity has a zero duration.
2. The deadline, judged on the gesture time: it is reached when that time is greater than or equal to the start plus
   the maximum duration. A reached deadline counts as ended.
3. The table:

| State of the key                                         | `DEBUT`                  | `NON_CONFORMITE`         | `FIN`                                                                                                      |
| -------------------------------------------------------- | ------------------------ | ------------------------ | ---------------------------------------------------------------------------------------------------------- |
| Nothing running (never opened, ended, closed or expired) | accepted                 | accepted                 | ignored: `APRES_ECHEANCE` if the latest activity of the key is expired and unended, else `AUCUNE_ACTIVITE` |
| An activity running                                      | ignored: `DEJA_EN_COURS` | ignored: `DEJA_EN_COURS` | accepted                                                                                                   |

**An ignored punch never enters the journal.** It writes one row in `pointage_ignore_d_atelier`: identifier, follow-up,
operator, workstation, type, gesture time, reception time, one of the four reasons and the identifier of the latest
accepted punch it was compared with. The table has no key and no constraint, only an index on the identifier, so that
nothing can prevent an ignored punch from being written. It is read in the database, with no endpoint and no screen.
The answer is 409 `pointage-ignore`, raised after the transaction that wrote the audit has committed, so the refusal
does not cancel it. The pupitre never displays it: it removes the local effect and realigns on the referential. The only
refusal shown to the operator is `suivi-d-atelier-cloture`. The follow-up lock is taken before the judgement, so an
ignored punch takes it too.

**The journal is read by gesture time**, the `FIN` before the opening at an equal time, then by identifier. Thanks to
`ANTERIEUR` this equals the order of arrival for the punches of the pupitre, and a regularisation that arrives late
falls in its place. The deadline is read the same way, without a reading instant.

**The maximum duration has one source**, the company setting of the `parametrage` context (13 hours until the manager
sets another one). It reaches the workshop and the pupitre through a port of a small shared kernel
(`shared/activityduration`, in English like the rest of `shared/`), which `parametrage` implements: neither `atelier`
nor `pupitre` imports the other, or `parametrage`, and no domain codes a constant. The referential sends it as
`dureeMaximaleDActivite`, an ISO 8601 string such as `"PT13H"` or `"PT8H"`. An activity keeps the duration in force
when it began ([ADR 0012](0012-freeze-the-maximum-duration-on-the-opening-punch.md)).

**Idempotence** follows [ADR 0001](0001-generate-event-identities-in-the-offline-first-pupitre.md): the event table,
then the audit, then the judgement. The table also covers a punch that arrives after the closure: it closes the running
activity of the key, so a later `FIN` finds nothing running (`AUCUNE_ACTIVITE`, or `APRES_ECHEANCE` if the activity had
already expired).

## Consequences

### Positive

- The manager handles one case, the automatic end, in one view.
- The journal has one reading, with no sequence, intention, target, relaunch or absorption.
- The pupitre sends only types and times, and the contexts that read activities lose their conflicts, `A_RESOUDRE`
  states and incomplete totals.

### Negative

- An ignored punch is lost to the accounting. Only the audit table remembers it, and nobody reads it yet.
- First come first served: a non-conformity punched at 10:00 that arrives after the stop of 12:00 is ignored
  (`ANTERIEUR`), and the work stays counted from the start to 12:00.
- A `FIN` exactly at the deadline is now `APRES_ECHEANCE`; it used to win.
- A wrong gesture cannot be corrected. The manager regularises the end of an expired activity
  ([ADR 0011](0011-regularise-an-automatic-end-directly.md)) and nothing else.
- The pupitre keeps the refusals in its local journal, and does not explain them to the operator.
- One operator per pupitre is assumed (version 1).
