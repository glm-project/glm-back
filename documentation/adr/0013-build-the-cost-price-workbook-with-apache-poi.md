# 0013 — Build the cost price workbook with Apache POI

## Status

Accepted on 10 October 2026 (glm-back#78, feature glm-back#77).

## Context

The manager hands the cost price of an element to the customer. The customer reworks it: filters, pivot tables and
macros. Amounts and durations must therefore be numbers, instants real Excel dates, and the pointages a named Excel
table whose column names the macros can rely on. The workbook must show exactly the screen's figures: it lays out the
`CoutDeRevient` that `GET /api/couts-de-revient/{elementId}` already computes, and recomputes nothing.

## Considered options

- A CSV file — rejected: no types, no number formats, no named table, and the separator and decimal mark depend on the
  customer's Excel locale.
- Building the workbook in the browser — rejected: the PDF is built by the back (glm-back#79), and both exports must
  share the same refusal rule and header, held by the domain.
- Writing the Office Open XML parts by hand — rejected: styles, shared strings and table parts are a large format to
  maintain for one workbook.
- **Apache POI** (`poi-ooxml`), the reference Java library for `.xlsx` — **kept**.

## Decision

`org.apache.poi:poi-ooxml` 5.5.1 is a compile dependency. It is used **only** in
`coutderevient/infrastructure/primary`, by `ClasseurDuCoutDeRevient`, which turns a `CompteRenduDuCout` into the bytes
of the response. The domain knows nothing of POI.

- Amounts are written from the already-rounded `Montant` values; totals are the domain's totals, not Excel formulas.
- Durations are decimal hours (`0.00`), amounts euros (`#,##0.00 "€";-#,##0.00 "€";""`): a cost of 0 € keeps the value
  0, so sums and pivot tables stay right, under a format that shows it empty.
- Instants are local date-times in the company's time zone, read through the port `FuseauHoraireDeLEntreprise`
  (Europe/Paris for now, as for the time sheets).
- Tables are named Excel tables (`TableStyleMedium2`, with filters) and the header rows are frozen.

## Consequences

### Positive

- The workbook is usable as is by the customer's pivot tables and macros.
- Tests read the produced bytes back with POI and check values, types and formats.

### Negative

- POI and its dependencies (xmlbeans, commons-compress, commons-io, log4j-api…) enter the application, about 20 MB.
- The workbook is built in memory: fine for a few hundred pointages, to revisit with `SXSSFWorkbook` if an element ever
  reaches tens of thousands.
