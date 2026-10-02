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

    @Test fun `no zoom while the user is zooming manually, resumes on release`() {
        val c = controller()
        c.onManualZoomActive()
        assertNull(c.onCandidates(listOf(square(10f)), 1f, 100, 100))
        c.onManualZoomReleased()
        assertTrue(c.onCandidates(listOf(square(10f)), 1f, 100, 100) != null)
    }

    @Test fun `resets to minimum zoom once no candidate is seen for the timeout`() {
        val c = controller()
        c.onCandidates(listOf(square(10f)), 1f, 100, 100)
        clock += 4999
        assertNull(c.onCandidates(emptyList(), 3f, 100, 100))
        clock += 1
        assertEquals(1f, c.onCandidates(emptyList(), 3f, 100, 100))
    }
}
