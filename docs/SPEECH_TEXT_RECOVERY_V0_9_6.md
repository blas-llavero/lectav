# LECTAV 0.9.6 — Recover narration text from visually rendered PDF lessons

## Root cause

The local PDF exporter uses Android WebView drawing to produce visually formatted pages. Although the PDF displays correctly, it may not contain an extractable text layer, so loading its text into the narrator can return an empty script.

## Implementation

When LECTAV generates a new PDF lesson, it also saves the original Markdown source locally in the app's private storage. The narration screen prefers that source text when the complete lesson is requested. This avoids unreliable text extraction from PDFs whose pages were painted onto a canvas. The PDF appearance and the preserved original are independent.

For older PDFs that have no source companion, teachers can paste and review the narration script manually and associate it with that course material. The script is stored by material ID. PDF text extraction remains a fallback only where an actual text layer is available.

Encrypted backups can contain eligible lesson/transcript text companions alongside the documents, so the script remains recoverable after transferring a valid archive. The restore operation must preserve both the PDF and its matching original text rather than attaching a script to the wrong material.

## Limits and tests

The original text has a defined maximum character count. Some page-range operations may require extraction instead of full-source reuse. Test freshly generated PDFs, older painted PDFs, manual transcript entry, interrupted synthesis and encrypted restore. Verify that the restored narration script matches the corresponding lesson. Do not upload private lesson data or student information to a public repository.
