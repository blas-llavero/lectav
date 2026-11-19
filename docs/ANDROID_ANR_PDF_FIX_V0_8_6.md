# LECTAV 0.8.6 — Android responsiveness during PDF generation

## Incident

A DOOGEE tablet showed an Android Application Not Responding (ANR) warning while rendering or saving a large generated educational PDF. The observed symptom did not establish a unique root cause; exporting a WebView-based PDF can be expensive in CPU time and memory.

## Mitigations

The app performs costly PDF writing/validation away from the main UI thread where possible, yields between page operations, permits one active export, applies output and page limits, and shows progress/cancellation. The generated Markdown preview can be collapsed to reduce rendering workload.

The renderer still has constraints around maximum pages, very long tables and complete WebView drawing. These are protective limits rather than an assurance that all Android models can render every document.

## Testing and diagnostics

Run increasingly long theory and exercise documents on the target tablet, with mathematics, tables and multi-page layouts. Check that touch input and the back action remain responsive. Inspect every output page for clipping or missing symbols. If an ANR recurs, capture the Android trace and document size before changing timeouts or memory limits.

PDF export correctness, valid fonts and visible completion feedback are separate from performance. GitHub Actions compilation is not sufficient to validate Android UI responsiveness.
