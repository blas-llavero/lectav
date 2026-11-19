# LECTAV 0.8.2 — Render mathematical notation in PDF lessons

## Original problem

The early PDF generator printed Markdown and LaTeX expressions as literal text using a basic Android graphics pipeline rather than displaying properly typeset fractions, equations and symbols.

## Rendering implementation

The application bundles an offline HTML template with KaTeX and markdown-it assets. An Android WebView formats Markdown and mathematical expressions locally and produces A4 pages through the Android PDF drawing system. The WebView must not load external network resources or evaluate untrusted remote scripts during generation.

Typical accepted notation includes inline math, display equations, fractions, radicals, arithmetic symbols, emphasis, headings and tables. Complex unsupported LaTeX should result in a visible warning rather than malformed output.

## Privacy and accessibility

The renderer works without uploading PDF contents to a third-party service. Preserve the original editable Markdown separately because painted pages may not provide a reliable text layer for screen readers or speech synthesis. Teachers should review mathematical output and solutions for correctness.

## Visual acceptance tests

Generate documents containing signed integers, fractions, multiplication, division, roots, long formula lines and tables. Inspect every page in the Android PDF viewer, particularly font embedding, clipping and missing glyphs. Test both on-device creation and individual document export to shared storage.
