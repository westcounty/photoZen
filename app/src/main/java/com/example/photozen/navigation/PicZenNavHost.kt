package com.example.photozen.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.photozen.ui.components.AchievementCelebration
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
    modifier: Modifier = Modifier,
    achievementViewModel: AchievementCelebrationViewModel = hiltViewModel()
) {
    val celebrationState by achievementViewModel.currentCelebration.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onNavigateToFlowSorter = { isDaily, target ->
                    navController.navigate(Screen.FlowSorter(isDailyTask = isDaily, targetCount = target))
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
                onNavigateToWorkflow = { isDaily, target ->
                    navController.navigate(Screen.Workflow(isDailyTask = isDaily, targetCount = target))
                },
                onNavigateToTagBubble = {
                    navController.navigate(Screen.TagBubble)
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements)
                },
                onNavigateToFilterSelection = { mode, target ->
                    // For daily task modes, we pass targetCount
                    navController.navigate(Screen.PhotoFilterSelection(mode = mode, targetCount = target))
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
                },
                onNavigateToLightTable = {
                    navController.navigate(Screen.LightTable)
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
                        "flow" -> navController.navigate(Screen.FlowSorter())
                        "workflow" -> navController.navigate(Screen.Workflow())
                        "quicktag" -> navController.navigate(Screen.QuickTag)
                        "flow_daily" -> navController.navigate(Screen.FlowSorter(isDailyTask = true, targetCount = route.targetCount))
                        "workflow_daily" -> navController.navigate(Screen.Workflow(isDailyTask = true, targetCount = route.targetCount))
                        else -> navController.navigate(Screen.FlowSorter())
                    }
                    // Note: The filter parameters are stored in PreferencesRepository
                    // for use by FlowSorter/Workflow/QuickTag
                }
            )
        }
    }
    
        // Achievement Celebration Overlay
        celebrationState?.let { event ->
            AchievementCelebration(
                achievementName = event.achievement.name,
                achievementDescription = event.achievement.description,
                isVisible = true,
                onDismiss = { achievementViewModel.clearCelebration() }
            )
        }
    }
}
