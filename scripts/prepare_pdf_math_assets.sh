#!/usr/bin/env bash
# Prepare third-party FOSS renderers for OFFLINE runtime in the LECTAV APK.
# Run before Gradle when building locally, or let GitHub Actions do it.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/assets/math"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

# Versions pinned deliberately. This script downloads dependencies at BUILD
# TIME only. At RUNTIME the Android app loads only packaged local assets.
npm install --prefix "$TEMP_DIR" --no-save --ignore-scripts --no-audit --no-fund \
  'katex@0.16.47' 'markdown-it@14.3.0'

KATEX="$TEMP_DIR/node_modules/katex"
MARKDOWN="$TEMP_DIR/node_modules/markdown-it"

mkdir -p "$OUT"
cp "$KATEX/dist/katex.min.js" "$OUT/katex.min.js"
cp "$KATEX/dist/katex.min.css" "$OUT/katex.min.css"
cp -R "$KATEX/dist/fonts" "$OUT/fonts"
cp "$MARKDOWN/dist/markdown-it.min.js" "$OUT/markdown-it.min.js"

for pkg in katex markdown-it; do
  license="$(find "$TEMP_DIR/node_modules/$pkg" -maxdepth 1 \
    -type f -iname 'LICENSE*' -print -quit)"
  test -n "$license"
  cp "$license" "$OUT/$pkg-LICENSE.txt"
done

# Compile-time smoke test; cannot replace an Android PDF visual check.
KATEX_DIR="$KATEX" MARKDOWN_DIR="$MARKDOWN" node <<'NODE'
const katex = require(process.env.KATEX_DIR);
const MarkdownIt = require(process.env.MARKDOWN_DIR);
const examples = [
  String.raw`\\sqrt{2}+(-\\sqrt{2})=0`,
  String.raw`\\mathbb{N} \\subset \\mathbb{Z} \\subset \\mathbb{Q} \\subset \\mathbb{R}`,
  String.raw`\\frac{3}{8}=0{,}375`,
  String.raw`\\frac{a}{b},\\quad a,b\\in\\mathbb{Z},\\quad b\\neq0`
];
for (const source of examples) {
  const html = katex.renderToString(source.replace(/\\\\/g, '\\'), {
    throwOnError: true, output: 'htmlAndMathml'
  });
  if (!html.includes('class="katex"')) throw Error('Math rendering failed');
}
const html = new MarkdownIt({ html: false }).render(
  '| Conjunt | Exemple |\\n|---|---|\\n| Naturals | 0, 1 |\\n'
);
if (!html.includes('<table>')) throw Error('Table rendering failed');
console.log('Math and Markdown compilation smoke tests passed.');
NODE

test -s "$OUT/katex.min.js"
test -s "$OUT/markdown-it.min.js"
test -s "$OUT/katex.min.css"
test -d "$OUT/fonts"
echo "Offline KaTeX and markdown-it assets prepared for Android APK."
