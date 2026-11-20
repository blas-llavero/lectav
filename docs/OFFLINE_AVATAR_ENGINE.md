# Offline avatar MP4 — experimental implementation

## Current functionality (draft branch)
The automatic generation queue now optionally adds **Avatar MP4** after a locally
generated narration WAV is available. The first edition is a basic, original
2D animated presenter. It is **not** 3D, motion capture, photo-realistic video,
speech-recognition alignment, or phoneme-accurate lip sync.

The Android-only pipeline:
1. Read the existing session's indexed narration WAV and transcript PDF source.
2. Check the WAV is bounded PCM mono 16-bit with a supported sample rate, and
   reject clips over **10 minutes** for this experimental encoder.
3. Draw a 320 × 240 stylized teacher with five frames per second. Derive
   mouth-open state from audio amplitude and approximate caption timings from
   the source text. This is *not* a word-aligned transcript.
4. Encode H.264 AVC and AAC locally with Android MediaCodec.
5. Combine both tracks into an MP4 with MediaMuxer. Reopen using MediaExtractor
   and verify that there are genuine audio and video tracks.
6. Index only the validated MP4 and export it using the existing streaming,
   SHA-256-verified Android Storage Access Framework folder exporter.

All temporary media files are private and discarded on completion or failure.
A failure must not create a checkmark for an invalid or missing MP4. Completed
earlier session files remain indexed; cancellation is checked during encoding.

## Required validation before merging
- GitHub Actions must compile Android Kotlin and execute the existing JVM tests.
- Confirm the device has compatible H.264/AAC encoders; different devices may
  support different color formats and PCM audio rates.
- Verify MP4 playback, video and sound on both Doogee T30 Ultra and T40 Pro,
  including a 30-second sample, longer samples, silent portions and cancellation.
- Verify the subtitles are approximate (do not claim precise synchronization).
- Verify stopping or killing the app does not corrupt previously generated files.
- Verify hashes, folder organization and export to internal storage and SD.
- Check edge cases: missing transcript, missing WAV, unsupported formats, storage
  exhaustion, revoked document-tree permission, and duplicate regeneration.
- Performance: measure device temperature, memory and battery while encoding.

## Limitations
This is a preliminary proof of implementation. Visual design is intentionally
simple. Video frame rate and resolution are conservative to support modest
Android tablets. No paid rendering service or network voice conversion is
involved. Actual compatibility must be confirmed on hardware, not inferred
from CI success alone.

## Stronger media verification
The MP4 encoder now reopens the final output and decodes **real sample payloads**
through `MediaExtractor` for both the audio and video tracks. Validation
checks non-empty samples, nondecreasing timestamps, plausible track duration,
and positive video dimensions; merely advertising AAC/H.264 tracks is not
accepted as successful generation. This is a structural inspection, **not** a
full playback/decoder test and does not replace testing on physical tablets.

## One-hour lesson segmentation (planner only)
`AvatarSegments` now provides a **tested scheduling primitive** for up to
60 minutes of source audio. A one-hour recording is divided into twelve
five-minute **half-open** windows by default. The planner also supports
30–600 second windows and maps boundaries deterministically to PCM samples
to avoid gaps or duplicated audio.

This change **does not increase the current MP4 renderer's 10-minute limit**.
Rendering a one-hour lesson requires further work:
- Extract individual PCM windows without loading the entire WAV in memory.
- Produce and verify an independent MP4 per segment, checkpointing completed
  segments so cancellation or Android termination does not require starting
  from the beginning.
- Remux compatible segments or publish a sequence of independently playable
  files; avoid claiming a single merged one-hour MP4 until it is actually made.
- Preserve subtitle timing relative to each segment and validate audio/video
  timestamps, storage permissions, and resulting playback on the two tablets.
- Add throttling and safeguards for thermal limits, memory, and battery.

`AvatarSegmentsTest` checks exact one-hour coverage, fractional final
segments, boundary continuity, sample alignment, and invalid inputs.
