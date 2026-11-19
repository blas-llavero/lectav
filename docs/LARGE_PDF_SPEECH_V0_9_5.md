# LECTAV 0.9.5 — Large theory PDFs and speech preparation

## Reported issue

An approximately 17 MB lesson PDF could not be processed reliably by the speech preparation tool because the extraction path attempted to load too much content into memory. A visually formatted PDF may also contain no extractable text layer.

## Improvements

The Android PDF text extractor uses a temporary file and a constrained PDFBox pathway rather than buffering an entire large file in the application heap. The user can choose a page range to limit the intended narration. Protective limits apply to PDF size, page count and the number of characters passed to the speech editor.

Version 0.9.6 later added original Markdown source companions for generated PDFs to avoid reliance on text extraction when WebView painting produces image-like pages. Old PDFs may require manually entered narration scripts.

## Recommended tests

Test an ordinary text PDF, a large PDF, one with formulas and a visually rendered PDF without extractable text. Verify that page-range selection behaves correctly, that failure messages are explicit, and that no partial narration is reported as complete. Compare the source lesson with the generated voice script before narration.
