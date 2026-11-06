# LECTAV 0.8.6 — resposta a l'ANR durant la generació de PDF

## Incidència

Un usuari va informar d'avisos repetits d'Android:
**«L'aplicació LECTAV no respon»**, després de generar un temari i uns
exercicis, crear-ne els PDF i exportar-ne una còpia. Va acabar desinstal·lant
la versió 0.8.5 per recuperar el funcionament.

La captura demostra un **ANR (Application Not Responding)**, no una
excepció concreta. Sense el rastre d'ANR (`adb bugreport`,
`logcat` o `ApplicationExitInfo`) no podem atribuir definitivament
el problema a una sola operació.

## Causa probable identificada al codi

A `TypesetPdfExporter.save()`, la v0.8.5 executava dins de
`withContext(Dispatchers.Main)`:
- Dibuix de **totes** les pàgines d'un `WebView` de gran alçada en
  diversos `PdfDocument` (possible càrrega elevada al processador).
- `PdfDocument.writeTo(...)`: escriure tot el PDF a fitxer.
- `checkNotBlank()`: tornar a obrir el PDF, renderitzar-ne una pàgina
  amb `PdfRenderer` i examinar la imatge.
- `WebView.enableSlowWholeDocumentDraw()` a cada nou desament.
- Una caixa d'edició Compose `OutlinedTextField` amb contingut potencialment
  molt llarg continuava oberta després de desar-lo.

L'ANR s'activa quan el fil principal no respon als esdeveniments dins del
termini d'Android. Aquests punts són *hipòtesis tècniques* verificades
per lectura del codi, no una atribució forense de l'ANR de la tauleta.

Referència: https://developer.android.com/topic/performance/anrs/keep-your-app-responsive

## Modificacions

1. **Escriptura del PDF al disc i verificació amb PdfRenderer a
   `Dispatchers.IO`**, en lloc del fil principal.
2. Dibuix de cada pàgina del WebView a `Dispatchers.Main` (requisit
   de la vista Android), però amb `yield()` i `delay()` per permetre
   processar interaccions i cancel·lació entre pàgines.
3. Nou màxim conservador de **18 pàgines per document** i una comprovació
   de durada durant el dibuix de pàgines; si es supera, es mostra un error
   i es recomana dividir el document en sessions.
4. Un **Mutex** impedeix dues conversions de PDF simultànies, amb
   missatge clar en cas de petició duplicada.
5. La inicialització de `enableSlowWholeDocumentDraw()` es fa com a
   màxim un cop per procés, abans de crear el primer WebView de PDF.
6. L'editor de continguts llargs es plega fins que l'usuari l'obre
   expressament; després de desar el PDF queda plegat.
7. A la pantalla d'esborrany apareixen un indicador de progrés i un botó
   **«Cancel·la la creació del PDF»**, que avorta la tasca entre pàgines.
8. La creació del PDF, la maquetació KaTeX i els salts de pàgina es
   conserven; no es canvia la connexió de ChatGPT, l'importació ni
   l'exportació a la SD.

## Limitacions i proves necessàries

- `WebView.draw(canvas)` ha d'executar-se al fil principal i una sola
  crida especialment pesada podria trigar massa. Les pauses entre pàgines
  redueixen el risc, però no poden interrompre una crida nativa bloquejada.
- La comprovació de temps té lloc entre pàgines, no interromp per força
  una única pàgina bloquejada. No s'ha d'afirmar eliminació absoluta d'ANR.
- Les sessions d'Android, preferències i fitxers guardats exclusivament
  a `/data/user/0/com.blasllavero.lectav/` normalment s'eliminen en
  desinstal·lar l'app. Els documents **exportats** a Documents/SD
  acostumen a continuar disponibles. Tornar-los a importar no consumeix
  intents de ChatGPT.
- Cal provar en una tauleta real; una compilació de GitHub Actions
  només confirma que el codi Kotlin/Android compila.
- Recomanació de prova: començar per un PDF de 1–2 pàgines, obrir-lo,
  exportar-lo, tancar LECTAV; després provar progressivament exercicis
  més llargs. No repetir de seguida una generació que deixi la UI penjada.
- Si reapareix, obtenir **un bug report d'Android** o el rastre ANR
  mitjançant ADB (sense incloure tokens o documents personals);
  estudiar la possibilitat de renderització independent amb serveis
  separats o motores de PDF per blocs.

## Estratègia de preservació de documents

Conservar còpies de materials importants fora de l'espai privat de LECTAV,
a **Documents** o una **targeta SD**, amb `Desa una còpia a una carpeta…`.
La propera línia de treball serà oferir backup/restore automàtic opcional
i un destí d'exportació predeterminat triat per l'usuari.
