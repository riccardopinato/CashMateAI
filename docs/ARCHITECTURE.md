# Architettura v0.1 — ricostruzione

- UI: Activity Compose e FinanceViewModel; nessun stato finanziario globale non osservato.
- Domain: FinanceMath con importi Long in centesimi, no Double per scrittura; parsing, saldo, ripartizione.
- Data: Room v1 con foreign key RESTRICT account→transactions. Nessun conto aggregato persistito. Room senza destructive migration.
- UserSettings: DataStore locale; tema.
- Provider esterni: volutamente assenti. Una futura integrazione Firebase richiederà separazione tra stato locale e account reale, regole di accesso per UID, conflitti, cifratura/backup e test ripristino; autenticazione ammessa solo email/password e Google.
- Futura AI: provider disabilitato. Nessuna elaborazione cloud di dati finanziari attiva.
- Basi di sicurezza: nessun INTERNET permission, no demo user, no credenziali repository, allowBackup=false, import transazioni solo da conto esistente.
- Non è garantita la compatibilità con eventuali APK storici (package/signer/database precedenti non recuperati).
