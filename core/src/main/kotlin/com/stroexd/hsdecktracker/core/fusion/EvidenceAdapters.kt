package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class TrackerEvidence(
    val source: FusionSource,
    val record: MatchRecord,
)

data class ExporterBundleEvidence(
    val artifactSha256: String,
    val schema: String?,
    val matches: List<Pair<FusionSource, JsonObject>>,
    val diagnostics: List<String>,
)

object EvidenceAdapters {
    fun tracker(bytes: ByteArray): TrackerEvidence {
        val hash = sha256(bytes)
        val record = AppJson.decodeFromString(MatchRecord.serializer(), bytes.toString(Charsets.UTF_8))
        return TrackerEvidence(
            FusionSource(
                id = "tracker:$hash",
                artifactSha256 = hash,
                type = SourceType.ANDROID_TRACKER_JSON,
                captureMethod = "screenshot-recognition",
                matchAlias = record.id,
                complete = true,
            ),
            record,
        )
    }

    fun powerDocument(bytes: ByteArray): Pair<FusionSource, JsonObject> {
        return powerDocument(bytes, sha256(bytes), emptyMap())
    }

    fun exporterBundle(bytes: ByteArray): ExporterBundleEvidence {
        val artifactHash = sha256(bytes)
        val matchEntries = linkedMapOf<String, ByteArray>()
        val contentHashes = linkedMapOf<String, String>()
        val truncationLines = linkedMapOf<String, Int>()
        var manifest: JsonObject? = null
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                when {
                    entry.name == "manifest.json" -> manifest = AppJson.parseToJsonElement(zip.readBytes().toString(Charsets.UTF_8)).jsonObject
                    entry.name.endsWith("/Power.log") -> {
                        val raw = zip.readBytes()
                        contentHashes[entry.name] = sha256(raw)
                        val markerLine = raw.toString(Charsets.UTF_8).lineSequence()
                            .indexOfFirst { it.contains("Truncating log, which has reached the size limit") }
                        if (markerLine >= 0) truncationLines[entry.name.substringBefore('/')] = markerLine + 1
                    }
                    entry.name.matches(Regex(".*/match-[0-9]+\\.json")) -> matchEntries[entry.name] = zip.readBytes()
                }
            }
        }
        val diagnostics = mutableListOf<String>()
        if (manifest == null) diagnostics += "Bundle has no manifest.json"
        val declared = manifest?.get("sessions")?.let { sessions ->
            sessions.jsonArray.associate { session ->
                val obj = session.jsonObject
                (obj["session"]?.jsonPrimitive?.content ?: "") to (obj["source_sha256"]?.jsonPrimitive?.content ?: "")
            }
        }.orEmpty()
        for ((name, actual) in contentHashes) {
            val session = name.substringBefore('/')
            val expected = declared[session]
            if (expected != null && !expected.equals(actual, ignoreCase = true)) diagnostics += "Power.log hash mismatch for $session"
        }
        for ((session, line) in truncationLines) diagnostics += "Source truncation marker observed for $session at line $line"
        val lastMatchBySession = matchEntries.keys.groupBy { it.substringBefore('/') }
            .mapValues { (_, names) -> names.maxOrNull() }
        val matches = matchEntries.map { (name, data) ->
            val relevantHash = contentHashes.filterKeys { it.substringBefore('/') == name.substringBefore('/') }
            val parsed = powerDocument(data, artifactHash, relevantHash)
            val session = name.substringBefore('/')
            val truncationLine = truncationLines[session]
            if (truncationLine == null || lastMatchBySession[session] != name) parsed
            else {
                val root = parsed.second
                val match = root.getValue("match").jsonObject
                val patchedMatch = JsonObject(match + mapOf(
                    "source_truncated" to JsonPrimitive(true),
                    "truncation_source_line" to JsonPrimitive(truncationLine),
                ))
                parsed.first to JsonObject(root + ("match" to patchedMatch))
            }
        }
        return ExporterBundleEvidence(artifactHash, manifest?.get("schema")?.jsonPrimitive?.contentOrNull, matches, diagnostics)
    }

    private fun powerDocument(bytes: ByteArray, artifactHash: String, contentHashes: Map<String, String>): Pair<FusionSource, JsonObject> {
        val root = AppJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
        val documentHash = sha256(bytes)
        val match = root["match"]?.jsonObject ?: error("Power evidence has no match object")
        val index = match["index"]?.jsonPrimitive?.content ?: "unknown"
        val sourceContentHash = contentHashes.entries.firstOrNull { it.key.endsWith("/Power.log") }?.value ?: documentHash
        val source = FusionSource(
            id = "power:$sourceContentHash:$index",
            artifactSha256 = artifactHash,
            type = SourceType.ANDROID_POWER_EVIDENCE,
            schema = root["schema"]?.jsonPrimitive?.content,
            captureMethod = "power-log-shizuku",
            sessionAlias = root["source_session"]?.jsonPrimitive?.content,
            matchAlias = index,
            complete = match["completed"]?.jsonPrimitive?.content?.toBooleanStrictOrNull(),
            contentSha256 = contentHashes + ("match-document" to documentHash),
            artifactAliases = listOf(artifactHash),
        )
        return source to root
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
