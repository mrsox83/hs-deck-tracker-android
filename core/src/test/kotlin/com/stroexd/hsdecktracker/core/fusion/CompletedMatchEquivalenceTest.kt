package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class CompletedMatchEquivalenceTest {
    private val session = "Hearthstone_2026_10_18_22_38_58"
    private val raw = "preamble\nCREATE_GAME\nSTATE COMPLETE\n"

    private fun bundle(log: String = raw, alias: String = session, completed: Boolean = true,
        truncated: Boolean = false, choice: String = "a", line: Int = 3, declared: String? = null,
        includeRaw: Boolean = true): ByteArray {
        val hash = declared ?: EvidenceAdapters.sha256(log.toByteArray())
        val document = buildJsonObject {
            put("schema", "test/1"); put("source_session", alias); put("source_sha256", hash)
            putJsonObject("match") {
                put("index", 1); put("completed", completed); put("source_truncated", truncated)
                put("started_log_time", "22:39:37.0000000"); put("ended_log_time", "22:50:34.0000000")
                putJsonArray("events") {
                    add(buildJsonObject { put("source_line", 2); put("sequence", 1); put("raw", "CREATE_GAME"); put("kind", "CREATE_GAME") })
                    add(buildJsonObject { put("source_line", line); put("sequence", 2); put("raw", "STATE COMPLETE"); put("kind", "OTHER") })
                }
                putJsonArray("choices") { add(buildJsonObject { put("id", choice) }) }
            }
        }
        return ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                fun entry(name: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
                if (includeRaw) entry("$alias/Power.log", log.toByteArray())
                entry("$alias/match-001.json", document.toString().toByteArray())
                entry("manifest.json", buildJsonObject {
                    putJsonArray("sessions") { add(buildJsonObject { put("session", alias); put("source_sha256", hash) }) }
                }.toString().toByteArray())
            }
        }.toByteArray()
    }

    private fun parsed(bytes: ByteArray) = EvidenceAdapters.exporterBundle(bytes).matches.single()
    private fun reduced(bytes: ByteArray) = parsed(bytes).let { PowerEvidenceReducer.reduce(it.first, it.second) }

    @Test fun exactCompletedPrefixCollapsesWithoutChangingSourceIdentity() {
        val early = reduced(bundle())
        val late = reduced(bundle(log = raw + "NEXT GAME\n"))
        assertNotNull(early.source.verifiedCompletedMatchKey)
        assertEquals(early.source.verifiedCompletedMatchKey, late.source.verifiedCompletedMatchKey)
        assertNotEquals(early.source.id, late.source.id)
        assertNotEquals(early.source.contentSha256, late.source.contentSha256)
        val forward = CompletedMatchEquivalence.representatives(listOf(early, late))
        assertEquals(1, forward.size)
        assertEquals(forward, CompletedMatchEquivalence.representatives(listOf(late, early)))
    }

    @Test fun changedChoicesOrRawPrefixOrSessionNeverCollapse() {
        val early = reduced(bundle())
        for (other in listOf(
            bundle(log = raw + "NEXT GAME\n", choice = "b"),
            bundle(log = raw.replace("preamble", "changed") + "NEXT GAME\n"),
            bundle(log = raw + "NEXT GAME\n", alias = "Hearthstone_2026_10_18_23_38_58"),
        )) assertEquals(2, CompletedMatchEquivalence.representatives(listOf(early, reduced(other))).size)
    }

    @Test fun incompleteTruncatedMissingRawAndOutOfBoundsHaveNoProof() {
        for (bytes in listOf(bundle(completed = false), bundle(truncated = true),
            bundle(includeRaw = false), bundle(line = 999), bundle(line = 0))) {
            assertNull(parsed(bytes).first.verifiedCompletedMatchKey)
        }
        val early = reduced(bundle())
        val late = reduced(bundle(log = raw + "NEXT GAME\n"))
        assertEquals(2, CompletedMatchEquivalence.representatives(listOf(early, late.copy(sourceTruncated = true))).size)
        assertEquals(2, CompletedMatchEquivalence.representatives(listOf(early, late.copy(completed = false))).size)
    }

    @Test fun mismatchedDeclaredHashDisablesProof() {
        val parsed = EvidenceAdapters.exporterBundle(bundle(declared = "0".repeat(64)))
        assertTrue(parsed.diagnostics.any { it.contains("hash mismatch") })
        assertNull(parsed.matches.single().first.verifiedCompletedMatchKey)
    }

    @Test fun standaloneDocumentAndSerializedSourcesCannotSupplyProof() {
        val (source, document) = parsed(bundle())
        assertNotNull(source.verifiedCompletedMatchKey)
        assertNull(EvidenceAdapters.powerDocument(document.toString().toByteArray()).first.verifiedCompletedMatchKey)
        val encoded = AppJson.encodeToString(FusionSource.serializer(), source)
        assertFalse(encoded.contains("verifiedCompletedMatchKey"))
        assertNull(AppJson.decodeFromString(FusionSource.serializer(), encoded).verifiedCompletedMatchKey)
        val withoutProof = reduced(bundle(log = raw + "NEXT GAME\n")).let { it.copy(source = it.source.copy(verifiedCompletedMatchKey = null)) }
        assertEquals(2, CompletedMatchEquivalence.representatives(listOf(reduced(bundle()), withoutProof)).size)
    }

    @Test fun finalNonNewlineBoundaryAndCrLfAreExactNotNormalized() {
        val unterminated = raw.trimEnd('\n')
        val early = parsed(bundle(log = unterminated)).first
        assertNotNull(early.verifiedCompletedMatchKey)
        assertNotEquals(early.verifiedCompletedMatchKey, parsed(bundle()).first.verifiedCompletedMatchKey)
        assertNotEquals(parsed(bundle()).first.verifiedCompletedMatchKey,
            parsed(bundle(log = raw.replace("\n", "\r\n"))).first.verifiedCompletedMatchKey)
    }

    @Test fun proofScanHonorsCancellation() {
        val bytes = bundle()
        val matches = EvidenceAdapters.exporterBundle(bytes).matches
        var checks = 0
        assertFailsWith<java.util.concurrent.CancellationException> {
            CompletedMatchEquivalence.attach(bytes, matches) {
                if (++checks == 5) throw java.util.concurrent.CancellationException("test cancellation")
            }
        }
    }

    @Test fun digestChunkBoundariesAndExtraIdentityMetadataRemainExact() {
        val log = "x".repeat(32 * 1024 - 1) + "\nCREATE_GAME\nSTATE COMPLETE\n"
        assertEquals(parsed(bundle(log = log)).first.verifiedCompletedMatchKey,
            parsed(bundle(log = log + "LATER GAME\n")).first.verifiedCompletedMatchKey)
        val bytes = bundle()
        val (source, document) = parsed(bytes)
        val reordered = JsonObject(document.entries.reversed().associate { it.toPair() })
        assertEquals(source.verifiedCompletedMatchKey,
            CompletedMatchEquivalence.attach(bytes, listOf(source to reordered)) {}.single().first.verifiedCompletedMatchKey)
        val changed = JsonObject(document + ("identity_metadata" to JsonPrimitive("changed")))
        assertNotEquals(source.verifiedCompletedMatchKey,
            CompletedMatchEquivalence.attach(bytes, listOf(source to changed)) {}.single().first.verifiedCompletedMatchKey)
    }
}
