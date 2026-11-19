# LECTAV — Repository language conventions

## English for the repository

The repository's technical documentation, pull-request descriptions, comments, commit messages, internal diagnostic strings, API names and implementation identifiers are written in **English**. Contributor-facing documentation and build instructions should use English even when the author develops or tests in Catalonia.

The preferred location for ongoing explanations is an English Markdown file under `docs/`. Older Catalan-only notes should be translated instead of duplicated in another language.

## Localized Android user experience

**English source documentation does not mean English-only users.** LECTAV serves a multilingual educational audience. Application labels, teacher-facing messages and accessibility text must be supplied from Android string resources, with English as the default (`res/values/strings.xml`) and translations in `res/values-ca`, `res/values-es` and other supported locale folders.

Existing Catalan strings embedded directly in Kotlin source are technical debt and should be progressively migrated into resource identifiers; untranslated English labels must not silently replace visible Catalan UI. The repository may therefore legitimately contain Catalan **translations** and Catalan **curriculum names/source quotations**. Those are data, not code documentation.

Never machine-translate legal titles, identifiers, mathematical LaTeX or proper nouns without verifying that the technical/curricular meaning is preserved.

## Pull-request checklist

- Document the change in English and keep the README installation instructions accurate.
- Explain what is implemented, what has only been designed, and what still requires on-device tests.
- Use English for comments and test names.
- Keep user-facing strings localizable; add English default and the necessary locale translations.
- Do not commit school/student private data, OAuth credentials, model weights or unapproved media assets.
- Keep LECTAV backwards-compatible with saved `.lectavbak` files where possible.

This policy is a target for ongoing code cleanup. The v0.9.15 change standardizes historical Markdown documentation but does not claim that every existing Kotlin UI literal has already been migrated.
