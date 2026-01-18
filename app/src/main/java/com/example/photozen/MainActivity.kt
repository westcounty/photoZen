package com.example.photozen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.repository.ThemeMode
import com.example.photozen.navigation.PicZenNavHost
import com.example.photozen.navigation.Screen
import com.example.photozen.ui.theme.PicZenTheme
import com.example.photozen.util.ShareData
import com.example.photozen.util.ShareHandler
import com.example.photozen.util.ShareMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for PicZen app.
 * Entry point annotated with @AndroidEntryPoint for Hilt DI.
 * 
 * Also handles share intents from external apps via activity-alias:
 * - ShareCopyAlias: Copy shared photos to album
 * - ShareCompareAlias: Compare shared photos in light table
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Parse share intent if present
        val shareData: ShareData? = ShareHandler.parseShareIntent(intent)
        
        setContent {
            // Observe theme mode from preferences
            val themeMode by preferencesRepository.getThemeMode().collectAsState(initial = ThemeMode.DARK)
            
            PicZenTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Determine start destination based on share data
                    val startDestination: Screen = if (shareData != null) {
                        val urisJson = shareData.uris.joinToString(",") { it.toString() }
                        when (shareData.mode) {
                            ShareMode.COPY -> Screen.ShareCopy(urisJson)
                            ShareMode.COMPARE -> Screen.ShareCompare(urisJson)
                        }
                    } else {
                        Screen.Home
                    }
                    
                    PicZenNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        onFinish = { finish() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
