package com.sbro.emucorea.data

import android.content.Context
import android.net.Uri
import com.sbro.emucorea.core.EmulatorStorage
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class PspSavedGame(val name: String, val bytes: Long)

/** PPSSPP uses a directory-backed Memory Stick; there is no 128 KiB card image. */
class PspMemoryStickRepository(private val context: Context) {
    val saveDirectory: File
        get() = EmulatorStorage.pspSaveDataDir(
            context, AppPreferences(context).getEmulatorDataPathSync())

    fun ensureMemoryStick(): Boolean = runCatching {
        val root = saveDirectory
        (root.isDirectory || root.mkdirs()) && root.isDirectory
    }.getOrDefault(false)

    fun saves(): List<PspSavedGame> = saveDirectory.listFiles().orEmpty()
        .filter(File::isDirectory)
        .map { directory ->
            PspSavedGame(directory.name, directory.walkTopDown()
                .filter(File::isFile).sumOf(File::length))
        }.sortedBy { it.name.lowercase() }

    fun backup(uri: Uri): Boolean = runCatching {
        val root = saveDirectory.canonicalFile
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                root.walkTopDown().filter(File::isFile).forEach { file ->
                    val relative = file.canonicalFile.relativeTo(root).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry("PSP/SAVEDATA/$relative"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } != null
    }.getOrDefault(false)

    /** Import only save data inside the Memory Stick. Existing files are preserved. */
    fun restore(uri: Uri): Boolean = runCatching {
        val root = saveDirectory.canonicalFile
        val buffer = ByteArray(64 * 1024)
        var entries = 0
        var totalBytes = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    require(entries <= 20_000)
                    val relative = entry.name.replace('\\', '/')
                        .removePrefix("PSP/SAVEDATA/")
                    require(relative != entry.name && relative.isNotBlank())
                    val destination = File(root, relative).canonicalFile
                    require(destination.toPath().startsWith(root.toPath()))
                    if (entry.isDirectory) {
                        destination.mkdirs()
                    } else {
                        destination.parentFile?.mkdirs()
                        // Never clobber a newer save during a restore.
                        if (destination.exists()) {
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                require(totalBytes <= 1024L * 1024 * 1024)
                            }
                        } else {
                            destination.outputStream().use { output ->
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    totalBytes += count
                                    require(totalBytes <= 1024L * 1024 * 1024)
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } != null && entries > 0
    }.getOrDefault(false)
}
