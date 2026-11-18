# LECTAV 0.9.11 — Autorització única de creació amb ChatGPT

## Objectiu
La confirmació repetida d'ús de ChatGPT abans de cada generació
desapareix del mode manual, la programació trimestral i la cua automàtica.

A **Configuració → Autorització d'ús de ChatGPT**, el docent pot marcar:

> He revisat la configuració d'ús i crèdits del meu compte i autoritzo la
> creació de contingut amb ChatGPT

Una vegada marcada, LECTAV permet generar materials des del compte
connectat sense tornar a preguntar a cada petició. També permet
desmarcar-la per aturar l'autorització de futures peticions.

## Proteccions conservades
- El compte ha d'estar connectat i habilitat per a la generació.
- La preferència es vincula al compte verificat mitjançant una empremta
  SHA-256 del correu retornat per l'autenticació.
- Quan s'utilitza un compte diferent, cal aprovar el seu ús a Configuració.
- La preferència es desa **localment**. Per seguretat no s'exporta dins
  `.lectavbak` ni s'accepta des d'una còpia importada. La resta de
  configuracions i els materials continuen exportant-se.
- La cua automàtica comprova que l'autorització continuï activa
  abans de cada document; si es revoca, s'atura amb un error comprensible.
- Es conserva el límit numèric de peticions per bloc configurat a
  LECTAV, que no és el saldo real del compte.
- Els errors del servidor, la quota i els límits reals de ChatGPT
  continuen aplicant-se. LECTAV no consulta el saldo real.
- La generació manual amb Ollama o mode de demostració no exigeix
  l'autorització de ChatGPT.

## Proves
1. Instal·la la 0.9.11 després de fer una còpia de seguretat.
2. Connecta ChatGPT i obre Configuració.
3. Marca el consentiment una sola vegada.
4. Genera dos PDFs manuals: no ha d'aparèixer cap diàleg de consentiment.
5. Genera una programació trimestral: tampoc no s'ha de repetir.
6. Executa una cua automàtica amb el límit de peticions configurat;
   no ha d'aparèixer cap checkbox addicional.
7. Desmarca l'opció, torna al generador i comprova que no permet
   començar una nova petició.
8. Desconnecta/reconnecta amb un compte diferent i comprova que la
   preferència no s'aplica automàticament a aquest compte.
9. Exporta i restaura una còpia de seguretat: els models i documents
   es mantenen, però cal autoritzar ChatGPT en la instal·lació restaurada.

La compilació automàtica no substitueix les proves funcionals a la tauleta.
