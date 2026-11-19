# LECTAV 0.9.1 — Accessible session materials and no artificial local ChatGPT attempt cap

## Removed restriction

The earlier installation-specific ChatGPT attempt counter must no longer lock the user out after a weekly or lifetime local threshold. Real usage and billing limitations are defined by the authenticated account and the service, not by a local counter that could become inconsistent after restoring an archive.

Later releases added persistent, revocable ChatGPT consent in Settings while preserving genuine account and model checks.

## Session-first filenames

Material naming was updated to begin with session details, for example:
- `Session 1 - Fractions and decimals - Theory.pdf`
- `Session 1 - Fractions and decimals - Exercises.pdf`
- `Session 1 - Fractions and decimals - Solutions.pdf`
- `Session 1 - Fractions and decimals - Avatar.mp4`

Android-invalid characters are normalized. Repeated exports should not accumulate duplicate labels.

## Direct access and linking

The course plan view provides an **Open** action for already linked theory, exercises, solutions and video resources. Materials imported in earlier versions that are not associated with a session can be linked manually after teacher review. The app should not infer a session from a file name alone.

Session associations, imported materials and user-approved progress belong in encrypted backups, together with the actual readable files. Files that have not been created cannot be represented as generated content.

## Verification

Check duplicate-free filenames, correct session links, existing files' availability, manual linking, library navigation, and a full backup/restore cycle. Confirm that old usage counters cannot re-enable a local block and that real account failures are displayed accurately.
