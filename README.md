# LECTAV — Open Learning Avatar

**Education without barriers.** LECTAV is a free, open-source Android educational application under development. Its goal is to offer official Catalonian curricula (ESO, Batxillerat, CFGM, CFGS and professional certificates) through accessible AI-assisted lessons, exercises and an educational avatar. This repository initially contains a **navigation and subtitle-calibration prototype**, not the finished AI application.

**APK installation location (immediately below the description):** After the first successful GitHub Actions build, open **Actions → Build Android debug APK → latest successful run → Artifacts → `lectav-debug-apk`** and unzip the downloaded artifact to obtain `app-debug.apk`. The local build output is `app/build/outputs/apk/debug/app-debug.apk`. **The first debug APK was successfully compiled on 4 November 2026** ([successful GitHub Actions run](https://github.com/blas-llavero/lectav/actions/runs/41831051287)); a production release has not yet been published. A future approved release may provide `lectav.apk` under GitHub **Releases**.

**Conceived and programmed by Blas Llavero.** Further information: [blasllavero.com](https://blasllavero.com).

LECTAV is open-source. Anyone may study, modify, share or use it for charitable and other lawful purposes, subject to the **GNU General Public License version 3 or later**. The source code is provided under `GPL-3.0-or-later`; all third-party dependencies and future media must have independently verified compatible licences. The aim is accessible education regardless of geography, language, disability or financial resources.

## Current Android prototype (v0.9.1)

- Native Android app written in Kotlin with Jetpack Compose.
- Five pathway choices: ESO, Batxillerat, intermediate vocational training (CFGM), higher vocational training (CFGS), professional certificates.
- Year selectors for ESO and Batxillerat; a **first, incomplete, source-attributed 2026–2027 subject selection** for ESO and Batxillerat, labelled by common subjects, optional choices, school-dependent distribution and modality. Official names are displayed in Catalan across interface languages.
- The official vocational and certificate programme catalogues are **not yet loaded**.
- Main-screen **About** and **Help** buttons.
- Subtitle-position calibration preview with a slider; the value is stored locally and will be used by the future avatar player.
- 10 interface languages based on Android device/app language: English, Catalan, Spanish, French, Portuguese, German, Arabic, Hindi, Simplified Chinese and Japanese.
- Subtitles are planned to begin in Catalan; the calibration preview contains Catalan sample subtitles.
- Respect for Android navigation and status bars using Compose safe-area insets; designed for phones and tablets.
- Lesson PDF, exercise PDF and avatar buttons are **intentionally disabled**, because the generation engines have not been developed yet.
- No user account, network access, telemetry, advertisements or cloud service in this prototype. The manifest requests **no INTERNET permission**.

## v0.9.1 — no local ChatGPT attempt cap, session-first names and direct resource access

Removed the historic **per-installation weekly ChatGPT attempts limit**.
LECTAV no longer disables generators when a local counter reaches zero,
and restoring older `.lectavbak` files cannot reactivate that cap.
OpenAI's **real plan limits and authorized credits still apply**, with
an explicit pre-generation confirmation and no paid-API fallback.

Session-linked PDFs and MP4s now use filenames like:
`Sessió 1 - Fraccions i decimals - diagnosi inicial - Teoria.pdf`,
`... - Exercicis.pdf`, `... - Solucions.pdf` and
`... - Avatar.mp4`. Android-incompatible filename characters are
normalised, and re-exporting does not duplicate labels.

In **Programació i sessions**, every session has a dedicated **Open**
action for its existing theory, exercises, solutions and avatar files,
with access to other saved versions. Teachers can manually link an
older unassigned PDF or MP4 from the course library to the correct
session, without regeneration or guessing by filename. These links
are included in the existing encrypted backups.

**Important:** avatar MP4 generation is still not implemented.
ChatGPT creates educational *text* and LECTAV saves PDFs; videos
must currently be imported. See
[the v0.9.1 session resource and quota changes](docs/SESSION_ACCESS_NO_LOCAL_LIMIT_V0_9_1.md).

## v0.9.0 — curriculum first; topic-matched teaching resources per session

**The default teaching workflow is now planning → unit → session → materials.**
Open **Els meus cursos → Programació i sessions** and expand a unit to
generate **Temari**, **Exercicis** or **Solucions** for the exact lesson
title. The PDF generator preselects the selected type and a topic like
**Sessió 1 - Fraccions i decimals: diagnosi inicial**, and the resulting
PDF is linked to the same course, trimester, unit and session.
Each session shows its linked resource counts and links to open PDFs.
Previously saved unassociated materials remain in the general library.

Existing MP4 videos can be attached to a specific lesson using
**Importa un vídeo de l'avatar**, with suggested names starting
**Avatar - Sessió N - …**. **Automatic avatar video generation is
not implemented yet**; the corresponding action is visibly disabled,
rather than implying ChatGPT has created a video.

The session-material links are stored locally, remain stable when a
session title is edited and are included in existing encrypted
`.lectavbak` export/import archives. Uninstalling the app still requires
a prior backup to preserve private files.

**Later**, a persistent, resumable queue could offer course-wide
generation in supervised batches with quota checks, stop/retry and
duplicate avoidance. **Batching does not make ChatGPT usage free or
provide unlimited credits.** A separate FOSS avatar/TTS/rendering engine
is needed to create video; text-only ChatGPT responses cannot directly
create a full avatar MP4. See
[session organisation and future batching design](docs/SESSION_MATERIALS_V0_9_0.md).

## v0.8.9 — direct trimester planning from course and subject selection

After choosing a course and a subject on the home screen, teachers can now
open **Genera programació trimestral amb ChatGPT** directly. No course-specific
topic sequences are preloaded, and no model request is sent before user
confirmation of the local usage limit. The teacher chooses the trimester
and total sessions, receives an indexed list of units and distinct lesson
titles, reviews the result and saves it locally. The generated number
of sessions must match the requested count exactly. Sessions are numbered
consecutively within their trimester. They prefill the material-generation
topic field.

Known ESO Maths and Biology-Geology official PDFs are optionally retrieved
**on demand**, parsed on the Android device and used as a reference context;
this preliminary crosswalk is not a legal validity guarantee. The app
explains when no official document was available and labels the plan
a non-verified teaching proposal. See
[direct term planning, curriculum provenance and test steps](docs/DIRECT_TERM_PLAN_V0_8_9.md).

## v0.8.8 — generate a term-by-term programme with a concrete topic for every session

From a saved teacher course and its selected subject, choose **Genera
programació trimestral amb ChatGPT**. Select trimester (1–3), expected
number of sessions (6–48) and the personal ChatGPT model. The existing
usage-limit and explicit confirmation screens still apply, so opening
a course does not automatically spend ChatGPT credits or attempt quota.

The response is a **subject-specific proposed index** (2–8 units per
term, each with individually named session topics) that the teacher can
review and approve. The three terms are stored independently; updating
the current term preserves the other two and warns before replacing
existing progress. All unit and session titles are editable locally
after saving. Selecting **Genera materials** for a saved session pre-fills
its exact topic in the PDF generator.

No mathematics or biology syllabus content is preloaded into the APK:
the subject and year come from the selected course, and ChatGPT creates
a proposal only on request. A previously fetched, independently checked
official curriculum PDF excerpt may be supplied as contextual evidence,
but in its absence the plan is clearly marked **not officially verified**.
The XTEC *Biologia i Geologia* ESO PDF link is now offered for on-demand
consultation in the existing curriculum tool.

Earlier saved topic lists remain readable. The existing encrypted
`.lectavbak` backup stores the new per-term and per-session details;
OAuth credentials must still be authorized after reinstalling. See
[planning design and Android test checklist](docs/AI_TRIMESTER_PLANNING_V0_8_8.md).

## v0.8.7 — descriptive file names and encrypted backup/restore

Generated and exported PDFs are now automatically named **Temari - Topic.pdf**,
**Exercicis - Topic.pdf** or **Solucions - Topic.pdf** (and exported videos
**Vídeo - Name.mp4**). The Android Save As dialog still lets users edit the
proposed name, and older saved documents receive the same descriptive prefix.

**Settings → Còpia de seguretat · Exporta i restaura** backs up locally
saved courses, selection dropdowns, subtitle calibration, lesson plans,
session progress, official curriculum extracts, generator preferences,
local ChatGPT request limits, and all accessible PDF/MP4 library files
(up to a total of 250 MB). The user selects shared internal storage or an
SD folder. The resulting `.lectavbak` file is encrypted with a password
using PBKDF2-HMAC-SHA256 + AES-256-GCM. Import authenticates it before
restoring and requires explicit confirmation to replace existing indexes.

**ChatGPT OAuth tokens, Android Keystore keys and authentication sessions are
NOT portable**. An encrypted backup retains the preferred model and an
account reminder, but the user must sign in and consent again after
reinstalling. No external API charges, cloud upload or GitHub commit is
required to create or restore the local archive.

**Important:** because earlier Android debug APKs can use changing signing
certificates, an upgrade may require reinstalling the app. **0.8.6 cannot
create this new backup format**, so export critical PDFs separately and
note current settings before uninstalling that version. From 0.8.7
onwards, create a backup *before* any required uninstall. The longer-term
fix is persistent APK signing via private GitHub Actions secrets.

See [backup instructions, limits and test steps](docs/BACKUP_RESTORE_V0_8_7.md).

## v0.8.6 — emergency fix for unresponsive PDF export (ANR)

An Android tablet reported repeated **"LECTAV is not responding"** prompts
after generating exercises and exporting PDF materials with v0.8.5. A code
inspection found expensive PDF serialization and PDF validation performed
on the Android **main/UI thread**, as well as repeated whole-document
WebView drawing and an expanded large Compose text editor.

This revision moves PDF file writing and post-export verification to
`Dispatchers.IO`, yields between UI-thread WebView page drawing calls,
prevents overlapping PDF exports, initializes whole-document draw once,
limits a single PDF to 18 pages, and collapses the long draft editor
after saving. The PDF screen has a progress message and a **Cancel PDF**
button; previously saved/exported PDFs and SD copy behavior remain.

**CI compilation is not proof that every ANR is resolved:** long individual
WebView draws can still block Android. First test a short 1–2-page PDF,
then longer exercises, and preserve important results with
**Save a copy to…** on shared internal storage or an SD card. If the
problem recurs, collect an Android ANR trace; do not include personal
records or authentication tokens. See
[incident analysis and tablet test checklist](docs/ANDROID_ANR_PDF_FIX_V0_8_6.md).

## v0.8.5 — visible file location and Save As to any Android-accessible folder

Users can now see where their documents live. **Generated PDFs** are stored
in Android's app-private internal folder (`filesDir/generated/`, usually
`/data/user/0/com.blasllavero.lectav/files/generated/`) and ordinary file
managers cannot explore that location. **Imported PDFs and MP4s** remain in
their selected original folders.

Each saved course's **Materials** screen and the embedded viewer now offer
**Desa una còpia a una carpeta…** for PDFs and MP4s. Android's official Save
As document picker lets users select an accessible location on shared internal
storage, removable SD card or another document provider. After generating
a new PDF, the same button also appears immediately below the confirmation
that it was saved inside LECTAV.

Exports are local, do not require broad storage permission, do not re-run
ChatGPT and do not alter or delete the original file. The materials index
remembers the most recent exported location, where the document provider
offers a meaningful location; some providers return only a URI and cannot
supply a normal filesystem path. Users can find exported copies in CX File
Explorer by navigating to the directory chosen with Android's picker.

**The internal originals can disappear on app uninstall or clearing data.**
Export important PDFs to an independent shared folder or SD card first.
See [storage locations, export behavior and manual checks](docs/FILE_LOCATIONS_EXPORT_V0_8_5.md).

## v0.8.4 — colon-ending lead-ins and live official curriculum PDFs

Further improves PDF page breaks: a short introduction such as "Activitats
següents:" or "Els conjunts són els següents:" is kept with the first block
of content that follows it. It also handles chains consisting of a heading,
a lead-in paragraph and the next table/list. Overlarge indivisible blocks
produce an explicit error rather than being cut.

Adds **Temari → Consulta el currículum oficial**. The app now retrieves a
**current PDF file directly from the official source on demand**, extracts
its text locally using Apache 2.0 PDFBox for Android and stores the source URL,
hash, date and selected excerpt in private local storage. No legal curriculum
contents are bundled into the APK. The first linked source covers Catalunya
ESO Mathematics (XTEC); direct HTTPS links to other supported official PDF
sources may be provided by the user.

The optional ChatGPT button (with existing eligibility, consent and usage
limits) can analyze an extract of the fetched source and propose an editable
didactic topic/session sequence; teachers explicitly review and approve it
before replacing the locally stored plan. **AI output is never labelled a
mandatory or legally verified topic order.** Supported official host checks
do not constitute universal legislation-version monitoring. Other autonomous
communities and countries need their own verified primary-source mappings.

See [technical details, tests and limitations](docs/CURRICULUM_LIVE_V0_8_4.md).

## v0.8.3 — smaller PDF text, intact tables, no orphan section headings

Feedback from the first on-device PDF tests showed that v0.8.2 produced text
that was too large, visually split words in table columns, and cut through
table rows at A4 page boundaries. Version 0.8.3 reduces body and heading
type sizes and gives educational 4-column tables more sensible widths.

Unlike CSS page-break hints (which the WebView canvas export ignores), the
renderer now **calculates safe DOM block boundaries** after KaTeX fonts load
and passes these page ranges to Android. Tables are kept intact and a section
heading stays with its first block; if any table or block is taller than
one page, a user-facing message asks for that content to be divided rather
than silently cropping it. Bottom whitespace on a page is intentional when
the following block needs to move to the next page.

This update applies **only to PDFs generated after the upgrade**.
The existing imported PDFs and media library are unchanged. See
[the pagination implementation and testing checklist](docs/PDF_PAGINATION_V0_8_3.md).
Compiled APKs still require manual on-device PDF verification before these
visual layout constraints can be considered fully validated.

## v0.8.2 — mathematical Markdown/LaTeX PDFs

Fixed the reported PDF-export bug: earlier LECTAV saved raw `\\sqrt{2}`,
`\\mathbb{N}` and Markdown `**bold**`/table markup as literal text.
Newly generated PDFs now go through an offline, locally bundled KaTeX and
markdown-it rendering pipeline before Android's A4 WebView print-to-PDF path.

Supported: fractions, radicals, set symbols, typical math notation, inline
`\\(...\\)`, block `\\[...\\]`, Markdown headings, bold text and tables.
Unparseable formulas show a recoverable error instead of silently producing
broken output. No document is uploaded to a third-party PDF conversion server.

**Important:** older exported PDFs cannot be fixed automatically — regenerate
them from the original draft. Editable text remains Markdown/LaTeX; the actual
saved PDF is typeset. KaTeX and fonts are packaged into the Android APK by
`scripts/prepare_pdf_math_assets.sh` at build time, so new PDF export remains
offline. CI compilation and JavaScript smoke tests **do not substitute**
for verifying rendered PDF pages on a tablet.

See [the rendering design, limitations and test cases](docs/MATH_PDF_RENDERING_V0_8_2.md).

## v0.8.1 — controllable ChatGPT generation limits

The account screen now offers a locally persisted, conservative **weekly attempt limit** (default 3; 0–30 adjustable; 0 disables ChatGPT generation). Generated drafts require an explicit **per-request confirmation** that the user has checked their official ChatGPT app usage limits and disabled the use of credits; API requests stop when the local cap is reached. Official quota-limit and unavailable-usage errors are shown instead of switching to another billing path.

**Important:** the local counter tracks attempts on this installation only — **not** real tokens, subscription quota, remaining credits, money, usage in other apps or reset times for an OpenAI account. For exact controls, users must visit **ChatGPT → Settings → Usage → App limits → LECTAV → Manage limits**. Per OpenAI documentation, setting an app limit **below 100%** prevents credits from being used by that app. Users should also review credit use and auto-recharge settings. LECTAV cannot read or change them. Offline PDF demonstration and self-hosted Ollama remain independent.

See [usage and credit-protection design and tests](docs/CHATGPT_USAGE_LIMITS_V0_8_1.md).

## v0.8.0 — first optional ChatGPT sign-in and AI inference integration

Android Settings → **Compte ChatGPT** has a sign-in action using OpenAI's **public-client ChatGPT-plan OAuth + PKCE** flow in the system browser. The app binds a temporary loopback callback, verifies OAuth state, ID-token signature / issuer / audience / nonce, encrypts session credentials using Android Keystore AES-GCM, and supports local disconnect with an attempt to revoke the refresh token.

On **Materials → Generate**, users may choose **ChatGPT (compte propi)** in addition to the existing offline demo and user-hosted Ollama. With explicit ChatGPT-plan consent and a compatible eligible account, the app lists available model choices and submits a user-initiated text-only educational prompt to the official Responses API, with `store:false`, `stream:true`. It requires `response.completed` to present the editable draft, and it saves the local PDF only after explicit confirmation.

**Limitations:** compilation does not equal successful device login. The OAuth callback, account eligibility, model list and generation must be tested on a personal Android device; this is a **single-account prototype, not safe for shared classroom devices or student account management**. ChatGPT plan usage has user/workspace limits. It cannot create video, images or voice with this text-only flow. Never include personal student data in prompts. The local library and offline modes work without an account. See [Android sign-in test and security notes](docs/CHATGPT_ANDROID_V0_8.md).

## v0.7.0 — teacher course plan and learner session tracking pilot

**Initial supported pilot: Catalunya / 3r ESO / Matemàtiques.** Open **My courses → Temari**. LECTAV seeds a proposed order of six topic units and their sessions (44 in total). Catalan regulations prescribe competences, assessment criteria and knowledge but **not a mandatory first unit**, so the initial order is a teaching proposal, not a legislated sequence. Teachers can rename units and choose a session, which pre-fills the v0.6 PDF draft generator. Learners can see sessions and check them complete, with progress persisted locally across restarts.

**Not implemented:** verified complete syllabus ingestion for every country/region, AI analysis of new legal curriculum documents, full didactic programme export, per-session full exercise generation with validated solutions, MP4 avatar video rendering, individual learner accounts or securely separated personal progress. The prototype shares progress within the device profile and must not be treated as real student assessment evidence.

See [scope, regulatory caveats and detailed roadmap](docs/COURSE_PLANNING_V0_7.md).

## v0.6.0 — on-demand content draft generation (Android)

**Every teacher or learner can generate content when needed**, rather than downloading large pre-bundled content archives. Open a saved subject in **My courses → Materials → Generate**; enter a topic, language and content type (theory, exercises or solutions). LECTAV provides two intentionally distinct generation methods:

1. **Offline PDF pipeline demonstration (without AI)**: creates a short, clearly labelled test draft. This proves PDF creation, editable preview, private storage and sharing, but **does not create a real educational lesson**.
2. **Optional self-hosted FOSS AI (Ollama)**: connects on explicit button press to a model already installed on the user's trusted local network and asks it for an original educational draft. Only numeric private IPv4 endpoints are accepted; the model identifier and topic are selected by the user. The app **does not bundle a model**, host an LLM service, download model weights, or rely on paid proprietary AI.

Review and edit the resulting text before choosing **Save this PDF to My courses**. PDF output is generated locally by Android `PdfDocument` and stored in the app's private files directory. The existing materials library indexes the saved file using a restricted FileProvider URI and labels it **generated** (rather than imported). Teacher courses are now ranked by the number of **generated** PDFs; imported-file counts are still displayed separately. A **Share** control opens Android's chooser to send a PDF/MP4 to another user or app *only when the user requests it*. No community upload or automatic GitHub publishing occurs.

**Important privacy details:** Using the LAN AI mode sends the educational generation request, which includes the subject, year, topic and requested content type, to the configured self-hosted Ollama server using unencrypted HTTP over the local network. Do **not** include student names, records or other sensitive information. Avoid use on untrusted networks. The Android app requests INTERNET permission for this optional service. External public domains are blocked in this prototype. The offline PDF demo and existing library work without an AI server or internet connection.

**Content safety and copyright:** AI output is an unverified *draft*. Always check mathematical correctness, curriculum alignment, pedagogical suitability, copyright, attribution and licensing before distributing it. Do not assume an exercise answer is correct simply because the model supplied it. Review and share intentionally, especially when working with minors.

**Current limitations:** generated PDFs are stored inside the app, and *may be removed if the app is uninstalled or its data are cleared*. Use Android **Share** to save a durable copy to Files/Drive or send it via another app; a direct SAF export-to-SD option will follow. The app does not yet support permanent cloud/community storage, human review/moderation of community materials, on-device LLM inference on the tablet, automatically validated official syllabuses or full PDF accessibility features. `Ollama` must be separately configured by the user on their own computer/server.

## v0.5.0 — offline import and viewing of existing educational files

From **My courses → Materials**, LECTAV can now import an existing **lesson PDF, exercise PDF or MP4 video** using Android's system document picker. The source may be internal storage or a supported removable SD card. Importing is not content generation; source documents and movies are **not added to GitHub, sent to any server, or bundled in the APK**.

- The app stores persistent read access to each selected file and a local metadata record; source files remain in their original folder.
- PDFs render page by page within LECTAV using Android PdfRenderer; MP4s play with Android VideoView when codecs are supported.
- Teacher library courses are ordered by number of **imported** materials (not generated content), and list imported PDFs/MP4s separately.
- Learners can record that a file was opened and separately mark PDFs and exercises completed, or undo completion. Opening a file never automatically means it is completed.
- Removing a record from LECTAV does not delete the original file. If a removable SD card becomes unavailable, opening the document will show an error while the saved library record remains.
- Works offline and requests no broad storage or network permission.
- Not yet available: generation of new PDFs/MP4s, copying/exporting files to a new device/SD location, a searchable PDF viewer, detailed video progress, web sync and student accounts. Media bundled with the project still requires explicit author approval.

**Test:** save an ESO subject under My courses; tap Materials; import PDF/MP4; open PDF and try pages; test video playback; switch to Learner mode and mark an exercise as completed; close and reopen; check that the list and statuses persist.

## v0.4.0 — My courses (Android)

**Now implemented:** Android users can save more than one course/subject selection and reopen it later using **My courses**. A saved entry is a curriculum selection, **not** a generated lesson or an enrollment. Open My courses from the home screen, switch between **Teacher** and **Learner**, and save the currently selected ESO/Batx subject into the chosen role's list. Reopen and remove entries individually; removal requires confirmation and leaves the current learning selection unchanged. Records are saved locally with DataStore, preserved on app restarts, and work offline.

The teacher and learner lists are separate on this device. Each is ordered by **last opened** until real teaching files exist; future ordering by number of generated PDFs and MP4s will only be meaningful after content generation. Teacher cards explicitly show that **no PDFs or videos have yet been generated**; learner cards explicitly show that **no PDFs, videos or activities are tracked**. The app does not fabricate progress, files or completion status. Full document management, activity history, PDF/MP4 viewing, SD-card export, website, accounts and sync are still planned.

### Test My courses

1. Open LECTAV, select ESO → 3rd year → Mathematics, then tap **Save this course**.
2. Choose a second course/subject and save that too. Open **My courses** to see both entries.
3. Tap **Open** on the first entry and verify the education selectors are restored.
4. In **My courses**, switch to **Learner** and verify the independent list. Save a course in learner mode; switch back to Teacher.
5. Remove a saved course using the confirmation dialog and verify other saved courses and the current selection remain.
6. Close/reopen the app and repeat. Prior language and subtitle calibration preferences should remain unchanged.

## v0.3.0 — saved preferences and language controls

- **Saved automatically:** last pathway, ESO/Batx course/year, subject, and the stable country/territory identifiers `ES` / `ES-CT` (only Catalonia is selectable at present).
- **Settings** shows the current selection and includes a reset action. Selecting a new pathway clears its previous course/subject; selecting a new course clears its previous subject.
- **10 app languages:** Settings includes a picker for English, Catalan, Spanish, French, Portuguese, German, Arabic, Hindi, Simplified Chinese and Japanese, plus Follow device language. AppCompat persists the choice on supported Android devices.
- **Existing users:** the previous subtitle calibration is migrated to AndroidX DataStore automatically when upgrading in place. Calibration saves when the slider is released.
- **Local storage only:** no online synchronisation or account. A future release will implement multiple saved courses, teacher and learner libraries, downloadable PDF/MP4, SD access and website sync.
- **Check:** select 3r ESO/Matemàtiques, choose Catalan in Settings, adjust subtitle calibration, close/reopen the app and verify all values remain. Clear course selection and confirm language/calibration stay unchanged.

## Catalonia starter catalogue — first working selection (v0.2.0)

The **subject selector now includes a reviewed but incomplete catalogue** of ESO years 1–4 and Batxillerat years 1–2. Its course/subject lists live in `app/src/main/java/com/blasllavero/lectav/CurriculumCatalogue.kt`; see [sources and limitations](docs/CATALONIA_CATALOGUE.md). The interface separates common, modality, optional and school-dependent offerings. An **official curriculum source** link opens the relevant Generalitat curriculum overview in the device browser. The app does not download, scrape, or send student data.

The underlying sources are **Decret 175/2022 and Decret 480/2024 (ESO)** and **Decret 171/2022 and Decret 103/2026 (Batxillerat)**. The 2026 amendment is introduced in year 1 of Batxillerat in 2026–2027; further year-2 implementation is scheduled for 2027–2028. The catalogue is intentionally not a declaration of an individual school's timetable. AI teaching and PDF creation remain disabled.

## Future learning functions (not yet implemented)

1. Import official, **versioned and source-attributed** curricula from Generalitat de Catalunya authorities. Verify whether professional certificates are listed under current nomenclature and qualification rules; never invent a programme or module.
2. Select an official qualification/course and its subject/module, then generate separate PDFs for theory and practical exercises. Include sources, learning outcomes and teacher-review warnings where needed.
3. Integrate an open-source language model with offline/on-device processing where feasible, or an explicitly user-controlled, self-hosted service. Do not silently upload copyrighted textbooks, student records or other private content.
4. Add an avatar with interactive questioning, Catalan subtitles that users can reposition, and auditable open-source TTS. Select and test individual voice model licences, language quality and attribution. Natural speech quality cannot be assumed in advance.
5. Generate downloadable educational videos in **60-minute teaching sessions**. Intensive rendering may require a separately installed local or self-hosted pipeline rather than on-device video creation.
6. Create a **30-second maximum** introductory video and a **30-second maximum** app tutorial in each supported language. Each version should use an appropriate natural-sounding voiceover. Every image, audio asset and video must be approved by Blas Llavero **before** it is included in the application or pushed to GitHub.
7. Consider text alternatives, TalkBack navigation, enlarged text, contrast, and accessibility preferences for people with different disabilities.

## Saved preferences and offline learning library (planned)

LECTAV will preserve **each user's course selection and accessibility preferences**, including country, curriculum jurisdiction, educational pathway, programme, course/year/level, subject/module, interface language, subtitle positioning and optional content language. Users may resume where they stopped, switch between their saved courses and change the selection without losing existing study records.

**Teacher workflow (Android tablet/phone and web):** a personal **My courses** library showing each course with counts of independently generated lessons, PDFs, exercise sets and avatar video sessions. Default ordering will prioritise courses with the **most generated teaching material**; alternative sorting by name or most recent activity will be available. Teachers can reopen content within LECTAV or export PDFs and MP4 videos to a user-selected device folder or supported removable SD card.

**Student workflow (Android tablet/phone and web):** a personal **My learning** library listing courses and their available PDFs, lesson/video access history, exercises opened, exercises started, exercises completed and optional self-marked reading completion. Show last access date and resume position where supported. **Opening a PDF will not count as having read or completed it**, and merely opening an exercise will not count as completing it. Students can reopen downloaded files and study offline.

All exported files should remain accessible after app uninstallation, where the selected storage provider supports it. Android will use the system **Storage Access Framework**, not broad storage permissions or fixed filesystem paths, for PDF/MP4 exports and removable SD access. A removed or revoked SD folder must show a recoverable error; the app must never silently delete course records.

Initially, profile and study data will be **stored locally**. Future Android–web synchronisation will require a properly designed account and explicit user choice, encrypted connections and privacy protections for minors. The web interface must provide equivalent workflows for both teachers and students. The present APK **does not yet implement** these features; see [the storage and learning-library specification](docs/USER_PROFILES_AND_LIBRARY.md).

## Long-term global learning and university platform (planned)

LECTAV is intended to expand beyond Catalonia to **all countries and their curriculum jurisdictions** (for example, Spain's autonomous communities; elsewhere, states, provinces or other education authorities). The future picker will follow **Country → Territory/Curriculum authority (if applicable) → Education level → Institution (where relevant) → Programme/qualification/degree → Year/semester → Subject/module → Topic**. A curriculum may be national, regional or institution-specific; the app must not present generated subjects as official without verified public sources.

The platform will also support university-level learning. Prioritised disciplines are **mathematics, computer science, cybersecurity, medicine, biotechnology and bioinformatics**, with scope to expand to other degrees. University curricula need institution/programme/year attribution, not a presumed global standard.

The planned **online-course platform** will be accessible through both a responsive **website** and the **Android app (phones and tablets)**, reusing a documented open-source backend and shared curriculum catalogue. **Teachers and learners will both use either interface:** teachers will prepare, create, edit, publish and manage courses from the Android tablet/app **or** the website; learners will enrol, study, interact with the avatar, take quizzes and access exercises, PDFs, subtitled videos and progress from the Android tablet/app **or** the website. Both clients should expose equivalent core workflows and comparable interfaces, adapted to each screen size and input method, rather than separate role-restricted applications. Teaching features may include course authoring, exercises, quizzes, enrolment, self-paced sessions, learning progress, accessible subtitles, transcripts and downloaded materials. Any completion certificate will be distinguished from an official qualification unless formally accredited. Personal accounts/sync should be optional when possible, with consent, data minimisation, secure storage, safeguarding for minors and applicable privacy obligations.

Initial deployment will remain focused on verified **Catalonian curricula**, while the data model will be designed for later jurisdictions and additional languages. Only open-source components are planned; running online infrastructure still entails hosting and compute costs. The ten initial UI languages remain English, Catalan, Spanish, French, Portuguese, German, Arabic, Hindi, Simplified Chinese and Japanese.

See [PHASES.md](PHASES.md) for the expanded roadmap. These global, university and web capabilities are **future requirements**, not features of the current APK.

## Install the debug APK on Android (once a build exists)

1. Download the artifact from the **APK installation location** listed above. On GitHub, you may need to sign in to download an Actions artifact.
2. Extract the ZIP and locate `app-debug.apk`.
3. On your own Android phone or tablet, open the APK using the Files app.
4. If Android blocks the installation, use the displayed settings prompt to temporarily allow installation from that trusted Files app. Check the origin of the APK; never install an unknown or modified package.
5. Tap **Install**, then **Open**. Afterwards, you may revoke the Files app's installation permission.
6. This debug APK is for local testing. It is **not** a signed production release.

## Run and use the prototype

1. Open **LECTAV** on Android. The interface follows the device language if that language is supported; otherwise English is the fallback.
2. Choose a learning pathway. For ESO and Batxillerat, choose a school year and one of the initially verified subjects; consult the category label and official source. School-specific offerings must be confirmed with the centre.
3. CFGM, CFGS and professional certificates visibly indicate that the official catalogue is pending. No fabricated programmes appear.
4. Use **About** to read the app's purpose and authorship, or **Help** for guidance.
5. The PDF and avatar buttons are disabled by design in v0.1.0; no content-generation services have yet been connected.

## Subtitle calibration

1. From the main screen tap **Calibrate subtitle position**, or open **Help → Calibrate subtitle position**.
2. Look at the subtitle preview in the demonstration area.
3. Move the slider: left = closer to the bottom, right = higher up.
4. The value is saved automatically **on this Android device**, even after restarting the app.
5. Return using **Back**. In a future phase, the avatar player will use this same stored position.

## Media approval policy

**Do not publish or commit any image, audio, video, generated voice or other media until Blas Llavero has reviewed and expressly approved it.** The `.gitignore` excludes common media directories. This is a workflow safeguard, not a guarantee: review files before every commit. Store media for review **outside the Git repository** until approval. No media is shipped in this starter archive.

## Curriculum integrity and accessibility

The initial curricular jurisdiction is **Catalonia**. Import the published curricula from the competent public authorities, track official revisions, and distinguish ESO/Batxillerat subjects from vocational modules and professional certificate training units. A teacher should be able to review AI-generated material against those sources. Support on-device language settings, right-to-left Arabic, readable text sizes, TalkBack, and screen-safe layouts. Validate translations and accessibility with native speakers and users.

## Build from source (Android Studio or command line)

Requirements: Android Studio with Android SDK Platform 36, JDK 17, Gradle **8.13** (the CI workflow installs Gradle), and network access **at build time** to resolve open-source dependencies. The app itself has no network permission in v0.1.0.

1. Unzip the source archive and open the `lectav-starter` folder with **Android Studio → Open**.
2. Let Android Studio sync the Gradle project. The starter archive intentionally contains no bundled Gradle wrapper JAR; if prompted, use a locally installed Gradle 8.13 distribution. Alternatively, run the included GitHub Actions workflow after uploading the source to your own repository.
3. Install SDK Platform 36 using **Settings → Android SDK** if needed.
4. In Android Studio, choose **Build → Build APK(s)**. The output will be `app/build/outputs/apk/debug/app-debug.apk`.
5. Or with Gradle 8.13 installed, run `gradle :app:assembleDebug` from the project root.
6. Install the APK locally using the instructions above. No signing keys, user files, model weights or media are included in the source archive.

**Build verification status:** The Android debug APK compiled successfully in GitHub Actions on 4 November 2026; see the linked build above. This does not replace testing installation, UI behaviour and accessibility on real devices.
