package com.example.photozen.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Dialog to prompt user for MANAGE_EXTERNAL_STORAGE permission.
 * 
 * This dialog is shown when the user tries to move a photo to an album
 * but doesn't have the necessary permission.
 * 
 * Flow:
 * 1. User sees explanation of why permission is needed
 * 2. User clicks "Go to Settings" to grant permission
 * 3. After returning, user clicks "Permission Granted" to retry
 * 4. If still no permission, shows error message
 * 
 * @param onOpenSettings Called when user wants to go to settings
 * @param onPermissionGranted Called when user claims to have granted permission
 * @param onDismiss Called when user dismisses the dialog
 * @param showRetryError Whether to show an error that permission is still not granted
 */
@Composable
fun StoragePermissionDialog(
    onOpenSettings: () -> Unit,
    onPermissionGranted: () -> Unit,
    onDismiss: () -> Unit,
    showRetryError: Boolean = false
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "需要文件管理权限",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "移动照片到其他相册需要「所有文件访问权限」。\n\n" +
                           "请在设置页面中找到「所有文件访问权限」并开启。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                if (showRetryError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "权限尚未授予，请先完成授权操作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary action: go to settings
                Button(
                    onClick = {
                        openManageStorageSettings(context as? Activity, context.packageName)
                        onOpenSettings()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往设置授权")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Secondary action: retry after granting permission
                OutlinedButton(
                    onClick = onPermissionGranted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("已授予权限")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不授权")
            }
        }
    )
}

/**
 * Check if the app has MANAGE_EXTERNAL_STORAGE permission.
 */
fun hasManageStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true // Not needed on Android 10 and below
    }
}

/**
 * Open system settings to grant MANAGE_EXTERNAL_STORAGE permission.
 */
private fun openManageStorageSettings(activity: Activity?, packageName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            activity?.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general storage settings
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity?.startActivity(intent)
            } catch (e2: Exception) {
                // Last resort: open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                activity?.startActivity(intent)
            }
        }
    }
}
