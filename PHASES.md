# LECTAV — development roadmap

**Mission:** Free, open-source, accessible learning without geographical, language, disability or economic barriers. LECTAV starts in Catalonia and is designed to expand worldwide across Android, tablets and the web.

## Implemented

- [x] Official name approved: LECTAV — Open Learning Avatar
- [x] Public source repository: https://github.com/blas-llavero/lectav
- [x] First Android Compose prototype with translated interface, About/Help, navigation-safe layout and subtitle-position calibration
- [x] First GitHub Actions debug APK build succeeded: https://github.com/blas-llavero/lectav/actions/runs/41831051287

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
- [ ] **Teacher's My courses:** manage multiple courses; show number of uniquely generated lessons, theory PDFs, exercises, answer keys and avatar video sessions per course, ordered by amount generated (default), last updated or title. Do not double-count repeated exports.
- [ ] **Teacher's downloads and playback:** save generated PDFs and MP4s to internal device storage or a removable SD folder selected with Android's file picker; reopen and view PDF/video directly in the Android app, with the same core features on the web.
- [ ] **Student's My learning:** saved enrolled/selected courses and accessible learning materials, history of PDFs opened, PDFs self-marked as read/completed, videos watched/resumed, and exercises opened, started, submitted or completed; bookmarks and last-visited topic.
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
