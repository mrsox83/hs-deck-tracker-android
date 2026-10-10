package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.fusion.PostMatchLogSnapshot
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import kotlin.test.*

class PostMatchLogSnapshotTest {
    private val session = "Hearthstone_2026_10_10_12_00_00"

    @Test fun snapshotHasExactBytesHashAndDefensiveCopy() {
        val result = PostMatchLogSnapshot.read(session, 3, { ByteArrayInputStream("abc".toByteArray()) })
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result.sourceSha256)
        assertEquals(3L, result.sourceBytes)
        result.copyBytes()[0] = 0
        assertEquals("abc", result.copyBytes().decodeToString())
    }

    @Test fun rejectsPathsAndInvalidNamesBeforeOpening() {
        for (name in listOf("", "../Power.log", "$session/Power.log", "Hearthstone_1\\2", "Other_1", "Hearthstone_1\n")) {
            assertFailsWith<IllegalArgumentException> { PostMatchLogSnapshot.read(name, 1, { error("must not open") }) }
        }
    }

    @Test fun rejectsUnknownEmptyAndOversizedBeforeOpening() {
        for (size in listOf(-1L, 0L, PostMatchLogSnapshot.MAX_BYTES + 1)) {
            assertFailsWith<IllegalArgumentException> { PostMatchLogSnapshot.read(session, size, { error("must not open") }) }
        }
    }

    @Test fun appendedBytesAreExcluded() {
        val snapshot = PostMatchLogSnapshot.read(session, 3, { ByteArrayInputStream("abcdef".toByteArray()) })
        assertEquals("abc", snapshot.copyBytes().decodeToString())
    }

    @Test fun shortenedSourceFailsAndCloses() {
        var closed = false
        val stream = object : ByteArrayInputStream(byteArrayOf(1, 2)) {
            override fun close() { closed = true; super.close() }
        }
        assertFailsWith<EOFException> { PostMatchLogSnapshot.read(session, 3, { stream }) }
        assertTrue(closed)
    }

    @Test fun missingOrUnreadableSourceIsNotSuccess() {
        assertFailsWith<java.io.FileNotFoundException> {
            PostMatchLogSnapshot.read(session, 3, { throw java.io.FileNotFoundException("not available") })
        }
    }

    @Test fun cancellationBeforeOpenAndMidReadPublishesNothing() {
        assertFailsWith<CancellationException> { PostMatchLogSnapshot.read(session, 1, { error("must not open") }, { true }) }
        var closed = false
        var cancel = false
        val stream = object : ByteArrayInputStream(byteArrayOf(1, 2)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                cancel = true
                return super.read(b, off, 1)
            }
            override fun close() { closed = true; super.close() }
        }
        assertFailsWith<CancellationException> { PostMatchLogSnapshot.read(session, 2, { stream }, { cancel }) }
        assertTrue(closed)
    }

    @Test fun stalledReadFailsAndCloses() {
        var closed = false
        val stream = object : InputStream() {
            override fun read(): Int = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int = 0
            override fun close() { closed = true }
        }
        assertFailsWith<IOException> { PostMatchLogSnapshot.read(session, 1, { stream }) }
        assertTrue(closed)
    }

    @Test fun repeatedAndRotatedNamesHaveContentBasedIdentity() {
        fun snapshot(name: String, value: String) = PostMatchLogSnapshot.read(name, value.length.toLong(), { ByteArrayInputStream(value.toByteArray()) })
        val first = snapshot(session, "abc")
        assertEquals(first.contentIdentity, snapshot(session, "abc").contentIdentity)
        assertEquals(first.contentIdentity, snapshot("Hearthstone_2", "abc").contentIdentity)
        assertNotEquals(first.contentIdentity, snapshot(session, "xyz").contentIdentity)
    }
}
