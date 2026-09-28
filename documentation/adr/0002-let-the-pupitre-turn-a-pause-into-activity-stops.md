# 0002 — Let the pupitre turn a pause into activity stops

## Status

Accepted on 28 September 2026. It replaces two rules of the `atelier` context: « the pause and the departure are
operator facts, written once, never copied into the element journals », and « never loop over the running elements to
propagate a pause ».

## Context

The product answers two questions: who works on what, and how much working time a manufacturing order has cost. It
does not pay the operators.

The pause is today a presence event. The working day of an operator records `ARRIVEE`, `PAUSE`, `REPRISE` and
`DEPART`, and its state can be `EN_PAUSE`. The activities stay open during a pause, and the effective time of an
element is the intersection of its raw intervals with the presence windows of its operator. Two written rules follow
from this model:

- the pause and the departure are operator facts, written once, and never copied into the element journals
  (`atelier/AGENTS.md`, `contexte-metier.md`);
- a client must never loop over the running elements to propagate a pause: a single presence gesture suffices
  (`atelier-api.md`).

The client describes pause, stop and resumption as « the same mechanism ». Pausing is stopping what one is doing;
resuming is starting it again. The global pause button, on the other hand, has never been validated first-hand: it
comes from a team meeting. This decision does not settle it; it moves it to the pupitre, which owns the screen.

Keeping the pause in the presence model also costs: the presence types and states owned by `atelier` are replicated
in `feuilledetemps`, `syntheseheures`, `coutderevient` and `pupitre`, each carrying the third state, and the payroll
question — amplitude or windows, pause deducted or not — stays open on a measure the product does not need.

## Considered options

- Keep the pause as a presence event — rejected: it keeps a payroll measure the product does not serve, and a third
  presence state in five contexts.
- Let the server stop every activity of an operator on a single pause gesture — rejected: the server would still know
  the pause, and resuming would require it to remember what the pause closed.
- Let the pupitre send one `FIN` per running personal activity, and on resumption one `DEBUT`, or one
  `NON_CONFORMITE` for the activity that was in non conformity, on the same workstation — **kept**.

## Decision

The back no longer knows the pause: neither `PAUSE`, nor `REPRISE`, nor `EN_PAUSE`, in any context. The presence
journal records `ARRIVEE` and `DEPART`; the presence state is `ABSENT` or `PRESENT`.

The pupitre turns PAUSE into one `FIN` per running personal activity on its workstation, and RESUME into one `DEBUT`
per suspended activity, or one `NON_CONFORMITE` for the one that was in non conformity, on the same workstation. The
server receives only stops and starts. The pause does not touch presence: an operator on pause stays present, and the
presence reports count the pause.

The two rules above become:

- the departure is an operator fact, written once; the pause does not exist for the server, the pupitre turns it into
  activity stops;
- the pupitre loops over the running activities of the operator, which is exactly what a pause is.

The presence itself — arrival, departure, working day — stays. Removing it is a later piece of work. The bound of a
forgotten activity does not change: amplitude threshold, abandoned day, presumed end.

The contract changes only through enumerations that shrink and descriptions: no route is renamed and no field is
removed. A Liquibase data migration removes old `PAUSE` and `REPRISE` presence events, including cancelled ones,
changes projected `EN_PAUSE` days to `PRESENT`, and recomputes their last known presence fact. Event identities remain
reserved to prevent reuse. No schema change is needed; the test pupitres are wiped.

## Consequences

### Positive

- Durations and costs of an element do not change: the reference scenarios, rewritten with a `FIN` at noon and a
  `DEBUT` at 1 pm before the pause was removed, return the same effective times and the same costs.
- The element journal says, on its own, when the work stopped and restarted.
- A presence state and two presence event types leave `atelier` and the four contexts that replicate them.
- The payroll question closes: presence does not pay.

### Negative

- On migrated data, an activity that was left open through an old presence pause now counts that pause in its
  effective time and cost. The data migration removes obsolete presence events but does not invent historical
  `FIN`/`DEBUT` events for each element.
- Correcting a wrong pause time took one presence correction; it now takes one per activity, on its end and its
  start.
- The server pause cut every activity of the operator; the pupitre pause closes only what its referential knows. An
  activity opened on another pupitre since the last refresh keeps running during the pause.
- The duration of a tile restarts from zero after RESUME: `depuis` becomes the instant of the new start. The
  supervision is affected the same way.
- The nature, the hourly cost and the hourly rate are copied again on resumption, as on any start: a rate changed
  during the pause applies to the afternoon.
- Each pause adds two events per activity to the follow-up journals.
- An abandoned day has a single presence window, so the whole day becomes presumed, where only the part after the
  last pause used to be.
- The supervision no longer tells an operator on pause from a present operator without assignment.
- Resumption only happens on the pupitre where the pause was taken.
- The presence reports count the pause.
