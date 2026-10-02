package com.atharok.barcodescanner.domain.library.scan

import com.atharok.barcodescanner.wechatqr.WeChatQrNative
import com.atharok.barcodescanner.wechatqr.WeChatQrNativeResult
import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.ByteBuffer

class WeChatQrEngineTest {

    private fun quad(side: Float) = floatArrayOf(0f, 0f, side, 0f, side, side, 0f, side)

    // Stub the exact frame the test passes: Mockito's any() returns null, which Kotlin's
    // call-site null check rejects for the non-null ByteBuffer parameter.
    private val frame: ByteBuffer = ByteBuffer.allocateDirect(100 * 100)

    private fun engineReturning(result: WeChatQrNativeResult, initOk: Boolean = true): WeChatQrEngine {
        val native = mock(WeChatQrNative::class.java)
        `when`(native.nativeInit(null)).thenReturn(initOk)
        `when`(native.nativeDetectAndDecode(frame, 100, 100, 100)).thenReturn(result)
        return WeChatQrEngine(native, modelDir = null)
    }

    private fun scan(engine: WeChatQrEngine) = engine.scanGray(frame, 100, 100, 100)

    @Test fun `decoded text becomes a QR code result with its quad`() {
        val outcome = scan(engineReturning(WeChatQrNativeResult(arrayOf("hello"), arrayOf(quad(50f)), emptyArray())))

        assertEquals(1, outcome.codes.size)
        assertEquals(BarcodeFormat.QR_CODE, outcome.codes[0].barcodeFormat)
        assertEquals("hello", outcome.codes[0].text)
        assertEquals(50f, outcome.codes[0].resultPoints[2].x)
        assertTrue(outcome.candidates.isEmpty())
    }

    @Test fun `detector candidates are reported as candidates`() {
        val outcome = scan(engineReturning(WeChatQrNativeResult(emptyArray(), emptyArray(), arrayOf(quad(10f)))))

        assertTrue(outcome.codes.isEmpty())
        assertEquals(1, outcome.candidates.size)
    }

    @Test fun `init failure yields empty outcomes without calling the decoder`() {
        val outcome = scan(engineReturning(WeChatQrNativeResult(arrayOf("x"), arrayOf(quad(1f)), emptyArray()), initOk = false))

        assertTrue(outcome.codes.isEmpty())
        assertTrue(outcome.candidates.isEmpty())
    }
}
