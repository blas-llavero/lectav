# LECTAV v0.4.0 — My courses (initial Android library)

## Shipped

- Users can save multiple official **course/subject selections** for the Catalonia catalogue.
- Home → **My courses** opens locally persisted lists, independently filtered by **Teacher** and **Learner**.
- Home → select ESO or Batxillerat/year/subject → **Save this course** inserts it into the active role's collection. Saving an already saved selection never creates duplicates.
- **Open** restores the curriculum selection and last access time; **Remove** is confirmation-protected and does not clear the selected subject or settings.
- The previous v0.3.0 DataStore preferences, language selector and subtitle migration remain unchanged.
- Identifiers use role and country/region/course/subject codes, not translated display labels.
- Saving and reading course entries requires no network or new Android permissions.

## Honest current limitations

A saved entry is **not** a generated course, official enrollment, or a lesson viewed. No PDFs, MP4 videos, assignments or avatar lessons exist in the app yet, so teacher material counters and learner activity tracking must not present any fabricated numbers or progress. Entries are sorted by last opened; sorting by content count will be implemented when actual file metadata exists. No web platform, account, cloud sync, device-to-device transfer or SD support yet.

For the prototype's limited number of saved selections, the items are stored as a JSON array inside a single Preferences DataStore value. This should be migrated to a Room database once the content/library tables (assets, learning progress, exercise attempts) are created. The list is capped at 500 entries on read to avoid unbounded parsing.

## Manual tests

1. Upgrade in place from v0.3.0; verify language, last course and subtitle calibration are not lost.
2. Save two different subjects in teacher mode; reload application and reopen each.
3. Saving the same subject again does not duplicate it.
4. Switch to learner mode: it starts with an independent list. Save a learner course; teacher list stays unchanged.
5. Remove a course and verify the confirmation. Cancel removal leaves the entry intact; confirming removes it but does not remove the active selection.
6. Rotate screen, including on a tablet; back navigation and scrolling remain accessible.
7. Open app without Internet and verify all saved entries are still accessible.
8. No generated content or learner completion status is implied by these course bookmarks.

## Next steps

Implement actual content creation and versioned file metadata, then derive teacher sorting from real generated PDF/video counts; implement student reading/exercise access state, Android local PDF/MP4 playback/export to internal or permitted SD folder, and subsequently a web client.
