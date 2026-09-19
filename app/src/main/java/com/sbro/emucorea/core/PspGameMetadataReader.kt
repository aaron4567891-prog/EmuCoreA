package com.sbro.emucorea.core

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

data class PspGameMetadata(val title: String?, val serial: String?)

/** Reads bounded PARAM.SFO and ICON0.PNG assets through the bundled PPSSPP readers. */
object PspGameMetadataReader {

    private val bridge by lazy { NativeCoreBridge() }

    fun read(context: Context, path: String): PspGameMetadata? {
        val bytes = readAsset(context, path, 0) ?: return null
        val fields = parseSfo(bytes)
        return PspGameMetadata(fields["TITLE"]?.takeIf(String::isNotBlank), fields["DISC_ID"]?.takeIf(String::isNotBlank))
    }

    fun extractIcon0(context: Context, path: String): String? = runCatching {
        val directory = File(context.cacheDir, "game-covers/embedded").apply { mkdirs() }
        val identity = if (path.startsWith("content://")) {
            val document = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, Uri.parse(path))
            "$path:${document?.length()}:${document?.lastModified()}"
        } else {
            val file = File(path)
            "$path:${file.length()}:${file.lastModified()}"
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val target = File(directory, "$hash.png")
        if (isImage(target)) return@runCatching target.absolutePath
        val bytes = readAsset(context, path, 1) ?: return@runCatching null
        if (bytes.size < 8 || !bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))) {
            return@runCatching null
        }
        val temporary = File.createTempFile(hash, ".tmp", directory)
        try {
            temporary.writeBytes(bytes)
            if (!isImage(temporary)) return@runCatching null
            if (!temporary.renameTo(target)) temporary.copyTo(target, overwrite = true)
            target.takeIf(::isImage)?.absolutePath
        } finally {
            temporary.delete()
        }
    }.getOrNull()

    private fun readAsset(context: Context, path: String, asset: Int): ByteArray? = runCatching {
        if (path.startsWith("content://")) {
            context.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
                bridge.readGameAssetFd(it.fd, asset)
            }
        } else {
            bridge.readGameAsset(path, asset)
        }
    }.getOrNull()

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

    private fun isImage(file: File): Boolean {
        if (!file.isFile || file.length() == 0L) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

}
