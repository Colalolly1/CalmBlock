package com.calmblock.app.data

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.time.LocalDate

object Export {
    /** Writes date,app,minutes rows to Documents/CalmBlock. Returns the file name. */
    fun usageCsv(context: Context, rows: List<UsageDayEntity>): String {
        val name = "calmblock-usage-${LocalDate.now()}.csv"
        val body = buildString {
            append("date,app,minutes\n")
            rows.forEach { append(it.date).append(',').append(csv(it.label)).append(',').append(it.minutes).append('\n') }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/CalmBlock")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: throw IllegalStateException("Could not create file")
        resolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) } ?: throw IllegalStateException("Could not open file")
        return name
    }

    private fun csv(s: String): String = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
