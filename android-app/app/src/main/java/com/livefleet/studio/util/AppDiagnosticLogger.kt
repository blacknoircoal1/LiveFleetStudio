package com.livefleet.studio.util

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.livefleet.studio.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight, bounded asynchronous session & hardware diagnostic logger.
 *
 * Writes structured, human-readable lines to `filesDir/logs/livefleet_diagnostics.log`
 * using a background `Dispatchers.IO` Channel so BLE/GATT callbacks and UI never block.
 * Automatically rotates at [MAX_LOG_BYTES] (512 KB) keeping one `.prev.log` backup (~1 MB max).
 */
object AppDiagnosticLogger {
    private const val TAG = "LiveFleetDiag"
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "livefleet_diagnostics.log"
    private const val PREV_LOG_FILE = "livefleet_diagnostics.prev.log"
    private const val MAX_LOG_BYTES = 512 * 1024L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lineChannel = Channel<String>(capacity = 512)
    private val seenSightingKeys = ConcurrentHashMap.newKeySet<String>()
    private val timeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    @Volatile
    private var logDir: File? = null

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        val appContext = context.applicationContext
        val dir = File(appContext.filesDir, LOG_DIR).apply { mkdirs() }
        logDir = dir
        if (!initialized) {
            initialized = true
            startWriterLoop()
        }
        logEnvironmentSnapshot(appContext)
    }

    fun log(category: String, message: String) {
        val ts = timeFormat.get()?.format(Date()) ?: ""
        val line = "$ts [$category] $message"
        Log.d(TAG, line)
        lineChannel.trySend(line)
    }

    fun warn(category: String, message: String) {
        val ts = timeFormat.get()?.format(Date()) ?: ""
        val line = "$ts [WARN:$category] $message"
        Log.w(TAG, line)
        lineChannel.trySend(line)
    }

    fun error(category: String, message: String, throwable: Throwable? = null) {
        val ts = timeFormat.get()?.format(Date()) ?: ""
        val suffix = throwable?.let { " | ${it::class.java.simpleName}: ${it.message}" } ?: ""
        val line = "$ts [ERR:$category] $message$suffix"
        Log.e(TAG, line)
        lineChannel.trySend(line)
    }

    /**
     * Logs a device discovery / classification sighting once per scan session for [sightingKey],
     * preventing advertisement floods while preserving every raw name, MAC, Company ID & UUID.
     */
    fun logUniqueSighting(sightingKey: String, category: String, message: String) {
        if (seenSightingKeys.add(sightingKey)) {
            log(category, message)
        }
    }

    fun clearSessionSightings() {
        seenSightingKeys.clear()
    }

    fun getCurrentLogFile(): File? {
        val dir = logDir ?: return null
        return File(dir, LOG_FILE)
    }

    fun readRecentLogText(maxChars: Int = 24_000): String {
        val file = getCurrentLogFile() ?: return "Logger not initialized."
        if (!file.exists() || file.length() == 0L) {
            return "No diagnostic events recorded yet.\nStart Live Scan or change settings to record events."
        }
        return try {
            val content = file.readText()
            if (content.length <= maxChars) {
                content
            } else {
                "... (showing last ${maxChars / 1024} KB) ...\n" + content.takeLast(maxChars)
            }
        } catch (e: Exception) {
            "Unable to read diagnostic log: ${e.message}"
        }
    }

    fun getLogSummary(): String {
        val file = getCurrentLogFile() ?: return "0 KB"
        val prev = logDir?.let { File(it, PREV_LOG_FILE) }
        val totalBytes = (if (file.exists()) file.length() else 0L) +
            (if (prev?.exists() == true) prev.length() else 0L)
        val kb = totalBytes / 1024.0
        return if (kb < 1.0 && totalBytes > 0L) "${totalBytes} B" else String.format(Locale.US, "%.1f KB", kb)
    }

    fun clearLogs() {
        seenSightingKeys.clear()
        scope.launch {
            val dir = logDir ?: return@launch
            try {
                File(dir, LOG_FILE).delete()
                File(dir, PREV_LOG_FILE).delete()
                log("ENV", "Diagnostic logs cleared by user")
            } catch (_: Exception) {
            }
        }
    }

    private fun logEnvironmentSnapshot(context: Context) {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btEnabled = btManager?.adapter?.isEnabled == true
        val scanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPerm(context, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPerm(context, Manifest.permission.BLUETOOTH)
        }
        val connectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPerm(context, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            true
        }
        val locationGranted = hasPerm(context, Manifest.permission.ACCESS_FINE_LOCATION)

        log(
            "ENV",
            "App=${AppConfig.VERSION_LABEL} | Device=${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT}) | " +
                "BT=$btEnabled | Perms[SCAN=$scanGranted, CONNECT=$connectGranted, LOC=$locationGranted]"
        )
    }

    private fun hasPerm(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun startWriterLoop() {
        scope.launch {
            for (line in lineChannel) {
                val dir = logDir ?: continue
                try {
                    val activeFile = File(dir, LOG_FILE)
                    if (activeFile.exists() && activeFile.length() >= MAX_LOG_BYTES) {
                        val prevFile = File(dir, PREV_LOG_FILE)
                        if (prevFile.exists()) prevFile.delete()
                        activeFile.renameTo(prevFile)
                    }
                    activeFile.appendText(line + "\n")
                } catch (_: Exception) {
                }
            }
        }
    }
}
