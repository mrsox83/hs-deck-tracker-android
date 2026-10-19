package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Exact evidence equivalence, not a heuristic game identity or reconnect rule. */
internal object CompletedMatchEquivalence {
    fun attach(bytes: ByteArray, matches: List<Pair<FusionSource, JsonObject>>, check: () -> Unit)
        : List<Pair<FusionSource, JsonObject>> {
        // Inspect parsed boundaries first, then stream raw bytes once. Never retain another log copy.
        val boundaries = matches.map { (source, document) ->
            check()
            val match = document["match"]?.jsonObject
            var lastLine = 0
            var valid = true
            fun visit(value: JsonElement) {
                check()
                when (value) {
                    is JsonObject -> value.forEach { (key, child) ->
                        if (key == "source_line") {
                            val line = (child as? JsonPrimitive)?.intOrNull
                            if (line == null || line <= 0) valid = false else lastLine = maxOf(lastLine, line)
                        } else visit(child)
                    }
                    is JsonArray -> value.forEach(::visit)
                    else -> Unit
                }
            }
            if (match != null) visit(match)
            val rawHashes = source.contentSha256.filterKeys { it.endsWith("/Power.log") }
            val session = source.sessionAlias
            if (!valid || lastLine == 0 || source.complete != true ||
                match?.get("source_truncated")?.jsonPrimitive?.booleanOrNull != false ||
                session.isNullOrBlank() || rawHashes.size != 1 ||
                rawHashes.keys.single() != "$session/Power.log" ||
                document["source_sha256"]?.jsonPrimitive?.contentOrNull != rawHashes.values.single()) null
            else lastLine
        }
        val requested = matches.indices.filter { boundaries[it] != null }.groupBy { matches[it].first.sessionAlias!! }
        if (requested.isEmpty()) return matches.map { (source, document) ->
            source.copy(verifiedCompletedMatchKey = null) to document
        }
        val prefixes = mutableMapOf<Pair<String, Int>, String>()
        val occurrences = mutableMapOf<String, Int>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                check()
                val entry = zip.nextEntry ?: break
                val session = entry.name.removeSuffix("/Power.log")
                val indexes = requested[session]
                if (entry.name != "$session/Power.log" || indexes == null) continue
                occurrences[session] = occurrences.getOrDefault(session, 0) + 1
                val targets = indexes.map { boundaries[it]!! }.toSet()
                val last = targets.max()
                val digest = MessageDigest.getInstance("SHA-256")
                var line = 1
                var finalLineHasBytes = false
                while (line <= last) {
                    check()
                    val count = zip.read(buffer)
                    if (count < 0) {
                        if (finalLineHasBytes && line in targets) snapshot(digest)?.let { prefixes[session to line] = it }
                        break
                    }
                    var start = 0
                    for (offset in 0 until count) {
                        if (buffer[offset] != 10.toByte()) continue
                        digest.update(buffer, start, offset - start + 1)
                        if (line in targets) snapshot(digest)?.let { prefixes[session to line] = it }
                        line++
                        start = offset + 1
                        finalLineHasBytes = false
                        if (line > last) break
                    }
                    if (line <= last && start < count) {
                        digest.update(buffer, start, count - start)
                        finalLineHasBytes = true
                    }
                }
            }
        }
        return matches.mapIndexed { index, (source, document) ->
            check()
            val boundary = boundaries[index]
            val prefix = boundary?.let { prefixes[source.sessionAlias to it] }
            val key = if (prefix == null || occurrences[source.sessionAlias] != 1) null else {
                // The full-log hash is independently checked above and deliberately differs on append.
                val exactContent = canonical(JsonObject(document - "source_sha256"), check)
                EvidenceAdapters.sha256("completed-prefix/1\u0000$boundary\u0000$prefix\u0000$exactContent".toByteArray())
            }
            source.copy(verifiedCompletedMatchKey = key) to document
        }
    }

    private fun canonical(value: JsonElement, check: () -> Unit): JsonElement {
        check()
        return when (value) {
            is JsonObject -> JsonObject(value.toSortedMap().mapValues { canonical(it.value, check) })
            is JsonArray -> JsonArray(value.map { canonical(it, check) })
            else -> value
        }
    }

    // A provider without digest cloning safely disables equivalence; never weaken the proof.
    private fun snapshot(digest: MessageDigest): String? = try {
        (digest.clone() as MessageDigest).digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    } catch (_: CloneNotSupportedException) { null }

    fun representatives(matches: List<ReducedPowerMatch>): List<ReducedPowerMatch> =
        matches.sortedBy { it.source.id }.distinctBy { match ->
            if (match.completed && !match.sourceTruncated && match.continuation == null)
                match.source.verifiedCompletedMatchKey?.let { "equivalent:$it" } ?: "source:${match.source.id}"
            else "source:${match.source.id}"
        }
}
