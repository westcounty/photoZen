package com.example.photozen.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * App info for external app chooser.
 */
data class ExternalAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Any? // Drawable
)

/**
 * Get available apps that can open images.
 */
fun getImageViewerApps(context: Context, imageUri: Uri): List<ExternalAppInfo> {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(imageUri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    val packageManager = context.packageManager
    val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    
    return resolveInfoList
        .filter { it.activityInfo.packageName != context.packageName } // Exclude self
        .distinctBy { it.activityInfo.packageName }
        .map { resolveInfo ->
            ExternalAppInfo(
                packageName = resolveInfo.activityInfo.packageName,
                appName = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager)
            )
        }
        .sortedBy { it.appName }
}

/**
 * Open image with a specific app.
 */
fun openImageWithApp(context: Context, imageUri: Uri, packageName: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(imageUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(packageName)
        }
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "无法打开应用", Toast.LENGTH_SHORT).show()
        false
    }
}

/**
 * Open image with system chooser.
 */
fun openImageWithChooser(context: Context, imageUri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(imageUri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "选择应用打开")
    context.startActivity(chooser)
}

/**
 * Modern action sheet for photo operations in tagged photos screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaggedPhotoActionSheet(
    imageUri: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRemoveTag: () -> Unit,
    onChangeTag: () -> Unit,
    onOpenWithApp: (String) -> Unit, // packageName
    defaultAppPackage: String? = null,
    onSetDefaultApp: (String?) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val context = LocalContext.current
    var showAppChooser by remember { mutableStateOf(false) }
    val uri = remember(imageUri) { Uri.parse(imageUri) }
    val availableApps = remember(imageUri) { getImageViewerApps(context, uri) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
        ) {
            if (showAppChooser) {
                // App chooser view
                AppChooserContent(
                    apps = availableApps,
                    defaultAppPackage = defaultAppPackage,
                    onAppSelected = { app, setAsDefault ->
                        if (setAsDefault) {
                            onSetDefaultApp(app.packageName)
                        }
                        onOpenWithApp(app.packageName)
                        onDismiss()
                    },
                    onUseSystemChooser = {
                        openImageWithChooser(context, uri)
                        onDismiss()
                    },
                    onBack = { showAppChooser = false }
                )
            } else {
                // Main action sheet
                Text(
                    text = "照片操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action items
                ActionSheetItem(
                    icon = Icons.Default.Edit,
                    title = "编辑照片",
                    subtitle = "裁切、创建虚拟副本",
                    onClick = {
                        onEdit()
                        onDismiss()
                    }
                )
                
                ActionSheetItem(
                    icon = Icons.Default.OpenInNew,
                    title = "使用其他应用打开",
                    subtitle = if (defaultAppPackage != null) "默认: ${getAppName(context, defaultAppPackage)}" else "选择应用查看或编辑",
                    onClick = {
                        if (defaultAppPackage != null && availableApps.any { it.packageName == defaultAppPackage }) {
                            onOpenWithApp(defaultAppPackage)
                            onDismiss()
                        } else {
                            showAppChooser = true
                        }
                    },
                    onLongClick = { showAppChooser = true }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                ActionSheetItem(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "修改标签",
                    subtitle = "将照片移至其他标签",
                    onClick = {
                        onChangeTag()
                        onDismiss()
                    }
                )
                
                ActionSheetItem(
                    icon = Icons.Outlined.Delete,
                    title = "移除标签",
                    subtitle = "从当前标签中移除照片",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onRemoveTag()
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * Get app name from package name.
 */
private fun getAppName(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName.substringAfterLast(".")
    }
}

/**
 * App chooser content.
 */
@Composable
private fun AppChooserContent(
    apps: List<ExternalAppInfo>,
    defaultAppPackage: String?,
    onAppSelected: (ExternalAppInfo, Boolean) -> Unit,
    onUseSystemChooser: () -> Unit,
    onBack: () -> Unit
) {
    var setAsDefault by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Text(
                text = "选择应用打开",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (apps.isEmpty()) {
            Text(
                text = "没有找到可用的应用",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                items(apps) { app ->
                    AppItem(
                        app = app,
                        isDefault = app.packageName == defaultAppPackage,
                        onClick = { onAppSelected(app, setAsDefault) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Set as default option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { setAsDefault = !setAsDefault }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "记住我的选择",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = setAsDefault,
                onCheckedChange = { setAsDefault = it }
            )
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        
        // System chooser option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUseSystemChooser() }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "使用系统选择器",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * App item in the chooser.
 */
@Composable
private fun AppItem(
    app: ExternalAppInfo,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (app.icon != null) {
                    AsyncImage(
                        model = app.icon,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                }
            }
            if (isDefault) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "默认",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Single action item in the sheet.
 */
@Composable
private fun ActionSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Simple action sheet for PhotoListScreen (uses DropdownMenu style but can be upgraded).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListActionSheet(
    imageUri: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMoveToKeep: (() -> Unit)? = null,
    onMoveToMaybe: (() -> Unit)? = null,
    onMoveToTrash: (() -> Unit)? = null,
    onResetToUnsorted: () -> Unit,
    onOpenWithApp: (String) -> Unit,
    defaultAppPackage: String? = null,
    onSetDefaultApp: (String?) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val context = LocalContext.current
    var showAppChooser by remember { mutableStateOf(false) }
    val uri = remember(imageUri) { Uri.parse(imageUri) }
    val availableApps = remember(imageUri) { getImageViewerApps(context, uri) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
        ) {
            if (showAppChooser) {
                AppChooserContent(
                    apps = availableApps,
                    defaultAppPackage = defaultAppPackage,
                    onAppSelected = { app, setAsDefault ->
                        if (setAsDefault) {
                            onSetDefaultApp(app.packageName)
                        }
                        onOpenWithApp(app.packageName)
                        onDismiss()
                    },
                    onUseSystemChooser = {
                        openImageWithChooser(context, uri)
                        onDismiss()
                    },
                    onBack = { showAppChooser = false }
                )
            } else {
                Text(
                    text = "照片操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ActionSheetItem(
                    icon = Icons.Default.Edit,
                    title = "编辑照片",
                    subtitle = "裁切、创建虚拟副本",
                    onClick = {
                        onEdit()
                        onDismiss()
                    }
                )
                
                ActionSheetItem(
                    icon = Icons.Default.OpenInNew,
                    title = "使用其他应用打开",
                    subtitle = if (defaultAppPackage != null) "默认: ${getAppName(context, defaultAppPackage)}" else "选择应用查看或编辑",
                    onClick = {
                        if (defaultAppPackage != null && availableApps.any { it.packageName == defaultAppPackage }) {
                            onOpenWithApp(defaultAppPackage)
                            onDismiss()
                        } else {
                            showAppChooser = true
                        }
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                onMoveToKeep?.let {
                    ActionSheetItem(
                        icon = Icons.Default.Check,
                        title = "移至保留",
                        iconTint = Color(0xFF4CAF50),
                        onClick = {
                            it()
                            onDismiss()
                        }
                    )
                }
                
                onMoveToMaybe?.let {
                    ActionSheetItem(
                        icon = Icons.Default.LocalOffer,
                        title = "标记待定",
                        iconTint = Color(0xFFFFC107),
                        onClick = {
                            it()
                            onDismiss()
                        }
                    )
                }
                
                onMoveToTrash?.let {
                    ActionSheetItem(
                        icon = Icons.Outlined.Delete,
                        title = "移至回收站",
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = {
                            it()
                            onDismiss()
                        }
                    )
                }
                
                ActionSheetItem(
                    icon = Icons.Default.Share,
                    title = "恢复未整理",
                    subtitle = "将照片状态重置为未整理",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        onResetToUnsorted()
                        onDismiss()
                    }
                )
            }
        }
    }
}
