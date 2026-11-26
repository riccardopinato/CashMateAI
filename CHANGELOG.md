# CashMate AI
## CHANGELOG.md

Questo file tiene traccia di tutte le modifiche e degli sviluppi rilasciati per ciascuno sprint del progetto CashMate AI.

---

## Sprint 16 (Release Candidate - Audit & Polish)
- **Architecture & Performance**: Eliminazione totale di classi o funzioni non utilizzate, ottimizzazione delle query Room e dei canali Flow, riduzione delle ricomposizioni non necessarie in Compose.
- **Robust Validation**: Controllo sistematico per prevenire la selezione di "Tutti i conti" o altri conti virtuali in fase di inserimento transazione.
- **Accessibility & Play Store Readiness**: Verifica touch targets, contrasto elevato, TalkBack support, e configurazione corretta di regole Proguard e manifest per la Release Candidate.
- **Pronto per Release**: Versione 1.0 dichiarata come "Release Candidate" stabile e priva di bug critici.

---

## Sprint 15 (Fix Critico Account / Metodo Pagamento)
- **Account Validation**: Impedito l'utilizzo dell'account virtuale "Tutti i conti" o "Tutto" in fase di aggiunta o modifica di una transazione, di regole automatiche, o transazioni ricorrenti.
- **Orphan Migration**: Integrazione automatica in `AccountViewModel` per migrare qualsiasi transazione orfana (senza accountId o con accountId non valido/nullo) verso il "Conto Corrente" predefinito.
- **Selector Polish**: Visualizzazione di un messaggio di errore chiaro ("Seleziona un metodo di pagamento per continuare.") qualora l'utente provi a salvare senza un account reale selezionato.

---

## Sprint 13
- **Smart Categories**: Categorizzazione intelligente basata sull'intelligenza artificiale e su regole locali.
- **Automatic Rules**: Regole automatiche per l'assegnazione di categorie e tag.
- **Quick Add**: Aggiunta rapida delle transazioni con un flusso semplificato e pochi tap.
- **Merchant Learning**: Apprendimento automatico dei dettagli del venditore per velocizzare l'inserimento futuro.

---

## Sprint 14
- **Savings Goals**: Gestione completa di obiettivi di risparmio personalizzati in stile Revolut Vaults (creazione, modifica, archiviazione, eliminazione, versamenti e collegamento con transazioni reali).
- **Achievements & Badges**: Sistema di gamification a trofei con sblocco automatico basato sulle abitudini dell'utente e livelli di Badge dinamici (Bronze, Silver, Gold, Platinum, Diamond).
- **Weekly Report**: Report settimanale generato interamente in locale che analizza entrate, uscite, andamenti e obiettivi raggiunti rispetto alla settimana precedente.
- **Financial Timeline**: Una cronologia degli eventi finanziari e personali ordinata e visualizzata verticalmente.
- **Daily Insights & Dashboard**: Integrazione immediata dei traguardi attivi e dei consigli finanziari locali sulla Home dell'applicazione senza stravolgere il layout esistente.

---

## Sprint 12
- **Cloud Sync**: Sincronizzazione in tempo reale con Firebase Firestore in modalità offline-first.
- **Google Sign-In**: Integrazione per l'accesso sicuro con account Google, incluso un solido fallback automatico in modalità offline locale per non bloccare l'esperienza utente in caso di errore.
- **User Profile**: Gestione e persistenza del profilo utente reale con identificativo univoco.
- **Backup & Restore**: Salvataggio sicuro sul cloud e ripristino istantaneo dello stato finanziario locale.
