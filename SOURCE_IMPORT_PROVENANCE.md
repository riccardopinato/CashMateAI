# CashMate AI — original-source recovery snapshot (2026-11-26)

Source: user-provided `cashmate-ai.zip` (SHA-256 `bd3241f7a9c4f67ad7747cf2436aa74350f6efa1ecf067f77397ad7775144aff`).

The imported files are from the historic Android Kotlin/Compose project, **not** from the later stripped-down reconstructed 0.1 branch. Two occurrences of a personal email address have been redacted for public GitHub. Ten launcher WebP resources were invalid in the ZIP because image bytes had been re-encoded as UTF-8; they were losslessly restored by UTF-8 → Latin-1 reversal and verified with Pillow. One `app/src/test/screenshots/greeting.png` asset was malformed and is omitted. No genuine secrets or keystores were found.

## Critical review gates
- Historic package/applicationId: `com.aistudio.cashmate.aqzwlx`; reconstructed 0.1 used `com.riccardopinato.cashmateai`. Neither APK identity nor signing-key equivalence is confirmed; do not claim in-place update or migrate user data until tested.
- Room schema version **6** uses `.fallbackToDestructiveMigration()` in the upload; changing schema or switching to the reconstruction's Room v1 can cause permanent local data loss. Requires a deliberate migration strategy.
- Auth contains offline/demo placeholder profiles and Firebase dummy initialization when secrets are missing. This conflicts with the documented real-profile requirement and requires removal/real offline identity strategy before release.
- Gradle release signing references absent keystore, debug signing references missing `debug.keystore`; do not commit signing keys.
- Firestore/cloud/auth/AI/Premium classes exist but **presence of source code != configured/validated runtime**. No `google-services.json` or real Gemini key included.
- AGP 9.1.1 + KSP 2.3.5 + Kotlin 2.2.10 + compileSdk 36.1 must be reconciled in CI.
- Product README/ROADMAP/CHANGELOG statements reflect authoring history, not fresh compilation/device/release certification.

This source snapshot is for auditable recovery. **Do not merge into main until CI and privacy/security/migration gates pass.**

## Gate P0 (26/11/2026, repair PR)
- Debug build uses Android's standard generated debug signing. This is **not** a release signature, therefore it cannot prove upgrades from historical APKs.
- Room v6 no longer has a destructive fallback. Previous unknown schema versions may fail to open until real, tested migration fixtures are provided; no data are silently erased by Room.
- A persistent per-installation local identity is explicitly selected for offline use. A failed Google sign-in **never** creates a false Google/offline user. Cloud backup and sync are guarded by Firebase UID session checks.
- Cloud uploads validate `Result` before tagging records SYNCED or reporting backup success.
- AI Coach fails explicitly when API key/provider response is missing and cannot return simulated financial advice.
- Premium remains FREE pending a verified billing provider, regardless of earlier in-app demo flags; old settings are preserved, not treated as purchases.
- Product release still blocked on migration tests from real old APK and verified Firebase/Play billing configurations.

- **Ulteriore blocco prudenziale:** backup, ripristino e sync cloud sono temporaneamente disabilitati. Il ripristino precedente cancellava tabelle locali prima di acquisire tutte le collezioni remote. Conservati i sorgenti storici nel commit iniziale della PR, ma nessuna funzione cloud va presentata come attiva.

- Eliminato l'onboarding con conti/saldi fittizi e la riassegnazione automatica dei movimenti orfani. Nuove entrate/uscite, eliminazioni e trasferimenti gestiscono saldo e scrittura in una singola transazione Room. Conti con movimenti non cancellabili automaticamente.

- Rimossa la creazione automatica di budget di esempio. Le opzioni UI senza backend reale (tema, notifiche, biometria, cloud) sono contrassegnate come non configurate e disattivate.
