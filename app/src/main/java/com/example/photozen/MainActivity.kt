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
import com.example.photozen.ui.theme.PicZenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for PicZen app.
 * Entry point annotated with @AndroidEntryPoint for Hilt DI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Observe theme mode from preferences
            val themeMode by preferencesRepository.getThemeMode().collectAsState(initial = ThemeMode.DARK)
            
            PicZenTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    PicZenNavHost(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
