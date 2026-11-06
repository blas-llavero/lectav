# LECTAV 0.8.8 — programació trimestral sota demanda per assignatura

## Objectiu

Quan el professor selecciona un curs i una assignatura de la biblioteca,
pot prémer **Genera programació trimestral amb ChatGPT**.

Per exemple, **3r d'ESO · Biologia i Geologia** generarà una proposta
específica de Biologia i Geologia, no els temes d'exemple de Matemàtiques
que les primeres versions havien precarregat.

La consulta requereix el **compte ChatGPT propi i autoritzat** i una
**confirmació d'ús per cada petició**. El límit local d'intents segueix actiu.
No es genera automàticament en entrar a la pantalla, per evitar consumir
quota o crèdits sense autorització. No existeix API de facturació alternativa.

## Flux

1. El professor desa o obre un curs (ESO / Batxillerat) i la seva matèria.
2. Des de **Els meus cursos**, prem «Genera programació trimestral amb ChatGPT»,
   o des de **Temari → Genera una programació trimestral amb ChatGPT**.
3. Tria 1r, 2n o 3r trimestre, curs acadèmic i nombre aproximat de sessions
   a programar (6–48, per defecte 30).
4. Si el professor ja havia consultat una font curricular oficial,
   s'inclou un **extracte del PDF**, identificat com a dades de context.
   Si no n'hi ha cap, LECTAV ho indica clarament i genera una proposta
   orientativa sense declarar-la normativa verificada.
5. Escull un model ChatGPT compatible i confirma explícitament la petició.
6. La IA retorna JSON amb **2–8 unitats** i una llista de **títols reals,
   diferents, de cada sessió**; el codi comprova el format, els camps i la
   mida abans de mostrar l'índex.
7. El professor **revisa i confirma** la desada. Només se substitueix
   el trimestre seleccionat; els altres dos es conserven. Si el trimestre
   ja contenia sessions marcades, l'avís de substitució indica que se'n
   reiniciarà el progrés.
8. La programació queda **a la memòria local** amb DataStore; es pot editar
   el títol de cada unitat i cada sessió sense consultar ChatGPT.
9. En prémer «Genera materials» dins una sessió, LECTAV incorpora el tema
   d'aquella sessió automàticament al generador de teoria, exercicis o
   solucionari.
10. El progrés de les sessions es continua registrant per als alumnes en
    la modalitat local actual.

## Estructura del nou registre

```json
{
  "trimester": 1,
  "units": [
    {
      "title": "Nom editable de la unitat",
      "sessions": [
        "Tema concret de la sessió 1",
        "Tema concret de la sessió 2"
      ]
    }
  ]
}
```

A `CoursePlanning.kt`, cada `TopicPlan` ara desa
`trimester` i `sessionTitles`, a més dels camps anteriors.
La lectura de les programacions de versions antigues conserva la compatibilitat
i mostra un nom provisional als seus títols de sessió encara no definits.

**Important:** els índexs generats són propostes didàctiques editables; les
lleis curriculars defineixen sabers, competències i criteris i no sempre
un ordre cronològic universal d'unitats. El nombre de sessions depèn dels
horaris de cada centre. El professor ha de verificar la normativa aplicable,
especialment els canvis de curs acadèmic i de territori.

## Font oficial de Biologia i Geologia de Catalunya

LECTAV inclou **només l'enllaç**, no el PDF ni els continguts. L'usuari pot
consultar-lo i extreure'n un fragment local des de la pantalla Currículum:

https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Biologia-i-Geologia.pdf

Font: Departament d'Educació / XTEC, ESO, Decret 175/2022.
Cal comprovar les modificacions vigents i les orientacions per curs.
L'anàlisi de ChatGPT no substitueix una revisió de les fonts oficials.

## Còpia de seguretat i compatibilitat

Les noves dades (`trimester` i `sessionTitles`) es desen a DataStore
i es conserven dins dels arxius xifrats `.lectavbak` de LECTAV 0.8.7+,
juntament amb la resta de programacions, materials i preferències.
**No es desen tokens OAuth**, i després de reinstal·lar cal tornar a
autoritzar ChatGPT.

Els índexs de temes antics queden disponibles. Si els substitueixes,
l'aplicació t'avisa de la pèrdua dels progressos del trimestre afectat.

## Limitacions i proves a Android

- Requereix connexió autoritzada a ChatGPT per generar la proposta; veure
  o editar unitats ja desades és local i no consumeix quota.
- La IA pot retornar JSON invàlid o ometre sessions: LECTAV mostra un
  error i no desa contingut incomplet. Una nova generació consumirà
  un nou intent; cal comprovar el comptador i els crèdits a ChatGPT.
- Els títols d'unitat i de sessió són revisables; el programa **no valida
  automàticament tots els decrets ni la cobertura de les competències**.
- El prototip comparteix dades d'alguns cursos entre rols en un mateix
  dispositiu, i encara no equival a un sistema de comptes escolars;
  no s'hi han d'incloure dades personals d'alumnes.
- Provar Matemàtiques 3r ESO i Biologia i Geologia 3r ESO sense continguts
  precarregats; comprovar que els títols segueixen la matèria seleccionada.
- Generar 1r i 2n trimestres, comprovar que el segon **no esborra** el primer.
- Editar el tema d'una sessió i obrir **Genera materials**; verificar
  que s'emplena amb el tema exacte, no amb un títol genèric.
- Exportar una còpia de seguretat, restaurar-la i comprovar que es
  mantenen els tres trimestres i les sessions.
- La compilació CI no verifica l'autenticació, els models disponibles ni
  la qualitat curricular del contingut produït amb un compte real.
