package com.example.photozen.util

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * ContentProvider that initializes CrashLogger before Application.onCreate().
 * 
 * ContentProviders are initialized before Application.onCreate(), making this
 * the earliest possible point to set up crash logging. This ensures we can
 * capture crashes that occur during Hilt injection or other early initialization.
 * 
 * The initOrder="9999" in manifest ensures this runs before other providers.
 */
class CrashLoggerInitProvider : ContentProvider() {
    
    companion object {
        private const val TAG = "CrashLoggerInit"
    }
    
    override fun onCreate(): Boolean {
        Log.d(TAG, "CrashLoggerInitProvider.onCreate() - early initialization")
        context?.let { ctx ->
            try {
                CrashLogger.initEarly(ctx)
                Log.d(TAG, "CrashLogger early initialization successful")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CrashLogger early", e)
            }
        } ?: Log.e(TAG, "Context is null in CrashLoggerInitProvider")
        return true
    }
    
    // Required ContentProvider methods - all return empty/null as we don't use them
    
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null
    
    override fun getType(uri: Uri): String? = null
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
