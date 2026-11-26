"""Fail-closed contract checks for restoring the historical Android code.

These are static safety invariants; they do not prove cloud security or data migration.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def source(path: str) -> str:
    return (ROOT / path).read_text(encoding='utf-8')

app = source('app/build.gradle.kts')
assert 'com.aistudio.cashmate.aqzwlx' in app
assert 'debug.keystore' not in app, 'Do not require a missing/historical debug keystore'
assert 'com.riccardopinato.cashmateai' not in app, 'Preserve original appId'
database = source('app/src/main/java/com/example/data/local/AppDatabase.kt')
assert 'version = 6' in database
assert 'fallbackToDestructiveMigration(' not in database
assert '"cashmate_database"' in database
firebase = source('app/src/main/java/com/example/data/remote/FirebaseServices.kt')
assert 'offline_user_id' not in firebase
assert 'google_offline_' not in firebase
assert 'isCloudSession(userId: String)' in firebase
assert 'Result.failure(IllegalStateException("Accesso Google non riuscito.' in firebase
main = source('app/src/main/java/com/example/MainActivity.kt')
assert 'cashmate-fallback-project' not in main
assert 'AIzaSyDummyApiKeyForLocalFallbackOnly' not in main
auth_ui = source('app/src/main/java/com/example/ui/screens/AuthScreens.kt')
assert 'loginOfflineWithDetails(' not in auth_ui, 'Google failures must not silently become offline logins'
assert 'val intent = googleAuthService.getSignInIntent()' not in auth_ui
premium = source('app/src/main/java/com/example/premium/PremiumManager.kt')
assert 'fun isPremiumActive(): Boolean = false' in premium
paywall = source('app/src/main/java/com/example/ui/screens/PremiumPaywallScreen.kt')
assert 'Sottoscrizione attivata con successo' not in paywall
assert 'Acquisti ripristinati correttamente' not in paywall
ai = source('app/src/main/java/com/example/ai/GeminiAiService.kt')
assert 'getFallbackResponse' not in ai
assert 'AI Coach non configurato' in ai
for file in ('SyncManager.kt', 'BackupRestoreManager.kt'):
    content=source('app/src/main/java/com/example/data/remote/'+file)
    assert '.getOrThrow()' in content
repo = source('app/src/main/java/com/example/data/repository/FinanceRepository.kt')
assert 'getAccountByIdSync' in repo
assert 'database.withTransaction' in repo
assert 'countTransactionsForAccount' in source('app/src/main/java/com/example/data/repository/AccountRepository.kt')
assert 'saldo = 2500.0' not in source('app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt')
assert 'Room.inMemoryDatabaseBuilder' in source('app/src/test/java/com/example/AccountRecoveryTest.kt')
assert 'invalidAccounts.forEach' not in source('app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt')
assert 'Backup cloud sospeso' in repo
assert 'Ripristino cloud sospeso' in repo
assert 'Sincronizzazione cloud sospesa' in repo
# Avoid storing account addresses in this public repository.
import re
for f in ROOT.rglob('*'):
    if f.is_file() and f.suffix in {'.kt', '.md', '.kts'}:
        text = f.read_text(encoding='utf-8', errors='replace')
        assert re.search(r'[\w.+-]+@(?:gmail|outlook|yahoo)\.(?:com|it)\b', text, re.I) is None, f
assert 'clearAllTransactions()' not in source('app/src/main/java/com/example/data/remote/BackupRestoreManager.kt')
print('PASS: original identity, Room safety, billing/auth/AI guards and cloud writes suspended')
