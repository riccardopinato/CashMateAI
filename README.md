# CashMate AI — recupero sorgente Android originale

Questa directory contiene il progetto **Kotlin / Jetpack Compose** recuperato dallo ZIP storico consegnato dall'utente, con package Android originale `com.aistudio.cashmate.aqzwlx`. **Non è** la riscrittura semplificata `com.riccardopinato.cashmateai` prodotta durante il recupero del repository vuoto.

## Stato verificabile (26 novembre 2026)

- Il codice sorgente, la UI storica, i modelli Room v6, DataStore, le funzioni di risparmio, regole, ricorrenze, analisi e divisione spese sono conservati.
- **Sorgenti originali recuperati in `main`** tramite [PR #3](https://github.com/riccardopinato/CashMateAI/pull/3), verificati da [CI #45861645951](https://github.com/riccardopinato/CashMateAI/actions/runs/45861645951) con compilazione Android e test unitari PASS. Il merge riguarda il **codice sorgente**, non certifica un aggiornamento in-place, una release Play o un collaudo su dispositivo; per quelli mancano la firma storica e fixture di migrazione pre-v6.
- Le operazioni locali sono state rafforzate: nessuna creazione di conti/saldi di esempio, nessuna riassegnazione automatica dei movimenti, gestione atomica delle scritture e dei trasferimenti, blocco di nuove transazioni senza conto reale.
- Room v6 **non cancella automaticamente** i dati durante una migrazione sconosciuta. Se la versione del vecchio DB non è supportata, l'apertura può fallire senza sovrascrivere il database: servirà una migrazione testata.
- Login locale esplicito, senza utente dimostrativo o finto account Google. Google/Firebase richiedono credenziali reali per essere utilizzati.
- **Backup, ripristino e sincronizzazione cloud sono disabilitati** durante il recupero: l'algoritmo originale di ripristino cancellava le tabelle locali prima di acquisire tutte le collezioni remote.
- **AI Coach non configurato** se manca un provider reale; nessuna analisi finanziaria sintetica viene restituita come risultato autentico. **Premium non attivo** senza un provider di acquisti verificato.
- Le impostazioni prive di backend reale (tema, notifiche, biometria, cloud) sono visualizzate come non configurate.

I sorgenti contengono le implementazioni storiche di vari moduli, ma **codice presente non significa funzionalità collaudata o rilasciata**.

## Build e verifiche

Aprire in Android Studio con JDK 17 e Android SDK 36.1. La CI di recupero usa Gradle 9.3.1 e AGP 9.1.1; usa gli strumenti SDK previsti dalla CI, non il vecchio `debug.keystore` assente.

```bash
python3 tool/verify_recovery_safety.py
gradle :app:assembleDebug :app:testDebugUnitTest --stacktrace --no-daemon
```

La firma di debug viene generata dagli strumenti Android e **non coincide necessariamente** con una firma di un APK storico. Non installare questo APK come aggiornamento di una vecchia installazione contenente dati importanti senza aver verificato package, firma, backup, schema e migrazione.

## Documentazione

- [`SOURCE_IMPORT_PROVENANCE.md`](SOURCE_IMPORT_PROVENANCE.md): provenienza dello ZIP, riparazione degli asset, limiti e gate di sicurezza.
- [`DECISIONS.md`](DECISIONS.md), [`ROADMAP.md`](ROADMAP.md), [`BACKLOG.md`](BACKLOG.md): documenti del progetto originale; alcune conclusioni restano **storiche** finché non riconciliate con la CI e il codice aggiornati.
- [Issue recovery #4](https://github.com/riccardopinato/CashMateAI/issues/4): controlli e blocchi ancora da risolvere.
- [Decision Studio #1](https://github.com/riccardopinato/CashMateAI/issues/1): proposta futura, **non implementata**.

I dati dell'utente non sono stati importati. Mancano `google-services.json`, credenziali reali, chiave di firma storica e database/fixture precedenti necessari a dimostrare un aggiornamento senza perdita di dati.

## Controlli di integrità aggiuntivi (26/11/2026)

- Importi nuovi convertiti e verificati in **centesimi interi**, mantenendo per compatibilità il formato SQLite v6 originale (`REAL`). Importi non finiti o con più di due decimali vengono rifiutati.
- Nuovi movimenti ammessi solo con ID nuovo, conto esistente e importo valido; nessuna sostituzione silenziosa tramite `INSERT OR REPLACE`.
- Eliminazione di un singolo lato di un trasferimento rifiutata; saldi e movimenti sono aggiornati in modo atomico.
- Accesso cloud email/Google, sincronizzazione, backup/ripristino cloud e Premium sospesi senza false conferme.
- Il backup automatico Android è disattivato; è ora disponibile l'esportazione locale cifrata con ripristino solo su un archivio vuoto. Il collaudo su dispositivo resta da completare: non utilizzare l'app come unico archivio finanziario.
- Le migrazioni da database con `user_version < 6` non sono state validate perché mancano schemi storici e APK firmato originale. L'app non deve cancellare automaticamente le tabelle per aggiornarsi.

## Esportazione e ripristino locale cifrati

La schermata Impostazioni permette un **backup locale crittografato** del database Room v6 tramite selettore documenti Android. Chi utilizza la funzione sceglie una passphrase di almeno 12 caratteri, che non viene memorizzata. Formato `CMVAULT1`: PBKDF2-HMAC-SHA1 con 310.000 iterazioni, sale casuale e AES-256-GCM (nonce casuale, autenticazione crittografica). **Non è compatibile** con backup di APK storici o altri database.

L'importazione verifica formato, autenticazione, versione Room v6, tabella e colonne, e ripristina in una transazione Room. **Non sovrascrive database già utilizzati**: per prevenire perdite, un ripristino si può effettuare soltanto in una nuova installazione con le tabelle utente vuote. Dopo l'importazione l'app va riavviata per aggiornare le schermate. Il backup automatico Android e il cloud restano disabilitati.

La passphrase non è recuperabile e un backup esportato non equivale a una migrazione v1-v5: servono schemi storici e keystore originale per aggiornamenti in-place certificati.

## Blocco preventivo per vecchi database

Prima di creare la ViewModel, l'attività controlla in lettura l'header SQLite dell'eventuale database `cashmate_database`. Se il numero di versione non è 6, visualizza un blocco esplicito senza aprire Room, migrare, sovrascrivere o eliminare dati. Il gate non risolve le migrazioni pregresse e la firma dell'APK originale rimane indispensabile per gli aggiornamenti in-place.
