# LECTAV 0.9.10 — Live generated-file history

## Feature

The bottom of Automatic mode contains a **Generated files** section showing material produced by LECTAV and saved in the course library, not just documents created during the current run.

Each entry shows the precise file name, generation date/time recorded by the tablet, course and subject, associated session topic and material type (theory, exercises, solutions, video/avatar, audio, podcast, transcript). An item is listed only if it actually exists in the library index.

The DataStore-backed list updates as soon as the associated file is saved. Items appear oldest to newest so recent files are at the bottom. The interface shows up to 80 entries, while the rest remain in the library.

## Timestamps and restoration

New material records include a `createdAt` timestamp in milliseconds. Encrypted backups preserve the timestamp through export and restoration. Older files may lack this information, in which case the app explicitly states **Date not recorded (earlier version)** rather than fabricating an import date.

## Device acceptance tests

Generate a new theory PDF, then exercises and solutions; verify labels, names and dates appear in order. Stop and restart the queue and check for duplicates. Export and restore a backup on a test tablet and verify timestamps and material associations. Confirm media entries appear only when real playable files exist. GitHub Actions compilation does not validate runtime UI behavior.
