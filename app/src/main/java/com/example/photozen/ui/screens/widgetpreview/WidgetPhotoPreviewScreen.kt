package com.example.photozen.ui.screens.widgetpreview

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.components.ConfirmDeleteSheet
import com.example.photozen.ui.components.DeleteType
import com.example.photozen.ui.components.fullscreen.FullscreenActionType
import com.example.photozen.ui.components.fullscreen.UnifiedFullscreenViewer
import com.example.photozen.ui.components.openImageWithChooser
import com.example.photozen.ui.components.shareImage
import kotlinx.coroutines.flow.collectLatest

/**
 * Widget photo preview screen.
 * Shows fullscreen preview of photos from the Memory Lane widget's configured photo range.
 *
 * @param photoId The ID of the initially displayed photo
 * @param widgetId The widget instance ID (for loading photo source config)
 * @param onNavigateBack Callback to navigate back (finish activity)
 * @param onNavigateToEditor Callback to navigate to built-in photo editor
 * @param viewModel The ViewModel
 */
@Composable
fun WidgetPhotoPreviewScreen(
    photoId: String,
    widgetId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    viewModel: WidgetPhotoPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Delete confirmation state
    var showDeleteConfirmSheet by remember { mutableStateOf(false) }
    var pendingDeletePhoto by remember { mutableStateOf<PhotoEntity?>(null) }

    // System delete dialog launcher (Android 11+)
    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteComplete(result.resultCode == Activity.RESULT_OK)
    }

    // Handle delete intent from ViewModel
    LaunchedEffect(uiState.deleteIntentSender) {
        uiState.deleteIntentSender?.let { intentSender ->
            try {
                deleteIntentLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            } catch (e: Exception) {
                viewModel.clearDeleteIntent()
            }
        }
    }

    // Handle events (Toast messages)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is WidgetPhotoPreviewEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Error",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.photos.isEmpty() -> {
                Text(
                    text = "No photos available",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                UnifiedFullscreenViewer(
                    photos = uiState.photos,
                    initialIndex = uiState.initialIndex,
                    onExit = onNavigateBack,
                    onAction = { actionType, photo ->
                        when (actionType) {
                            FullscreenActionType.COPY -> {
                                viewModel.duplicatePhoto(photo.id)
                            }
                            FullscreenActionType.OPEN_WITH -> {
                                openImageWithChooser(context, Uri.parse(photo.systemUri))
                            }
                            FullscreenActionType.EDIT -> {
                                onNavigateToEditor(photo.id)
                            }
                            FullscreenActionType.SHARE -> {
                                shareImage(context, Uri.parse(photo.systemUri))
                            }
                            FullscreenActionType.DELETE -> {
                                pendingDeletePhoto = photo
                                showDeleteConfirmSheet = true
                            }
                        }
                    },
                    overlayContent = {
                        // Delete confirmation sheet
                        if (showDeleteConfirmSheet && pendingDeletePhoto != null) {
                            ConfirmDeleteSheet(
                                photos = listOf(pendingDeletePhoto!!),
                                deleteType = DeleteType.PERMANENT_DELETE,
                                onConfirm = {
                                    showDeleteConfirmSheet = false
                                    pendingDeletePhoto?.let { photo ->
                                        viewModel.requestPermanentDelete(photo.id)
                                    }
                                    pendingDeletePhoto = null
                                },
                                onDismiss = {
                                    showDeleteConfirmSheet = false
                                    pendingDeletePhoto = null
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}
