# Provenienza della ricostruzione — 2026-11-26

L'archivio GitHub CashMateAI era vuoto. Le conversazioni storiche descrivono Kotlin/Jetpack Compose, Room, DataStore, Firebase/Firestore, login email/password+Google, analisi, split bill e AI coach; non era disponibile un archivio sorgente verificabile da importare. Pertanto la presente baseline è un'implementazione nuova e **non una copia del precedente codice**.

Preservato come vincolo forte: «Tutti i conti» = vista aggregata; ogni entrata/uscita richiede un account reale esplicito, non un conto placeholder. Niente Facebook Auth, fake login o demo account. V1 offline-first senza AI costosa.

Ambito realizzato: persistenza finanziaria locale, schermate fondamentali, logica di denaro centesimale, split, statistiche e test.

Esclusioni esplicite: APK precedente/keystore sconosciuti, Firebase Auth/Firestore, OAuth, backup cloud/restore, RevenueCat, AI generativa, OCR, completa parità del prodotto storico. Questi non vanno chiamati «completati» o «certificati». Nessun dato utente è stato importato.
