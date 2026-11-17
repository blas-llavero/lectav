# LECTAV 0.9.10 — Historial de fitxers generats

## Millora
A la part inferior del **Mode automàtic** hi ha ara l'apartat
**Fitxers generats**. Mostra els fitxers de la biblioteca creats amb
LECTAV, no només els PDFs creats durant l'execució actual.

Cada entrada inclou:
- **Nom exacte del fitxer**.
- **Data i hora de generació/desament** segons el rellotge de la tauleta.
- **Curs i assignatura** si encara es conserva el curs al catàleg.
- **Tema de la sessió** extret de la programació associada.
- **Tipus de contingut**: teoria, exercicis, solucions, avatar,
  àudio, podcast o transcripció, únicament si existeix un fitxer real.

L'historial és observable des de DataStore i s'actualitza després
de completar cada desament. Ordena del més antic al més recent
amb els fitxers nous a la part inferior. Mostra els últims 80
registres; tots els fitxers continuen a la biblioteca.

## Dates i còpies de seguretat
Els registres nous inclouen `createdAt`, una marca temporal
en mil·lisegons capturada en el moment de desar el material.
`.lectavbak` preserva aquest valor en exportar i restaurar.
El camp és opcional i compatible amb còpies anteriors.

Per als fitxers anteriors a 0.9.10, la biblioteca no sempre
disposa de la data real de creació. En aquests casos es mostra
**«Data no registrada (versió anterior)»** i NO s'hi assigna la
data actual ni es dedueix cap data del nom del fitxer.

## Proves a Android
1. Desar una còpia de seguretat abans d'actualitzar.
2. Generar un PDF nou al mode automàtic i comprovar que apareix
   al final de l'historial amb la data, curs, matèria, tema i tipus.
3. Interrompre i reprendre la cua: els fitxers existents no han
   de duplicar-se i conserven la mateixa data.
4. Comprovar que els fitxers manuals generats també figuren a l'historial.
5. Comprovar que un fitxer anterior sense timestamp mostra
   «Data no registrada (versió anterior)».
6. Exportar i restaurar un `.lectavbak` i verificar les dates.
7. La llista mostra Avatar, Àudio o Podcast només quan aquests
   fitxers hagin estat generats i registrats realment.
8. La compilació GitHub Actions no substitueix la prova a la tauleta.
