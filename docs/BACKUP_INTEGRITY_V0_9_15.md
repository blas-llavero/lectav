# LECTAV 0.9.15 — Verified encrypted backups and restore diagnosis

## Problem addressed

Some users reported being able to import a configuration archive while PDF and media files were not visible after restoration. Historically, LECTAV exported **only files in the saved material index**, not arbitrary PDFs elsewhere on the device. A settings-only archive could therefore be mistaken for a complete materials backup. A successful restore message did not independently prove that the archived bytes were readable or that every material index entry had been persisted.

This change improves validation and error reporting. **It does not recover files that were never contained in an old backup.**

## Portable format

The supported extension is `.lectavbak`. The file is an authenticated AES-256-GCM encryption envelope with an internal ZIP payload. Renaming it to `.zip` will not make it readable by ordinary ZIP tools; the encrypted file must be inspected or restored through LECTAV.

Metadata is stored in the encrypted `manifest.json`; source files use `files/0000.pdf`, `files/0001.wav`, etc. Source narration text, when present, uses `sources/0000.txt`.

New backups also include:
- `checksums/0000.json`, etc., one per indexed material.
- Recorded original ZIP member name, byte count and SHA-256 digest.
- The full list of saved courses, their material associations, AI settings and saved lesson/session metadata.

Older backups without per-file checksums remain supported, subject to authentication, archive validation and the original file-format constraints.

## Export workflow

1. On the source installation open **Settings → Backup**.
2. Before exporting, examine the selected course count and **selected material-file count**. If this is zero, the archive will contain settings but no PDFs, WAVs or MP4s.
3. For a complete backup choose **Export everything** rather than a selective export.
4. Enter and confirm a password of at least 10 characters.
5. Choose a destination in the Android document picker.
6. LECTAV writes the archive and then **opens and independently inspects the actual exported file**. A successful message is displayed only if it can decrypt the output, verify its file list and hashes, and confirm the expected number of indexed materials.
7. Keep the original archive and password. Do not uninstall the source installation before testing a restore on a second device or safe test installation.

The archive is limited to 1 GiB of material bytes and 500 material records. Export fails explicitly if indexed content cannot be read. The external document provider must permit reopening the newly saved archive for verification.

## Safe inspection before restore

The restore confirmation dialog now includes **Inspect archived documents**. With the original password, this operation verifies the encrypted archive **without modifying any local data**. It reports the saved course count, material-file count and total material bytes.

- **Zero material files:** This backup does **not** contain the desired PDFs/audio/video. Recover those files from the source installation or a different archive. The app cannot reconstruct absent content.
- **Nonzero material files:** Restore, then open the course library and verify each PDF and recording.
- **Validation error:** Do not uninstall LECTAV or delete the archive. Preserve the exact error for diagnosis.

## Restore safeguards

Before any user data is overwritten, LECTAV authenticates and decrypts the entire archive; validates expected ZIP members; confirms each file has nonzero content; validates PDF headers; verifies checksums when available; ensures every material is attached to a course exported in the same archive; creates private target URIs; and verifies the restored material is readable.

After saving the library data, LECTAV checks that all restored material IDs are present in DataStore before reporting success. A full backup that contains **no** materials will refuse to erase a target installation that already contains documents. Selective backups retain unrelated courses and materials.

A rare filesystem or DataStore failure during the multiple-stage save can still lead to partially persisted changes; keep the original archive and retry rather than uninstalling.

## Regression checklist

1. Export a real course with one generated PDF, an exercise PDF and one WAV.
2. Inspect the newly exported file and verify the exact count and nonzero byte total.
3. Restore it in a separate test installation with ChatGPT disconnected.
4. Open each course, PDF and audio; inspect names and session links.
5. Export a selective backup with one file and confirm unrelated target courses survive.
6. Attempt to restore an archive with no materials into a populated library: it must refuse to erase the documents.
7. Test an older `.lectavbak` and confirm backwards compatibility.
8. Test corrupted, truncated and wrong-password archives: restoration must fail before modifying the library.
9. Test a revoked imported-file URI: export must report failure, not success.

An Android GitHub Actions build verifies compilation; it cannot replace an actual two-device backup/restore test.
