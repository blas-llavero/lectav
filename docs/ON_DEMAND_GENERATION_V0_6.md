# LECTAV 0.6.0 — on-demand PDF drafts and optional open-source AI

## Principle

Each teacher or learner initiates content creation when needed. Content is NOT
pre-generated in the APK. Generated drafts are private until the user chooses
to share. Courses preserve their curriculum provenance separately from
AI-generated explanations and questions.

## Implemented first step

- Open a saved course → Materials → Generate.
- Select topic, material language, and type: theory / exercises / solutions.
- **Offline demonstration:** creates a short clearly labelled non-AI draft;
  it validates PDF creation and the storage/review workflow, not curricular accuracy.
- **Optional LAN Ollama:** submit an educational topic to an explicitly configured
  FOSS Ollama server on a numeric RFC1918/LAN address. The user has to configure
  and host the model separately. Responses are drafts; no claim of validation.
- Editable preview before saving, using Android PdfDocument into the app's
  private generated/ directory. FileProvider exposes only that directory to
  viewers/sharing recipients when the user requests a share.
- Existing saved-course library now distinguishes generated from imported
  materials, with teacher sorting by generated count.
- Share through Android's ACTION_SEND chooser, not automatic publication.

## Storage and sharing decisions

- Device private storage is appropriate for drafts. Copies must be exported or
  shared to preserve them across uninstall/app-data deletion.
- The default GitHub repository hosts **code and reviewed templates**, not
  private user documents, unmoderated PDFs or student progress.
- A future community library should use separate object storage with
  opt-in upload, authenticated authorship, permissions, licensing, withdrawal,
  moderation, abuse reports and curriculum-version tags.
- Offline-first Android and the future website will use equivalent workflows.
- Never commit generated images/audio/videos or other multimedia assets before
  the author's explicit review/approval.

## Privacy and security

- The optional Ollama route currently uses **HTTP on a private LAN**; the
  request is not encrypted on that network. Warn users not to use public Wi-Fi
  or include names, grades, medical records or other sensitive student data.
- The Android app only connects to literal private IPv4 addresses, not general
  domains or public internet model endpoints. The connection is opt-in.
- For future secure remote AI, use an authenticated HTTPS service with correct
  data handling. Avoid exposing Ollama directly on the public internet.
- All AI-written math/science content needs factual, pedagogical and copyright
  review before publishing or classroom use.

## Limitations

- An LLM is NOT bundled and does not run on the tablet in v0.6.0.
- FOSS AI works only when the user's separate Ollama server is reachable.
- The offline fallback is a **technical demonstration**, not real AI or a
  complete lesson.
- No automatic public community publication, no server storage, no real student
  accounts, and no direct export folder picker in this version.
- A later release should add an accessible, searchable document pipeline,
  author/edit history, curricular alignment checks and fully multilingual
  typesetting.
