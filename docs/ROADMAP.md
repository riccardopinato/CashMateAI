# Roadmap ricostruzione CashMateAI (non Product Bible certificata)

## Fatto nel ramo feat/rebuild-local-finance-core-20261126
- Baseline offline Kotlin Compose / Room v1 / DataStore e relativi unit test.
- Conti reali, saldo, entrate/uscite, eliminazione consapevole, analisi mensile, split bill locale, impostazioni.
- CI testDebugUnitTest / assembleDebug (stato determinato dalla PR e dai suoi checks, non da questo testo).

## Da valutare prima di una release
1. Collaudo installazione su Android vero; verificare formati/locale, accessibilità, dark mode, corruzione db e migrazioni.
2. Backup/esportazione/import SAF locale con checksum, ripristino e protezione dati.
3. Identificare appId/chiave firma originali prima di promettere aggiornamento senza perdita dati.
4. Autenticazione reale solo email/password o Google e Firebase/Firestore sincronizzazione con account isolation, sicurezza e device tests; niente account demo.
5. Premium via RevenueCat/Google Play (solo dopo real billing QA); AI cloud solo con consenso e budget/costi verificati.
6. Budget, transazioni ricorrenti, filtri avanzati e Decision Studio solo dopo separata approvazione delle issue.
7. Design review con repository Design e accessibilità.

## Decision Studio
Research only: https://github.com/riccardopinato/CashMateAI/issues/1
Non implementato nella 0.1.
