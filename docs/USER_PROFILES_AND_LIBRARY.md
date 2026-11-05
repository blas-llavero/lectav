# LECTAV — user settings, course libraries and offline access

**Status:** approved future requirements; **not implemented in the 0.2.0 APK**.

**Scope:** Android smartphone/tablet and planned browser-based course platform. Teacher and learner workflows must work in **both** clients. This document does not authorise upload of any photos, video, audio or user-generated materials to GitHub.

## 1. Saved user configuration

Persist independently of any single screen:
- User role and chosen workspace (teacher, learner or both); role changes must not erase data.
- Country, education jurisdiction/territory, education type/pathway, institution where applicable, programme/qualification, course/year/level, subject/module and currently open topic.
- Interface language override (one of the 10 supported initially, or follow device settings) and separate optional teaching/subtitle languages when available. Explicit selection should override device language, but users can return to auto-detect.
- Subtitle position/size, accessibility settings, preferred download location and optional recently opened content.
- Several **saved course selections** rather than only the last choice; reselecting a course restores its workspace.

Use canonical curriculum IDs and edition/academic-year identifiers rather than names as persistent keys. A newer curriculum must not silently modify content associated with a previous edition.

## 2. Teacher workspace: My courses

Each course appears as a card or row with:
- Official title, subject, level, country/region and academic edition.
- **Counts of generated artefacts:** lesson PDFs, exercise PDFs, answer keys, avatar MP4 session files and other versions (not merely number of times downloaded).
- Timestamp of latest content generation/change and last opened lesson; offline/online state.
- Actions: Open, Create lesson, Generate exercises, Download/Export PDF, Download/Export MP4, Play MP4, View PDF, Review/Edit and (where applicable) Share/Publish.

**Default sort:** number of **unique generated materials** in descending order. Example counts are illustrative: a course with 8 PDFs + 3 MP4s (11 items) precedes a course with 4 PDFs + 2 MP4s (6). Do not count repeated downloads twice, and let users choose latest updated, alphabetic, or level sort. Show component counts so ordering is transparent.

Differentiate theory PDF, exercises, solutions and video session, and associate each file with a course/subject/topic and version. User-generated content should never be assumed to be officially accredited.

## 3. Student workspace: My learning

The student's course page lists available documents, exercises and avatar sessions plus:
- **Accessed/opened:** PDF/video/lesson opened; include last access time.
- **Reading status:** Not started / Opened / In progress / Marked as read. Do **not** automatically mark a PDF read when opened; offer a manual completion control.
- **Exercises:** Not opened / Opened / In progress / Submitted / Completed, preserving attempts and answers as appropriate.
- **Videos:** Available / Started / In progress / Finished; playback position stored for resume.
- **Study shortcuts:** favourites, bookmarks, continue from where I stopped, recently accessed and optionally completion overview.

Course progress is a learning aid; it must not automatically claim official examination results or qualifications. Avoid sharing minor learner records with third parties. Student data must remain private unless explicit educational-role permissions and consent/other valid legal basis permit access.

## 4. Android storage and removable SD

- **Storage on device:** local application database contains index, settings and progress. Publicly exportable PDF and MP4 assets use a user-selected location when requested.
- **Export one file:** Android's `ACTION_CREATE_DOCUMENT` lets users choose a location/name. Use the correct MIME type (`application/pdf` or `video/mp4`).
- **Choose export folder:** `ACTION_OPEN_DOCUMENT_TREE` grants access to a chosen folder where supported, including a removable SD card subfolder. Android 11+ restricts selecting some roots, including the SD root, so show a clear instruction to choose a permitted subfolder.
- **Remember access:** persist the granted `content://` URI permission when offered. Never hard-code `/sdcard/...` or demand all-files access.
- **Reliability:** handle removed SD, storage full, read-only media, renamed/deleted documents and revoked permissions. Keep course metadata even if the exported copy becomes unavailable; allow locate/re-export. Ask before overwriting, removing or duplicating.
- **Offline viewers:** open PDF inside Android (with accessible zoom/text assistance where available) and play MP4 with a FOSS-compatible player. Videos must support subtitles, accessible playback and resume. The website uses appropriate browser viewers and media playback.
- **Export is user-controlled:** never upload exported media or documents to GitHub. Media assets used in app distribution continue to require Blas Llavero's prior review and approval.

Reference Android documentation:
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/training/data-storage

## 5. Proposed implementation components

- **Android small settings:** Jetpack DataStore; migrate existing subtitle position safely from the prototype's SharedPreferences, preserving user choices.
- **Android relational records:** Room database for CourseSelection, Asset, LearningActivity, ExerciseAttempt, Progress and Bookmark. Associate rows by stable IDs, not display-language labels.
- **Android filesystem:** Storage Access Framework + `ContentResolver` for user-chosen destinations; avoid unnecessary permissions.
- **Web client:** same core workspaces and user-visible states; offline caching of permitted assets as feasible, with transparent controls.
- **Cross-device sync (future):** opt-in server-side user accounts, scoped authorisation, encrypted communication, conflict resolution for offline edits, export/delete data tools, and EU privacy and children-safety requirements. Sync never implied by merely enabling Android storage.

## 6. Acceptance criteria

1. Close and reopen Android; previous country, territory, level, course, subject, language and subtitle position are restored.
2. Teacher creates/generates several items in two courses; **My courses** ranks by number of unique generated content items and correctly breaks ties by last updated/title.
3. Teacher exports a PDF and MP4 to internal user-selected storage and, where supported, to an SD subfolder. Each file opens from **within** LECTAV while offline.
4. Removal of the SD does not crash the app or destroy metadata; it prompts the user to reconnect or pick another storage location.
5. Student opens a PDF; it is recorded as **opened** but not yet **read**. Manually marking it read updates the state. Accessed exercises remain different from submitted/completed ones.
6. Student can resume the last course, topic, reading place and video playback position where supported.
7. Teachers and students can access equivalent course-management/study workflows on both Android and web once the web platform exists.
8. All generated PDF/video contents stay on device or the explicitly chosen storage location unless the user authorises separate upload/sync.
9. The app still respects Android navigation bars, supports accessible controls, and does not include unapproved video, audio or images.

## Delivery sequence

A. Persist currently available selections and language/calibration options in the Android prototype.
B. Add local course/library data model and a basic My courses/My learning interface with test/example records clearly labelled.
C. Add functional generated-file registration, PDF viewing, MP4 playback and safe export to local storage/SD when engines exist.
D. Add activity states, bookmarks and progress tracking.
E. Implement equivalent web UX and optional privacy-safe cross-device sync.

Keep the short-term curriculum and generation work incremental. Do not present this specification as functionality already delivered.
