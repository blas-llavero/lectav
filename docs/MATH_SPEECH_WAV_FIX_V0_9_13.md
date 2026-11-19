# LECTAV 0.9.13 — Mathematical narration and WAV diagnostics

## Reported issue

The session **Integer and fraction arithmetic diagnosis**, for third-year compulsory secondary education, contained LaTeX expressions such as `\\frac34-\\frac16`, `(-4)\\cdot(-3)`, and `3-(-5)`. The original local speech engine attempted to pronounce mathematical markup almost literally and did not consistently display synthesis errors.

**The source of a WAV failure cannot be established from text alone.** The voice model, native inference library, available memory or device-specific configuration can also be responsible.

## Changes

`MathSpeechNormalizer` prepares an oral-language copy of common classroom expressions: integer signs, operations, fractions, products, divisions and parentheses. It handles `\\(...\\)` and `\\[...\\]` math delimiters and basic Markdown. The original stored text and PDF appearance are unchanged. Complex or unfamiliar LaTeX is not guaranteed to be pronounced correctly, and the teacher should review all audio.

The screen also includes **Test voice engine with a short phrase**, a simple synthesis test independent from the mathematical lesson. Error messages from WAV generation are now shown near the speech controls. JUnit tests cover common integer/fraction expressions and run in GitHub Actions before assembling the APK.

## Diagnosis on Android

1. Select a locally installed Catalan voice such as Ona or Pau.
2. Run the short-phrase engine test.
3. If it passes, load the integer/fraction lesson and generate WAV.
4. Open the output from the session material library.
5. If either attempt fails, preserve the exact visible error message to identify whether the problem lies in the engine, model or script.

Compilation and unit tests do not prove that voice generation works on every physical tablet.
