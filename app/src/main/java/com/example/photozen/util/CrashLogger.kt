package com.example.photozen.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crash logger that saves crash logs to a file for debugging.
 * 
 * Usage:
 * 1. Initialize in Application.onCreate(): CrashLogger.init(this)
 * 2. After crash, reopen app and check: /sdcard/Android/data/com.example.photozen/files/crash_logs/
 * 3. Or share logs from Settings -> About -> "导出崩溃日志"
 */
object CrashLogger {
    
    private const val TAG = "CrashLogger"
    private const val CRASH_LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 10
    
    private var applicationContext: Context? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    
    /**
     * Initialize the crash logger. Call this in Application.onCreate()
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash log", e)
            }
            
            // Call the default handler to let the app crash normally
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        Log.i(TAG, "CrashLogger initialized")
    }
    
    /**
     * Save crash log to file
     */
    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        val context = applicationContext ?: return
        
        val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        // Clean up old logs
        cleanOldLogs(logDir)
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val logFile = File(logDir, "crash_$timestamp.txt")
        
        val stackTrace = StringWriter()
        throwable.printStackTrace(PrintWriter(stackTrace))
        
        val logContent = buildString {
            appendLine("=== PicZen Crash Log ===")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine()
            appendLine("=== Device Info ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("App Version: ${getAppVersion(context)}")
            appendLine()
            appendLine("=== Exception ===")
            appendLine("Type: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("=== Stack Trace ===")
            appendLine(stackTrace.toString())
            appendLine()
            appendLine("=== Cause Chain ===")
            var cause = throwable.cause
            var depth = 1
            while (cause != null && depth <= 5) {
                appendLine("Cause $depth: ${cause.javaClass.name}: ${cause.message}")
                cause = cause.cause
                depth++
            }
        }
        
        logFile.writeText(logContent)
        Log.i(TAG, "Crash log saved to: ${logFile.absolutePath}")
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Clean up old log files, keeping only the most recent ones
     */
    private fun cleanOldLogs(logDir: File) {
        val logFiles = logDir.listFiles { file -> file.name.startsWith("crash_") && file.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        
        if (logFiles.size > MAX_LOG_FILES) {
            logFiles.drop(MAX_LOG_FILES).forEach { it.delete() }
        }
    }
    
    /**
     * Get all crash logs
     */
    fun getCrashLogs(context: Context): List<File> {
        val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
        return logDir.listFiles { file -> file.name.startsWith("crash_") && file.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
    
    /**
     * Get the latest crash log content
     */
    fun getLatestCrashLog(context: Context): String? {
        val logs = getCrashLogs(context)
        return logs.firstOrNull()?.readText()
    }
    
    /**
     * Clear all crash logs
     */
    fun clearCrashLogs(context: Context) {
        val logDir = File(context.getExternalFilesDir(null), CRASH_LOG_DIR)
        logDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Get crash log directory path for user reference
     */
    fun getCrashLogPath(context: Context): String {
        return File(context.getExternalFilesDir(null), CRASH_LOG_DIR).absolutePath
    }
}
