package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Point
import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import zxingcpp.BarcodeReader

class ZxingCppResultMapperTest {

    // android.graphics.Point's constructor is a throwing stub on the JVM test runner; a mock reads as (0, 0).
    private val point: Point = mock(Point::class.java)
    private val position = BarcodeReader.Position(point, point, point, point, 0.0)

    private fun fakeResult(
        format: BarcodeReader.Format = BarcodeReader.Format.QR_CODE_MODEL_2,
        text: String? = "hello",
        error: BarcodeReader.Error? = null
    ) = BarcodeReader.Result(
        format = format,
        bytes = null,
        text = text,
        contentType = BarcodeReader.ContentType.TEXT,
        position = position,
        orientation = 0,
        ecLevel = null,
        symbologyIdentifier = null,
        sequenceSize = -1,
        sequenceIndex = -1,
        sequenceId = null,
        readerInit = false,
        lineCount = 0,
        error = error
    )

    @Test fun `decoded result becomes a code`() {
        val outcome = ZxingCppResultMapper.toOutcome(listOf(fakeResult()))

        assertEquals(1, outcome.codes.size)
        assertTrue(outcome.candidates.isEmpty())
        assertEquals(BarcodeFormat.QR_CODE, outcome.codes[0].barcodeFormat)
        assertEquals("hello", outcome.codes[0].text)
        assertEquals(4, outcome.codes[0].resultPoints.size)
    }

    @Test fun `errored result becomes a candidate, not a code`() {
        val outcome = ZxingCppResultMapper.toOutcome(
            listOf(fakeResult(text = null, error = BarcodeReader.Error(BarcodeReader.ErrorType.FORMAT, "bad")))
        )

        assertTrue(outcome.codes.isEmpty())
        assertEquals(1, outcome.candidates.size)
    }

    @Test fun `unmappable format is dropped rather than surfaced`() {
        val outcome = ZxingCppResultMapper.toOutcome(listOf(fakeResult(format = BarcodeReader.Format.RMQR_CODE)))

        assertTrue(outcome.codes.isEmpty())
        assertTrue(outcome.candidates.isEmpty())
    }
}
