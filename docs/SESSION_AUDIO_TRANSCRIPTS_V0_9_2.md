# LECTAV 0.9.2 — session resources, audio and transcript groundwork

## Session export filenames

New or exported resources use `Uxx_Syy_Session_title_Type.ext` whenever
their stable session key can be resolved against the current course plan.

- `U03_S05_Equacions_de_segon_grau_Teoria.pdf`
- `U03_S05_Equacions_de_segon_grau_Exercicis.pdf`
- `U03_S05_Equacions_de_segon_grau_Solucions.pdf`
- `U03_S05_Equacions_de_segon_grau_Avatar.mp4`
- `U03_S05_Equacions_de_segon_grau_Audio.mp3`
- `U03_S05_Equacions_de_segon_grau_Podcast.mp3`
- `U03_S05_Equacions_de_segon_grau_Transcripcio.pdf`

Units are indexed according to their stored order across the course.
Sessions are numbered sequentially within the trimester, as displayed in
the curriculum screen. Files not attached to a session retain compatible
generic filenames. Existing files are **not renamed or overwritten**:
the name is suggested when exporting a copy.

## New resource types

- **Audio**: teacher may import MP3 into any session.
- **Podcast**: teacher may import MP3 into any session.
- **Transcript**: PDF resource linked to a session, either imported or
  newly drafted from that session's topic with the regular text generator.
  The draft is a written narration script, *not* speech recognition from MP3.
- MP3s open in LECTAV with play/pause, restart and playback-speed choices.
- All types are included in local library, Android document export,
  encrypted backup and restore (subject to existing archive size limits).
- The MP3 player runs on the device and does not upload recordings.

## Not yet implemented

Automatic offline TTS, dual-voice podcast generation, and speech-to-text
transcription of a recording are **not** delivered in this patch.
They need suitable openly licensed, on-device or self-hosted models and
practical device testing. The session UI explicitly marks automatic
podcast generation as future work; it does not simulate it.

## Manual Android checks

1. Back up existing courses before updating.
2. Create a course, plan, unit, and session; generate a theory PDF.
3. Export its copy; verify the `Uxx_Syy` filename and the title.
4. Generate a transcript draft, review its claims, and export PDF.
5. Import a locally stored MP3 as Audio and again as Podcast.
6. Open both MP3s, pause/restart, change speed, export to SD.
7. Export and restore an encrypted backup containing PDF, MP3 and MP4.
8. Confirm existing documents and course-session bindings still work.
9. Confirm no media assets were uploaded to GitHub.

Compilation does not substitute for physical testing on the Doogee tablets.
