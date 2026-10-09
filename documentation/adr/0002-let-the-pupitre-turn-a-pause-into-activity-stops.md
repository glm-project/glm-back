# 0002 — Let the pupitre turn a pause into activity stops

## Status

Accepted on 28 September 2026 for the server/pupitre boundary. Scope amended to the delivered activity model
and its gestures; current rules are in the [business guide](../contexte-metier.md) and
[API contract](../atelier-api.md). This revised text preserves the reasons for that boundary.

Amended on 9 October 2026 (glm-front#254): a `FIN` no longer targets an activity. It closes the running activity of
its key, and the server judges it with the reception rule of [ADR 0010](0010-judge-each-punch-when-it-is-received.md).
The activities in conflict and the corrections that the earlier text mentioned no longer exist.

## Context

The product follows activities and the cost of manufacturing elements. The client described stopping
and resuming work as the same mechanism. The global button was reported in the August team discussion;
its current confirmation comes from the subsequent product decisions, rather than a new client quotation.

A pause needs to remember which activities may be resumed, their workstation and category. The pupitre
owns this interaction and its local memory. Making the server remember it would couple workshop facts
to the state of one shared screen and require a server protocol for resumption.

## Considered options

- Let the server stop all activities with one pause command — rejected: the server would own the pause
  and would have to remember which activities it had stopped.
- Let the pupitre send a finish for each actionable activity, then new openings on resumption — **kept**.

## Decision

The pupitre turns PAUSE into one `FIN` per actionable personal activity on its workstation,
with local memory of the activities to resume. RESUME creates a new `DEBUT`, or `NON_CONFORMITE` for a
suspended non-conformity activity, on the same workstation. It is an opening of its own. The server receives
only finishes and openings, and none of them names an activity.

Only actionable activities are paused and remembered. Expired activities are excluded from pause and global stop. Resumption opens the locally remembered activities anew.
The local expiry calculation writes no synthetic finish. The global stop finishes the remaining actionable activities
and durably clears resumption memory, including when no finish is necessary. The local operation keeps
the finishes and memory change atomic and retains historical and queued gestures.

## Consequences

### Positive

- Each element journal records when its work stopped and restarted.
- The server interprets activity facts while the pupitre owns pause and resumption.
- A new opening has its own stable identity and expiry; the finish stays replayable.

### Negative

- A wrong pause time cannot be corrected: a journal fact is never changed, and the manager only regularises the end of
  an expired activity.
- A pause can stop only activities known by the pupitre's referential. Work opened on another pupitre
  after its last refresh may continue.
- Resumption is available only on the pupitre where the pause was recorded; it requires its durable local memory.
- The elapsed indication starts again at the new opening; it is distinct from accounted duration.
- Nature, machine cost and human rate are copied again at resumption, as at any opening. A changed rate
  therefore applies to the new activity.
- Every pause and resumption adds two facts per resumed activity. A resumption that finds its key already
  running is ignored by the reception rule (`DEJA_EN_COURS`), and the pupitre realigns on the referential.
