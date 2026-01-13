package com.example.photozen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.photozen.ui.screens.editor.PhotoEditorScreen
import com.example.photozen.ui.screens.flowsorter.FlowSorterScreen
import com.example.photozen.ui.screens.home.HomeScreen
import com.example.photozen.ui.screens.lighttable.LightTableScreen
import com.example.photozen.ui.screens.photolist.PhotoListScreen
import com.example.photozen.ui.screens.settings.SettingsScreen
import com.example.photozen.ui.screens.trash.TrashScreen

/**
 * Main navigation host for PicZen app.
 * Manages navigation between all screens.
 */
@Composable
fun PicZenNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onNavigateToFlowSorter = {
                    navController.navigate(Screen.FlowSorter)
                },
                onNavigateToLightTable = {
                    navController.navigate(Screen.LightTable)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                },
                onNavigateToPhotoList = { status ->
                    navController.navigate(Screen.PhotoList(status.name))
                },
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash)
                }
            )
        }
        
        composable<Screen.FlowSorter> {
            FlowSorterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLightTable = {
                    navController.navigate(Screen.LightTable)
                }
            )
        }
        
        composable<Screen.LightTable> {
            LightTableScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.PhotoList> { backStackEntry ->
            PhotoListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { photoId ->
                    navController.navigate(Screen.PhotoEditor(photoId))
                }
            )
        }
        
        composable<Screen.PhotoEditor> {
            PhotoEditorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPhoto = { photoId ->
                    navController.navigate(Screen.PhotoEditor(photoId))
                }
            )
        }
        
        composable<Screen.Trash> {
            TrashScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.Settings> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
