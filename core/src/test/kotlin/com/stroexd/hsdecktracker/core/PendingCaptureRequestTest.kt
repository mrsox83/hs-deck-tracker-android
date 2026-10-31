package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.PendingCaptureRequest
import kotlin.test.*

/** Virtual handler drives the actual callback slot used by MediaProjectionSource. No sleeps. */
class PendingCaptureRequestTest {
    private class Handler {
        data class Task(val runnable: Runnable, val at: Long)
        var now = 0L
        var accepts = true
        var postThrows = false
        var removeThrows = false
        val tasks = mutableListOf<Task>()
        fun post(task: Runnable, delay: Long): Boolean {
            if (postThrows) error("Synthetic scheduler failure")
            if (accepts) tasks += Task(task, now + delay)
            return accepts
        }
        fun remove(task: Runnable) {
            if (removeThrows) error("Synthetic cleanup failure")
            tasks.removeAll { it.runnable === task }
        }
        fun next() {
            val task = tasks.minBy { it.at }
            tasks.remove(task);now = task.at;task.runnable.run()
        }
    }
    private fun slot(h: Handler, timeout: () -> Boolean = { false }) =
        PendingCaptureRequest<String>(700, h::post, h::remove, timeout)

    @Test fun normalFrameCompletesOnceAndCancelsDeadline() {
        val h=Handler();val s=slot(h);val seen=mutableListOf<String?>()
        assertTrue(s.begin { seen+=it });s.take()?.invoke("frame")
        assertEquals<List<String?>>(listOf("frame"),seen);assertTrue(h.tasks.isEmpty());assertNull(s.take())
    }
    @Test fun noImageCompletesAtDeadline() {
        val h=Handler();val s=slot(h);val seen=mutableListOf<String?>()
        s.begin { seen+=it };h.next()
        assertEquals(700L,h.now);assertEquals<List<String?>>(listOf(null),seen);assertNull(s.take())
    }
    @Test fun fallbackWithoutImageHasOneBoundedGraceNotUnarmedWait() {
        val h=Handler();val s=slot(h) { true };val seen=mutableListOf<String?>()
        s.begin { seen+=it };h.next()
        assertTrue(seen.isEmpty());assertEquals(1,h.tasks.size)
        h.next();assertEquals(1400L,h.now);assertEquals<List<String?>>(listOf(null),seen);assertTrue(h.tasks.isEmpty())
    }
    @Test fun imageDuringGraceWinsExactlyOnce() {
        val h=Handler();val s=slot(h) { true };val seen=mutableListOf<String?>()
        s.begin { seen+=it };h.next();val old=h.tasks.single().runnable
        s.take()?.invoke("fallback frame");old.run()
        assertEquals<List<String?>>(listOf("fallback frame"),seen);assertTrue(h.tasks.isEmpty())
    }
    @Test fun lateImageAfterTimeoutHasNoCallbackAndNextRequestWorks() {
        val h=Handler();val s=slot(h);val first=mutableListOf<String?>();val second=mutableListOf<String?>()
        s.begin { first+=it };h.next();assertNull(s.take())
        assertTrue(s.begin { second+=it });s.take()?.invoke("new frame")
        assertEquals<List<String?>>(listOf(null),first);assertEquals<List<String?>>(listOf("new frame"),second)
    }
    @Test fun stopOrResizeCompletionCancelsOriginalAndGraceDeadlines() {
        for (inGrace in listOf(false,true)) {
            val h=Handler();val s=slot(h) { true };val seen=mutableListOf<String?>()
            s.begin { seen+=it };if(inGrace)h.next();val stale=h.tasks.single().runnable
            s.take()?.invoke(null);stale.run();s.take()?.invoke(null)
            assertEquals<List<String?>>(listOf(null),seen);assertTrue(h.tasks.isEmpty())
        }
    }
    @Test fun staleTimeoutCannotCompleteNewerRequest() {
        val h=Handler();val s=slot(h);val seen=mutableListOf<String?>()
        s.begin { seen+=it };val stale=h.tasks.single().runnable;s.take()?.invoke("first")
        s.begin { seen+=it };stale.run();assertEquals<List<String?>>(listOf("first"),seen)
        s.take()?.invoke("second");assertEquals<List<String?>>(listOf("first","second"),seen)
    }
    @Test fun overlappingRequestIsRejectedWithoutLosingOriginal() {
        val h=Handler();val s=slot(h);val seen=mutableListOf<String?>()
        assertTrue(s.begin { seen+="first:$it" })
        assertFalse(s.begin { seen+="second:$it" });s.take()?.invoke("frame")
        assertEquals<List<String?>>(listOf("second:null","first:frame"),seen)
    }
    @Test fun schedulerRefusalOrExceptionCompletesInsteadOfOrphaning() {
        for (throws in listOf(false,true)) {
            val h=Handler().apply { accepts=false;postThrows=throws };val s=slot(h);val seen=mutableListOf<String?>()
            assertFalse(s.begin { seen+=it });assertEquals<List<String?>>(listOf(null),seen);assertNull(s.take())
        }
    }
    @Test fun graceSchedulingFailureStillCompletesOnce() {
        val h=Handler();val s=slot(h) { h.accepts=false;true };val seen=mutableListOf<String?>()
        s.begin { seen+=it };h.next();assertEquals<List<String?>>(listOf(null),seen);assertNull(s.take())
    }
    @Test fun timeoutHookFailureStillCompletesOnce() {
        val h=Handler();val s=slot(h) { error("Synthetic attachment failure") };val seen=mutableListOf<String?>()
        s.begin { seen+=it };h.next();assertEquals<List<String?>>(listOf(null),seen);assertNull(s.take())
    }
    @Test fun failedRemovalLeavesOnlyHarmlessOldDeadline() {
        val h=Handler();val s=slot(h);val seen=mutableListOf<String?>()
        s.begin { seen+=it };h.removeThrows=true;s.take()?.invoke("frame")
        h.next();assertEquals<List<String?>>(listOf("frame"),seen);assertNull(s.take())
    }
    @Test fun repeatedNoImageAndFallbackCyclesAlwaysMakeProgress() {
        val h=Handler();var count=0;val s=slot(h) { true }
        repeat(20) { assertTrue(s.begin { assertNull(it);count++ });h.next();h.next() }
        assertEquals(20,count);assertEquals(28000L,h.now);assertTrue(h.tasks.isEmpty())
    }
    @Test fun callbackCanReenterWithoutOldDeadlineOwningNewRequest() {
        val h=Handler();val s=slot(h);val seen=mutableListOf<String?>()
        s.begin { seen+=it;assertTrue(s.begin { seen+=it }) };val stale=h.tasks.single().runnable
        s.take()?.invoke("first");stale.run();s.take()?.invoke("second")
        assertEquals<List<String?>>(listOf("first","second"),seen)
    }
    @Test fun callbackExceptionDoesNotLeavePendingRequest() {
        val h=Handler();val s=slot(h)
        s.begin { error("Synthetic consumer failure") }
        assertFailsWith<IllegalStateException> { s.take()?.invoke("frame") }
        assertNull(s.take());assertTrue(h.tasks.isEmpty());assertTrue(s.begin {})
    }
}
