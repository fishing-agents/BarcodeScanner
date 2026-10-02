package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanOutcomeTest {

    @Test fun `EMPTY has no codes and no candidates`() {
        assertTrue(ScanOutcome.EMPTY.codes.isEmpty())
        assertTrue(ScanOutcome.EMPTY.candidates.isEmpty())
    }

    @Test fun `holds decoded codes and undecoded candidates independently`() {
        val code = Result("hi", null, arrayOf(ResultPoint(0f, 0f)), BarcodeFormat.QR_CODE)
        val candidate = arrayOf(ResultPoint(0f, 0f), ResultPoint(1f, 0f), ResultPoint(1f, 1f), ResultPoint(0f, 1f))
        val outcome = ScanOutcome(codes = listOf(code), candidates = listOf(candidate))

        assertEquals(1, outcome.codes.size)
        assertEquals(1, outcome.candidates.size)
    }
}
