package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.*
import com.stroexd.hsdecktracker.core.util.AppJson
import java.nio.file.Files
import kotlin.test.*

class CaptureDiagnosticsTest {
    @Test fun boundedMetadataPreservesEventOrderClocksAndFailureReasons() {
        val dir = Files.createTempDirectory("synthetic-capture-log").toFile()
        try {
            val file = dir.resolve("capture.jsonl")
            var elapsed = 100L
            val log = CaptureDiagnosticLog(file, { 1234L }, { elapsed++ }, sessionId = "synthetic-session")
            val kinds = listOf(CaptureDiagnosticKind.REQUEST, CaptureDiagnosticKind.TIMEOUT,
                CaptureDiagnosticKind.EMPTY_CAPTURE, CaptureDiagnosticKind.RESIZE, CaptureDiagnosticKind.VISIBILITY,
                CaptureDiagnosticKind.OCR_FAILED, CaptureDiagnosticKind.OCR_REUSED)
            kinds.forEach { log.record(it, mapOf("captureId" to 2L)) }
            val events = file.readLines().map { AppJson.decodeFromString(CaptureDiagnostic.serializer(), it) }
            assertEquals(kinds, events.map { it.kind })
            assertEquals((1..kinds.size).toList(), events.map { it.sequence })
            assertTrue(events.zipWithNext().all { (a,b) -> b.elapsedMillis > a.elapsedMillis })
            assertTrue(events.all { it.wallTimeMillis == 1234L && it.sessionId == "synthetic-session" && it.values["captureId"] == 2L })
        } finally { dir.deleteRecursively() }
    }

    @Test fun eventAndByteBudgetsStopAppendingAndDeclareLimit() {
        val dir = Files.createTempDirectory("synthetic-capture-limit").toFile()
        try {
            val f = dir.resolve("events.jsonl")
            val log = CaptureDiagnosticLog(f, { 1 }, { 2 }, maxEvents = 3)
            repeat(20) { log.record(CaptureDiagnosticKind.REQUEST) }
            val lines = f.readLines()
            assertEquals(3, lines.size)
            assertEquals(CaptureDiagnosticKind.LIMIT, AppJson.decodeFromString(CaptureDiagnostic.serializer(), lines.last()).kind)
            val g = dir.resolve("bytes.jsonl")
            val bytes = CaptureDiagnosticLog(g, { 1 }, { 2 }, maxBytes = 900)
            repeat(20) { bytes.record(CaptureDiagnosticKind.VIEWPORT, mapOf("width" to 2000L)) }
            assertTrue(g.length() <= 900)
            val before = g.readBytes()
            bytes.record(CaptureDiagnosticKind.REQUEST)
            assertContentEquals(before, g.readBytes())
            val recreated = CaptureDiagnosticLog(g, { 1 }, { 2 }, maxBytes = 900)
            repeat(20) { recreated.record(CaptureDiagnosticKind.REQUEST) }
            assertTrue(g.length() <= 900)
        } finally { dir.deleteRecursively() }
    }

    @Test fun metadataRejectsUnboundedKeysAndContainsNoContentFields() {
        val dir = Files.createTempDirectory("synthetic-capture-fields").toFile()
        try {
            val f = dir.resolve("capture.jsonl")
            val log = CaptureDiagnosticLog(f, { 1 }, { 2 })
            assertFailsWith<IllegalArgumentException> { log.record(CaptureDiagnosticKind.REQUEST, mapOf("x".repeat(49) to 1)) }
            assertFailsWith<IllegalArgumentException> { log.record(CaptureDiagnosticKind.REQUEST, (1..25).associate { "key$it" to 1L }) }
            assertFalse(f.exists())
            log.record(CaptureDiagnosticKind.VIEWPORT, mapOf("maskLeft" to -1, "contentWidth" to 2000))
            val event = AppJson.decodeFromString(CaptureDiagnostic.serializer(), f.readLines().single())
            assertEquals(mapOf("maskLeft" to -1L, "contentWidth" to 2000L), event.values)
        } finally { dir.deleteRecursively() }
    }

    @Test fun failingSinkIsDisabledAfterFirstWriteFailure() {
        val dir = Files.createTempDirectory("synthetic-capture-io").toFile()
        try {
            val invalidFile = dir.resolve("directory").apply { mkdir() }
            val log = CaptureDiagnosticLog(invalidFile, { 1 }, { 2 })
            assertFailsWith<java.io.IOException> { log.record(CaptureDiagnosticKind.REQUEST) }
            // Must not keep attempting failed disk writes from capture callbacks.
            repeat(100) { log.record(CaptureDiagnosticKind.REQUEST) }
            assertTrue(invalidFile.isDirectory)
        } finally { dir.deleteRecursively() }
    }

    @Test fun concurrentCallbacksHaveOneSequenceAndRespectBudget() {
        val dir = Files.createTempDirectory("synthetic-capture-concurrent").toFile()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val file = dir.resolve("capture.jsonl")
            val log = CaptureDiagnosticLog(file, { 1 }, { 2 }, maxEvents = 50, maxBytes = 20_000)
            val jobs = (1..4).map { pool.submit { repeat(100) { log.record(CaptureDiagnosticKind.DELIVERED) } } }
            jobs.forEach { it.get() }
            val events = file.readLines().map { AppJson.decodeFromString(CaptureDiagnostic.serializer(), it) }
            assertEquals(50, events.size)
            assertEquals((1..50).toList(), events.map { it.sequence })
            assertEquals(CaptureDiagnosticKind.LIMIT, events.last().kind)
            assertTrue(file.length() <= 20_000)
        } finally { pool.shutdownNow(); dir.deleteRecursively() }
    }
}
