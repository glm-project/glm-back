# 0015 — Let the manager correct an operator's day

## Status

Accepted on 13 October 2026 (glm-front, "Corriger la journée"). It amends
[ADR 0011](0011-regularise-an-automatic-end-directly.md) on one point, "a regularised end is final", and refines the
facts that [ADR 0010](0010-judge-each-punch-when-it-is-received.md) compares a punch with.

## Context

The manager has three corrections to make on the day of an operator, and none of them was possible since the
corrections of events were removed (`b16c0b0`):

- **add** an activity nobody punched (a forgotten start and end, the end possibly falling the next day);
- **change the workstation** of an activity punched on the wrong one;
- **remove** an activity punched by mistake.

The journal is the source of truth and a fact is never modified afterwards. The reception rule of ADR 0010 judges
each punch on the journal as it stands, and the readers (hours summary, supervision, cost price) interpret the journal
by key. Whatever the manager does must reach every reader without any of them learning a new rule.

## Considered options

- Correct punch by punch (change a time, cancel one event) — rejected: the manager reasons on activities, and one
  request ("I was on the lathe, not the mill") would be several punches that recreate the incoherent sequences ADR 0010
  removed. This is also the reason `b16c0b0` removed the correction of events.
- Edit or delete facts of the journal — rejected: it rewrites history and breaks the bitemporal trace.
- Add facts that **target an activity** and let the one interpretation of the journal apply them — **kept**.

## Decision

**Where.** In the `atelier` context, which owns the journal, the follow-up and its revision, in a `correctiondesactivites`
sub-package of each layer (the way `gestionanomalies` is). The new facts and how they are applied stay in the core of
the domain (`SuiviDAtelier`, `JournalDAtelier`, `SequenceDActivites`, `RegleDeReception`), because every reader of the
journal must apply them.

**Three writes, on the follow-up**, reserved to the `GESTIONNAIRE`. Each carries the identifier of the entry, generated
once by the client and kept across resends; an identifier already in `evenement_d_atelier` answers 200 and writes
nothing, before any rule. A new entry answers 201, with no body. A stale entry answers `saisie-concurrente` (the
revision of the follow-up, as for a regularisation); the client rereads the day.

- `POST /api/atelier/suivis/{id}/activites` — `{id, operateur, poste, categorie, debut, fin}`. It writes an opening
  (`DEBUT` or `NON_CONFORMITE`, from the category) at `debut` and a `FIN` at `fin`, both with the origin
  `REGULARISATION`. The `FIN` targets the new activity (`activiteVisee`) and takes an identifier derived from the entry's,
  so a resend finds both facts. The end may fall the next day, and the overlap is checked on the whole activity. The
  nature, the hourly cost and the hourly rate are copied from the workstation and the operator, and the maximum
  duration in force is copied on the opening, as for a punch. The workstation is optional, like everywhere in the
  context.
- `POST /api/atelier/suivis/{id}/activites/{activite}/poste` — `{id, poste}`. It writes a `CHANGEMENT_DE_POSTE` that
  targets the activity, dated at the time of the correction, and copies the nature and the hourly cost of the new
  workstation; the hourly rate stays the activity's.
- `POST /api/atelier/suivis/{id}/activites/{activite}/retrait` — `{id}`. It writes a `RETRAIT` that targets the
  activity, dated at the time of the correction.

`RETRAIT` and `CHANGEMENT_DE_POSTE` are new types, always of origin `REGULARISATION`, never a gesture: only `DEBUT`,
`NON_CONFORMITE` and `FIN` are gestures, and the pointage route refuses the others (400).

**What can be corrected.** An activity that is finished, normally, by an automatic end or by a regularised end, in a
follow-up that is not closed. A running activity is refused (`activite-en-cours`): the pupitre holds it open. A removed
activity is unknown (`activite-visee-introuvable`). Because the two new facts are dated at the time of the correction,
they obey the invariant "no event dated after the closure": on a closed follow-up they are refused, with the new code
`evenement-posterieur-a-la-cloture` (never `suivi-d-atelier-cloture`, which keeps meaning a punch on a closed
follow-up, and the refusal of moving the closure before an event). The consequence is that a closure cannot be moved
before a correction already made.

**The habilitation** is checked on what places the operator on a workstation: the workstation of an add and the new
workstation of a change, against the habilitations as they are now. It is never checked on a removal, so that an
activity punched on a workstation the operator has lost since can be removed.

**The refusals**, in the order they are checked. Add: `suivi-d-atelier-introuvable`, `operateur-introuvable`,
`poste-de-travail-introuvable`, `operateur-non-habilite`, `fin-avant-debut`, `date-de-survenue-future`,
`evenement-anterieur-a-l-engagement`, `evenement-posterieur-a-la-cloture`, `chevauchement`. Change: `suivi-d-atelier-introuvable`,
`activite-visee-introuvable`, `activite-en-cours`, `poste-de-travail-introuvable`, `operateur-non-habilite`, `meme-poste`,
`chevauchement`, `evenement-posterieur-a-la-cloture`. Remove: `suivi-d-atelier-introuvable`, `activite-visee-introuvable`,
`activite-en-cours`, `evenement-posterieur-a-la-cloture`. An overlap is judged on the key (operator, workstation) of
the follow-up, between a new or moved activity and the others of that key; an activity that no end closed counts up to its
deadline, and a running one has no end.

**How the journal is read.** The interpretation stays in one place. A first reading gives the activities and binds each
end to the activity it closes, without looking at a correction. A `FIN` that targets an activity (a regularisation, the
end of an add) closes it whatever key it was written on. A pointed `FIN` closes no named activity: it closes the one its key
had running when the server received it, so the pointages of a key are replayed in the order the server received them
(registration date, then the order of the journal), not in the order of the gesture times. Removed and moved activities
stay in this reading, which only binds ends; the openings of an add never take part in it, since their end is targeted.
Replayed by gesture time instead, a pupitre punch accepted on a key a correction had freed would steal the pointed end of
the activity that used to hold it. A second reading applies the corrections: the activities a `RETRAIT` targets disappear,
and the activities a `CHANGEMENT_DE_POSTE` targets count on the workstation of the last one. An activity carries its
`porteur`, the fact that speaks for its workstation, its nature and its tariffs: its opening, or its last change. The
projection `activite_d_atelier` gains `porteur_id` for that; `ouverture_id` stays the opening of origin, which is the
address of an anomaly's dossier, and the cost price reads its tariffs from `porteur_id`. The projection drops the rows
of removed activities; a follow-up whose activities were all removed stays `INTERROMPU`, since its journal still holds
facts — the list of elements by cycle reads that state from the journal, not from the projection.

**The reception rule** judges a punch on the journal as it is after the corrections, restricted to its key:

1. only gestures bound `ANTERIEUR`, never a `RETRAIT` or a `CHANGEMENT_DE_POSTE` — dated at the time of the
   correction, they would make the server ignore every offline punch of the key until then;
2. the facts of a removed activity no longer count;
3. after a change of workstation, the facts of the activity count on the new key;
4. the `FIN` of an add counts like a regularised end: a punch of the pupitre on the same key, dated before it and
   received after the add, is ignored (`ANTERIEUR`) and audited, with no screen.

Removing an automatic end makes its anomaly disappear; changing its workstation keeps it open, on the new key.

**The read.** `GET /api/atelier/operateurs/{operateurId}/journees/{jour}?evaluation=…` gives the manager the day: the
operator, the day, its bounds (`debut`, `fin` and `finDuLendemain`, computed in the timezone of the company), the
operational duration, the activities that touch the day with their follow-up, element, workstation, state, portion of
the day and portions of other days, and what an add may choose (the follow-ups present that day with their engagement and
closure, and the habilitated workstations). The timezone is a port of this read only (`FuseauHoraireDeLEntreprise`,
fixed to `Europe/Paris` for now, like its twins in the other contexts); the core of the context still knows neither
a zone nor a calendar day. A portion is the real elapsed time between two instants, so the night of a clock change counts
its real duration.

## Consequences

### Positive

- A correction is a handful of facts; the trace (who, when, what) is the journal itself, with no table and no screen.
- Every reader recomputes the days an activity touches without a rule of its own: they read the projection, which is
  written from the one interpretation of the journal.
- A regularised end can be removed or moved, which ADR 0011 did not allow.

### Negative

- ADR 0011 said a regularised end is final; it now is not. A second regularisation of the same activity still answers
  `activite-deja-regularisee`.
- A punch kept offline during a correction is lost or ignored, as it already was for a regularisation.
- Binding a pointed end follows the order of reception. The reception rule only accepts a pointage that is not older than
  the facts of its key, so for the pointages it accepted this order is the order of the gestures; the two differ only
  for a key a correction has freed. Two pointages of one key registered at the very same instant are ordered by gesture
  time (a `FIN` before an opening).
- Moving the closure of a follow-up before a correction already made is refused.
- The raw journal of the hours summary still lists the gestures of a removed activity: it is the log of the punches, not
  of the activities. The corrections themselves are not listed.
