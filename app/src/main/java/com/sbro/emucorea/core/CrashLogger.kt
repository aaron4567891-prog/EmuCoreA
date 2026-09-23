package com.sbro.emucorea.core

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object CrashLogger {

    private const val TAG = "CrashLogger"
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "crash.log"
    private const val MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024L  // 5 MB
    private const val KEEP_BYTES_AFTER_ROTATION = 4 * 1024 * 1024  // keep last 4 MB

    @Volatile
    private var logFile: File? = null

    @Volatile
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun init(context: Context) {
        logFile = resolveLogFile(context)
        installUncaughtExceptionHandler()
        writeStartupLine(context)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val body = buildString {
            append("[$tag] $message")
            if (throwable != null) {
                append("\n")
                append(throwable.stackTraceString())
            }
        }
        writeEntry("ERROR", body)
    }

    fun logInfo(tag: String, message: String) {
        writeEntry("INFO", "[$tag] $message")
    }

    fun logContext(key: String, value: Any?) {
        writeEntry("CTX", "$key = $value")
    }


    /**
     * Builds a shareable diagnostic report from the persistent crash/runtime log.
     * The caller can write this text to any Storage Access Framework Uri selected
     * by the user, so no broad storage permission is required.
     */
    fun buildDiagnosticReport(context: Context): String {
        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("?")
        val existingLog = runCatching { logFile?.takeIf(File::exists)?.readText() }
            .getOrNull()
            .orEmpty()

        return buildString {
            appendLine("EmuCoreA Diagnostic Report")
            appendLine("Generated: ${dateFmt.format(Date())}")
            appendLine("App version: $appVersion")
            appendLine("Package: ${context.packageName}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Hardware: ${Build.HARDWARE}")
            appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            appendLine("=== EmuCoreA runtime/crash log ===")
            if (existingLog.isBlank()) appendLine("No runtime log entries were recorded.")
            else append(existingLog)
        }
    }

    /** Writes a diagnostic report to a Uri chosen with ACTION_CREATE_DOCUMENT. */
    fun exportDiagnosticReport(context: Context, uri: android.net.Uri): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri, "wt").use { stream ->
            requireNotNull(stream) { "Unable to open the selected destination." }
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(buildDiagnosticReport(context))
            }
        }
        logInfo(TAG, "Diagnostic report exported")
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun resolveLogFile(context: Context): File? {
        return try {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            File(dir, LOG_FILE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve log file", e)
            null
        }
    }

    private fun writeStartupLine(context: Context) {
        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("?")

        val device = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVer = Build.VERSION.RELEASE
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "?"

        writeEntry("START", "APP START — v$appVersion | Android $androidVer | $device | $abi")
    }

    private fun installUncaughtExceptionHandler() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeEntry(
                    "CRASH",
                    "Thread: ${thread.name}\n${throwable.stackTraceString()}"
                )
            } catch (_: Exception) {
                // Don't let logger crash prevent the original crash from propagating
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeEntry(level: String, body: String) {
        val file = logFile ?: return
        val timestamp = dateFmt.format(Date())
        val line = "[$timestamp] $level: $body\n"

        try {
            rotateIfNeeded(file)
            FileWriter(file, /* append = */ true).use { it.write(line) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log entry", e)
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() < MAX_LOG_SIZE_BYTES) return
        try {
            val content = file.readBytes()
            // Keep only the last KEEP_BYTES_AFTER_ROTATION bytes to avoid trimming mid-line chaos
            val trimStart = (content.size - KEEP_BYTES_AFTER_ROTATION).coerceAtLeast(0)
            // Find the next newline after trimStart so we start on a clean line
            var safeStart = trimStart
            while (safeStart < content.size && content[safeStart] != '\n'.code.toByte()) safeStart++
            safeStart++ // skip the newline itself

            val rotationHeader = "=== LOG ROTATED at ${dateFmt.format(Date())} (old entries trimmed) ===\n"
            file.writeBytes(rotationHeader.toByteArray() + content.copyOfRange(safeStart.coerceAtMost(content.size), content.size))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log", e)
        }
    }

    private fun Throwable.stackTraceString(): String {
        val sw = StringWriter()
        printStackTrace(PrintWriter(sw))
        return sw.toString().trimEnd()
    }
}
