# LECTAV 0.8.7 — fitxers amb nom descriptiu i còpia de seguretat

## Noms automàtics a l'exportació

A partir de 0.8.7, en crear/desar PDF o en prémer
**Desa una còpia a una carpeta…**, el nom proposat serà:
- **Temari - Nombres racionals, reals i operacions.pdf** (`lesson`)
- **Exercicis - Nombres racionals, reals i operacions.pdf** (`exercise`)
- **Solucions - Nombres racionals, reals i operacions.pdf** (`solutions`)
- **Vídeo - Nom del material.mp4** (`video`)

També es proposa un nom descriptiu per als materials antics.
El selector d'Android permet modificar el nom abans de guardar.

## Exportar una còpia completa a la memòria compartida o targeta SD

Configuració → **Còpia de seguretat · Exporta i restaura**.
Escriu i repeteix una contrasenya d'almenys 10 caràcters; tria una
carpeta a Android i exporta `LECTAV-backup-AAAA-MM-DD_HH-mm.lectavbak`.

És un únic arxiu **xifrat** amb AES-256-GCM, amb clau derivada
de la contrasenya mitjançant PBKDF2-HMAC-SHA256 (220.000 iteracions).
La contrasenya **no es guarda** a LECTAV.

Contingut de la còpia:
- País/territori, itinerari, curs, matèria, rol i cursos marcats,
  idioma d'interfície i calibratge de subtítols.
- Temaris, títols editats i progrés marcat en sessions.
- Configuració del motor IA (ChatGPT/Ollama), idioma de generació,
  tipus de document, últim model escollit i URL LAN d'Ollama.
- Límit i comptador **locals** d'intents ChatGPT (no són la quota
  real del compte); referència de l'últim compte utilitzat.
- Extractes curriculars oficials consultats, dates i empremtes.
- PDF generats i materials PDF/MP4 importats que LECTAV
  encara pugui llegir, juntament amb les etiquetes, tipus i
  estat de lectura/completat. **Límit: 250 MB de materials**.

Els documents restaurats tenen referències locals noves: no es
conserven les antigues autoritzacions `content://` del selector,
ja que poden caducar després de desinstal·lar Android. Els materials
importats es restauren com a còpies privades, no a la carpeta antiga
d'origen. Les còpies que hagis exportat abans a SD són independents.

**No s'exporten**: contrasenya de ChatGPT, access token, refresh token,
ID token, claus Android Keystore, dades de facturació ni cap clau API.
Aquestes credencials NO són transportables amb seguretat.
Després de restaurar, **torna a iniciar sessió i autoritza ChatGPT**.
LECTAV no pot restaurar una sessió d'OpenAI d'una instal·lació
que s'ha desinstal·lat; la còpia conté, com a recordatori, l'adreça
del compte anterior si l'aplicació la coneix.

## Restaurar

1. Instal·la LECTAV i obre Configuració → Còpia de seguretat.
2. Escriu la contrasenya **original** de l'arxiu.
3. Prem «Selecciona un fitxer .lectavbak» i tria l'arxiu de la SD
   o memòria compartida.
4. Confirma l'avís de **substitució** de la biblioteca i dels cursos.
5. LECTAV autentica íntegrament el fitxer xifrat abans de restaurar
   cap dada, extreu els documents al directori intern privat i
   reconstrueix els registres de materials.
6. Comprova les preferències, els cursos, les sessions, els PDF i els
   MP4. Torna a connectar ChatGPT.

No és una sincronització automàtica. No es pengen dades al núvol
excepte si l'usuari tria expressament un proveïdor de documents
al núvol durant el desament.

## Precaució amb els APK de GitHub Actions

Els APK `debug` de GitHub Actions poden tenir una **signatura diferent
a cada compilació**. Si Android impedeix actualitzar sense desinstal·lar,
la còpia de seguretat s'ha de fer *abans de la desinstal·lació*.

**Important per a aquesta primera versió 0.8.7:** l'app antiga 0.8.6
encara no tenia exportació de configuració. Si no és possible
instal·lar 0.8.7 sobre 0.8.6, **no podrà migrar-se la configuració
privada anterior** sense mecanismes addicionals. Exporta els PDF
individualment abans de desinstal·lar, i pren nota dels cursos
i ajustos. A partir de 0.8.7 es podran recuperar amb l'arxiu
`.lectavbak` si es crea abans de desinstal·lar.

**Pròxima millora estructural:** signar totes les versions amb una
clau persistent guardada com a secret de GitHub Actions (mai
en un repositori públic), per actualitzar Android sense perdre dades.

## Consideracions

- Arxiu xifrat: si s'oblida la contrasenya, no es pot recuperar.
- El sistema limita a 250 MB les còpies completes per reduir
  bloquejos i ús de memòria. Exporta separadament vídeos grans.
- La importació reemplaça els registres d'aquesta instal·lació
  només després de confirmar; no barreja comptes ChatGPT.
- Mantenir còpies xifrades fora del dispositiu és recomanable,
  però sense pujar arxius amb dades d'alumnes a repositoris públics.
- Els continguts exportats des de l'app via *Save As* continuen
  sent documents independents en Documents/SD.
- Si es trenca una restauració durant l'escriptura de preferències,
  la biblioteca podria haver-se actualitzat parcialment: no
  desinstal·lis l'app, torna a provar la importació i conserva
  l'arxiu original.
- CI compilada no equival a prova de dispositiu: provar exportar,
  restaurar, obrir PDF, llegir vídeos i tornar a connectar ChatGPT.

Documentació Android: https://developer.android.com/training/data-storage/shared/documents-files
