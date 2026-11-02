package net.prototux.aare

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** User-initiated local export only; no network upload or broad storage permissions. */
object LogExport {
    fun fileName(): String =
        "AARE_logs_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + ".zip"

    fun saveToDownloads(context: Context): String {
        require(Build.VERSION.SDK_INT >= 29)
        val resolver = context.contentResolver
        val name = fileName()
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }) ?: error("Could not create Downloads entry")
        try {
            resolver.openOutputStream(uri, "w")?.use { writeArchive(context, it) }
                ?: error("Could not open Downloads file")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
            return name
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    fun writeToUri(context: Context, uri: Uri) {
        context.contentResolver.openOutputStream(uri, "w")?.use { writeArchive(context, it) }
            ?: error("Could not open selected file")
    }

    internal fun writeArchive(context: Context, destination: OutputStream) {
        ZipOutputStream(destination).use { zip ->
            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(("AARE logs. Review/redact before sharing: navigation records may contain route text.\n" +
                "This export contains app-private diagnostic files, not system-wide logcat.\n").toByteArray())
            zip.closeEntry()
            for (name in listOf("connection.log", "aare-tbt-probe.prev.jsonl", "aare-tbt-probe.jsonl")) {
                val source = File(context.filesDir, name)
                if (!source.isFile) continue
                zip.putNextEntry(ZipEntry(name))
                source.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var left = 2L * 1024L * 1024L
                    while (left > 0) {
                        val count = input.read(buffer, 0, minOf(left, buffer.size.toLong()).toInt())
                        if (count < 0) break
                        zip.write(buffer, 0, count)
                        left -= count
                    }
                }
                zip.closeEntry()
            }
        }
    }
}
