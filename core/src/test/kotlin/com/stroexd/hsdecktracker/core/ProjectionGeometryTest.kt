package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.vision.*
import kotlin.test.*

class ProjectionGeometryTest {
    private fun gate(geometry: CaptureContentGeometry?, projection: Boolean = true, interactive: Boolean = true, active: Boolean = true,
        width: Int = 1080, height: Int = 2520) = captureSkipReason(interactive, width, height,
        projection = projection, content = geometry, isActive = { active })
    private fun source() = ProjectionGeometry().apply { start(); resized(2520, 1080, 2000, 856, 100) }

    @Test fun `current known landscape projection proceeds despite portrait window metrics`() {
        assertNull(gate(source().current(101)))
    }
    @Test fun `accessibility retains physical orientation policy regardless of source geometry`() {
        assertEquals(2, gate(source().current(101), projection = false))
        assertNull(gate(null, projection = false, width = 2520, height = 1080))
    }
    @Test fun `unknown source geometry keeps conservative physical fallback`() {
        val state = ProjectionGeometry().apply { start() }
        assertEquals(2, gate(state.current(1)))
        assertNull(gate(state.current(1), width = 2520, height = 1080))
        state.delivered(2000, 856, 2, 1000)
        assertNull(state.current(2)) // Reader shape alone never bootstraps trusted app geometry.
    }
    @Test fun `known portrait source is skipped even with landscape physical metrics`() {
        val state = ProjectionGeometry().apply { start(); resized(1080, 2520, 856, 2000, 1) }
        assertEquals(2, gate(state.current(2), width = 2520, height = 1080))
    }
    @Test fun `expired geometry and backwards clock do not override physical guard`() {
        val state = source()
        assertNotNull(state.current(10_100))
        assertNull(state.current(10_101))
        assertEquals(2, gate(state.current(10_101)))
        assertNull(state.current(99))
    }
    @Test fun `matching newly timestamped frames renew freshness without extra capture`() {
        val state = source()
        state.delivered(2000, 856, 9_000, 1000)
        assertNotNull(state.current(15_000))
        state.delivered(2000, 856, 15_000, 2000)
        assertNotNull(state.current(24_000))
    }
    @Test fun `repeated or missing image timestamps cannot renew geometry indefinitely`() {
        val state = source()
        state.delivered(2000, 856, 200, 1000)
        state.delivered(2000, 856, 9_000, 1000)
        state.delivered(2000, 856, 9_001, -1)
        assertNull(state.current(10_201))
    }
    @Test fun `resize transition clears old orientation until applied geometry arrives`() {
        val state = source(); state.resizing()
        assertNull(state.current(101))
        state.resized(1080, 2520, 856, 2000, 102)
        assertEquals(2, gate(state.current(103)))
        state.resized(2520, 1080, 2000, 856, 104)
        assertNull(gate(state.current(105)))
    }
    @Test fun `invalid square or mismatched reader geometry remains unknown`() {
        for ((w,h,rw,rh) in listOf(listOf(0,1080,2000,856),listOf(1080,1080,1000,1000),listOf(2520,1080,856,2000))) {
            val state = source(); state.resized(w,h,rw,rh,101)
            assertNull(state.current(102))
        }
        assertEquals(2, gate(CaptureContentGeometry(0,0)))
    }
    @Test fun `delivered reader mismatch invalidates rather than relabeling geometry`() {
        val state = source(); state.delivered(856,2000,101,1000)
        assertNull(state.current(102))
    }
    @Test fun `stop and new start reject previous session geometry and late callbacks`() {
        val state = source(); state.stop()
        state.resized(2520,1080,2000,856,101)
        state.delivered(2000,856,102,1000)
        assertNull(state.current(102))
        state.start(); assertNull(state.current(103))
    }
    @Test fun `screen off and inactive source still block trusted landscape content`() {
        val geometry = source().current(101)
        assertEquals(1,gate(geometry,interactive=false))
        assertEquals(3,gate(geometry,active=false))
    }
    @Test fun `long screen off or inactive interval expires geometry and requires fresh evidence`() {
        val state = source()
        assertEquals(1,gate(state.current(101),interactive=false))
        assertEquals(3,gate(state.current(102),active=false))
        assertEquals(2,gate(state.current(20_000)))
        // A fresh real source callback can restore eligibility; no blind probe is introduced.
        state.resized(2520,1080,2000,856,20_001)
        assertNull(gate(state.current(20_002)))
    }
    @Test fun `conflicting mask coordinates preserve delivery but forbid interpreted observations`() {
        assertNull(gate(source().current(101)))
        assertEquals(4,projectionProcessingSkipReason(2000,856,1080,2520,true))
        assertNull(projectionProcessingSkipReason(2000,856,1080,2520,false))
    }
    @Test fun `compatible legacy mask mapping permits rounded source scale`() {
        assertNull(projectionProcessingSkipReason(2000,856,2520,1080,true))
        assertEquals(4,projectionProcessingSkipReason(2000,856,0,0,true))
        assertEquals(4,projectionProcessingSkipReason(2000,856,1920,1080,true))
    }
    @Test fun `portrait delivered content is not parsed even if eligibility raced a resize`() {
        assertEquals(2,projectionProcessingSkipReason(856,2000,2520,1080,false))
    }
}
