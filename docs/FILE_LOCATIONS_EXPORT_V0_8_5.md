# LECTAV 0.8.5 — File locations and Android export to chosen folders

## Private source versus shared storage

Generated PDFs are stored in an app-private folder under `filesDir/generated/`. Standard file explorers, including CX File Explorer, generally cannot browse this protected directory without special access. The fact that a PDF opens in LECTAV does not mean it has been copied to Downloads or an SD card.

Imported files remain at the location chosen through Android's Storage Access Framework and are referenced by granted `content://` URIs. Cloud and third-party document providers may not expose meaningful physical paths. The app must not fabricate an absolute file path for them.

## Explicit export

In Materials or the document viewer, use **Save a copy to a folder**. Android opens a `CreateDocument` picker for shared storage, removable media or another user-chosen provider. The original remains unchanged. The proposed filename includes useful material and session information, and users can edit it.

A successful export records the destination URI for later reference, subject to provider permissions and availability. Failure should be displayed and must not be confused with a completed copy. A user-selected cloud provider may upload a document; LECTAV itself does not automatically publish files.

## Safeguards and limitations

Do not request broad external-storage permission merely to export user-selected files. Verify source/read and destination/write streams, handle full SD cards and expired grants, and do not delete private originals when export fails. The Android document picker can represent a provider rather than a physical filesystem directory.

The encrypted `.lectavbak` feature is separate from saving individual PDFs. Users should test both when migrating devices.
