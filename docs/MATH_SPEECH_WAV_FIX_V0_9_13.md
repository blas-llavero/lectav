# LECTAV 0.9.13 — Correcció de la generació WAV en sessions de matemàtiques

## Problema reproduït conceptualment
En una sessió de 3r d'ESO sobre enters i fraccions, el guió conté fórmules LaTeX com
`\\frac34-\\frac16`, `(-4)\\cdot(-3)` i `3-(-5)`.
El sintetitzador local de la 0.9.12 rebia el codi matemàtic sense
traducció oral; a més, si fallava, el missatge no sempre era visible.

**La causa concreta de la fallada WAV no es pot determinar exclusivament
a partir del text:** els models de veu, el motor natiu i la memòria
del dispositiu també poden impedir la síntesi.

## Solució a 0.9.13
- `MathSpeechNormalizer` prepara una còpia oral del guió:
  fraccions comunes, suma, resta, producte, divisió i parèntesis.
- Manté intactes els PDFs originals i el text educatiu guardat.
- Processa fórmules `\\(...\\)` i `\\[...\\]`, i Markdown bàsic
  com taules i encapçalaments. No assumeix que pugui interpretar
  totes les expressions LaTeX avançades.
- Inclou el botó **Comprova el motor amb una frase curta** per
  diferenciar problemes del motor de problemes amb les fórmules.
- Mostra els errors retornats pel motor al final de la pantalla,
  amb més informació que a la 0.9.12.
- Les proves JUnit de conversió de fraccions i enters s'executen
  a GitHub Actions abans de compilar l'APK.

## Prova a la tauleta
1. Desa una còpia de seguretat abans d'actualitzar.
2. Selecciona la veu integrada catalana Ona o Pau ja descarregada.
3. Prem «Comprova el motor amb una frase curta».
4. Si s'indica que la prova és funcional, carrega la teoria de
   «Diagnosi de càlcul amb enters i fraccions».
5. Prem «Genera àudio amb LECTAV» i comprova el WAV a la biblioteca.
6. Si torna a fallar, adjunta el text literal de l'error que apareix
   sota el generador. Aquest error permet concretar si el problema
   prové del model de veu, la memòria o la normalització matemàtica.

Nota: la prova del motor també sintetitza un fitxer de comprovació local;
no l'afegeix a la biblioteca. Una compilació correcta no garanteix
que els models de veu funcionin en cada tauleta.

Les lectures generades són aproximacions pedagògiques. Cal escoltar
i revisar les explicacions matemàtiques abans de distribuir-les.
