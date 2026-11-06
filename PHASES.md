# LECTAV — development roadmap

**Mission:** Free, open-source, accessible learning without geographical, language, disability or economic barriers. LECTAV starts in Catalonia and is designed to expand worldwide across Android, tablets and the web.

## Implemented

- [x] Official name approved: LECTAV — Open Learning Avatar
- [x] Public source repository: https://github.com/blas-llavero/lectav
- [x] First Android Compose prototype with translated interface, About/Help, navigation-safe layout and subtitle-position calibration
- [x] First GitHub Actions debug APK build succeeded: https://github.com/blas-llavero/lectav/actions/runs/41831051287

- [x] v0.3.0 — DataStore for last educational selection, subtitle-calibration migration, saved country/region identifiers and a 10-language Android picker.

- [x] v0.4.0 — first working My courses library: locally saved multiple course selections, independent teacher/learner lists, reopen/remove/role switch and no invented learning records.
- [ ] v0.5+ — actual generated material indexing and counts, automatic sorting by content amount, read/exercise/video progress, offline PDF/MP4 viewers and SD exports.

- [x] v0.5.0 — user-selected existing PDF/MP4 import via Android Storage Access Framework, offline in-app display, per-course imported-file counts, learner opened/manual completion states.
- [x] v0.6 — optional private LAN AI draft generation with teacher review, local PDF creation and manual share.
- [ ] v0.7+ — direct SAF save-to-SD, on-device inference, richer reader/playback features and course progress database.

- [x] v0.6.0 — user-initiated PDF draft generation UI, offline demonstration with honest labelling, optional LAN-only FOSS Ollama generation, editable preview, privately saved generated PDFs, teacher sort by genuine generated-file counts and manual Android sharing.
- [ ] v0.7+ — direct PDF export to user-selected SD/internal folder, real on-device FOSS LLM options, reviewed per-topic content/assessment rubrics, opt-in moderated community sharing and role-based access.

- [x] v0.7.0 — first teacher-editable curriculum topic proposal (Matemàtiques 3r ESO), sessions pre-fill the draft generator, learner can mark sessions complete locally. Proposed sequence is **not** claimed as prescribed by law.
- [ ] v0.8+ — verified versioned curriculum ingestion/AI-assisted crosswalk, full teacher programme, session exercises, assessment/rubrics and solution files, per-user progress, and FOSS per-session avatar MP4 generation.

## ChatGPT account integration — design approved for investigation

- [x] Verify current OpenAI Sign in with ChatGPT open-source client documentation and eligible ChatGPT-plan inference routes (6 November 2026).
- [x] Document LECTAV's secure optional sign-in and AI-mode architecture in [CHATGPT_ACCOUNT_INTEGRATION.md](docs/CHATGPT_ACCOUNT_INTEGRATION.md).
- [ ] Validate native Android OAuth 2.0 + PKCE with the officially required loopback callback; secure device token storage, refresh/revocation and user identity/role isolation.
- [ ] With explicit user consent, connect the user's *own eligible ChatGPT plan* to supported streamed Responses API text inference (theory, programmes, exercises, worked solutions, avatar scripts).
- [ ] Preserve local-only/offline mode and optional FOSS Ollama/llama.cpp paths for users without access to ChatGPT-plan inference.
- [ ] Create genuine per-session MP4 avatars using independently audited FOSS speech/animation/video tools. ChatGPT plan token-sharing does not itself enable video generation.
- [ ] Implement responsible teacher publishing, learner permissions, separate progress and GDPR/minor safeguards. Never pool or lend subscriptions across users.

- [x] v0.8.0 (Android pilot) — implement ChatGPT public-client PKCE/loopback, OIDC JWT verification, Keystore credential storage, consent, optional account-authorized model listing and streamed text draft generation.
- [ ] Verify v0.8.0 live on a personal Android device with an eligible ChatGPT account, including callback, token refresh, revocation, scope denial and usage limits; CI compilation alone cannot validate these.
- [ ] Improve multi-account and individual student privacy isolation before any shared school-device deployment, with required parental and institution controls.
- [ ] Separate FOSS avatar video engine, Spanish/Catalan curricular authority index, full teaching plans, and actual exercise/solution validation.

- [x] v0.8.2 — fix exported PDF math: offline KaTeX for fractions, radicals and set notation; markdown-it for headings, bold and tables; A4 PDF printing with embedded local math fonts and explicit parse errors.
- [ ] Test the formatted PDFs visually on Android with multiple pages and verify Unicode/math font placement. Improve editable rendered preview later.

- [x] v0.8.3 — reduce PDF font sizes, improve table column widths and calculate semantic page boundaries so headings are not orphaned and full tables are not split. Overlarge blocks return a descriptive error instead of silently cropping.
- [ ] Validate all PDF pages on Android; later support continuation of very long tables with repeated table headers and richer page-numbered layouts.

- [x] v0.8.4 — keep short colon-ending introductions with their following tables/exercises in local PDF layout; on-demand retrieval of official source PDFs with local text extraction, source provenance and teacher-approved AI-generated suggested topic/session sequences.
- [ ] Expand an official legal-curriculum registry to all Spanish autonomous communities and countries, with verified versioned primary-source documents, precise per-course criteria/sabers mappings and update detection.
- [ ] Add full, multi-user classroom roles before saving learner-specific history or replacing shared progress; improve PDF table auto-splitting with repeated headers.

- [x] v0.8.5 — show genuine location of private generated PDFs versus imported documents; support explicit Android Save As to a selected internal/shared folder or removable SD for PDFs and MP4s, from Materials, the viewer and after PDF creation; retain last exported destination.
- [ ] Future: optional persistent default export directory chosen via Android Storage Access Framework; library backup/restore, free-space monitoring and user-controlled retention policies.

- [x] v0.8.6 — mitigate reported PDF-generation ANRs: move expensive PDF writing and verification off UI thread, yield between page draws, single active export, 18-page limit, responsive progress/cancel, collapse large draft editor.
- [ ] Verify responsiveness and memory behavior on DOOGEE Android tablet with progressively longer PDF exercises; gather ANR trace if the problem recurs. Do not claim a complete root-cause determination without device trace.
- [ ] Add user-selectable default SD/internal export directory and library backup/restore, especially before app reinstall/uninstall.

- [x] v0.8.7 — filenames start with Temari, Exercicis or Solucions; local AES-GCM encrypted portable backup/restore of selections, saved courses, programme/session progress, IA generator preferences, local ChatGPT request limit and accessible library PDFs/MP4s.
- [ ] Fix persistent signing across GitHub Actions builds using a secure private keystore stored as encrypted CI secrets, allowing future APK updates without uninstalling and losing application-private data.
- [ ] Device-test backup corruption/wrong password, user-selected SD backup, all restored PDF/MP4 contents, account reauthorization and consistent usage limits.

## Short-term phases — initial Catalonia rollout

- [x] 1A. Initial, explicitly partial and source-attributed 2026–2027 subject selection for ESO and Batxillerat: common, school-dependent, optional and modality distinctions; user-visible official source links.
- [ ] 1B. Complete and verify all officially defined subjects and subject availability by year, modality, school and academic version; add curricula, learning outcomes, module/subject codes, versioning and update tests.
- [ ] 1C. Import the official CFGM, CFGS and professional-certificate catalogues, including official source links, versioning, learning outcomes and subject/module codes.
- [ ] 2. Generate teacher-reviewable lesson and exercise PDFs with references, level, syllabus version and separate answer materials where relevant.
- [ ] 3. Integrate an auditable open-source AI model (on-device where feasible or explicitly self-hosted), interactive educational avatar, quizzes, natural FOSS speech and repositionable Catalan subtitles.
- [ ] 4. Enable downloadable, one-hour teaching video sessions; distinguish these from the <=30-second promotional and tutorial videos.
- [ ] 5. Prepare introductory and screen-recorded tutorial videos in the ten initial app languages, with accessible natural FOSS voiceover. **No image, sound or video is to be published or added to GitHub/app before Blas Llavero explicitly approves it.**
- [ ] 6. Test accessibility, reliability, curriculum accuracy, source licensing, Android navigation-bar insets, release signing and reproducible builds.

## Saved configuration, content library and offline use (new requirements)

- [ ] **Persistent user profile:** save and restore selected country, educational territory, pathway, programme, course/year, level, subject, app language, content language where supported, and subtitle/accessibility calibration. Allow deliberate clearing or switching between profiles and courses.
- [x] **Teacher's My courses — first step:** save several official course/subject selections, reopen and remove entries, persist across restarts.
- [ ] **Teacher's My courses — later:** real generated-file counts, sort by amount of generated content, last updated or title; do not double-count repeated exports.
- [ ] **Teacher's downloads and playback:** save generated PDFs and MP4s to internal device storage or a removable SD folder selected with Android's file picker; reopen and view PDF/video directly in the Android app, with the same core features on the web.
- [x] **Learner library — first step:** independently save, reopen and remove course/subject selections, with no fabricated access/progress records.
- [ ] **Student's My learning — later:** materials and history of PDFs opened, PDFs marked as read, videos watched/resumed, exercises opened/started/submitted/completed, bookmarks and last visited topic.
- [ ] **Offline-first:** course index/progress and already downloaded materials work without an internet connection. Warn clearly when AI generation or website sync is unavailable offline.
- [ ] **Storage reliability and privacy:** use Android DataStore for small preferences, Room for relational progress/asset metadata, and Storage Access Framework for user-selected export folders and SD cards; avoid blanket storage permissions. Recover from missing SD cards, deleted documents, ungranted URIs or duplicate names gracefully.
- [ ] **Cross-platform parity:** teachers and students can perform these same essential tasks in Android and on the web. Any automatic account sync or cloud storage needs opt-in, authentication, secure transport, appropriate EU data handling and extra protections for children.
- [ ] **Automated testing:** restart restores preferences, changed selection doesn't erase previously saved course records, generated-material sorting is deterministic, file exports survive restart, SD removal is handled, read/completion statuses are distinguished, and privacy controls work.

[Technical and UX specification](docs/USER_PROFILES_AND_LIBRARY.md).

## Worldwide academic catalogue (future)

**Important:** Every country can define its own levels, curriculum authority, regional subdivisions, academic calendar and professional qualifications; do not assume that every country uses Spain's autonomous-community structure.

Proposed selection hierarchy:

1. **Country**
2. **Territory / curriculum jurisdiction** — autonomous community, state, province, region, devolved nation or national curriculum, as applicable; optional where not relevant
3. **Education level / type** — primary, lower/upper secondary, vocational, professional certificate, university, continuing education and relevant local levels
4. **Education provider** — optional for school programmes, but institution + campus/faculty should be selectable for universities
5. **Programme / qualification / degree**
6. **Academic year / semester / stage**
7. **Subject / module**
8. **Topic / learning outcome / lesson session**

University priority subject areas: **mathematics, computer science, cybersecurity, medicine, biotechnology and bioinformatics**, followed by the wider officially documented academic catalogue. University modules are often institution- and programme-specific and must not be conflated with a single nationwide syllabus.

Catalogues should use durable IDs rather than translated strings as keys, with jurisdiction, authority, source URL, source licence, language, institution, academic year, version, effective dates, qualification code and programme/module codes. Keep original official names while offering clearly labelled translations; identify withdrawn/expired courses; provide review workflows rather than treating AI-generated lists as official facts.

**Start with Catalonia/Spain as a test case** for the new multi-jurisdiction data model, before adding other countries. Community-maintained curriculum contributions must cite verifiable primary sources and pass editorial review.

## Web and online-course platform (future)

- [ ] A **responsive web platform** alongside the Android app (smartphones and tablets), sharing verified catalogue data, accounts where needed and educational content through a documented, open-source API. The **same core functions** must be available on web and Android with layouts tailored to each device.
- [ ] **Teachers on both Android tablets/app and web:** create, prepare, edit, publish and manage courses, lessons and learning materials; schedule sessions; set assignments and quizzes; review learner progress.
- [ ] **Learners on both Android tablets/app and web:** find and enrol in courses, study lessons with the avatar, practise with exercises/quizzes, submit assignments, follow progress and download PDFs and subtitled videos.
- [ ] Support equivalent user journeys across clients; do not make the web teacher-only or the tablet learner-only. Consider offline study/authoring on Android with optional consent-based sync when internet is available.
- [ ] Learner enrolment and optional progress tracking, assignments, feedback and certificates of completion **clearly distinguished from official accredited qualifications**.
- [ ] Shared student progress between Android and the website **only with explicit consent and appropriate account security**; offer anonymous browsing and useful offline download/caching where possible.
- [ ] 10 initially supported UI languages (English, Catalan, Spanish, French, Portuguese, German, Arabic, Hindi, Simplified Chinese and Japanese) with modular localisation for more.
- [ ] Free/open-source, independently hostable components; plan for the real operating cost of hosting and rendering without making paid proprietary AI a mandatory dependency.
- [ ] Accessibility by design (subtitles, transcripts, screen readers, keyboard navigation, subtitle calibration), privacy/GDPR, child safeguarding, audit logs, teacher moderation, and secure handling of educational records.

## Engineering guidance

Aim for a modular architecture: native Android client + responsive web client + versioned curriculum service + content/assessment engine + optional local/self-hosted AI/voice/video workers. Keep curriculum metadata and source attributions independent of presentation language. Defer implementation of the global catalogue and web learning-management system until the Catalonia prototype has passed its core tests.

**Current status:** Only the small Android UI/calibration prototype is implemented. No worldwide catalogues, university teaching modules, AI lessons, user accounts, online-class platform, avatar or video generator exist yet.
