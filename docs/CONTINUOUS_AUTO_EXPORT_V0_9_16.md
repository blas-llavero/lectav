# LECTAV 0.9.16 — Continuous automatic generation and verified exports

## Automatic mode

Once the teacher's ChatGPT account and model permissions, local voice model (for WAV), persistent account authorization and export directory are configured, choosing **Automatic mode** starts the pending queue without an additional confirmation. LECTAV iterates saved teacher courses, their units and sessions in order.

The currently implemented sequence is:
1. Theory PDF
2. Exercise PDF, based on saved lesson source
3. Worked-solutions PDF, based on saved exercise source
4. Narration/transcript PDF, based on lesson source
5. Local narration WAV, when an embedded FOSS voice is installed

Only genuine, indexed material is marked as generated. A separate export checkmark appears only after an independently validated file copy to the selected Android directory.

**Not implemented yet:** a genuine animated talking avatar MP4 generator and a two-speaker podcast generator. Their status explicitly reads *Not implemented*; the UI never claims these outputs were created. Audio remains WAV rather than PDF; eventual avatar videos must be MP4. PDF applies to the textual documents, not multimedia.

## Pause, resume and cancel

**Pause** requests a stop at the next safe task boundary; a PDF, TTS inference call or export that is already running is allowed to finish before work waits. **Resume** continues. **Cancel** cancels the active coroutine and saves any already completed files; it never deletes earlier successful PDFs. If Android closes the screen or terminates the app, a new visit can resume from files indexed in the material library.

The worker runs in the foreground with the automatic screen open. It is **not** a guaranteed background task; Android can suspend or terminate the process. Remaining tasks are computed from DataStore on the next visit.

The configurable ChatGPT calls-per-run cap remains in effect to control account usage. The queue stops at the cap and can be continued in a later run. A subscription does not imply unrestricted API or account quota.

## Selected storage folder

The teacher chooses an export tree once from the Android Storage Access Framework picker in Settings or in Automatic mode. LECTAV persists read/write permissions locally. The selected URI and provider permissions cannot safely be transported to another device in a backup, so the destination folder must be selected again after migration.

Nested folders are created under the chosen tree:

```text
LECTAV (user-selected storage folder)/
  ESO/
    eso3/
      math_subject_identifier/
        Trimester_1/
          Unit_01_t1-u1-identifier/
            Session_01/
              U01_S01_Lesson_Teoria.pdf
              U01_S01_Lesson_Exercicis.pdf
              U01_S01_Lesson_Solucions.pdf
              U01_S01_Lesson_Transcripcio.pdf
              U01_S01_Lesson_Audio.wav
```

The actual subject identifiers and session filenames come from the saved course and session. Invalid filename characters are removed. An existing identical document is reused; a different document gets a revision suffix rather than being overwritten. Copies are streamed to avoid loading an entire PDF/WAV into RAM. Their SHA-256 hashes and byte counts are compared with the source before the export is recorded as complete.

The **Material library** remains the authoritative source of original generated files. The user-selected directory is a second, verified copy and is not automatically uploaded to a cloud service unless the selected Android document provider itself is a cloud provider.

## Resume and export reconciliation

When Automatic mode restarts, it first exports any previously indexed generated materials that are not marked as exported to the current destination. It then generates missing tasks and exports each newly indexed material before continuing. The on-screen checklist distinguishes *Generated*, *Exported*, *Pending*, and *Not implemented*. The archive format `.lectavbak` continues to provide encrypted portable backup independently of the chosen export folder.

The source text sidecar reader now supports lesson, exercise, solutions and transcript PDFs; encrypted backups preserve eligible source text for all four types so automatic generation can resume after restore.

## Acceptance tests to perform on the tablets

- Choose a writable internal or SD tree; generate one complete session. Inspect every PDF and WAV in the nested folder using a file manager.
- Check that the five supported checkmarks appear and that the multimedia tasks without engines are not falsely completed.
- Pause during theory generation; verify the current operation ends cleanly and no subsequent task starts until Resume.
- Cancel midway, reopen Automatic mode, and verify completed documents are not regenerated.
- Select a different export tree and verify existing generated materials get copied to the new destination rather than being treated as already exported.
- Revoke the SD card permission or unplug the card: display an error and preserve the original library.
- Restore a `.lectavbak` archive on the second tablet, select the new device's export folder, and verify materials and source text can be exported again.
- Verify account model errors, authorization changes and request-cap behavior.
- Test with long mathematical texts and local narration before relying on classroom deployment.

A successful GitHub Actions build only verifies compilation and unit tests, not end-to-end Android storage provider behavior.
