package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.navigation.AppNavGraph
import com.example.data.local.LegacyDatabaseGuard
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.premium.PremiumManager
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import android.util.Log

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initializeFirebase()
    PremiumManager.init(this)
    enableEdgeToEdge()
    val legacyBlock = LegacyDatabaseGuard.blockReason(applicationContext)
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          if (legacyBlock != null) {
            Column(
              modifier = Modifier.fillMaxSize().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Text("Archivio storico protetto", style = MaterialTheme.typography.headlineSmall)
              Text(legacyBlock)
              Text("Il codice di recupero non cancellerà il tuo database. Serve una migrazione verificata dalla vecchia versione.")
              Button(onClick = { finish() }) { Text("Chiudi l'app") }
            }
          } else {
            val viewModel: FinanceViewModel = viewModel()
            AppNavGraph(viewModel = viewModel)
          }
        }
      }
    }
  }

  private fun initializeFirebase() {
    // Cloud is opt-in and only available with genuine Google Services resources.
    val resourceId = resources.getIdentifier("google_app_id", "string", packageName)
    if (resourceId == 0) {
      Log.i("MainActivity", "Cloud not configured. Local-only mode is available.")
      return
    }
    try {
      val app = FirebaseApp.initializeApp(this)
      if (app == null) Log.w("MainActivity", "Firebase configuration incomplete; cloud disabled.")
    } catch (e: Exception) {
      Log.w("MainActivity", "Firebase unavailable; cloud disabled.", e)
    }
  }
}
