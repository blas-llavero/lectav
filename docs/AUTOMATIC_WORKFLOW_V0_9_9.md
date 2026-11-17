# LECTAV 0.9.9 — mode automàtic i manual, models per material, exportació

## Navegació
La pantalla inicial conté «Mode manual» i «Mode automàtic».
El mode manual conserva tots els botons de generació existents.
El mode automàtic processa els cursos docents amb programacions desades
ordenadament per unitat, sessió i tipus de material.

## Implementació funcional d'aquesta versió
- Per sessió: **Teoria PDF → Exercicis PDF → Solucions PDF → Guió/Transcripció PDF**.
- Els exercicis utilitzen el text conservat de la teoria, les solucions el text
  dels exercicis i el guió el text de la teoria, evitant perdre el context.
- Cada generació es desa a l'emmagatzematge privat i a l'índex de la sessió
  **abans** de passar al següent material; cap document existent és substituït.
- En una següent execució s'ometen els materials que ja estan indexats, de
  manera que es pot reprendre una cua interrompuda.
- La cua es deté davant d'errors de quota, credencials, model o document i
  conserva el treball desat. No fa reintents automàtics ni bucles de despesa.
- El compte ChatGPT ha d'estar connectat i autoritzat. Un consentiment inicial
  cobreix **només el bloc actual** de fins a N peticions (per defecte 20,
  configurable fins a 500); no hi ha preguntes entre passos.
- No s'assegura execució en segon pla: el motor s'executa mentre la pantalla
  està oberta; Android pot suspendre l'app. En reobrir-la es pot reprendre.
- No s'utilitza cap model arbitrari inventat: només els models realment
  disponibles segons el compte connectat. En perdre la connexió o la quota,
  la cua s'atura.

## Configuració de models
- Model global per a tots els cursos a Configuració.
- Model per tipus de contingut (Teoria, Exercicis, Solucions, Avatar, Àudio,
  Podcast, Transcripció), amb herència del model global.
- Menú «IA» al costat de cada material de cada sessió que permet escollir un
  model explícit de la llista del compte, o heretar-ne un de global/per tipus.
- Prioritat: model explícit de la sessió > tipus de material > global.
- Tots els identificadors de model i preferències són exportables dins
  `.lectavbak`. Els tokens OAuth o l'autorització massiva **no** es copien.

## Exportació i importació
La còpia xifrada conté país, regió, itinerari, curs, matèria, llista de
cursos, programacions, preferències de generació i models, PDFs, MP3, WAV,
MP4 indexats i els textos originals si estan disponibles. La mida màxima
de materials augmenta a **1 GiB**. Si es supera aquest límit, manca espai
al dispositiu o algun URI ja no és accessible, la còpia produeix error:
no es presenta una exportació parcial com a completa. El fitxer de destinació
pot quedar incomplet i s'ha d'eliminar.

## No disponible en aquesta primera fase
LECTAV no disposa encara de generador **automàtic** d'avatar MP4, ni
d'un motor TTS integrat de podcast/àudio sense instal·lar una app.
Les seves preferències de model es poden preparar, però la cua no crea
fitxers multimèdia ficticis ni presenta els passos com completats.
La generació assistida/manual amb Piper continua sent una opció mentre
es desenvolupa el motor FOSS incorporat.

## Proves de la versió
1. Exporta una còpia de seguretat de LECTAV 0.9.8 abans d'actualitzar.
2. Configura un model de ChatGPT autoritzat i un màxim de 4 peticions.
3. Utilitza una programació amb una sessió de prova i inicia mode automàtic.
4. Comprova els 4 PDFs i la coherència de teoria, exercicis i solucions.
5. Atura una cua i reprèn-la: no ha de duplicar els documents ja guardats.
6. Comprova un model incorrecte, límit esgotat, sense xarxa, sense guió font.
7. Selecciona un model per material de sessió i torna'l a deixar heretat.
8. Exporta i restaura `.lectavbak` en un entorn de prova; verifica cursos,
   programacions, configuració, models i tots els materials.
9. Comprova que una fallada d'exportació no es comuniqui com a èxit.
10. Verifica que el mode manual continua funcionant a la Doogee.

Compilar Android a GitHub Actions no garanteix les proves funcionals ni
de compatibilitat amb el compte o el dispositiu.
