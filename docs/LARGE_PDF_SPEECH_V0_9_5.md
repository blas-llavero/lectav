# LECTAV 0.9.5 — PDF de teoria de 17 MB i preparació del TTS integrat

## Correcció immediata
El lector anterior restringia la mida a 12 MB perquè llegia els bytes complets en RAM.
Ara copia el PDF a un fitxer temporal privat, el llegeix amb PDFBox mitjançant fitxers temporals
i extreu el text de les pàgines seleccionades. Un document de 17 MB es pot processar, sense
comprimir-lo, sempre que hi hagi espai lliure i conté text seleccionable.

- Mida màxima per a aquesta via: 80 MB (límit preventiu de recursos i espai).
- Pàgines: fins a 250; es pot indicar un rang a la mateixa pantalla.
- Guió: màxim 8.000 caràcters per procés d'àudio de la versió actual.
- Avís explícit quan s'arriba als 8.000 caràcters: no implica que s'hagi narrat tot el PDF.
- No s'envia el document a cap servidor i el temporal s'elimina amb `finally`.
- No converteix imatges escanejades en text: caldrà OCR local.

## Motor de veu incorporat a LECTAV: fase següent, no implementada en aquesta correcció

L'objectiu confirmat és evitar instal·lar una altra aplicació. L'arquitectura
recomanada és integrar la biblioteca lliure `piper-kotlin` dins de LECTAV
(`https://github.com/piper-tts-apps/piper-kotlin`), que exposa un motor JNI
amb espeak-ng i ONNX Runtime. **No** afegir una app de sistema separada.

Requisits abans de distribuir:
1. Compilar les biblioteques natives per a Android amb NDK/CMake i empaquetar-les;
   és un canvi de build diferent de la compilació Kotlin de la 0.9.5.
2. Verificar el format i les llicències dels models `.onnx` i `.onnx.json`
   (català/castellà inclosos). Cal escollir models realment disponibles.
3. Importar els models mitjançant el selector de fitxers de LECTAV o un catàleg
   revisat, sense incloure enregistraments o pesos de veu abans de l'aprovació.
4. Desar-los localment i sintetitzar per fragments, amb cancel·lació,
   alliberament de recursos i exportació WAV/MP3 si s'incorpora codificador FOSS.
5. Mesurar rendiment, memòria i bateria a Doogee T30 Ultra i T40 Pro.

La versió 0.9.5 encara utilitza Piper Android com a motor de veu extern opcional.
No afirma que el motor natiu integrat ja estigui preparat.

## Prova
- Guardar còpia de seguretat abans d'instal·lar.
- Obrir una sessió amb PDF de teoria de ~17 MB i extreure pàgines 1–3.
- Comprovar que s'extreu text i que es mostra el recompte de pàgines.
- Provar rangs diferents, un PDF sense text i una selecció fora de rang.
- Si el text és llarg, verificar l'avís de tall i narrar en diversos rangs.
- Comprovar que els PDFs antics i els fitxers MP3/WAV continuen accessibles.

La compilació automàtica d'Android no equival a haver fet aquestes proves al dispositiu.
