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
 * Crash logger that saves crash logs to internal storage for debugging.
 * 
 * This logger uses internal storage (filesDir) instead of external storage for reliability:
 * - No permissions required
 * - Always available, even during early app startup
 * - Works on all devices
 * 
 * Usage:
 * 1. Early init via CrashLoggerInitProvider (automatic, before Application.onCreate)
 * 2. Full init in Application.onCreate(): CrashLogger.init(this)
 * 3. Get logs via ADB: adb shell run-as com.example.photozen cat files/crash_logs/startup_log.txt
 * 
 * Startup logging:
 * - Call logStartupEvent() to record startup progress
 * - Check startup_log.txt for debugging startup crashes
 */
object CrashLogger {
    
    private const val TAG = "CrashLogger"
    private const val CRASH_LOG_DIR = "crash_logs"
    private const val STARTUP_LOG_FILE = "startup_log.txt"
    private const val MAX_LOG_FILES = 10
    private const val MAX_STARTUP_LOG_SIZE = 50 * 1024 // 50KB
    
    private var applicationContext: Context? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var isEarlyInitialized = false
    private var isFullyInitialized = false
    
    /**
     * Early initialization called from ContentProvider (before Application.onCreate).
     * Sets up the uncaught exception handler to capture early crashes.
     */
    fun initEarly(context: Context) {
        if (isEarlyInitialized) return
        
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
        
        isEarlyInitialized = true
        
        // Log the early initialization
        logStartupEventInternal(context, "CrashLogger early init (ContentProvider)")
        Log.i(TAG, "CrashLogger early initialization complete")
    }
    
    /**
     * Full initialization called from Application.onCreate().
     * Clears old logs and starts fresh session logging.
     */
    fun init(context: Context) {
        if (isFullyInitialized) return
        
        // If early init wasn't done, do it now
        if (!isEarlyInitialized) {
            initEarly(context)
        }
        
        applicationContext = context.applicationContext
        
        // Clear old startup log and start fresh session
        clearStartupLog(context)
        logStartupEventInternal(context, "CrashLogger full init (Application.onCreate)")
        
        isFullyInitialized = true
        Log.i(TAG, "CrashLogger full initialization complete")
    }
    
    /**
     * Get the log directory using internal storage (filesDir).
     * This is more reliable than external storage as it:
     * - Requires no permissions
     * - Is always available
     * - Works during early startup
     */
    private fun getLogDir(context: Context): File {
        return File(context.filesDir, CRASH_LOG_DIR)
    }
    
    /**
     * Log a startup event for debugging startup crashes.
     * This helps identify which component is causing the crash.
     */
    fun logStartupEvent(context: Context, event: String) {
        logStartupEventInternal(context, event)
    }
    
    /**
     * Internal implementation of startup event logging.
     */
    private fun logStartupEventInternal(context: Context, event: String) {
        try {
            val logDir = getLogDir(context)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val logFile = File(logDir, STARTUP_LOG_FILE)
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logEntry = "[$timestamp] $event\n"
            
            logFile.appendText(logEntry)
            Log.d(TAG, "Startup: $event")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log startup event: $event", e)
        }
    }
    
    /**
     * Clear the startup log file (called at each app start)
     */
    private fun clearStartupLog(context: Context) {
        try {
            val logDir = getLogDir(context)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val logFile = File(logDir, STARTUP_LOG_FILE)
            if (logFile.exists() && logFile.length() > MAX_STARTUP_LOG_SIZE) {
                // Keep the last part of the log if it's too large
                val content = logFile.readText()
                val lastPart = content.takeLast(MAX_STARTUP_LOG_SIZE / 2)
                logFile.writeText("... (truncated)\n$lastPart")
            }
            
            // Add separator for new session
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            logFile.appendText("\n========== NEW SESSION: $timestamp ==========\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear startup log", e)
        }
    }
    
    /**
     * Get the startup log content for debugging
     */
    fun getStartupLog(context: Context): String? {
        return try {
            val logFile = File(getLogDir(context), STARTUP_LOG_FILE)
            if (logFile.exists()) logFile.readText() else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Save crash log to file
     */
    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        val context = applicationContext ?: return
        
        val logDir = getLogDir(context)
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
            appendLine("=== Initialization State ===")
            appendLine("Early Init: $isEarlyInitialized")
            appendLine("Full Init: $isFullyInitialized")
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
        
        // Also log to startup log for debugging
        try {
            val startupLogFile = File(logDir, STARTUP_LOG_FILE)
            val crashSummary = "CRASH: ${throwable.javaClass.simpleName}: ${throwable.message}"
            startupLogFile.appendText("\n[CRASH] $crashSummary\n")
        } catch (e: Exception) {
            // Ignore
        }
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
        val logDir = getLogDir(context)
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
        val logDir = getLogDir(context)
        logDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Get crash log directory path for user reference.
     * Note: This is now internal storage, accessible via:
     * adb shell run-as com.example.photozen ls files/crash_logs/
     */
    fun getCrashLogPath(context: Context): String {
        return getLogDir(context).absolutePath
    }
}
