# 0014 — Render the cost price PDF from an HTML template

## Status

Accepted on 10 October 2026 (glm-back#79, feature glm-back#77). Extended on 12 October 2026 to the operator activity
report (glm-back#154, feature glm-back#151).

## Context

The PDF is the document the manager hands to the customer, in two versions: a summary (one line per nature) and a
detailed one (plus one line per pointage). It must look the same whatever the browser, so the back generates it
(the browser print was rejected). It lays out the same `CompteRenduDuCout` as the workbook
([ADR 0013](0013-build-the-cost-price-workbook-with-apache-poi.md)): no amount is recomputed, a cost of 0 € is not
shown. A4 landscape, header and footer repeated on every page with the page number, a pointage never cut between two
pages, readable when printed in black and white.

## Considered options

- Drawing the PDF in Java with PDFBox or OpenPDF — rejected: every layout change becomes coordinates in code, and
  page breaks, repeated table headers and running headers have to be written by hand.
- A headless browser (Chromium, Playwright) — rejected: a heavy runtime to ship and run beside the application.
- JasperReports — rejected: a reporting engine and its own designer for two pages.
- **An HTML/CSS template rendered by OpenHTMLtoPDF** — **kept**.

## Decision

- `io.github.openhtmltopdf:openhtmltopdf-pdfbox` 1.1.97 (the maintained fork, on PDFBox 3) renders the page. Its
  CSS paged media support gives `@page` size and margins, running elements for the header and footer,
  `counter(page)` / `counter(pages)`, repeated `thead` and `page-break-inside: avoid` on table rows.
- `org.thymeleaf:thymeleaf` (without its Spring integration, so no view resolver is configured) fills the template
  `templates/coutderevient/compte-rendu.html`. The template only places strings: durations, amounts and dates are
  formatted in Java, in French, in the company's time zone.
- The fonts are embedded: DejaVu Sans regular and bold (Bitstream Vera licence), shipped in `src/main/resources/fonts`.
  They cover the accents, `€`, `→` and the narrow no-break space of French number grouping.
- Everything stays in `coutderevient/infrastructure/primary` (`DocumentDuCoutDeRevient`); the domain knows nothing of
  HTML or PDF.
- The operator activity report follows the same recipe in `syntheseheures/infrastructure/primary`
  (`DocumentDuRapportDActivite`, template `templates/syntheseheures/rapport-d-activite.html`, A4 portrait). It keeps
  its own copy of the few lines that drive OpenHTMLtoPDF and its own read of the company logo: bounded contexts never
  import each other, and moving the renderer to `shared` would buy little for some thirty lines.

## Consequences

### Positive

- The layout is HTML and CSS, reviewed like the mockups it reproduces.
- Tests read the produced PDF back with PDFBox and check its text and page format.

### Negative

- OpenHTMLtoPDF implements CSS 2.1 and paged media, not flexbox or grid: the template lays out with tables.
- The template must be well-formed XHTML.
- About 1.5 MB of fonts and the PDFBox, OpenHTMLtoPDF and Thymeleaf jars enter the application.
