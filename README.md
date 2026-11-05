# LECTAV — Open Learning Avatar

**Education without barriers.** LECTAV is a free, open-source Android educational application under development. Its goal is to offer official Catalonian curricula (ESO, Batxillerat, CFGM, CFGS and professional certificates) through accessible AI-assisted lessons, exercises and an educational avatar. This repository initially contains a **navigation and subtitle-calibration prototype**, not the finished AI application.

**APK installation location (immediately below the description):** After the first successful GitHub Actions build, open **Actions → Build Android debug APK → latest successful run → Artifacts → `lectav-debug-apk`** and unzip the downloaded artifact to obtain `app-debug.apk`. The local build output is `app/build/outputs/apk/debug/app-debug.apk`. **The first debug APK was successfully compiled on 4 November 2026** ([successful GitHub Actions run](https://github.com/blas-llavero/lectav/actions/runs/41831051287)); a production release has not yet been published. A future approved release may provide `lectav.apk` under GitHub **Releases**.

**Conceived and programmed by Blas Llavero.** Further information: [blasllavero.com](https://blasllavero.com).

LECTAV is open-source. Anyone may study, modify, share or use it for charitable and other lawful purposes, subject to the **GNU General Public License version 3 or later**. The source code is provided under `GPL-3.0-or-later`; all third-party dependencies and future media must have independently verified compatible licences. The aim is accessible education regardless of geography, language, disability or financial resources.

## Current Android prototype (v0.4.0)

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
