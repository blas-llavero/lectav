# LECTAV 0.9.12 — Embedded free-software audio generation

## Goal and architecture

Generate classroom narration on an Android tablet without a separate Piper application, PC, cloud speech service or subscription. LECTAV integrates open-source VoxSherpa and sherpa-onnx for on-device VITS/Piper speech synthesis.

After user approval, the app downloads a compatible voice model and eSpeak-NG pronunciation data from the public [sherpa-onnx TTS models release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models). Data is stored in LECTAV's private app directory. Models and voice recordings are not automatically uploaded to GitHub; verify each model's licence independently before redistributing the weights.

Initially offered voices include Catalan **Ona** and **Pau**, and Spanish **Miro** and **Sharvard**. The user chooses and downloads a voice in the session's audio-generation screen. Once installed, local WAV synthesis works without network access. Voice selection is remembered for subsequent generations.

## Manual procedure

1. Open an existing session that has theory or an approved narration script.
2. Open **Generate audio / podcast**.
3. Select an embedded voice and download it if not installed.
4. Review the script and press **Generate audio with LECTAV**.
5. Open the generated 16-bit PCM WAV in the course material library.

A mathematical-notation preprocessing step was introduced in 0.9.13; it transforms common LaTeX into words for speech without changing the original PDFs.

## Automatic queue

When a supported voice is installed, automatic generation adds a WAV task after each narration/transcript PDF. Speech synthesis is local and does not consume an additional ChatGPT text request. A generated audio file is indexed against the session, exported with the other available files, and not duplicated when the queue is resumed.

The automatic synthesis engine accepts chunked scripts within its defined limits; a manual editor can have a lower text limit. Large WAVs consume significant storage and may be slower on tablets with limited memory. The program must surface engine failures rather than claiming success.

## Current limitations

A complete two-speaker podcast producer, the animated avatar MP4 renderer, and embedded MP3 encoding are separate development tasks. The older external Piper workflow may remain available as a fallback. Validate voice licensing, playback, offline synthesis, naming, backup and device performance before distributing material.
