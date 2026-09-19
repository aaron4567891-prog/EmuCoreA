package com.sbro.emucorea.core

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest

data class PspGameMetadata(val title: String?, val serial: String?)

/** Reads PARAM.SFO and ICON0.PNG from uncompressed PSP ISO and PBP files. */
object PspGameMetadataReader {
    private const val SECTOR_SIZE = 2048L
    private const val MAX_ENTRY_SIZE = 4 * 1024 * 1024
    private const val MAX_DIRECTORY_SIZE = 2 * 1024 * 1024

    fun read(context: Context, path: String): PspGameMetadata? = withChannel(context, path) { channel ->
        val entry = entries(channel, path).firstOrNull { it.name == "PARAM.SFO" } ?: return@withChannel null
        val bytes = readAt(channel, entry.offset, entry.size.coerceAtMost(MAX_ENTRY_SIZE)) ?: return@withChannel null
        val fields = parseSfo(bytes)
        PspGameMetadata(fields["TITLE"]?.takeIf(String::isNotBlank), fields["DISC_ID"]?.takeIf(String::isNotBlank))
    }

    fun extractIcon0(context: Context, path: String): String? = withChannel(context, path) { channel ->
        val entry = entries(channel, path).firstOrNull { it.name == "ICON0.PNG" } ?: return@withChannel null
        if (entry.size !in 8..MAX_ENTRY_SIZE) return@withChannel null
        val directory = File(context.cacheDir, "game-covers/embedded").apply { mkdirs() }
        val identity = "$path:${channel.size()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val target = File(directory, "$hash.png")
        if (isImage(target)) return@withChannel target.absolutePath
        val bytes = readAt(channel, entry.offset, entry.size) ?: return@withChannel null
        if (!bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))) {
            return@withChannel null
        }
        val temporary = File(directory, "$hash.tmp")
        try {
            temporary.writeBytes(bytes)
            if (!isImage(temporary)) return@withChannel null
            if (!temporary.renameTo(target)) temporary.copyTo(target, overwrite = true)
            target.takeIf(::isImage)?.absolutePath
        } finally {
            temporary.delete()
        }
    }

    private data class Entry(val name: String, val offset: Long, val size: Int)

    private fun entries(channel: FileChannel, path: String): List<Entry> {
        return when (path.substringAfterLast('.', "").lowercase()) {
            "pbp" -> pbpEntries(channel)
            "iso" -> isoEntries(channel)
            else -> emptyList()
        }
    }

    private fun pbpEntries(channel: FileChannel): List<Entry> {
        val header = readAt(channel, 0, 40) ?: return emptyList()
        if (!header.copyOfRange(0, 4).contentEquals(byteArrayOf(0, 0x50, 0x42, 0x50))) return emptyList()
        val data = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val offsets = (0..7).map { data.getInt(8 + it * 4).toLong() and 0xffffffffL }
        val end = offsets[2]
        return listOf(
            Entry("PARAM.SFO", offsets[0], (offsets[1] - offsets[0]).toInt()),
            Entry("ICON0.PNG", offsets[1], (end - offsets[1]).toInt())
        ).filter { it.offset >= 40 && it.size in 1..MAX_ENTRY_SIZE && it.offset + it.size <= channel.size() }
    }

    private fun isoEntries(channel: FileChannel): List<Entry> {
        val descriptor = readAt(channel, 16 * SECTOR_SIZE, SECTOR_SIZE.toInt()) ?: return emptyList()
        if (descriptor[0].toInt() != 1 || String(descriptor, 1, 5, Charsets.US_ASCII) != "CD001") return emptyList()
        val root = directoryEntry(descriptor, 156) ?: return emptyList()
        val gameDir = readDirectory(channel, root).firstOrNull { it.name == "PSP_GAME" } ?: return emptyList()
        return readDirectory(channel, gameDir).filter { it.name == "PARAM.SFO" || it.name == "ICON0.PNG" }
    }

    private fun readDirectory(channel: FileChannel, directory: Entry): List<Entry> {
        if (directory.size !in 1..MAX_DIRECTORY_SIZE) return emptyList()
        val bytes = readAt(channel, directory.offset, directory.size) ?: return emptyList()
        val result = ArrayList<Entry>()
        var offset = 0
        while (offset < bytes.size) {
            val length = bytes[offset].toInt() and 0xff
            if (length == 0) {
                offset = ((offset / SECTOR_SIZE.toInt()) + 1) * SECTOR_SIZE.toInt()
                continue
            }
            if (offset + length > bytes.size) break
            directoryEntry(bytes, offset)?.let(result::add)
            offset += length
        }
        return result
    }

    private fun directoryEntry(bytes: ByteArray, offset: Int): Entry? {
        if (offset + 34 > bytes.size) return null
        val length = bytes[offset].toInt() and 0xff
        val nameLength = bytes[offset + 32].toInt() and 0xff
        if (length < 34 || offset + length > bytes.size || 33 + nameLength > length) return null
        val name = String(bytes, offset + 33, nameLength, Charsets.US_ASCII).substringBefore(';').uppercase()
        val extent = littleInt(bytes, offset + 2).toLong() and 0xffffffffL
        val size = littleInt(bytes, offset + 10)
        return Entry(name, extent * SECTOR_SIZE, size)
    }

    private fun parseSfo(bytes: ByteArray): Map<String, String> {
        if (bytes.size < 20 || !bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(0, 0x50, 0x53, 0x46))) {
            return emptyMap()
        }
        val keyStart = littleInt(bytes, 8)
        val dataStart = littleInt(bytes, 12)
        val count = littleInt(bytes, 16)
        if (count !in 0..1024 || keyStart !in bytes.indices || dataStart !in bytes.indices) return emptyMap()
        val result = HashMap<String, String>()
        for (index in 0 until count) {
            val item = 20 + index * 16
            if (item + 16 > bytes.size) break
            val keyOffset = (bytes[item].toInt() and 0xff) or ((bytes[item + 1].toInt() and 0xff) shl 8)
            val valueLength = littleInt(bytes, item + 4)
            val valueOffset = dataStart.toLong() + (littleInt(bytes, item + 12).toLong() and 0xffffffffL)
            val keyAt = keyStart + keyOffset
            if (keyAt !in bytes.indices || valueLength !in 1..4096 || valueOffset < 0 || valueOffset + valueLength > bytes.size) continue
            val keyEnd = (keyAt until bytes.size).firstOrNull { bytes[it] == 0.toByte() } ?: continue
            val key = String(bytes, keyAt, keyEnd - keyAt, Charsets.UTF_8)
            if (key != "TITLE" && key != "DISC_ID") continue
            val value = String(bytes, valueOffset.toInt(), valueLength, Charsets.UTF_8).trimEnd('\u0000').trim()
            result[key] = value
        }
        return result
    }

    private fun littleInt(bytes: ByteArray, offset: Int): Int = ByteBuffer.wrap(bytes, offset, 4)
        .order(ByteOrder.LITTLE_ENDIAN).int

    private fun readAt(channel: FileChannel, offset: Long, size: Int): ByteArray? {
        if (size <= 0 || offset < 0 || offset + size > channel.size()) return null
        val data = ByteBuffer.allocate(size)
        var position = offset
        while (data.hasRemaining()) {
            val read = channel.read(data, position)
            if (read <= 0) return null
            position += read
        }
        return data.array()
    }

    private fun isImage(file: File): Boolean {
        if (!file.isFile || file.length() == 0L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    private inline fun <T> withChannel(context: Context, path: String, action: (FileChannel) -> T?): T? = runCatching {
        if (path.startsWith("content://")) {
            context.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use(action)
            }
        } else {
            RandomAccessFile(path, "r").use { action(it.channel) }
        }
    }.getOrNull()
}
