# LECTAV 0.9.0 — programació trimestral per unitats i sessions amb ChatGPT

## Funció nova

Quan un professor escull **curs + assignatura** a la pantalla inicial, pot
prémer **«Genera programació trimestral amb ChatGPT»**. LECTAV conserva el
curs seleccionat, presenta els trimestres 1, 2 i 3, i permet indicar el
**nombre previst de sessions lectives** del trimestre (de 8 a 40).

Des d'**Els meus cursos → Temari → Genera un trimestre amb ChatGPT**,
també es pot generar o actualitzar un trimestre.

En una sola petició a un model autoritzat per l'usuari es genera un
**esborrany estructurat** d'entre dues i vuit unitats, cadascuna amb un
títol i una llista de sessions amb **temes específics diferents**.
La suma exacta de les sessions ha de coincidir amb el nombre indicat
pel professor. Les propostes no es guarden automàticament: el professor
pot editar els títols i ha de confirmar explícitament el desament.

La pantalla **Temari** mostra la seqüència com un índex jeràrquic:

- 1r trimestre
  - Unitat 1. Nom pedagògic
    - Sessió 1 · Tema concret
    - Sessió 2 · Tema concret
  - Unitat 2. Nom pedagògic
    - Sessió 3 · Tema concret
- 2n trimestre (pendent o generat)
- 3r trimestre (pendent o generat)

Cada sessió té un botó per anar al generador de PDF, amb **el tema
preemplenat automàticament**. El professor pot editar després els
títols individuals de les sessions. L'alumne pot consultar l'índex
i marcar la seva sessió com a completada en aquest prototip local
(sense usuaris realment separats).

La substitució d'un trimestre ja existent es confirma explícitament;
es conserven intactes els altres dos trimestres. El progrés del
trimestre substituït es reinicia després de confirmar-ho.

## No hi ha temari precuinat al codi

S'ha suprimit l'antiga llista fixa de sis unitats de Matemàtiques
de 3r d'ESO. Les propostes es creen amb el model ChatGPT del
compte autoritzat i es guarden localment perquè no calgui generar
cada vegada el mateix índex. Els plans de versions anteriors segueixen
sent compatibles amb la nova base de dades.

## Font curricular

Per a cursos d'ESO, el programa pot descarregar sobre la marxa:
- Matemàtiques:
  https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Matematiques.pdf
- Biologia i Geologia:
  https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Biologia-i-Geologia.pdf

Aquests enllaços no són una còpia del contingut empaquetada dins
de l'APK. La lectura és local (PDFBox-Android), el text seleccionat
queda associat a l'assignatura, i els models de ChatGPT reben
**únicament el fragment necessari** quan l'usuari decideix generar.

Per a altres assignatures, es pot consultar el menú
«Consulta o canvia la font oficial» i introduir un PDF d'un domini
administratiu admès. Sense font consultada, la generació encara
pot funcionar, però la pantalla la identifica com a **proposta
orientativa no verificada curricularment**.

**Nota jurídica:** la normativa defineix competències, sabers i
criteris, no una primera unitat obligatòria ni una successió
imposada de sessions. L'usuari ha de revisar el currículum
oficial vigent per al seu nivell i territori (i les esmenes
posteriors). LECTAV no certifica la vigència normativa.

## Control de quota i privacitat

- No s'envien peticions en seleccionar un desplegable: primer es
  presenta la pantalla, després cal prémer **Genera** i acceptar
  la confirmació de la quota del compte ChatGPT.
- Cada generació consumeix **un intent del límit local LECTAV**;
  els límits econòmics reals es gestionen a ChatGPT Settings → Usage.
- No hi ha cap clau API ni canvi automàtic a la facturació API.
- En cas de resposta JSON incompleta o nombre erroni de sessions,
  l'app informa de l'error i no desa un índex parcial.
- La petició només conté nivell, assignatura, any, nombre de sessions,
  títols de trimestres previs i, si n'hi ha, extracte de la font
  curricular. No cal enviar noms ni notes d'alumnes.
- El sistema és un prototip de dispositiu únic: no substitueix
  un sistema de gestió de centre ni separa comptes d'alumnes
  en una tauleta compartida.

## Limitacions

- El resultat generat és un **índex orientatiu de planificació**,
  no una programació didàctica oficial completa amb rúbriques,
  competències, criteris, temporalització exacta, mesures d'inclusió
  i avaluació. Aquests elements requereixen una fase posterior.
- El temps lectiu depèn de l'horari real del centre; el professor
  escull 8–40 sessions per trimestre.
- Les pàgines de documentació i dades existents amb la temàtica antiga
  segueixen disponibles, però les noves propostes són sota demanda.
- La llicència FOSS del client no implica que el servei remot ChatGPT
  sigui FOSS o local: és una opció optativa i connectada.

## Prova recomanada

1. **Primer exporta un backup amb LECTAV 0.8.7**, perquè una
   desinstal·lació pot esborrar cursos, documents i credencials.
2. Instal·la LECTAV 0.9.0. Si Android no permet instal·lar-la sobre
   l'anterior, desinstal·la només quan hagis verificat que tens
   el backup .lectavbak i en coneixes la contrasenya.
3. Restaura el backup, comprova els desplegables i reconnecta
   ChatGPT (els tokens OAuth no són transportables).
4. Tria **3r ESO → Biologia i Geologia** i prem
   «Genera programació trimestral amb ChatGPT».
5. Selecciona 1r trimestre, 24 sessions. Revisa la font oficial
   (si s'ha pogut descarregar).
6. Revisa els límits de ChatGPT i autoritza una generació.
7. Comprova que apareixen unitats i 24 títols de sessió, modifica'n
   un i desa'ls. Obre Temari i comprova les sessions numerades.
8. Prem «Genera materials de la sessió»: comprova que el tema
   s'omple automàticament.
9. Genera el 2n trimestre i comprova que el 1r es conserva.
10. Exporta un nou backup i restaura'l en una instal·lació de prova.

Una compilació exitosa de GitHub Actions no garanteix la qualitat
curricular del pla ni substitueix la prova real a Android.
