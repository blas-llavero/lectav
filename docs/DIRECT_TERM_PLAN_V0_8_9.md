# LECTAV 0.8.9 — programació per trimestres des de la selecció del curs

## Accés directe

Des de **Pantalla inicial → Ensenyament → Curs → Assignatura**, el
professor pot prémer **«Genera programació trimestral amb ChatGPT»**.

La selecció es desa primer a «Els meus cursos», i immediatament s'obre
la programació sense haver de tornar enrere a la biblioteca. El botó
només apareix en assignatures seleccionables i en el rol de professor.

Cap canvi dels desplegables **no genera automàticament peticions d'IA**:
l'usuari ha de triar el trimestre i confirmar explícitament que vol
consumir una petició de ChatGPT. La connexió pot continuar subjecta als
límits i crèdits del compte i al límit local de LECTAV.

## Trimestre, unitats i sessions

- Tres trimestres disponibles.
- El professor estableix de 6 a 48 sessions previstes (30 per defecte).
- ChatGPT genera dues a vuit unitats i un títol propi per a cada sessió.
- La validació ara exigeix que el recompte generat sigui **exactament**
  el nombre sol·licitat, abans de mostrar un esborrany.
- A **Els meus cursos → Temari**, l'índex mostra
  «Trimestre → Unitat → Sessió número N · tema concret».
- La numeració és consecutiva dins del trimestre encara que hi hagi
  diverses unitats, i es torna a iniciar al primer tema del següent trimestre.
- Després de desar, el professor pot modificar tant el nom de la unitat
  com cada títol de sessió sense cridar novament ChatGPT.
- En obrir «Genera materials» per una sessió, el camp «Tema o concepte»
  s'emplena automàticament amb el títol concret d'aquella sessió.
- Un trimestre nou es desa sense esborrar els altres. Si el professor
  substitueix un trimestre existent, LECTAV adverteix que les marques
  de progrés d'aquell trimestre es reiniciaran.

## Currículum oficial sota demanda

Sense incloure un temari precarregat, LECTAV pot descarregar directament
els PDF oficials d'ESO publicats a XTEC per:
- Matemàtiques
  https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Matematiques.pdf
- Biologia i Geologia
  https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Biologia-i-Geologia.pdf

A la pantalla de planificació, si no existeix una còpia local,
LECTAV intenta descarregar el PDF corresponent i extreure'n el text
**en el propi dispositiu**. Aquesta descàrrega no utilitza la quota de
ChatGPT. Si no es pot recuperar, mostra un avís i la generació amb IA
continua com a **proposta orientativa sense verificació curricular**.
En altres matèries sense font associada, es pot consultar i importar
manualmente un PDF oficial des de la pantalla Currículum.

**No es declara que l'ordre dels temes sigui una prescripció legal**.
El PDF pot incloure sabers aplicables a diferents cursos i cal verificar
la normativa vigent, la distribució real al centre i les
modificacions legislatives. La programació segueix sent un índex
didàctic, no un document institucional complet d'avaluació,
competències, mesures d'atenció a la diversitat i rúbriques.

## Seguretat, continuïtat i còpia

- Els models d'IA estan vinculats al compte de ChatGPT de l'usuari.
  La generació de l'índex requereix consentiment explícit i consumeix
  una petició del límit local (no representa el saldo real del pla).
- Els títols i el progrés desats continuen disponibles sense Internet.
- Es manté la compatibilitat amb la còpia de seguretat xifrada
  `.lectavbak` introduïda a LECTAV 0.8.7.
- **Abans de desinstal·lar** LECTAV, exporta i comprova el backup.
  Les credencials ChatGPT no es poden transportar; cal iniciar sessió
  de nou després de reinstal·lar.
- La compilació CI comprova que es crea l'APK, però no substitueix
  una prova amb un compte ChatGPT real ni valida l'adequació
  pedagògica del temari.

## Proves a Android

1. A LECTAV 0.8.7/0.8.8 exporta la còpia de configuració i materials.
2. Instal·la la versió 0.8.9; restaura la còpia si has hagut de desinstal·lar.
3. Des de la pantalla principal, tria **3r ESO / Biologia i Geologia**.
   Prem «Genera programació trimestral amb ChatGPT».
4. Comprova que consulta la font oficial si hi ha Internet, sense
   generar encara contingut amb ChatGPT.
5. Tria el 1r trimestre, 18 sessions; connecta ChatGPT, revisa el límit,
   accepta la petició i comprova que retorna exactament 18 temes
   distribuïts per unitats.
6. Desa l'índex, obre el temari i comprova les sessions numerades.
7. Edita una sessió i prem «Genera materials» en aquella sessió;
   comprova que el seu tema apareix automàticament.
8. Genera el 2n trimestre i comprova que el 1r segueix intacte.
9. Exporta una nova còpia de seguretat; comprova que preserva
   trimestres i títols de sessions.
