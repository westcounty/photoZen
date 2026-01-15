package com.example.photozen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.photozen.ui.screens.achievements.AchievementsScreen
import com.example.photozen.ui.screens.editor.PhotoEditorScreen
import com.example.photozen.ui.screens.filterselection.PhotoFilterSelectionScreen
import com.example.photozen.ui.screens.flowsorter.FlowSorterScreen
import com.example.photozen.ui.screens.home.HomeScreen
import com.example.photozen.ui.screens.lighttable.LightTableScreen
import com.example.photozen.ui.screens.photolist.PhotoListScreen
import com.example.photozen.ui.screens.quicktag.QuickTagScreen
import com.example.photozen.ui.screens.settings.SettingsScreen
import com.example.photozen.ui.screens.tags.TagBubbleScreen
import com.example.photozen.ui.screens.tags.TaggedPhotosScreen
import com.example.photozen.ui.screens.trash.TrashScreen
import com.example.photozen.ui.screens.workflow.WorkflowScreen

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
                },
                onNavigateToWorkflow = {
                    navController.navigate(Screen.Workflow)
                },
                onNavigateToTagBubble = {
                    navController.navigate(Screen.TagBubble)
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements)
                },
                onNavigateToFilterSelection = { mode ->
                    navController.navigate(Screen.PhotoFilterSelection(mode))
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
                },
                onNavigateToQuickTag = {
                    navController.navigate(Screen.QuickTag)
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
        
        composable<Screen.Workflow> {
            WorkflowScreen(
                onExit = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.TagBubble> {
            TagBubbleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPhotoList = { tagId ->
                    navController.navigate(Screen.PhotoListByTag(tagId))
                }
            )
        }
        
        composable<Screen.PhotoListByTag> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.PhotoListByTag>()
            TaggedPhotosScreen(
                tagId = route.tagId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { photoId ->
                    navController.navigate(Screen.PhotoEditor(photoId))
                }
            )
        }
        
        composable<Screen.QuickTag> {
            QuickTagScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.Achievements> {
            AchievementsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.PhotoFilterSelection> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.PhotoFilterSelection>()
            PhotoFilterSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onConfirm = { albumIds, startDate, endDate ->
                    // Pop filter selection and navigate to the target screen
                    navController.popBackStack()
                    when (route.mode) {
                        "flow" -> navController.navigate(Screen.FlowSorter)
                        "quicktag" -> navController.navigate(Screen.QuickTag)
                        else -> navController.navigate(Screen.FlowSorter)
                    }
                    // Note: The filter parameters are stored in a shared ViewModel or PreferencesRepository
                    // for use by FlowSorter/QuickTag
                }
            )
        }
    }
}
