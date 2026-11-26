# CashMate AI — Android

Ricostruzione verificabile (26 novembre 2026) del progetto di finanza personale Android concepito in precedenza. Il repository originale era vuoto: **questi file sono una nuova implementazione basata sui requisiti storici, non il recupero byte-per-byte del vecchio codice**.

## Funzioni presenti nel baseline 0.1
- Kotlin / Jetpack Compose, Room e DataStore; dati salvati **solo sul dispositivo**.
- Conti reali registrati manualmente (nome e saldo di apertura).
- Entrate e uscite in centesimi interi, categorie, note e cronologia, con cancellazione esplicita.
- Dashboard aggregata oppure per conto; analisi per categoria e suddivisione di una spesa.
- «Tutti i conti» è SOLO un filtro di lettura. Per registrare un'operazione occorre scegliere un conto esistente: nessuna transazione può avere un account virtuale/aggregato.
- Tema chiaro/scuro, impostazione persistita con DataStore.
- Nessun utente demo, dati finti, provider cloud fittizio, falsa AI o acquisizione credenziali.

## Non implementato o non configurato
Firebase Auth (solo email/password o Google, senza Facebook), Firestore/backup/restore, RevenueCat, coach Gemini e OCR. Queste integrazioni richiedono credenziali, requisiti privacy e test specifici; sono disattivate e NON vengono presentate come funzionanti. Decision Studio è [un'idea di ricerca](https://github.com/riccardopinato/CashMateAI/issues/1), non parte della 0.1.

## Come compilare
Aprire il progetto Android Studio con JDK 17 e Android SDK 36. Gradle 8.13. In alternativa:
- installare Gradle 8.13
- eseguire gradle :app:testDebugUnitTest :app:assembleDebug

La CI esegue questi stessi controlli. Non è garantita la compatibilità d'aggiornamento con eventuali APK storici: mancano sorgente originale, package ID e firma di distribuzione verificabili.

## Requisiti canonici
- Niente login simulato; eventuale autenticazione futura *esclusivamente* email/password e Google Sign-In.
- La scelta del conto reale è obbligatoria nell'inserimento.
- Monetizzazione, cloud sync e funzioni AI onerose non bloccano la V1 locale.
- Nessun import automatico di dati personali.
