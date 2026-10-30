<div align="center">

# Diario delle Emozioni

**Un diario tascabile per riconoscere, registrare e rileggere le tue emozioni.**
Privato, senza account, tutto resta sul tuo telefono.

![Versione beta](https://img.shields.io/badge/stato-BETA-orange?style=flat-square)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
[![Licenza MIT](https://img.shields.io/badge/licenza-MIT-blue?style=flat-square)](LICENSE)

<br>

[![Scarica per Android](https://img.shields.io/badge/Scarica_per_Android-APK-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/source-kobe/diario-emozioni/releases/latest/download/Diario-Emozioni.apk)

<sub>Il tasto scarica subito l'ultima versione. Tutte le versioni e le novità sono nella pagina delle <a href="https://github.com/source-kobe/diario-emozioni/releases/latest">Release</a>.</sub>

</div>

---

> [!WARNING]
> **L'app è in beta.** Può ancora contenere errori e cambiare da una versione all'altra.
> Ogni tanto **esporta i tuoi dati** (Impostazioni › Esporta i dati) per averne una copia.
> Se trovi un problema, segnalalo nella sezione [Issues](https://github.com/source-kobe/diario-emozioni/issues).

## Cos'è

Diario delle Emozioni ti aiuta a fermarti un momento e dare un nome a quello che senti.
Scegli l'emozione, indica quanto è forte e, se vuoi, annota cosa è successo.
Col tempo lo storico ti mostra come cambiano le tue giornate, e puoi rileggerlo
insieme al tuo psicologo o alla tua psicologa.

### Le funzioni

- **10 emozioni**: gioia, calma, tristezza, paura, ansia, rabbia, sorpresa, disgusto, vergogna, colpa. Puoi nascondere quelle che non ti servono.
- **Intensità da 1 a 10**: trascina il dito sulla barra, il colore si fa più intenso insieme al numero.
- **Schema A · B · C**, facoltativo: *Situazione* (cosa è successo), *Pensiero* (cosa ti è passato per la testa), *Conseguenza* (cosa hai fatto o sentito).
- **Storico** con riepilogo, grafico dell'andamento e conteggio per emozione. Ogni voce si può modificare o eliminare.
- **Rilettura con lo psicologo**: le voci scorrono a schermo intero, una alla volta, a partire dal giorno che scegli.
- **Invia come testo**: condividi il diario in forma leggibile, per esempio prima di una seduta.
- **Blocco con PIN** e **sblocco con l'impronta**. Con il blocco attivo il diario non compare nelle app recenti e non si può fotografare lo schermo.
- **Travestimento**: nella schermata Home l'app può comparire come «Sconti», con un'icona anonima.
- **Cambio di telefono**: esporta i dati in un file e importali sul nuovo telefono.
- **Tema chiaro e scuro**, che segue quello del telefono.

## Come si installa

L'app non è sul Play Store: si installa scaricando il file dal tasto qui sopra.
Ci vogliono un paio di minuti.

1. **Dal telefono**, apri questa pagina e tocca il tasto verde **Scarica per Android**.
2. Se il browser avvisa che il file *potrebbe essere dannoso*, tocca **Scarica comunque**.
   L'avviso compare per ogni app che non viene dal Play Store.
3. A download finito, tocca la notifica (oppure apri l'app **File** › **Download**) e scegli **Diario-Emozioni.apk**.
4. La prima volta Android chiede il permesso di installare app da quella fonte:
   tocca **Impostazioni**, attiva **Consenti da questa fonte** e torna indietro.
5. Tocca **Installa**. Se compare *Play Protect* con «App sconosciuta», tocca **Altri dettagli** › **Installa comunque**.
6. Fatto: trovi **Diario Emozioni** tra le app.

Serve Android 8.0 o successivo.

## Aggiornamenti

Dentro l'app: **Impostazioni › Cerca aggiornamenti**. Se c'è una versione nuova,
l'app te lo dice e, se tocchi **Scarica**, il browser scarica il file; poi lo installi come la prima volta (passi 3–5).
L'aggiornamento si installa sopra quello vecchio: **le tue voci, il PIN e le preferenze restano**.

## Cambiare telefono

1. Sul vecchio telefono: **Impostazioni › Esporta i dati**. Il file finisce nella cartella *Download*.
2. Porta il file sul nuovo telefono (cavo, Google Drive, un messaggio a te stesso…).
3. Installa l'app sul nuovo telefono e apri **Impostazioni › Importa i dati**.
4. Quando hai finito, **cancella il file** da entrambi i telefoni: non è protetto dal PIN.

## Privacy

- **Nessun account, nessuna pubblicità, nessuna statistica.** Le voci sono salvate solo nella memoria dell'app sul tuo telefono.
- **Internet serve solo per cercare gli aggiornamenti**, e solo quando tocchi il tasto. In quel momento l'app chiede a GitHub qual è l'ultima versione e nient'altro: il diario non viene mai inviato.
- **Il PIN blocca l'accesso all'app, ma non cifra i dati.** Protegge da chi prende in mano il telefono, non da chi riesce ad accedere alla memoria interna (per esempio su un telefono con permessi di root).
- **Il file esportato è leggibile da chiunque lo apra.** Tienilo al sicuro e cancellalo dopo l'uso.
- **Backup di Android**: se sul telefono è attivo il backup Google, i dati dell'app possono esservi inclusi, come succede per le altre app.
- **Cancella tutti i dati** (in Impostazioni) elimina voci, PIN e preferenze, dopo una doppia conferma.

> [!NOTE]
> Questa app è uno strumento personale di auto-osservazione. **Non è un dispositivo medico** e non sostituisce
> il parere di un o una professionista. Se stai attraversando un momento difficile, parlane con qualcuno di cui
> ti fidi o rivolgiti al tuo medico.

---

## Per chi sviluppa

L'interfaccia è un'unica pagina, [`index.html`](index.html), che funziona anche da sola in un browser
(in quel caso i dati restano nel browser). L'app Android ([`android/`](android)) è un guscio nativo leggero
attorno a quella pagina: aggiunge PIN con impronta, travestimento, salvataggio dei file e aggiornamenti.

### Compilare l'APK

Non serve Gradle: lo script [`build-apk.ps1`](build-apk.ps1) usa direttamente gli strumenti dell'SDK.

Requisiti, su Windows:

- Android SDK con la piattaforma **android-34** e i *build-tools*
  (in `%LOCALAPPDATA%\Android\Sdk`, oppure indicato da `ANDROID_HOME`).
- JDK **Eclipse Temurin** in `C:\Program Files\Eclipse Adoptium`.

```powershell
.\build-apk.ps1
```

Lo script chiede la password della chiave di firma (oppure la legge dalla variabile `DIARIO_KS_PASS`).
Se `chiave-diario.jks` non esiste, ne crea una nuova con quella password.

> [!IMPORTANT]
> La chiave di firma **non è nel repository**, e non deve esserci. Un APK compilato con una chiave diversa
> non può aggiornare quello pubblicato qui: Android chiederà prima di disinstallare l'app, e con lei il diario.

### Pubblicare una versione

1. In `build-apk.ps1` aumenta `$versioneCodice` di uno e cambia `$versioneNome` (per esempio `1.3-beta`).
2. Compila l'APK.
3. Su GitHub crea una **Release** con il tag uguale al nome, preceduto da `v` (per esempio `v1.3-beta`).
   Non segnarla come *pre-release*: l'app e il tasto di download guardano solo l'ultima release normale.
4. Allega l'APK con il nome **`Diario-Emozioni.apk`**: è il nome a cui punta il tasto di download.

## Licenza

Il codice è distribuito con licenza [MIT](LICENSE): puoi usarlo, modificarlo e ridistribuirlo,
anche in altri progetti, purché mantieni l'avviso di copyright.

Caratteri tipografici: [Instrument Sans](https://github.com/Instrument/instrument-sans) e
[Newsreader](https://github.com/productiontype/Newsreader), distribuiti con licenza
[SIL Open Font License 1.1](fonts/OFL.txt).
