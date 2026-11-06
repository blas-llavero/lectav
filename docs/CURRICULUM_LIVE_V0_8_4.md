# LECTAV 0.8.4 — live curriculum PDF and non-orphan paragraphs

## Issue: orphaned lead-in text ending with a colon

In v0.8.3, headings and tables were kept on one page, but regular paragraphs
such as "Resol els exercicis següents:" or "Els nombres es classifiquen així:"
could be separated from the exercise list or table they introduce.

The local `print-template.html` page-break planner now treats:
- HTML headings (`h1`–`h6`)
- Short introductory paragraphs that end in a colon
- Short "següents", "siguientes", "following", "a continuació" and similar lead-ins

as **keep-with-next** elements. Chains such as **heading → paragraph: → table**
are not broken. If linked content is taller than a complete A4 printable area,
LECTAV reports that the block must be divided rather than silently cropping it.
This is an initial semantic heuristic, not a complete professional pagination
engine; full-page long tables still need manual splitting.

## On-demand curriculum import (initial supported official PDFs)

Opening a saved course → **Temari → Consulta el currículum oficial** shows
a new screen. It opens with the *link* (not bundled content) to the official
Mathematics ESO curriculum PDF published by XTEC when that course is selected:
https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Matematiques.pdf

For other subjects, the user can supply a direct HTTPS official PDF link.
This pilot accepts document hosts belonging to XTEC/Generalitat, DOGC, BOE,
Spanish `.gob.es` and UK `.gov.uk` domains. This **does not** implement
automatic recognition of every regional/international government portal.
It also **does not** guarantee that a source's contents are the latest legal
amendment: the user must check version/effective date.

Pressing **Descarrega i extreu el currículum**:
1. Downloads the source over HTTPS from an allowlisted official domain, no
   redirects or credentials, size max 10 MB.
2. Reads up to 150 PDF pages locally using Apache-2.0 `PdfBox-Android`.
   Unsupported scanned-only PDFs show an explanatory no-OCR error.
3. Saves, in private Android preferences for the selected course, a capped
   extracted text excerpt (up to about 24k characters), source URL, retrieval
   timestamp, number of pages and SHA-256 fingerprint.
4. Shows source attribution and local text preview. **The official PDF is
   downloaded on demand**: it is *not* preloaded in the APK, and fetching it
   does not consume a ChatGPT request.
5. The local excerpt may be incomplete; users must consult the full official
   PDF before relying on the proposed plan.

Only if the user requests it, the account has opted in, and the usage-guard
confirmation is accepted, LECTAV sends **the excerpt** (up to ~22k characters),
selected course, subject, academic-year field and source reference to the
connected ChatGPT model through the existing plan-authorized Responses API.
This is counted as an AI generation attempt. No user/student personal records
are required. The AI output is required to be a JSON teaching **proposal** of
4–12 units and estimated sessions, never a falsely official "unit order".

Teachers receive a short editable `Title | sessions` list and an explicit
confirmation before replacing their previous programming; replacement can
reset completed-session marks on this shared single-device prototype.
Learners may consult a source or proposed sequence but may not overwrite
the teacher's course programme.

## Source of truth and legal caution

The current reference is the XTEC ESO curriculum for Decret 175/2022.
XTEC notes that *sabers* do not imply a mandatory chronological order.
An AI-generated plan should be explicitly named an **editable didactic
sequence**, with the actual official source preserved for verification.
Subsequent country/region implementations must add versioned official
authority registries, legislation effective dates, primary-text citation
mapping, and jurisdiction-specific validation.

Official sources:
- https://xtec.gencat.cat/ca/curriculum/eso/curriculum-175-2022/
- https://xtec.gencat.cat/ca/curriculum/eso/normativa/
- https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Matematiques.pdf

Library licence and source: https://github.com/TomRoush/PdfBox-Android
(Apache License 2.0).

## Testing

1. Create a PDF with a heading, a short paragraph ending in ":" and a table
   or an exercise list. Confirm the lead-in is not orphaned. Check all A4 pages.
2. With no ChatGPT sign-in, consult the linked XTEC PDF from a saved 3r ESO
   Maths course. Confirm extracted text, official source and retrieval date.
3. Reopen the screen offline; the text excerpt should still be cached.
4. With an eligible account, review LECTAV's weekly limit and credits
   settings before authorising AI analysis. Make sure the model produces
   a non-official proposed sequence rather than inventing statutes.
5. Edit one title and session count and save as professor. Reopen `Temari`.
6. In student mode, verify proposed topics cannot replace teacher programming.
7. For non-Maths courses, paste a direct official PDF URL; never claim the
   country/region/source has been legally verified automatically.

**CI builds are not device tests.** Verify the downloaded PDF format, extracted
text and draft proposal on the user's tablet before broader deployment.
