# LECTAV 0.7.0 — initial curriculum planning and session tracking

## Regulatory distinction

The Catalan curriculum sets out competencies, assessment criteria and knowledge
areas (sabers). It does **not** mandate a universally fixed first textbook
unit or the same session order for every school. The initial mathematics
sequence is explicitly labelled a **proposed, editable didactic sequence**,
not an official "Theme 1". The teacher must validate it against the currently
applicable educational authority, academic year and school programming.

Initial pilot: Spain → Catalonia → ESO → 3rd year → Mathematics.
For other educational paths/subjects, the screen shows that a reviewed proposal
has not yet been loaded; LECTAV must not fabricate statutory syllabuses.

Primary references:
- https://projectes.xtec.cat/nou-curriculum/educacio-basica/decret-educacio-basica/
- https://portaldogc.gencat.cat/utilsEADOP/PDF/8762/1928585.pdf (Decret 175/2022)
- https://projectes.xtec.cat/nou-curriculum/ (current education portal)
- Academic version/date metadata must be independently verified before
  automatic rollouts to other autonomous communities or countries.

## Implemented pilot

- Each previously saved course has an **Temari** entry.
- For the initial supported subject, opening Temari seeds a proposed sequence
  of 6 topic units with 44 planned sessions in total. Their names and lesson
  distribution are planning suggestions, not legally mandated allocations.
- Teachers can edit and save topic titles on device.
- The listed sessions link into the existing optional Ollama/demonstration
  PDF generator; its topic field is pre-filled from the chosen session.
- Learners can see sessions and mark each as completed; completion totals
  are stored locally even after an app restart.
- No cloud upload, fabricated student attendance, or official assessment
  records are created.

## Future model — necessary for the full user request

A versioned academic-authority database should hold:
Country, jurisdiction, education level, institution when relevant, programme,
academic year, curricular norm/version/effective dates, official competencies,
assessment criteria, sabers, official sources and user-editable topic mapping.

Teacher course programme:
calendar, target class, teaching hours, unit order, situations of learning,
specific competencies and criteria, session objectives, differentiation,
materials, lesson PDF, daily exercises, worked answer keys, and avatar MP4 per
session. Generated content is a draft, distinct from reviewed/published work.

Learner workspace:
course enrolment when supported, lesson and PDF access, video position and
subtitles, exercises attempted/submitted/marked, progress per learner,
bookmarks, accessibility and review/resume modes.

All permanent generated assets should be indexed in a Room database and kept
in app-private storage or user-selected internal/SD folders. Uninstalling or
clearing app data can remove private files, so add explicit SAF export and
backup first. A future website may use privacy-respecting opt-in sync.

## AI curriculum analysis — not yet deployed

AI may assist in **extracting and mapping** official normative texts, but
cannot serve as the legal source of truth. A human-reviewed and provenance-
tracked import pipeline must verify official primary sources, dates and
amendments, separate legal curriculum from teacher sequencing, and mark
confidence/unverified suggestions. A later implementation can run a local
open model or self-hosted provider to generate the teacher's planned programme
and each lesson/sheet/solution set.

## Avatar

Per-session avatar videos are **planned but not implemented**. They will
require a separately audited FOSS audiovisual generation pipeline, voice,
subtitles, accessible controls, practical rendering hardware and user review
of media before GitHub/app distribution. Producing hour-long videos on an
ordinary Android tablet should not be assumed feasible.

## Current caveat

This is a **single-device prototype**, not a secure multi-user LMS.
Completion is not isolated into personal student accounts yet. Teacher
course sequencing and learner completion are currently stored in one local
device profile; do not use this as authoritative student grading data.
