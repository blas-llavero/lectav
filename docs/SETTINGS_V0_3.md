# LECTAV 0.3.0 — saved settings

The Android prototype stores country (ES), Catalonia (ES-CT), learning pathway,
course/year, subject and subtitle calibration using DataStore. These jurisdiction
values are currently fixed to the only verified catalogue and will become
selectable after further official curricula have been integrated.

An AppCompat language picker supports 10 languages plus the Android device
language. AppCompat persists explicit selections across app restarts, including
Android 12 and earlier through the autoStoreLocales service.

Upgrading from v0.2.0 migrates the previous `subtitle_height` setting from
SharedPreferences. Closing or reopening the app restores the last selection.

Manual test: choose ESO -> Year 3 -> Mathematics; change language; calibrate the
subtitle position; close/reopen; verify values; clear the selection and verify
that the language and calibration remain.

Not implemented: multiple saved courses, external storage/SD, generated PDFs,
MP4 video, avatar, learning history or cross-device syncing.
