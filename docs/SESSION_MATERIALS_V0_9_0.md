# LECTAV 0.9.0 — Plan first, then generate materials per session

## Teacher workflow

The recommended sequence is **Choose pathway and course → Select subject → Review trimester plan → Select unit and session → Generate and review materials**.

A saved course contains independently editable unit/session metadata. The selected session's actual title is passed into each generation request. The teacher can create theory, exercises and worked solutions, then open the saved PDFs directly from the same session.

The system must not populate the library with fabricated documents or present AI-generated material as an official government-prescribed curriculum without verified citations.

## File associations

Each material keeps an internal course key, material ID, type and stable session key. The session key is derived from the trimester, unit ID and lesson number. These identifiers survive renaming the visible title and are included in encrypted backups.

Imported PDF or MP4 files may be manually assigned to a session after reviewing their content. Automatically guessing the session from the filename can attach unrelated material and is not supported.

## Media availability

A video import is not equivalent to native avatar video generation. Until a real renderer is available, the app must clearly state that avatar output is pending. Later releases can add audio and transcript types, but the index must represent only real files.

## Batch generation outlook

A sequential automatic worker can process sessions with explicit consent and a request cap, save every completed item before continuing, skip existing materials and stop on service errors. Automatic generation must never be treated as unlimited ChatGPT access. Local FOSS video/voice rendering is a distinct subsystem.

## Acceptance tests

Create two courses with similarly named sessions and verify material links remain independent. Edit a session title, import an older PDF, mark progress, export/restore a backup, and check that all links and files remain accessible without duplicates.
