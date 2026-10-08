package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
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
        var manifest: JsonObject? = null
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                when {
                    entry.name == "manifest.json" -> manifest = AppJson.parseToJsonElement(zip.readBytes().toString(Charsets.UTF_8)).jsonObject
                    entry.name.endsWith("/Power.log") -> contentHashes[entry.name] = sha256(zip.readBytes())
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
        val matches = matchEntries.map { (name, data) ->
            val relevantHash = contentHashes.filterKeys { it.substringBefore('/') == name.substringBefore('/') }
            powerDocument(data, artifactHash, relevantHash)
        }
        return ExporterBundleEvidence(artifactHash, manifest?.get("schema")?.jsonPrimitive?.contentOrNull, matches, diagnostics)
    }

    private fun powerDocument(bytes: ByteArray, artifactHash: String, contentHashes: Map<String, String>): Pair<FusionSource, JsonObject> {
        val root = AppJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
        val documentHash = sha256(bytes)
        val match = root["match"]?.jsonObject ?: error("Power evidence has no match object")
        val index = match["index"]?.jsonPrimitive?.content ?: "unknown"
        val source = FusionSource(
            id = "power:$artifactHash:$index",
            artifactSha256 = artifactHash,
            type = SourceType.ANDROID_POWER_EVIDENCE,
            schema = root["schema"]?.jsonPrimitive?.content,
            captureMethod = "power-log-shizuku",
            sessionAlias = root["source_session"]?.jsonPrimitive?.content,
            matchAlias = index,
            complete = match["completed"]?.jsonPrimitive?.content?.toBooleanStrictOrNull(),
            contentSha256 = contentHashes + ("match-document" to documentHash),
        )
        return source to root
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
