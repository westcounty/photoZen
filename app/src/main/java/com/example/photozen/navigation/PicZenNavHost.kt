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
import com.example.photozen.BuildConfig
import com.example.photozen.ui.components.AchievementCelebration
import com.example.photozen.ui.screens.achievements.AchievementsScreen
import com.example.photozen.ui.screens.editor.PhotoEditorScreen
import com.example.photozen.ui.screens.filterselection.PhotoFilterSelectionScreen
import com.example.photozen.ui.screens.flowsorter.FlowSorterScreen
import com.example.photozen.ui.screens.home.HomeScreen
import com.example.photozen.ui.screens.lighttable.LightTableScreen
import com.example.photozen.ui.screens.photolist.PhotoListScreen
import com.example.photozen.ui.screens.settings.SettingsScreen
import com.example.photozen.ui.screens.share.ShareCompareScreen
import com.example.photozen.ui.screens.share.ShareCopyScreen
import com.example.photozen.ui.screens.smartgallery.LabelBrowserScreen
import com.example.photozen.ui.screens.smartgallery.LabelPhotosScreen
import com.example.photozen.ui.screens.smartgallery.MapLibreScreen
import com.example.photozen.ui.screens.smartgallery.PersonDetailScreen
import com.example.photozen.ui.screens.smartgallery.PersonListScreen
import com.example.photozen.ui.screens.smartgallery.SimilarPhotosScreen
import com.example.photozen.ui.screens.smartgallery.SmartGalleryScreen
import com.example.photozen.ui.screens.smartgallery.SmartSearchScreen
import com.example.photozen.ui.screens.timeline.TimelineScreen
import com.example.photozen.ui.screens.albums.AlbumBubbleScreen
import com.example.photozen.ui.screens.albums.AlbumPhotoListScreen
import com.example.photozen.ui.screens.stats.StatsScreen
import com.example.photozen.ui.screens.trash.TrashScreen
import com.example.photozen.ui.screens.workflow.WorkflowScreen

/**
 * Main navigation host for PicZen app.
 * Manages navigation between all screens.
 * 
 * 有两个重载版本：
 * 1. startDestination: String - 用于底部导航模式（使用 MainDestination.xxx.route）
 * 2. startDestination: Screen - 用于旧模式和 Share 场景
 * 
 * @param navController The navigation controller
 * @param startDestination The start destination
 * @param onFinish Callback to finish the activity (used for share screens)
 * @param modifier Modifier for the NavHost
 * @param achievementViewModel ViewModel for achievement celebrations
 */

/**
 * PicZenNavHost - 使用 Screen 类型作为起始目的地
 * 用于旧模式和 Share 场景
 */
@Composable
fun PicZenNavHost(
    navController: NavHostController,
    startDestination: Screen,
    onFinish: () -> Unit = {},
    modifier: Modifier = Modifier,
    achievementViewModel: AchievementCelebrationViewModel = hiltViewModel()
) {
    PicZenNavHostInternal(
        navController = navController,
        startDestination = startDestination,
        onFinish = onFinish,
        modifier = modifier,
        achievementViewModel = achievementViewModel
    )
}

/**
 * PicZenNavHost - 使用 String 路由作为起始目的地
 * 用于底部导航模式
 */
@Composable
fun PicZenNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    achievementViewModel: AchievementCelebrationViewModel = hiltViewModel()
) {
    PicZenNavHostInternal(
        navController = navController,
        startDestination = startDestination,
        onFinish = {},
        modifier = modifier,
        achievementViewModel = achievementViewModel
    )
}

/**
 * 内部实现 - 支持 Any 类型的 startDestination
 */
@Composable
private fun PicZenNavHostInternal(
    navController: NavHostController,
    startDestination: Any,
    onFinish: () -> Unit,
    modifier: Modifier,
    achievementViewModel: AchievementCelebrationViewModel
) {
    val celebrationState by achievementViewModel.currentCelebration.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ==================== Phase 1-C: 主 Tab 路由 ====================
        // 当启用底部导航时，这些路由作为主 Tab 的目的地
        // 与 Screen.Home 等保持兼容，都可以正常工作
        
        composable(MainDestination.Home.route) {
            // 底部导航模式的首页
            HomeScreen(
                onNavigateToFlowSorter = { isDaily, target ->
                    navController.navigate(Screen.FlowSorter(isDailyTask = isDaily, targetCount = target))
                },
                onNavigateToLightTable = {
                    navController.navigate(Screen.LightTable)
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
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements)
                },
                onNavigateToFilterSelection = { mode, target ->
                    navController.navigate(Screen.PhotoFilterSelection(mode = mode, targetCount = target))
                },
                onNavigateToSmartGallery = {
                    if (BuildConfig.ENABLE_SMART_GALLERY) {
                        navController.navigate(Screen.SmartGallery)
                    }
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats)
                }
                // 注意：移除了 onNavigateToSettings、onNavigateToTimeline、onNavigateToAlbumBubble
                // 这些由底部导航栏处理
            )
        }
        
        composable(MainDestination.Timeline.route) {
            // 底部导航模式的时间线
            TimelineScreen(
                // 移除 onNavigateBack（由底部导航处理）
                onPhotoClick = { photoId ->
                    navController.navigate(Screen.PhotoEditor(photoId))
                },
                onNavigateToSorter = {
                    navController.navigate(Screen.FlowSorter())
                },
                onNavigateToSorterListMode = {
                    navController.navigate(Screen.FlowSorter(initialListMode = true))
                }
            )
        }
        
        composable(MainDestination.Albums.route) {
            // 底部导航模式的相册
            AlbumBubbleScreen(
                // 移除 onNavigateBack（由底部导航处理）
                onNavigateToAlbumPhotos = { bucketId, albumName ->
                    navController.navigate(Screen.AlbumPhotoList(bucketId, albumName))
                },
                onNavigateToQuickSort = { bucketId ->
                    navController.navigate(Screen.FlowSorter(albumBucketId = bucketId))
                }
            )
        }
        
        composable(MainDestination.Settings.route) {
            // 底部导航模式的设置
            SettingsScreen(
                // 移除 onNavigateBack（由底部导航处理）
            )
        }
        
        // ==================== 原有 Screen 路由（保持向后兼容）====================
        
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
                onNavigateToAlbumBubble = {
                    navController.navigate(Screen.AlbumBubble)
                },
                onNavigateToAchievements = {
                    navController.navigate(Screen.Achievements)
                },
                onNavigateToFilterSelection = { mode, target ->
                    // For daily task modes, we pass targetCount
                    navController.navigate(Screen.PhotoFilterSelection(mode = mode, targetCount = target))
                },
                onNavigateToSmartGallery = {
                    // Only navigate if Smart Gallery is enabled
                    if (BuildConfig.ENABLE_SMART_GALLERY) {
                        navController.navigate(Screen.SmartGallery)
                    }
                },
                onNavigateToTimeline = {
                    navController.navigate(Screen.Timeline)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats)
                }
            )
        }
        
        // ==================== Smart Gallery Screens (Feature Flag Controlled) ====================
        // These routes are only registered when ENABLE_SMART_GALLERY is true
        
        if (BuildConfig.ENABLE_SMART_GALLERY) {
            composable<Screen.SmartGallery> {
                SmartGalleryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToLabels = {
                        navController.navigate(Screen.LabelBrowser)
                    },
                    onNavigateToPersons = {
                        navController.navigate(Screen.PersonList)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.SmartSearch)
                    },
                    onNavigateToSimilar = {
                        navController.navigate(Screen.SimilarPhotos)
                    },
                    onNavigateToMap = {
                        navController.navigate(Screen.MapView)
                    },
                    onNavigateToTimeline = {
                        navController.navigate(Screen.Timeline)
                    }
                )
            }
            
            composable<Screen.LabelBrowser> {
                LabelBrowserScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToLabel = { label ->
                        navController.navigate(Screen.LabelPhotos(label))
                    }
                )
            }
            
            composable<Screen.LabelPhotos> {
                LabelPhotosScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEditor = { photoId ->
                        navController.navigate(Screen.PhotoEditor(photoId))
                    }
                )
            }
            
            composable<Screen.PersonList> {
                PersonListScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPersonDetail = { personId ->
                        navController.navigate(Screen.PersonDetail(personId))
                    }
                )
            }
            
            composable<Screen.PersonDetail> {
                PersonDetailScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPhoto = { photoId ->
                        navController.navigate(Screen.PhotoEditor(photoId))
                    }
                )
            }
            
            composable<Screen.SmartSearch> {
                SmartSearchScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.PhotoEditor(photoId))
                    }
                )
            }
            
            composable<Screen.SimilarPhotos> {
                SimilarPhotosScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.PhotoEditor(photoId))
                    }
                )
            }
            
            composable<Screen.MapView> {
                MapLibreScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.PhotoEditor(photoId))
                    }
                )
            }
        }
        
        // ==================== Timeline Screen (Independent Feature) ====================
        // Timeline is a standalone feature that groups photos by time, not dependent on Smart Gallery
        
        composable<Screen.Timeline> {
            TimelineScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPhotoClick = { photoId ->
                    navController.navigate(Screen.PhotoEditor(photoId))
                },
                onNavigateToSorter = {
                    navController.navigate(Screen.FlowSorter())
                },
                onNavigateToSorterListMode = {
                    navController.navigate(Screen.FlowSorter(initialListMode = true))
                }
            )
        }
        
        // ==================== Core Photo Organization Screens ====================
        
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
        
        composable<Screen.AlbumBubble> {
            AlbumBubbleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAlbumPhotos = { bucketId, albumName ->
                    navController.navigate(Screen.AlbumPhotoList(bucketId, albumName))
                },
                onNavigateToQuickSort = { bucketId ->
                    // Navigate to flow sorter filtered by this album
                    navController.navigate(Screen.FlowSorter(albumBucketId = bucketId))
                }
            )
        }
        
        composable<Screen.AlbumPhotoList> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.AlbumPhotoList>()
            AlbumPhotoListScreen(
                bucketId = route.bucketId,
                albumName = route.albumName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { photoId ->
                    navController.navigate(Screen.PhotoEditor(photoId))
                },
                onNavigateToQuickSort = { bucketId ->
                    navController.navigate(Screen.FlowSorter(albumBucketId = bucketId))
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
        
        composable<Screen.Stats> {
            StatsScreen(
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
                        "flow_daily" -> navController.navigate(Screen.FlowSorter(isDailyTask = true, targetCount = route.targetCount))
                        "workflow_daily" -> navController.navigate(Screen.Workflow(isDailyTask = true, targetCount = route.targetCount))
                        else -> navController.navigate(Screen.FlowSorter())
                    }
                    // Note: The filter parameters are stored in PreferencesRepository
                    // for use by FlowSorter/Workflow
                }
            )
        }
        
        // ==================== Share Screens ====================
        // These screens handle external share intents
        
        composable<Screen.ShareCopy> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.ShareCopy>()
            ShareCopyScreen(
                urisJson = route.urisJson,
                onFinish = onFinish
            )
        }
        
        composable<Screen.ShareCompare> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.ShareCompare>()
            ShareCompareScreen(
                urisJson = route.urisJson,
                onFinish = onFinish
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
