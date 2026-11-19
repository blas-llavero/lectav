# LECTAV 0.8.7 — Encrypted backup and descriptive filenames

## Exported filenames

LECTAV proposes descriptive PDF and video filenames when exporting individual materials. Users can change the proposed filename in the Android document picker. Course and session information is stored separately in the internal material index.

## Complete backups

Open **Settings → Backup → Export everything** to produce a password-protected `.lectavbak` file. Choose a destination in shared internal storage, an SD card or a selected document provider. A backup is **not an ordinary ZIP**: its ZIP payload is encrypted with AES-256-GCM and authenticated using a password-derived PBKDF2-HMAC-SHA256 key (220,000 iterations). The password must be 10–200 characters long, and LECTAV does not retain it.

The archive is intended to include:
- Country, region, education pathway, year, subject, selected role and saved courses.
- Teacher-approved trimester plans, unit and session titles, and recorded progress.
- AI-generation settings, selected models and general non-secret app preferences.
- Accessible indexed theory, exercise, solution and transcript PDFs; audio, podcasts and MP4 materials when they actually exist.
- Original text companions for applicable PDF lessons, allowing narration to be regenerated.
- User-selected interface locale and curriculum source information.

The current full backup has a 1 GiB material limit and up to 500 indexed materials. Files not indexed in the LECTAV library are **not** included merely because they exist on device storage. A complete archive with zero materials is settings-only and must not be mistaken for a document backup.

**Security:** OAuth access/refresh/ID tokens, keystore data, login secrets and ChatGPT usage authorization are excluded. Reconnect and reauthorize ChatGPT after installing or restoring a backup.

## Restoring a backup

1. Keep the original archive and password. Do not uninstall the old app before verifying the backup.
2. On the target device open **Settings → Backup → Select .lectavbak file**.
3. Choose the encrypted archive and enter its original password.
4. Disconnect ChatGPT before starting restore.
5. LECTAV decrypts and authenticates the archive and validates metadata and files before writing the material index.
6. **Full** restore replaces saved courses, plans and library metadata; **selective** restore merges the selected courses and files with existing content.
7. Open each course and verify that real PDF and audio files can be opened or played.

Restored files receive new local Android `content://` addresses; the old provider permissions do not transfer between installations. Imported materials become private LECTAV copies rather than references to the old file location.

## Integrity improvements prepared after 0.9.14

The pending integrity patch adds SHA-256/byte-count records for each archived material, accepts older backups without checksums, verifies extracted files and course associations, and prevents an empty full backup from erasing a populated library. **It cannot recover content omitted from a previously exported archive.** Existing archives must be tested on a separate installation before any destructive migration.

## Android debug APK signature warning

Debug APK signing may vary across builds. Android can refuse an update with a different certificate, and uninstalling deletes the app's private documents. Always create and verify a complete backup before uninstalling, and consider using a stable signing key for future releases. A successful GitHub Actions build is not a substitute for restoring the archive on an actual tablet.

Do not upload archives containing private student data to public GitHub repositories. Preserve password and archive separately.
