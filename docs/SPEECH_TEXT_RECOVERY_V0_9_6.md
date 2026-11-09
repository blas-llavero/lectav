# LECTAV 0.9.6 — guió narrable recuperable, encara que el PDF sigui visual

## Problema verificat en el codi
Els PDFs creats amb `WebView.draw(canvas)` poden ser renderitzats com una
representació gràfica sense text seleccionable. Per tant, extraure'n la teoria
amb PDFBox pot retornar una cadena buida encara que el PDF es visualitzi
correctament. La mida de 17 MB no determina si conté text seleccionable.

## Millora
- En desar **qualsevol nou PDF creat amb el generador de LECTAV**, també es
  conserva el text original aprovat en un fitxer privat UTF-8.
- En carregar teoria d'una sessió, la pantalla prova primer aquest text
  original, sempre que s'hagi sol·licitat el document complet; en altres
  casos manté l'extracció del rang de pàgines seleccionat.
- Els PDFs de versions antigues **no recuperen automàticament** un text que
  mai no es va conservar. Si l'extracció no funciona, apareix un avís i es
  pot enganxar el guió directament a l'editor.
- **Conserva aquest guió amb la teoria** associa la transcripció corregida al
  material actual, de manera que es pot recuperar en una altra sessió.
- Els textos originals associats als PDFs s'inclouen a les còpies xifrades
  i es restauren juntament amb els documents.
- Es manté el límit de 8.000 caràcters **per generació de veu** per evitar
  errors a Android. Mai es promet narrar el document complet si s'ha truncat.
- No s'afegeix cap servei remot ni es puja contingut pedagògic a GitHub.

## Prova recomanada a la tauleta
1. Fes una còpia de seguretat de la versió anterior.
2. Genera un PDF de teoria **nou** i desa'l.
3. Torna a la sessió i prem «Carrega la teoria del PDF» amb rang 1–final.
4. Comprova que l'editor mostri text i que la font indiqui «Text original».
5. Prova amb un PDF visual de la versió 0.9.5: enganxa text i prem
   «Conserva aquest guió amb la teoria».
6. Torna enrere i obre la mateixa sessió: carrega el text conservat.
7. Exporta una còpia de seguretat i comprova la recuperació del guió en una
   restauració de prova, preferentment en un perfil de prova.
8. Comprova que el botó de transcripció PDF funcioni fins i tot sense Piper.
9. La lectura en veu continua requerint Piper Android a la v0.9.6; la
   integració del motor natiu lliure dins de l'APK és una fase diferent.

## Limitacions
- Aquesta fase no incorpora OCR ni garanteix recuperar PDFs antics gràfics.
- Seleccionar un rang diferent d'1–final fa servir el lector de PDF, perquè
  el text original no es troba mapat a les pàgines.
- Un ús prolongat requerirà fragments amb continuació i codificació eficient
  per no esgotar la RAM/emmagatzematge de la tauleta.

Nota de compatibilitat: una teoria prèvia sense guió ni text seleccionable
necessita un guió nou o una transcripció manual. Els arxius de còpia anteriors
continuen essent compatibles amb el nou lector de còpies.
