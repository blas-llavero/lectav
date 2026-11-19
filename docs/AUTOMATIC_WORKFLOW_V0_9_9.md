# LECTAV 0.9.9 — Manual and automatic course generation

## Modes

The home screen offers **Manual mode**, retaining individual generation actions, and **Automatic mode**, which processes already saved teacher courses in unit/session order.

For each session, the initial queue creates **Theory PDF → Exercises PDF → Worked solutions PDF → Narration/transcript PDF**. Subsequent versions can create a local WAV after the transcript when an approved voice is installed. The next task starts only when the previous file has been written and indexed.

The exercise prompt uses the preserved lesson text; worked solutions use the exercise text, and narration uses the lesson content. Imported or legacy PDFs without recoverable source text require a reviewed transcript. The queue skips materials already associated with the same session and stops if the source, authentication, model, credits or connection is unavailable. Previously completed files are not overwritten.

The worker is foreground-based. Android may stop it when LECTAV is closed or suspended; reopening allows a new run from the missing documents. Background completion is not guaranteed.

## AI model configuration

- A default ChatGPT model can be chosen for all courses in Settings.
- Each material type can inherit that model or override it.
- Each course/session/material has an additional model selector.
- Resolution priority: **session override > material type > global default**.
- Only models actually offered to the connected account can be used; LECTAV does not invent model identifiers.
- Model preferences are included in encrypted backups, but authentication tokens and account authorizations are not.

The application implements a user-configurable maximum number of ChatGPT requests per batch, distinct from the account's real credits or quota. The persistent account-specific consent in Settings is checked before generation, and can be revoked.

## Export and supported media

LECTAV archives index-linked, accessible PDFs, WAV/MP3 and MP4 materials plus app configuration and lesson sources, within the configured limits. The automatic queue must never claim that podcast or avatar MP4 files exist if the relevant local generation engine is not implemented.

## Suggested acceptance tests

Generate a single four-document test session. Verify that each generated file opens, has the correct session association, and appears in the history. Interrupt and resume; ensure that completed materials are not duplicated. Test wrong model, disconnected account, missing original PDF text, quota error, and a backup/restore round-trip. Review mathematical exercise and solution content before use in class.
