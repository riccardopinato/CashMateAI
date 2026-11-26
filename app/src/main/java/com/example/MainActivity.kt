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
import com.example.premium.PremiumManager
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import android.util.Log

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initializeFirebase()
    PremiumManager.init(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val viewModel: FinanceViewModel = viewModel()
          AppNavGraph(viewModel = viewModel)
        }
      }
    }
  }

  private fun initializeFirebase() {
    val googleAppIdId = resources.getIdentifier("google_app_id", "string", packageName)
    if (googleAppIdId == 0) {
      Log.w("MainActivity", "google-services.json is missing. Initializing Firebase with local fallback options.")
      setupFallbackFirebase()
    } else {
      try {
        val defaultApp = FirebaseApp.initializeApp(this)
        if (defaultApp == null) {
          setupFallbackFirebase()
        } else {
          Log.i("MainActivity", "Firebase initialized successfully with default resource options.")
        }
      } catch (e: Exception) {
        Log.w("MainActivity", "Exception during default Firebase initialization. Trying fallback setup.", e)
        setupFallbackFirebase()
      }
    }
  }

  private fun setupFallbackFirebase() {
    try {
      val options = FirebaseOptions.Builder()
        .setApplicationId("1:1234567890:android:abcdef123456")
        .setApiKey("AIzaSyDummyApiKeyForLocalFallbackOnly")
        .setProjectId("cashmate-fallback-project")
        .build()
      FirebaseApp.initializeApp(this, options)
      Log.i("MainActivity", "Firebase successfully initialized with fallback local options.")
    } catch (e: Exception) {
      Log.e("MainActivity", "Critical: Failed to initialize fallback Firebase.", e)
    }
  }
}
