package com.stroexd.hsdecktracker.core.stats

import com.stroexd.hsdecktracker.core.util.PrettyJson
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object MatchJsonExport {
    fun encode(record: MatchRecord): String = PrettyJson.encodeToString(MatchRecord.serializer(), record)

    fun filename(record: MatchRecord): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'")
            .withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(record.timestamp))
        val id = MessageDigest.getInstance("SHA-256").digest(record.id.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        if (record.revision <= 1) return "match_${timestamp}_$id.json"
        val content = MessageDigest.getInstance("SHA-256").digest(encode(record).toByteArray(Charsets.UTF_8))
            .take(8).joinToString("") { "%02x".format(it.toInt() and 255) }
        return "match_${timestamp}_${id}_r${record.revision}_$content.json"
    }
}
