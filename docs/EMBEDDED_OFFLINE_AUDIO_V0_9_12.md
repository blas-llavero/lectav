# LECTAV 0.9.12 — Narració de teoria amb veus lliures integrades

## Objectiu
Generar àudio directament a la tauleta amb biblioteques lliures (GPL),
sense ordinador, sense compte extern de TTS i **sense instal·lar una altra
aplicació**. La síntesi és local: després de descarregar la veu, la teoria
no surt del dispositiu.

## Motor
LECTAV integra el motor VoxSherpa-TTS (`techempower-org/VoxSherpa-TTS`)
i sherpa-onnx a través de dependències Android, amb importació inicial
de models des de les versions oficials de sherpa-onnx.

La instal·lació d'una veu és **opcional i iniciada per l'usuari** a la
pantalla «Genera àudio/podcast» de la sessió:
- Català: Ona o Pau
- Castellà: Miro o Sharvard

Els arxius de veu es descarreguen des de:
`https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models`

També es descarreguen les dades obertes d'espeak-ng. Els arxius es
descomprimeixen a la carpeta privada de LECTAV, amb proteccions de
ruta, mides i espai. Els models es guarden al dispositiu per no haver-los
de descarregar a cada generació. L'usuari ha de verificar les condicions
i llicències de les veus que seleccioni. No s'inclouen ni es pugen pesos
de veus, enregistraments, imatges o vídeos al repositori de LECTAV.

## Generació manual
1. Entra en una sessió amb teoria o guió.
2. Obre «Genera àudio/podcast a la tauleta».
3. Carrega la teoria o conserva'n un guió manual (fins a 8.000
   caràcters per generació).
4. Selecciona una veu integrada i descarrega-la si encara no existeix.
5. Prem «Genera àudio amb LECTAV».
6. El motor processa fragments de la sessió sense enviar-ne text a cap
   servei, desa un WAV PCM de 16 bits i el vincula a la biblioteca.

La transcripció PDF de la sessió continua disponible. Els WAV es poden
exportar amb el nom `Uxx_Syy_Tema_Audio.wav`.

## Mode automàtic
Quan hi ha una veu integrada instal·lada, a la cua de materials apareix
«Àudio WAV» després de teoria, exercicis, solucions i transcripció.
Per a cada sessió, la narració utilitza el text original conservat
amb el guió/transcripció PDF; el model local desat és el mateix
seleccionat a la pantalla de generació manual.

No consumeix una crida de ChatGPT per sintetitzar l'àudio.
El límit de peticions configurat s'aplica només als documents
generats amb ChatGPT. La cua desa cada àudio abans de continuar
i evita duplicar fitxers ja indexats.

## Limitacions
- Els podcasts de dues veus, l'avatar MP4 i l'MP3 integrats encara no
  estan disponibles automàticament. Piper extern es manté com a opció
  temporal per als podcasts.
- Els guions de més de 8.000 caràcters s'han de dividir; el sistema
  mostra un error clar en lloc de produir una narració incompleta.
- La pronunciació de notació LaTeX, expressions matemàtiques i sigles
  s'ha de revisar abans de desar l'àudio.
- No s'ha provat encara el rendiment del model en una Doogee T30 Ultra.
  Els dispositius amb menys memòria poden trigar més o patir
  restriccions del sistema durant la síntesi.
- Les llicències dels models poden diferir de la del motor: cal validar-les
  individualment abans de redistribuir-ne pesos.

## Validació
- Fer una còpia de seguretat de la instal·lació anterior.
- Descarregar la veu catalana Ona des de LECTAV.
- Generar WAV d'un guió curt (100–300 caràcters).
- Comprovar reproducció, nom, data, matèria, tema i exportació.
- Repetir la prova sense Wi-Fi una vegada descarregada la veu.
- Seleccionar mode automàtic amb una sessió i comprovar que el WAV
  es desa després del guió i s'omet si ja existeix.
- Verificar que els documents antics i les còpies .lectavbak continuen
  funcionant.
- Confirmar les llicències de cada model abans de considerar
  redistribució de fitxers de veu.

Una compilació correcta a GitHub Actions no equival a proves a Android.
