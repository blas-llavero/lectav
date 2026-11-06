# LECTAV 0.8.3 — correcció de tipografia i paginació dels PDF

## Quins problemes corregeix

Les captures de la DOOGEE T30 Ultra (6 de novembre de 2026) mostren que, a la
versió 0.8.2, la lletra del PDF era massa gran, algunes paraules de les taules
de quatre columnes es fragmentaven i la paginació tallava la taula i podia
deixar un títol sol a peu de pàgina.

**Causa:** LECTAV renderitzava Markdown/KaTeX correctament, però escrivia el
PDF amb talls cada N píxels mitjançant `WebView.draw(PdfDocument.Canvas)`.
Aquest mecanisme **no** respecta automàticament els `break-inside` ni
`break-after` de CSS, que només estan pensats per a la impressió del navegador.

## Modificacions

1. Tipografia reduïda (text de cos 11 pt → 9 pt en CSS; capçalera 18 → 14 pt;
   taules 9,5 → 8,4 pt), interlineat i marges més compactes.
2. Taules de quatre columnes amb proporcions 22% / 13% / 40% / 25%;
   `overflow-wrap: break-word` en lloc de `anywhere` per reduir la fragmentació.
3. **Paginació calculada al DOM després de carregar les fonts de KaTeX**:
   es busquen límits de blocs complets, no intervals de píxels arbitraris.
   El títol d'un apartat i el primer bloc no es poden separar. Les taules
   s'han de traslladar **senceres** a una pàgina; si una sola taula és més
   alta que una pàgina, es mostra un error descriptiu en lloc de tallar-la.
4. L'Android `PdfDocument` dibuixa exclusivament els intervals aprovats,
   amb la part inferior de cada pàgina en blanc quan un bloc passa a la
   següent. El motor aplica un marge de seguretat per evitar retalls.
5. Els salts es converteixen de píxels CSS a coordenades de WebView
   utilitzant l'amplada reportada pel document; no s'assumeix un factor
   fix d'escalat entre tauletes i telèfons.

## Proves manuals imprescindibles

1. Instal·lar 0.8.3 sense eliminar 0.8.2 i sense perdre la biblioteca.
2. Generar/desar un document de Matemàtiques amb una taula de conjunts,
   fórmules d'arrels, fraccions i símbols \(\mathbb{R}\).
3. Obrir totes les pàgines i verificar mida de text, columnes llegibles,
   taules íntegres i títols acompanyats de contingut.
4. Comparar la primera i última línia dels blocs abans i després dels
   salts per descartar continguts repetits o omesos.
5. Comprovar un PDF de moltes pàgines, un PDF amb llistes i una taula tan
   llarga que no cap: s'ha de mostrar una instrucció per dividir-la.
6. Comprovar que els PDF i vídeos anteriors de la biblioteca no han canviat.
7. Obrir l'APK sense Internet; KaTeX i markdown-it ja estan empaquetats
   localment.

## Limitacions actuals

- Les pàgines poden tenir espais en blanc al final: és preferible a tallar
  text, taules o apartats.
- Les taules que superen una pàgina encara **no es divideixen automàticament**
  en taules més petites amb capçalera repetida. L'usuari pot dividir-les
  abans de desar-les; aquesta funció es pot millorar en una altra fase.
- Els paràgrafs indivisibles més alts que una pàgina generen un missatge
  perquè es divideixin.
- Les fórmules molt llargues poden necessitar ajustaments d'edició.
- Un PDF ja desat amb la paginació antiga no es modifica retroactivament:
  cal regenerar-lo a partir del text.
- La compilació Android i les proves de KaTeX no substitueixen una revisió
  visual del PDF final a la tauleta.

Fonts: `app/src/main/assets/math/print-template.html` i
`app/src/main/java/com/blasllavero/lectav/TypesetPdfExporter.kt`.
