# LECTAV 0.8.8 — On-demand trimester curriculum planning

## Objective

After the teacher chooses an education pathway, course/year and subject, LECTAV can request a proposed trimester plan from a connected, authorized ChatGPT account. Nothing is preloaded as an official prescribed sequence: the teacher must review and approve the generated proposal.

## Workflow

1. Select a teacher course and subject.
2. Choose **Generate trimester plan with ChatGPT**.
3. Select the trimester, proposed number of sessions (6–48) and available account model.
4. Generate a proposed index with topic units and specific session titles.
5. Review each title and amend it before saving.
6. Save the accepted plan to the local course plan repository. Existing plans from other trimesters remain intact.

The AI proposal may reference relevant official curricular excerpts when those documents have been retrieved, validated and explicitly selected. The generator must not imply that a proposed unit order is mandated by Catalan regulation. Any claim of official alignment should include source and version context.

## Session metadata and replacements

Every unit stores a stable ID, title, trimester, number of sessions, origin and editable session titles. Lesson names are prefilled into the theory/exercise/solution generators, linking each saved file to its exact session.

Replacing a trimester resets affected session progress only after a visible confirmation. New unit IDs prevent PDFs from an older approved plan from being silently attached to a different lesson.

## Constraints and acceptance tests

Teacher-only edits require a saved course. Proposal structure must contain an allowed number of units and sessions and meaningful titles. Verify that generated session counts exactly match the requested count, that other trimesters remain unchanged and that restored backups preserve the plan. Teacher review of curriculum and generated mathematics remains essential.
