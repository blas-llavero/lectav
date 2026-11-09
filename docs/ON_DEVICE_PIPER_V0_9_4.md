# LECTAV 0.9.4 — on-device open-source speech (no PC or LAN server)

## User choice and architecture

This version replaces the optional Windows/LAN Piper companion of v0.9.3 with
**the GPL Piper Android system Text-to-Speech engine**. The user's own tablet
generates voice locally: no remote computer, commercial AI API or external
speech service is needed after a voice has been downloaded and installed.

The Android implementation explicitly targets engine package
`dev.ihorshevchuk.piper`. It checks the active engine and never silently
falls back to Google or another closed-source/cloud provider.

## One-time setup on the tablet

1. Download Piper Android from the developer's official releases:
   https://github.com/piper-tts-apps/piper-app-android/releases
2. Open Piper, download or import a compatible offline voice for the language
   you intend to use. Ensure each voice's license and pronunciation are
   appropriate. For a two-speaker podcast, install two distinct voices.
3. In Android Settings → Text-to-speech output, select **Piper** as the preferred/default engine. This is required so LECTAV never silently selects another installed engine.
4. Return to LECTAV → Els meus cursos → Programació i sessions.
4. Open a session with an existing theory PDF and choose the Piper audio button.
5. Load the PDF text, review formulas and proper nouns, select the voice(s),
   and press **Genera i desa àudio WAV**.
6. Save the reviewed script separately as a transcript PDF if desired.
7. Play or export the audio from the session's Materials list.

## Output format and limitations

On-device Android TTS supports `synthesizeToFile` output as PCM WAV.
LECTAV combines synthesized segments into **WAV**, not MP3, to avoid an
external MP3 encoder or unsupported codec dependency. Existing imported
MP3s remain supported. Exports are named `Uxx_Syy_Title_Audio.wav` or
`Uxx_Syy_Title_Podcast.wav`; matching transcript PDFs retain their format.

- A teacher reviews the generated dialogue. The two-voice mode requires
  separate lines with `A:` / `B:`. It is *not* conversational AI.
- The two selected Piper voices must have the same PCM sample rate;
  otherwise an explicit compatibility error is shown.
- The first implementation supports **up to 8,000 characters** per request.
  Long documents can be split by sessions, and each speech segment is
  constrained to avoid the Android TTS 4,000-character limit.
- Voice synthesis may be slow on older/entry-level tablets. Only use models
  compatible with your device; smaller INT8 voices may improve performance.
- No source PDFs, texts, models, voice files, media or personal data are sent
  to an external service by this feature. Voice downloads in the Piper app
  are separate, user-initiated network actions.
- This does not install Piper automatically; an independent GPL app is
  necessary at this stage. Nor is it a built-in MP3 encoder.
- No audio, voice weights, videos, images, or voice-model binaries have been
  uploaded to the LECTAV repository.

## Validation before wider deployment

- Back up LECTAV 0.9.3 (or 0.9.2) before updating.
- Check theory PDF text extraction on your Doogee T30 Ultra.
- Generate one-voice WAV using a downloaded Piper voice.
- Generate A/B podcast after choosing two distinct compatible voices.
- Verify WAV playback, Android Save As, and an encrypted backup/restore
  containing both MP3 and WAV.
- Verify behavior with Piper missing or no installed voices.
- Validate Catalan and Spanish text in the actual available voices, rather
  than assuming every model covers every language.
- Verify no fallback to other Android speech engines.
- For long lessons, assess performance, memory use and WAV file size.

GitHub Actions compilation is a syntax/build check, not on-device validation.
