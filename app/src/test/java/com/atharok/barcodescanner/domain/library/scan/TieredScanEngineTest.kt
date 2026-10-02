package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

private val FAKE_IMAGE: ImageProxy = mock(ImageProxy::class.java)
private val FAKE_BITMAP: Bitmap = mock(Bitmap::class.java)

private class FakeEngine(private val outcomes: MutableList<ScanOutcome> = mutableListOf()) : ScanEngine {
    var calls = 0
    private fun next(): ScanOutcome { calls++; return outcomes.removeFirstOrNull() ?: ScanOutcome.EMPTY }
    override fun scan(image: ImageProxy) = next()
    override fun scan(bitmap: Bitmap) = next()
}

private fun decoded() = ScanOutcome(listOf(Result("x", null, arrayOf(ResultPoint(0f, 0f)), BarcodeFormat.QR_CODE)), emptyList())
private fun withCandidate() = ScanOutcome(emptyList(), listOf(Array(4) { ResultPoint(it.toFloat(), 0f) }))

class TieredScanEngineTest {

    @Test fun `primary decode short-circuits, fallback never runs`() {
        val fallback = FakeEngine()
        val outcome = TieredScanEngine(FakeEngine(mutableListOf(decoded())), fallback).scan(FAKE_IMAGE)

        assertEquals(1, outcome.codes.size)
        assertEquals(0, fallback.calls)
    }

    @Test fun `a visible candidate triggers the fallback and its result is returned in the same call`() {
        val fallback = FakeEngine(mutableListOf(decoded()))
        val tiered = TieredScanEngine(FakeEngine(MutableList(5) { withCandidate() }), fallback, candidateRetryFrames = 2)

        assertTrue(tiered.scan(FAKE_IMAGE).codes.isEmpty())
        val second = tiered.scan(FAKE_IMAGE)

        assertEquals(1, fallback.calls)
        assertEquals(1, second.codes.size)
    }

    @Test fun `fallback is rate-limited while the candidate stays undecodable`() {
        val fallback = FakeEngine()
        val tiered = TieredScanEngine(FakeEngine(MutableList(6) { withCandidate() }), fallback, candidateRetryFrames = 2)

        repeat(6) { tiered.scan(FAKE_IMAGE) }

        assertEquals(3, fallback.calls)
    }

    @Test fun `no candidate waits for the empty-frame threshold before falling back`() {
        val fallback = FakeEngine()
        val tiered = TieredScanEngine(FakeEngine(), fallback, emptyFrameThreshold = 3)

        repeat(2) { tiered.scan(FAKE_IMAGE) }
        assertEquals(0, fallback.calls)

        tiered.scan(FAKE_IMAGE)
        assertEquals(1, fallback.calls)
    }

    @Test fun `after the fallback decodes, it runs again on the very next miss`() {
        // A code only the CNN can read (zxing-cpp finds no candidate) must be re-read every
        // frame, or ScanController never sees it twice in a row.
        val fallback = FakeEngine(MutableList(3) { decoded() })
        val tiered = TieredScanEngine(FakeEngine(), fallback, emptyFrameThreshold = 3)

        repeat(3) { tiered.scan(FAKE_IMAGE) }
        assertEquals(1, fallback.calls)
        assertEquals(1, tiered.scan(FAKE_IMAGE).codes.size)
        assertEquals(2, fallback.calls)
    }

    @Test fun `once the fallback stops decoding it falls back to the normal rate`() {
        val fallback = FakeEngine(mutableListOf(decoded()))
        val tiered = TieredScanEngine(FakeEngine(), fallback, emptyFrameThreshold = 3)

        repeat(4) { tiered.scan(FAKE_IMAGE) } // hit on 3rd, retried on 4th (miss)
        assertEquals(2, fallback.calls)
        repeat(2) { tiered.scan(FAKE_IMAGE) }
        assertEquals(2, fallback.calls)
    }

    @Test fun `missing fallback degrades to primary-only`() {
        val outcome = TieredScanEngine(FakeEngine(mutableListOf(withCandidate())), fallback = null).scan(FAKE_IMAGE)

        assertTrue(outcome.codes.isEmpty())
        assertEquals(1, outcome.candidates.size)
    }

    @Test fun `bitmap miss runs the fallback inline and returns its codes`() {
        val outcome = TieredScanEngine(FakeEngine(), FakeEngine(mutableListOf(decoded()))).scan(FAKE_BITMAP)

        assertEquals(1, outcome.codes.size)
    }

    @Test fun `bitmap hit skips the fallback`() {
        val fallback = FakeEngine()
        TieredScanEngine(FakeEngine(mutableListOf(decoded())), fallback).scan(FAKE_BITMAP)

        assertEquals(0, fallback.calls)
    }
}
