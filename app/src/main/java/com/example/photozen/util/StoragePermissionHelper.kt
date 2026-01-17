package com.example.photozen.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing storage permissions for album operations.
 * 
 * Uses a dual-layer permission strategy:
 * 1. Primary: MANAGE_EXTERNAL_STORAGE (Android 11+) for seamless file operations
 * 2. Fallback: MediaStore createWriteRequest for per-file user confirmation
 * 
 * The app can function without MANAGE_EXTERNAL_STORAGE permission by falling back
 * to the per-file confirmation approach, ensuring file operations never fail.
 */
@Singleton
class StoragePermissionHelper @Inject constructor() {
    
    /**
     * Check if the app has MANAGE_EXTERNAL_STORAGE permission.
     * 
     * @return true if the app can freely manage external storage, 
     *         or if running on Android 10 or lower (where this permission isn't needed)
     */
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // On Android 10 and below, we don't need this special permission
            true
        }
    }
    
    /**
     * Check if MANAGE_EXTERNAL_STORAGE permission is applicable.
     * Only applies to Android 11 (R) and above.
     */
    fun isManageStoragePermissionApplicable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }
    
    /**
     * Get the Intent to open the system settings page for granting
     * MANAGE_EXTERNAL_STORAGE permission.
     * 
     * @param packageName The app's package name
     * @return Intent to open the settings page, or null if not applicable
     */
    fun getManageStorageSettingsIntent(packageName: String): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
        } else {
            null
        }
    }
    
    /**
     * Open the system settings page to grant MANAGE_EXTERNAL_STORAGE permission.
     * 
     * @param activity The current activity
     */
    fun openManageStorageSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general storage settings
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    activity.startActivity(intent)
                } catch (e2: Exception) {
                    // Last resort: open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                }
            }
        }
    }
    
    /**
     * Check if we can use direct file operations (move/rename).
     * This is possible when:
     * 1. MANAGE_EXTERNAL_STORAGE is granted, OR
     * 2. Running on Android 10 or below
     */
    fun canUseDirectFileOperations(): Boolean {
        return hasManageStoragePermission()
    }
    
    /**
     * Get a human-readable description of why the permission is needed.
     */
    fun getPermissionRationale(): String {
        return "移动照片到其他相册需要文件管理权限。授权后，移动操作将无需每次确认。"
    }
    
    /**
     * Get a short description for the settings dialog.
     */
    fun getSettingsDialogMessage(): String {
        return "移动照片需要文件管理权限，是否前往设置授权？"
    }
}
