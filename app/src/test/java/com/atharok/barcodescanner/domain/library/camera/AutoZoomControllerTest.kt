package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoZoomControllerTest {

    private var clock = 0L
    private fun controller(max: Float = 6f) = AutoZoomController(minZoomRatio = 1f, maxZoomRatio = max, now = { clock })
    private fun square(side: Float) = arrayOf(ResultPoint(0f, 0f), ResultPoint(side, 0f), ResultPoint(side, side), ResultPoint(0f, side))

    @Test fun `small candidate zooms toward the target size`() {
        // 10x10 in 100x100 = 1% area; to reach 25% the linear size grows 5x.
        assertEquals(5f, controller().onCandidates(listOf(square(10f)), 1f, 100, 100)!!, 0.001f)
    }

    @Test fun `candidate already large enough does not zoom`() {
        assertNull(controller().onCandidates(listOf(square(50f)), 1f, 100, 100))
    }

    @Test fun `steps are throttled`() {
        val c = controller()
        assertTrue(c.onCandidates(listOf(square(10f)), 1f, 100, 100) != null)
        assertNull(c.onCandidates(listOf(square(10f)), 1f, 100, 100))
        clock += 300
        assertTrue(c.onCandidates(listOf(square(10f)), 1f, 100, 100) != null)
    }

    @Test fun `target is capped at the camera's max zoom`() {
        assertEquals(2f, controller(max = 2f).onCandidates(listOf(square(1f)), 1f, 100, 100))
    }

    @Test fun `manual zoom suppresses auto-zoom for the hold window`() {
        val c = controller()
        c.onManualZoom()
        clock += 2999
        assertNull(c.onCandidates(listOf(square(10f)), 1f, 100, 100))
        clock += 1
        assertTrue(c.onCandidates(listOf(square(10f)), 1f, 100, 100) != null)
    }

    @Test fun `after the timeout without a candidate it restores the pre-auto-zoom ratio`() {
        val c = controller()
        c.onCandidates(listOf(square(10f)), 1.5f, 100, 100)
        clock += 4999
        assertNull(c.onCandidates(emptyList(), 6f, 100, 100))
        clock += 1
        assertEquals(1.5f, c.onCandidates(emptyList(), 6f, 100, 100))
        assertNull(c.onCandidates(emptyList(), 1.5f, 100, 100))
    }

    @Test fun `never resets a zoom the user chose`() {
        val c = controller()
        c.onCandidates(listOf(square(10f)), 1f, 100, 100)
        c.onManualZoom()
        clock += 10_000
        assertNull(c.onCandidates(emptyList(), 3f, 100, 100))
    }

    @Test fun `no candidate and no prior auto-zoom leaves the zoom alone`() {
        clock += 10_000
        assertNull(controller().onCandidates(emptyList(), 2f, 100, 100))
    }
}
