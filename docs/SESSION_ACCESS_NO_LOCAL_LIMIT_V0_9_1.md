# LECTAV 0.9.1 — sense bloqueig local i materials accessibles per sessió

## S'ha eliminat el límit artificial d'intents

La versió 0.9.0 conservava una política antiga de fins a 30 intents locals
per setmana i desactivava el botó de generar quan el comptador arribava
a zero. Això impedia generar nous documents encara que el compte ChatGPT
pogués continuar generant.

A la versió **0.9.1**:
- La comptabilització i el bloqueig locals **s'han eliminat**.
- No es mostra ni es modifica el comptador de generacions.
- No es reintrodueix el bloqueig en restaurar còpies de seguretat
  `.lectavbak` d'una versió anterior.
- La pantalla «Compte ChatGPT» informa que LECTAV no pot llegir
  la quota real ni els crèdits disponibles.
- Abans d'enviar una petició, es manté el consentiment explícit sobre
  el possible consum de crèdits i les preferències del compte.
- Si OpenAI retorna un error de quota esgotada o ús no disponible,
  la petició s'atura; **no** es canvia a cap API de pagament alternativa.

**Això NO elimina els límits reals del compte ChatGPT ni garanteix
que generar sigui gratuït** si s'han autoritzat crèdits addicionals.
Per a controls reals, comprovar ChatGPT → Configuració → Ús.

## Noms de fitxers per sessió

Per a cada sessió programada, el nom proposat és:

- `Sessió 1 - Fraccions i decimals - diagnosi inicial - Teoria.pdf`
- `Sessió 1 - Fraccions i decimals - diagnosi inicial - Exercicis.pdf`
- `Sessió 1 - Fraccions i decimals - diagnosi inicial - Solucions.pdf`
- `Sessió 1 - Fraccions i decimals - diagnosi inicial - Avatar.mp4`

Els dos punts del títol de l'exemple es substitueixen per un guió perquè
la còpia pugui desar-se a carpetes compatibles amb targetes SD/Windows.

Aquest format s'utilitza tant en desar PDF nous com en el selector
«Desa una còpia a una carpeta…» i per suggerir el nom dels vídeos
MP4 que s'importin a la sessió. Una exportació repetida no duplica
la paraula «Teoria»/«Exercicis»/«Solucions»/«Avatar» al nom.

Els noms dels fitxers ja creats **no es canvien destructivament**:
la biblioteca conserva l'original i, quan es tornen a exportar,
proposa un nom amb el nou format.

## La programació com a índex navegable

Els materials es vinculen al camp local
`sessionKey = trimestre|identificadorUnitat|sessióLocal`,
que no depèn del text del títol. Per cada sessió,
LECTAV ofereix quatre apartats:

- **Teoria**: obrir PDF existent o generar-ne un de nou.
- **Exercicis**: obrir PDF existent o generar-ne un de nou.
- **Solucions**: obrir PDF existent o generar-ne un de nou.
- **Avatar**: obrir el MP4 existent o importar-ne un. El botó
  «Genera vídeo de l'avatar» continua desactivat fins implementar
  un motor audiovisual veritable.

Si un tipus ja té diversos fitxers, s'ofereixen també els enllaços
de versions anteriors. Els fitxers no s'esborren en generar-ne un de nou.

Per als PDF/MP4 de la biblioteca creats **abans d'existir el camp
`sessionKey`**, el professor pot prémer
«Vincula un document anterior a aquesta sessió» i escollir-lo
manualment després de revisar que el contingut correspon a
aquella sessió. No es vinculen automàticament fitxers antics
basant-se en noms, perquè això podria confondre sessions
que tinguin un títol semblant.

La vinculació està inclosa als arxius xifrats `.lectavbak`.
En regenerar un trimestre, les sessions noves poden tenir altres
identificadors: els documents antics es conserven a Materials
i no s'adjudiquen automàticament a un temari diferent.

## Estat del vídeo i de la generació massiva

En aquesta versió ChatGPT només genera el **text** de teoria,
exercicis i solucions, que LECTAV exporta a PDF. El vídeo de
l'avatar requereix un motor separat d'animació/veu/render local
o autoallotjat; **encara no se'n pot generar un MP4 directament
a partir de ChatGPT**. Només es pot importar un vídeo existent.

La futura cua de generació de tot el curs per blocs necessitarà
pausa/represa, deduplicació, comprovació de quota real del servei
i confirmació abans de qualsevol bloc que pugui consumir crèdits.
Dividir peticions no produeix quota il·limitada.

## Proves manuals a Android

1. Crear un backup `.lectavbak` abans d'actualitzar l'APK.
2. Verificar que el generador de ChatGPT ja **no bloqueja**
   quan l'antic comptador local havia arribat a zero.
3. Generar un PDF de Teoria d'una sessió de prova i desar-lo.
4. Repetir per Exercicis i Solucions; comprovar el sufix de cada nom.
5. Obrir «Programació i sessions», buscar la sessió i prémer
   «Obre» a la fila corresponent. Verificar el document correcte.
6. Importar un MP4 associat a la sessió; comprovar
   `Sessió ... - Avatar.mp4` com a nom proposat i l'enllaç al visor.
7. Vincular manualment un PDF antic a una sessió, sense tornar a
   gastar cap crèdit, i obrir-lo des de la programació.
8. Exportar un backup, restaurar-lo en una instal·lació de prova
   i comprovar que es mantenen les vinculacions de sessió
   i que no torna el bloqueig local.
9. Comprovar que els errors reals de quota de ChatGPT
   s'aturen sense canvi de model/proveïdor de pagament.

**Una compilació Android correcta no demostra que aquestes funcions
hagin estat validades físicament a la tauleta.**
