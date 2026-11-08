# LECTAV 0.9.3 — Piper local audio and dual-voice session podcasts

## Features
- Each planned session with an existing **theory PDF** offers
  **Genera àudio o podcast des de la teoria (Piper)**.
- LECTAV extracts the PDF text **on the Android device** (no external OCR).
- The teacher reviews the editable extracted text before sending it to the
  explicitly configured **private LAN Piper server**.
- One-voice narration generates an MP3; the two-voice mode accepts a reviewed
  script with dialogue lines beginning with `A:` or `B:`.
- The UI proposes a simple alternating-voice draft; **it is not AI-generated
  conversation** and must be revised if conversational quality is required.
- A separate button saves **exactly the edited script** as a transcript PDF
  linked to the same session, independent of generating the MP3.
- MP3 and PDF are stored privately, appear in Materials, can be copied to SD,
  and can participate in encrypted backups (within existing size limits).
- No audio, model, voice asset, or PDF has been committed to GitHub.

## How to run the optional Piper service on Windows (PowerShell)

The computer and tablet must be on a trusted **private LAN**.
Piper inference runs on your Windows PC, not on the tablet.
Obtain Piper and its compatible `.onnx` + `.onnx.json` voice files from
the maintained open-source Piper distribution. Verify the license of
**each voice model separately**, and choose voices supporting your intended
language. For bilingual podcasts, both models must pronounce that language.

Install and make available on PATH:
- the Piper command-line program (`piper`),
- FFmpeg with MP3 encoding (`ffmpeg`),
- Python 3.10+ for the bundled **stdlib-only** HTTP companion.

Run from the repository root in a PowerShell terminal:

```powershell
# Configure the path for the already-downloaded open voice models.
$env:LECTAV_VOICE_A = "C:\\LectavVoices\\voice_a.onnx"
$env:LECTAV_VOICE_B = "C:\\LectavVoices\\voice_b.onnx"

# Replace the IP with the PC's own private-LAN IP (ipconfig).
$env:LECTAV_BIND = "192.168.1.10"
$env:LECTAV_PORT = "8765"

# Choose a random secret of at least 24 characters; keep it private.
$env:LECTAV_TOKEN = "REPLACE-WITH-A-LONG-RANDOM-SECRET"
python tools/piper_lan_server.py
```

In Android, choose a session containing a generated theory PDF:
1. Open the new **Piper audio / podcast** action.
2. Load the PDF text and check mathematical expressions and pronunciation.
3. Choose one voice or two voices and edit the script as appropriate.
4. Enter `http://PC-LAN-IP:8765` and the same token.
5. Choose **Genera i desa MP3**.
6. Choose **Desa el guió actual com a transcripció PDF**.
7. Play both files from the session, then export copies if desired.

## Privacy and practical caveats
- This is **HTTP on a trusted LAN, not encrypted HTTPS**. Keep only teaching
  material without personal student data in the editor. Use a long secret,
  limit network access via OS firewall, and stop the server when finished.
- The server uses the supplied token and accepts only the configured endpoint.
  The Android app only connects to literal private IPv4 addresses.
- The first version is manual/on-demand, not a background batch renderer.
  A lengthy session may be slow. User cancellation is not yet implemented.
- PDF text extraction is not OCR and may misread mathematical notation.
- Automatic AI dialogue writing, video/avatar lip sync, streaming playback,
  speech-to-text from recordings, and offline Piper **on Android** remain
  future work.
- The app does not ship Piper binaries or third-party voice weights.
- Maintain review/attribution for voices and content. Windows Defender
  Firewall may need an inbound allow rule **only** for the private network.
- Existing Android versions support MP3 playback and local PDF rendering.

## Test checklist
1. Back up LECTAV 0.9.2 before installing this APK.
2. Verify that imported and pre-existing PDF/MP3/MP4 materials still open.
3. Use an original short PDF, load theory text, verify PDF text extraction.
4. Edit one-voice narration, synthesize MP3 with Piper and play it.
5. Edit lines using `A:` and `B:`, generate and play dual-voice podcast.
6. Save the edited script as PDF; verify session naming and association.
7. Export both files to SD, then back up and restore their entries.
8. Test wrong token, missing server, oversized text, and missing voice model.
9. Check the app stays usable and no student data goes outside the LAN.

Successful GitHub Actions compilation cannot certify on-device behavior.
