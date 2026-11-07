# LECTAV 0.9.0 — programació primer, materials per sessió després

## Ordre de treball

1. Escollir **ensenyament, curs i assignatura**.
2. Amb el compte ChatGPT autoritzat, **proposar i revisar la programació
   trimestral**: trimestres, unitats i cadascuna de les sessions amb un
   nom concret, no un títol genèric.
3. Entrar a **Els meus cursos → Programació i sessions**, desplegar
   la unitat i veure l'inventari de materials de cada sessió.
4. Generar **Temari**, **Exercicis** o **Solucions** en PDF
   des dels botons específics de la sessió. L'aplicació obre
   el generador amb el tipus i el tema preseleccionats.
5. Revisar el text generat i confirmar el desament del PDF.
   El fitxer queda registrat a **Materials** i a la sessió exacta.
6. Opcionalment, associar un MP4 preparat prèviament mitjançant
   «Importa un vídeo de l'avatar (MP4)». La generació automàtica
   del vídeo encara **no existeix** en aquesta versió.
7. L'alumne consulta els recursos vinculats a cada sessió
   i registra el progrés local.

## Exemple de noms

Sessió del primer trimestre:
**Sessió 1 - Fraccions i decimals: diagnosi inicial**

En generar el PDF, el nom predeterminat és
`Temari - Sessió 1 - Fraccions i decimals_ diagnosi inicial.pdf`.
En generar els exercicis:
`Exercicis - Sessió 1 - Fraccions i decimals_ diagnosi inicial.pdf`.
Les solucions:
`Solucions - Sessió 1 - Fraccions i decimals_ diagnosi inicial.pdf`.
Per a un vídeo MP4 importat:
`Avatar - Sessió 1 - Fraccions i decimals_ diagnosi inicial.mp4`.
El sistema d'Android pot sanejar els caràcters que no admet
en noms de fitxers, com ara els dos punts.

## Identificador estable de sessió

Cada fitxer `LocalMaterial` desa un camp opcional
`sessionKey = "trimestre|idUnitat|numSessioLocal"`.

L'associació no depèn del nom del tema: si es canvia el títol
des de la programació, els documents continuen a la mateixa sessió
i els noms antics dels fitxers NO es canvien automàticament.
Els materials generats en versions anteriors apareixen a la biblioteca
general perquè no tenen aquest identificador de sessió.

La clau també forma part de la còpia xifrada `.lectavbak`,
de manera que les relacions entre sessions i recursos persisteixen
en exportar i restaurar els cursos. Els arxius antics (sense aquesta
clau) es poden continuar llegint.

## Prova orientada a la tauleta

- Desar un backup amb la versió que hi hagi instal·lada,
  comprovar que es pot obrir el fitxer i recordar la contrasenya.
- Instal·lar LECTAV 0.9.0, fent restauració només si la instal·lació
  anterior s'ha hagut de desinstal·lar.
- Tria 3r d'ESO Matemàtiques i crea un trimestre de prova
  (o recupera la programació desada).
- Desplega Unitat 1 / Sessió 1.
- Prem **Teoria** i comprova que s'omple el títol exacte
  i està seleccionat **Teoria**; genera i desa un PDF curt.
- Torna a la programació i comprova «Teoria 1».
  Prem «Obre…» i visualitza el PDF.
- Prem **Exercicis**; comprova que apareix el mateix
  nom de sessió amb prefix Exercicis i no es reescriu el temari.
- Prem **Solucions** i desa una mostra curta.
- Importa un MP4 de prova i comprova que el recurs apareix
  com a Avatar 1, amb el nom «Avatar - Sessió N ...».
- Desa un backup `.lectavbak`, restaura'l només després
  de verificar el contingut i comprova les associacions.
- Comprova que una altra sessió no veu els PDF de la primera.

## Fase posterior: generar un curs complet sense desenes de clics

Aquesta versió **NO implementa** la creació automàtica de tots els
materials i vídeos d'un curs. Abans cal incorporar un sistema local
de **cua persistent** amb tasques independents per trimestre →
unitat → sessió → tipus de recurs, amb estats
`pendent/en_curs/revisio/completat/error`, cancel·lació i represa.

Proposta de política de costos:
- Permetre escollir l'abast: només programació, només teoria,
  exercicis, solucions, avatar o tot el curs.
- Fer una previsió de tasques i estimació aproximada de peticions,
  **sense afirmar que es coneixen els crèdits reals de ChatGPT**.
- Obrir una confirmació de quota i crèdits abans de qualsevol
  bloc nou. **Aturar immediatament la cua** quan s'assoleixi
  el límit local o un error de quota del servei.
- Evitar duplicitats: no tornar a generar fitxers ja existents,
  llevat que el professor ho confirmi.
- Desar resultats progressivament, amb còpies a SD escollides
  explícitament, i poder reprendre sense recomençar.
- Dividir les generacions en blocs petits per fiabilitat,
  **sense promeses de gratuïtat o de quota il·limitada**:
  els lots també poden consumir tokens, intents o crèdits.
- Els vídeos requereixen un motor específic d'avatar/TTS/render
  (FOSS local o autoallotjat, subjecte a capacitat del dispositiu);
  la generació de text amb ChatGPT no produeix automàticament
  un MP4 amb avatar.
- No enviar dades identificatives d'alumnes a serveis externs.
- Mantenir la revisió pedagògica i validació de fonts oficials
  abans de publicar materials compartits.

## Limitacions actuals

Els documents desats només a la memòria privada de LECTAV es poden
perdre en desinstal·lar; exportar còpia a Documents/SD i conservar
una còpia xifrada `.lectavbak`.

Aquest prototip només registra progrés local; no ofereix comptes
d'alumnes separats ni gestió d'autoritzacions d'un centre.
La compilació CI comprova Kotlin/Gradle, no el comportament
de les tauletes ni la correcció pedagògica.
