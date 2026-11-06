# LECTAV 0.8.2 — PDF amb fórmules matemàtiques reals

## Error corregit

Fins a la 0.8.1, LECTAV feia servir `android.graphics.pdf.PdfDocument`
i `Canvas.drawText()` per escriure literalment l'esborrany. Això feia que
apareguessin `\\frac{a}{b}`, `\\sqrt{2}`, `\\mathbb{N}`, `**negreta**`
i les barres verticals de taules Markdown sense interpretar.

## Nou procés de desament

1. L'usuari genera un esborrany i el revisa a l'editor.
2. Quan prem «Desa aquest PDF», el contingut és processat **al dispositiu**
   mitjançant un WebView temporal no visible.
3. El Markdown es converteix en HTML amb `markdown-it`, amb `html:false`
   perquè un text d'IA no pugui injectar HTML arbitrari.
4. Les fórmules `\\(...\\)`, `\\[...\\]`, `$...$` i `$$...$$`
   es representen amb KaTeX, incloses les tipografies matemàtiques.
5. Quan les fonts s'han carregat, Android utilitza el
   `WebView.createPrintDocumentAdapter` per escriure un PDF A4 real
   dins de la carpeta privada de LECTAV.
6. El PDF conserva el mateix flux FileProvider i es vincula a «Materials».
7. Si alguna expressió no es pot interpretar, la conversió falla explícitament
   i l'usuari pot corregir l'esborrany abans de repetir-la.

**Cap contingut del PDF s'envia a un servidor de conversió.** Els fitxers JS,
CSS i WOFF2 de KaTeX/markdown-it s'inclouen a l'APK. El script de preparació
els descarrega només durant la compilació; no hi ha cap CDN en temps d'execució.

## Eines de codi obert

- KaTeX (MIT): https://katex.org/
- markdown-it (MIT): https://github.com/markdown-it/markdown-it
- Android WebView Printing: https://developer.android.com/reference/android/webkit/WebView#createPrintDocumentAdapter(java.lang.String)

La configuració de GitHub Actions executa
`bash scripts/prepare_pdf_math_assets.sh` abans de Gradle. Per compilar
en un ordinador, cal Node.js/npm i executar el mateix script abans de
`gradle :app:assembleDebug`. Es conserven els avisos de llicència dins de l'APK.

## Limitacions

- KaTeX només admet un subconjunt de LaTeX; una fórmula mal formada produeix
  un error llegible en comptes d'exportar un PDF falsament correcte.
- Les fórmules molt llargues i algunes taules grans poden requerir
  adaptar l'esborrany abans de desar-lo.
- Els PDF **antics** ja generats no es reparen sols: cal tornar a generar
  el document, ja que l'aplicació només en conservava el PDF, no necessàriament
  tot el text original.
- Els continguts i les solucions de la IA s'han de revisar pedagògicament.
- La compilació automàtica no substitueix la prova visual al dispositiu,
  especialment en documents llargs i amb diverses pàgines.
- La vista prèvia editable continua mostrant Markdown/LaTeX com a codi;
  el nou format es representa en el **PDF exportat**. Una vista prèvia
  visual completa és una millora posterior.

## Proves recomanades

- Genera una explicació de Matemàtiques de 3r d'ESO amb
  `\\sqrt{2}`, `\\frac{3}{8}`, `\\mathbb{N}`, `x^2`
  i una taula Markdown.
- Desa el PDF i obre'l a «Els meus cursos → Materials».
- Comprova que surten les arrels, fraccions, negretes i taules maquetades.
- Prova pàgines 2 i 3, quan n'hi hagi; comprova que el text no es retalla.
- Comprova que altres PDF i MP4 importats encara funcionen, sense perdre'ls.
- Comprova que el PDF es pot obrir sense Internet un cop desat.
