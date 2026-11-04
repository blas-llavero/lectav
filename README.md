# LECTAV — Open Learning Avatar

**Education without barriers.** LECTAV is a free, open-source Android educational application under development. Its goal is to offer official Catalonian curricula (ESO, Batxillerat, CFGM, CFGS and professional certificates) through accessible AI-assisted lessons, exercises and an educational avatar. This repository initially contains a **navigation and subtitle-calibration prototype**, not the finished AI application.

**APK installation location (immediately below the description):** After the first successful GitHub Actions build, open **Actions → Build Android debug APK → latest successful run → Artifacts → `lectav-debug-apk`** and unzip the downloaded artifact to obtain `app-debug.apk`. The local build output is `app/build/outputs/apk/debug/app-debug.apk`. **No APK has been compiled or published with this starter archive.** A future approved release may provide `lectav.apk` under GitHub **Releases**.

**Conceived and programmed by Blas Llavero.** Further information: [blasllavero.com](https://blasllavero.com).

LECTAV is open-source. Anyone may study, modify, share or use it for charitable and other lawful purposes, subject to the **GNU General Public License version 3 or later**. The source code is provided under `GPL-3.0-or-later`; all third-party dependencies and future media must have independently verified compatible licences. The aim is accessible education regardless of geography, language, disability or financial resources.

## Current prototype (v0.1.0)

- Native Android app written in Kotlin with Jetpack Compose.
- Five pathway choices: ESO, Batxillerat, intermediate vocational training (CFGM), higher vocational training (CFGS), professional certificates.
- Year selectors for ESO and Batxillerat; a **Mathematics demo choice only**, deliberately not presented as an official complete list.
- The official vocational and certificate programme catalogues are **not yet loaded**.
- Main-screen **About** and **Help** buttons.
- Subtitle-position calibration preview with a slider; the value is stored locally and will be used by the future avatar player.
- 10 interface languages based on Android device/app language: English, Catalan, Spanish, French, Portuguese, German, Arabic, Hindi, Simplified Chinese and Japanese.
- Subtitles are planned to begin in Catalan; the calibration preview contains Catalan sample subtitles.
- Respect for Android navigation and status bars using Compose safe-area insets; designed for phones and tablets.
- Lesson PDF, exercise PDF and avatar buttons are **intentionally disabled**, because the generation engines have not been developed yet.
- No user account, network access, telemetry, advertisements or cloud service in this prototype. The manifest requests **no INTERNET permission**.

## Future learning functions (not yet implemented)

1. Import official, **versioned and source-attributed** curricula from Generalitat de Catalunya authorities. Verify whether professional certificates are listed under current nomenclature and qualification rules; never invent a programme or module.
2. Select an official qualification/course and its subject/module, then generate separate PDFs for theory and practical exercises. Include sources, learning outcomes and teacher-review warnings where needed.
3. Integrate an open-source language model with offline/on-device processing where feasible, or an explicitly user-controlled, self-hosted service. Do not silently upload copyrighted textbooks, student records or other private content.
4. Add an avatar with interactive questioning, Catalan subtitles that users can reposition, and auditable open-source TTS. Select and test individual voice model licences, language quality and attribution. Natural speech quality cannot be assumed in advance.
5. Generate downloadable educational videos in **60-minute teaching sessions**. Intensive rendering may require a separately installed local or self-hosted pipeline rather than on-device video creation.
6. Create a **30-second maximum** introductory video and a **30-second maximum** app tutorial in each supported language. Each version should use an appropriate natural-sounding voiceover. Every image, audio asset and video must be approved by Blas Llavero **before** it is included in the application or pushed to GitHub.
7. Consider text alternatives, TalkBack navigation, enlarged text, contrast, and accessibility preferences for people with different disabilities.

## Install the debug APK on Android (once a build exists)

1. Download the artifact from the **APK installation location** listed above. On GitHub, you may need to sign in to download an Actions artifact.
2. Extract the ZIP and locate `app-debug.apk`.
3. On your own Android phone or tablet, open the APK using the Files app.
4. If Android blocks the installation, use the displayed settings prompt to temporarily allow installation from that trusted Files app. Check the origin of the APK; never install an unknown or modified package.
5. Tap **Install**, then **Open**. Afterwards, you may revoke the Files app's installation permission.
6. This debug APK is for local testing. It is **not** a signed production release.

## Run and use the prototype

1. Open **LECTAV** on Android. The interface follows the device language if that language is supported; otherwise English is the fallback.
2. Choose a learning pathway. For ESO and Batxillerat, choose a school year and the Mathematics demo subject.
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

**Build verification status:** The source and XML structure were checked when the starter archive was generated, but an Android SDK/Gradle build was not available in the creation environment. The first CI build must be treated as the initial compilation test and errors, if any, resolved before distribution.
