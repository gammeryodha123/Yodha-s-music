package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Programmatically initialize Firebase with the correct active project options
        try {
            val options = com.google.firebase.FirebaseOptions.Builder()
                .setApiKey("AIzaSyAcwyg4E4bMwqiGodKhzZXluwkiC7WNCDQ")
                .setApplicationId("1:684033752161:web:42fdec3268ee81e3c87e20")
                .setProjectId("circular-hangar-csmzh")
                .setStorageBucket("circular-hangar-csmzh.firebasestorage.app")
                .build()
            
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            } else {
                try {
                    com.google.firebase.FirebaseApp.getInstance().delete()
                } catch (e: Exception) {
                    // Ignore if delete fails
                }
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
            android.util.Log.d("MainActivity", "FirebaseApp successfully initialized programmatically.")
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to initialize FirebaseApp programmatically: ${e.message}")
        }

        // Initialize AppDatabaseHelper for Room Database local access
        com.example.database.AppDatabaseHelper.init(applicationContext)

        // Initialize AdMob Mobile Ads SDK with try-catch safety and emulator guard
        if (!isEmulator()) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(this)
                // Preload Interstitial Ad
                com.example.ui.components.AdMobInterstitialHelper.loadAd(this)
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "Failed to initialize AdMob SDK: ${e.message}")
            }
        } else {
            android.util.Log.d("MainActivity", "Running on container emulator - skipping AdMob MobileAds initialization.")
        }

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu"))
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("main") {
            MainScreen()
        }
    }
}
