# 0004 — Split the operator cost to the cent

## Status

Accepted on 2 October 2026 for the cost price report (glm-back#72). It replaces the rule "round once per line"
of the [business guide](../contexte-metier.md#partage-et-arrondi).

## Context

The client wants to justify every euro of a cost price by unfolding a nature and reading each clocking, with
the share of its operator. The report used to sum every part at six decimals and round once per line. Its
total was exact, but the amounts a clocking or a part would show did not always add up to it.

Rounding per line also breaks the client's rule that a person is paid once for each hour. An hour at
35.00 €/h on three workstations is worth 11.666… € on each. When these parts fall in different lines or
different elements, each line rounds its own part: 3 × 11.67 = 35.01 € for an hour that cost 35.00 €.

## Considered options

- Round once per line — rejected: the displayed parts do not add up, and the operator's hour drifts across
  lines and elements.
- Round each part independently — rejected: every displayed sum adds up, but each split creates or loses up to
  half a cent. Halves of cents are frequent with two workstations and always rounded up, so the drift is biased.
- Round the operator's cost once for each sharing window, then split it in whole cents — **kept**.

## Decision

The operator's cost is split per **sharing window**: a maximal period where the set of workstations the operator
occupies does not change and its divisor is known. Clocking boundaries that leave this set unchanged do not cut
a window, so an hour that is not shared is never cut into rounded pieces.

In a window, each clocking part is worth `rate × duration ÷ workstations`, computed from milliseconds at a
working scale. The window total is rounded half up to the cent. Each part first receives its amount rounded down
to the cent; the missing cents go to the largest remainders, then to the clocking that **started first**, then
to a stable order of the part's own values. The result is the same whichever element is read.

Identical parts in a window, the same workstation over the same period at the same rates, form one share and
receive the same amount. This keeps the existing client rule: several activities on one workstation count as one
workstation, and each element still pays the full share.

A part without a human rate is worth zero. A window made uncertain by an activity to resolve keeps an unknown
human cost, as before.

The machine cost is never shared: it is rounded once per clocking. A line and the report only add amounts that
are already rounded.

## Consequences

### Positive

- An operator's shared hour is worth exactly its cost, across lines and elements (11.67 + 11.67 + 11.66).
- Every amount the screen shows adds up exactly to the total above it.
- Halves of cents are no longer rounded up one by one.

### Negative

- Two identical shares can differ by one cent (13.13 € and 13.12 €); the "started first" rule explains it.
- Cost prices already read can move by a few cents: the scenario with three shared workstations goes from
  73.33 € to 73.34 € because the clocking that started first receives the remaining cent.
- The rule is longer to explain than "round once per line".
