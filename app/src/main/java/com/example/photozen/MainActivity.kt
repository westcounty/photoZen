package com.example.photozen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.repository.ThemeMode
import com.example.photozen.navigation.MainDestination
import com.example.photozen.navigation.PicZenNavHost
import com.example.photozen.navigation.Screen
import com.example.photozen.ui.MainScaffold
import com.example.photozen.ui.state.SnackbarManager
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
    
    // Phase 3-8: 全局 Snackbar 管理器
    @Inject
    lateinit var snackbarManager: SnackbarManager
    
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
                    
                    // 判断是否是 Share Intent
                    if (shareData != null) {
                        // Share 场景：不使用底部导航，直接进入 Share 页面
                        val urisJson = shareData.uris.joinToString(",") { it.toString() }
                        val startDestination: Screen = when (shareData.mode) {
                            ShareMode.COPY -> Screen.ShareCopy(urisJson)
                            ShareMode.COMPARE -> Screen.ShareCompare(urisJson)
                        }
                        PicZenNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            onFinish = { finish() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 正常启动：使用 MainScaffold（带底部导航）
                        // Phase 3-8: 传递全局 SnackbarManager
                        // 使用 Screen.Home 作为起始目的地（类型安全导航）
                        // MainScaffold 会通过监听路由变化来同步底部导航高亮状态
                        MainScaffold(
                            navController = navController,
                            snackbarManager = snackbarManager
                        ) { _ ->
                            // 不使用底部 padding，让内容填满整个区域
                            // 底部导航栏会覆盖在内容上方
                            PicZenNavHost(
                                navController = navController,
                                startDestination = Screen.Home,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
