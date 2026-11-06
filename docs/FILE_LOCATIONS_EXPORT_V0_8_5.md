# LECTAV 0.8.5 — ubicació dels fitxers i exportació a carpeta escollida

## Per què CX File Explorer no trobava els PDF?

LECTAV 0.8.4 genera PDF a la **memòria interna privada** de l'aplicació:

```
/data/user/0/com.blasllavero.lectav/files/generated/
```

La ruta efectiva és `context.filesDir/generated/` i es mostra directament
a l'aplicació perquè pot variar segons el dispositiu. Aquesta carpeta
pertany a l'emmagatzematge privat d'Android; els gestors de fitxers ordinaris
(com CX File Explorer) **no poden explorar-la** en dispositius sense accés
privilegiat/root. Tampoc no és una carpeta de la targeta SD. Si s'esborren
les dades de LECTAV o se'n desinstal·la l'APK, es poden perdre els originals
privats.

Els PDF i MP4 importats amb el selector de documents d'Android **no es
copien a la carpeta privada**: continuen a la seva ubicació d'origen,
que pot ser l'emmagatzematge compartit o una targeta SD.

## Nova funcionalitat

### Materials → Desa una còpia a una carpeta…

- A cada material PDF (generat o importat) i vídeo MP4 (importat), el botó
  d'exportació obre el **selector oficial de documents d'Android**
  (`ActivityResultContracts.CreateDocument`, Storage Access Framework).
- L'usuari selecciona on desar la còpia, per exemple
  `Memòria interna → Documents → LECTAV` o
  `Targeta SD → Curs 2026-2027 → Matemàtiques`, sempre que la carpeta
  aparegui entre els proveïdors accessibles al dispositiu.
- Android crea el fitxer nou al destí escollit; LECTAV n'escriu els bytes
  en streaming amb `ContentResolver.openInputStream/openOutputStream`.
  No cal cap permís per llegir o modificar tota la memòria.
- **No es mou ni esborra l'original**; la còpia es pot trobar a CX File
  Explorer navegant a la mateixa carpeta escollida.
- La biblioteca registra l'URI de l'última còpia exportada, sense substituir
  la ubicació original. Quan la còpia és en un volum extern d'Android
  (`com.android.externalstorage.documents`), es mostra el volum i ruta
  relativa, per exemple `Memòria interna compartida / Documents/LECTAV.pdf`
  o `Targeta SD (XXXX-XXXX) / LECTAV.pdf`. Per a altres proveïdors no es
  menteix inventant un path del sistema de fitxers: Android només proporciona
  una URI `content://`.
- La mateixa opció apareix a la pantalla del visor PDF/MP4.
- Just després de **Desa aquest PDF a Els meus cursos**, la pantalla del
  generador també ofereix **Desa una còpia a una carpeta…**, sense tornar a
  gastar intents de ChatGPT ni regenerar el PDF.

### Transparència de la ubicació

La biblioteca explica si un document és:

1. **Generat**: original a la carpeta interna privada de LECTAV;
   ruta real de `context.filesDir/generated/`, no accessible a CX.
2. **Importat**: material en la ubicació original escollida per Android.
   Quan el proveïdor ho permet, indica volum i ruta relativa.
3. **Exportat**: última còpia desada en la carpeta que va escollir
   l'usuari. Si el destí va ser la targeta SD, la còpia continua a la SD.

## Política de privacitat i conservació

- Cap document s'envia a serveis de conversió ni a GitHub per exportar-lo.
- No es demana `MANAGE_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`
  ni cap permís d'accés global. Android controla les destinacions.
- Desar una còpia fora de l'emmagatzematge privat facilita conservar-la
  fins i tot quan s'esborren les dades de LECTAV o es desinstal·la.
- Algunes carpetes com `Android/data` i l'arrel de volums poden estar
  restringides per les versions modernes d'Android.
- La còpia pot fallar si la SD no està inserida, el proveïdor no permet
  escriure o no hi ha espai lliure. L'app mostra l'error i no esborra
  mai el material original.

## Comprovacions manuals

1. Instal·lar 0.8.5 sobre la 0.8.4 **sense desinstal·lar** i comprovar
   que els materials anteriors són visibles.
2. Obrir `Els meus cursos → Materials`. Verificar que el PDF generat
   mostra l'avís de la carpeta privada i la ruta.
3. Prémer **Desa una còpia a una carpeta…** en un PDF generat. Seleccionar
   una carpeta com `Documents/LECTAV`, confirmar, i trobar-lo amb CX File
   Explorer. Obre'l i verifica pàgines i fórmules.
4. Fer el mateix amb una carpeta existent d'una **targeta SD inserida**.
5. Tornar a la biblioteca i verificar que es veu la darrera destinació.
6. Importar un MP4 i exportar-ne una còpia sense alterar l'original.
7. Generar un PDF nou: després de desar-lo, provar el botó d'exportació
   sense fer una nova consulta a ChatGPT.
8. Simular la cancel·lació del selector: no s'ha de crear ni modificar cap
   fitxer. Comprovar també una SD desconnectada i un destí sense espai.
9. Si s'esborra la referència d'un material de LECTAV, confirmar que la
   **còpia exportada** continua a la carpeta de CX File Explorer.

La compilació automàtica de GitHub valida el codi i l'APK, però no
garanteix el funcionament de cada proveïdor de documents o targeta SD;
cal fer una prova a Android.

Documentació d'Android:
https://developer.android.com/training/data-storage/shared/documents-files
https://developer.android.com/training/data-storage/app-specific
