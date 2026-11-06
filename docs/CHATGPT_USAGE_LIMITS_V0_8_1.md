# LECTAV 0.8.1 — control de peticions de ChatGPT

## Regla principal

LECTAV no pot llegir el saldo real d'ús, els crèdits, els diners, ni els límits del compte ChatGPT. No els inventa. L'usuari ha de consultar **ChatGPT → Configuració → Ús → Límits de les aplicacions → LECTAV → Gestiona els límits**.

Segons l'ajuda oficial d'OpenAI, un límit d'aplicació inferior al 100% impedeix que aquella aplicació faci servir crèdits de ChatGPT. L'usuari també ha de revisar l'autorització d'ús de crèdits i les compres automàtiques del compte. LECTAV no pot modificar aquests ajustos.

Documentació oficial:
https://help.openai.com/es-es/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites
https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery

## Proteccions implementades

- **Límit local per defecte: 3 intents setmanals**, configurable de 0 a 30 a la pantalla «Compte ChatGPT». Valor 0 bloqueja totes les noves peticions de generació de ChatGPT des d'aquesta instal·lació.
- Es compten **intents**, no tokens, euros ni quota del pla. Els intents fallits i les peticions interrompudes també compten, de manera conservadora.
- Els recomptes persisteixen entre reinicis de LECTAV. Es reinicien els dilluns segons la data local del dispositiu. No pretenen reproduir el període real de quota del compte.
- **Confirmació obligatòria per cada nova generació**: l'usuari ha de declarar que ha revisat els límits a ChatGPT i que no vol utilitzar crèdits. Si no marca la casella, no s'envia la petició.
- Les peticions arriben al motor d'inferència només després de reservar un intent al comptador local. El límit es comprova una segona vegada al límit de xarxa.
- Quan ChatGPT retorna `subscription_sharing_usage_limit_exceeded` o `subscription_sharing_usage_unavailable`, LECTAV atura la petició i mostra una indicació per consultar «Ús».
- El motor de ChatGPT no inclou cap clau API ordinària, cap canvi automàtic a una API facturada per separat i cap opció de compra.
- La prova de PDF local i el motor Ollama autoallotjat continuen sent independents del límit de ChatGPT.

## Límits de seguretat

LECTAV **no pot garantir per si sol cost zero** si l'usuari ha autoritzat l'ús de crèdits al seu compte. Les configuracions econòmiques reals es gestionen des de ChatGPT. El límit local és una protecció addicional, no un substitut del límit oficial. No s'ofereixen previsions falses de diners, tokens restants ni dates de reinici del pla.

Aquesta versió no factura cap pagament per si mateixa, no sol·licita dades de targetes i no permet un pas automàtic a un proveïdor de pagament.

## Proves proposades

1. Instal·lar 0.8.1 sense desinstal·lar 0.8.0.
2. Configuració → Compte ChatGPT: confirmar límit inicial de 3 i ajustar-lo a 1.
3. Generació → ChatGPT: verificar que hi ha advertiment i que cal confirmar abans de cada petició.
4. Generar un esborrany de prova i comprovar el comptador local.
5. Verificar que es bloqueja la segona petició de ChatGPT quan el límit local és 1.
6. Tornar a Configuració i posar el límit a 0: totes les peticions noves de ChatGPT han de quedar desactivades.
7. Reobrir l'aplicació: comprovar persistència del comptador.
8. PDF locals i Ollama continuen funcionant, independentment dels intents ChatGPT.
9. No cal compartir captures amb dades del compte ni tokens d'autenticació.

**Nota:** les proves de compilació no acrediten el comportament del compte d'OpenAI ni el saldo; cal verificar-lo al dispositiu i al web de ChatGPT.
