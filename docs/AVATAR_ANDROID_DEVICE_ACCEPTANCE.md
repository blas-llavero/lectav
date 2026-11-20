# LECTAV — Manual acceptance test for offline avatar MP4

This is a **device test**, not a claim that playback has been validated.
Use a debug APK built from the exact commit being tested on both the
**Doogee T30 Ultra** and **Doogee T40 Pro**.

## Preparation

1. In GitHub Actions open the successful `Build Android debug APK` run
   from `feat/podcast-dialogue-validation` and download
   `lectav-debug-apk`. Extract `app-debug.apk`.
2. Back up all existing LECTAV course materials and configuration.
   A debug install may update the existing app; do not uninstall unless
   backups and app data migration have been verified.
3. Install the APK on one tablet at a time. Grant the requested Android
   folder permissions explicitly and choose a writable export directory.
4. Configure the ChatGPT educational-content consent and an available
   model as required by the automatic workflow. Install a local narration
   voice, or ensure that an existing session already has a valid WAV.
5. Start with a **test course/session**, not valuable unsaved classroom data.

## Checklist

| Test | Steps | Pass condition |
| --- | --- | --- |
| Basic video | Generate an avatar from a short (about 30 s) narration | MP4 opens in a local Android video player; audible speech and moving 2D presenter |
| Caption preview | Play the short video | Phrases appear and change; acknowledge that timing is approximate |
| Five-minute video | Generate/play a 5-minute session | MP4 plays through to the end without missing audio |
| Two-plus chunks | Generate/play a 12-minute narration | Playback crosses ~5 and ~10 minute boundaries, no crashes, audible speech |
| Long video | Attempt a 60-minute WAV, if available | Output is present and can be played/searched to the end; inspect joins and tablet temperature |
| Cancel | Cancel during a multi-chunk render | No incomplete video is marked generated/exported; earlier indexed materials remain |
| Resume | Start the same unfinished session again | Previously verified cached chunks are reused; output must be checked after completion |
| Offline voice | Disable network after narration WAV already exists | Existing WAV can be used to render the avatar; no network voice service is invoked |
| Export | Select internal/SD storage and generate | MP4 exists under pathway/course/subject/trimester/unit/session; exported mark appears only after checksum verification |
| Storage errors | Revoke folder access or use low free space | Clear error; no false success checkmark |
| Audio/video mismatch | Try a device that rejects the local H.264/AAC format | Clear failure instead of an invalid final file |
| Reopen | Close and reopen the app after completion | Saved material remains indexed and exported material can be opened |

## Report template

Record separately for each tablet:

- Device model / Android version:
- APK Actions run URL / commit SHA:
- Voice model and PCM sample rate:
- Video duration tested:
- Does the audio play, including segment boundaries?
- Is the video seekable past five minutes?
- Are subtitles legible and reasonably timed?
- Did Pause, Resume, Cancel and SD export work?
- File size and free storage remaining:
- Errors, screenshots, or logs (remove personal data):
- Result: **Pass / Fail / Not tested**.

## Release guard

Do **not** merge this feature into `main` until both devices pass basic
playback, multi-segment continuity, cancellation and verified export.
The experimental renderer is intentionally conservative (320×240, 5 fps).
GitHub Actions validates build and JVM tests; it does not decode MP4s on
these physical tablets.
