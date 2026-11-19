# Offline animated avatar — implementation plan and testable timeline

This initial step adds **pure-Kotlin, tested timing primitives** for an offline
avatar renderer. It is deliberately **not** a playable MP4 encoder yet:
the existing automatic-screen avatar status must remain *Not implemented*.

## Audio-driven animation contract
- Consume an already indexed, locally generated narration WAV for the session.
- Validate PCM mono 16-bit WAV and read duration from the audio data chunk.
- Derive approximate start/end times for short on-screen subtitle phrases.
- Determine a simple open/closed mouth state from the audio PCM amplitude.
- Render a stylized original teacher figure; never impersonate a real person.
- Encode H.264 video and AAC audio locally using Android MediaCodec, mux
  both tracks into a real .mp4 using MediaMuxer, and verify both tracks by
  reading them back with MediaExtractor before indexing or exporting.
- Keep temporary files private, enforce an upper duration/size limit, clean
  up after failure/cancellation, and preserve already completed materials.
- Mark **Generated** only after the MP4 exists, has video+audio streams, and
  is indexed; **Exported** only after existing SHA-256 checked SAF export.

## Known accuracy limit
The new `AvatarCaptions` timing uses the supplied narration text and
proportional phrase lengths. It is **not real speech recognition, forced
alignment, or phoneme-accurate lip sync**. Later implement genuine local
alignment if compatible FOSS tooling and a robust model are available.

## Tests
`AvatarCaptionsTest` exercises continuous timing, deterministic output,
empty input, audio-amplitude mouth state and invalid limits. Run the
repository's existing Gradle unit tests and GitHub Actions before any merge.

## Future integration
Add a renderer, then append the avatar task after narration to the automatic
queue. It must retain the current request cap, pause/cancel checkpoints and
never claim that a placeholder or missing MP4 was generated.
