package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultPointGeometryTest {

    private fun square(side: Float) = arrayOf(ResultPoint(0f, 0f), ResultPoint(side, 0f), ResultPoint(side, side), ResultPoint(0f, side))

    @Test fun `area ratio of a quarter-frame square is one quarter`() {
        assertEquals(0.25f, square(50f).boundingBoxAreaRatio(100, 100), 0.001f)
    }

    @Test fun `area ratio is clamped to the frame`() {
        assertEquals(1f, square(200f).boundingBoxAreaRatio(100, 100), 0.001f)
    }

    @Test fun `empty quad or empty frame has zero area`() {
        assertEquals(0f, emptyArray<ResultPoint>().boundingBoxAreaRatio(100, 100), 0f)
        assertEquals(0f, square(10f).boundingBoxAreaRatio(0, 100), 0f)
    }

    @Test fun `centroid of a square is its center`() {
        val c = square(10f).centroid()
        assertEquals(5f, c.x, 0.001f)
        assertEquals(5f, c.y, 0.001f)
    }
}
