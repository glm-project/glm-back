# 0012 — Freeze the maximum duration on the opening punch

## Status

Accepted on 9 October 2026 (glm-back#117, part A of glm-front#254). It amends
[ADR 0010](0010-judge-each-punch-when-it-is-received.md).

## Context

The deadline of an activity is its start plus the maximum activity duration. That duration is a company setting the
manager can change (`parametrage`, glm-back#123). The deadline is read in many places: the reception rule, the reading
of the journal, the list of automatic ends, the regularisation, the shop-floor referential and the projection
`activite_d_atelier.echeance`, which the neighbouring contexts and the SQL filters read. That projection is recomputed
from the journal at every new gesture on the follow-up.

## Considered options

- Read the setting whenever a deadline is needed — rejected: an activity already open would change deadline the day the
  manager changes the setting, a punch received earlier would be judged differently when it is replayed, and the SQL
  readers would need the setting as a parameter.
- Apply a change to the activities already open, by recomputing the projection — rejected: a reader at 20:59 could see
  an activity running that an earlier reader saw ended.
- Copy the duration in force on the event that opens the activity, and derive the deadline from it — **kept**.

## Decision

A `DEBUT` or a `NON_CONFORMITE` that the reception rule accepts reads the duration in force through the port
`MaximumActivityDurations` of the shared kernel and writes it on its journal event (`EvenementDAtelier.dureeMax`,
column `duree_max_secondes`). The activity takes its deadline from its opening event: `Echeance.apres(start, duration)`.
Nothing else reads the setting: an end, a regularisation and an ignored punch carry no duration and call no port.

**There is no retroactivity.** An activity keeps the duration in force when it began; the manager's change applies to
the activities opened afterwards. The same copy pattern already holds the hourly cost, the hourly rate and the nature
of the operation.

**No fallback for an opening without a duration.** The databases are purged with this change, and the event refuses to
exist without it.

## Consequences

### Positive

- One deadline per activity, the same for every reader and every replay, whatever the manager sets later.
- The projection and the SQL filters need no setting: they read the deadline.

### Negative

- A shorter duration does not end the activities already open earlier than their deadline: the manager waits for them or
  regularises them once they are due.
- One column more on every opening event.
