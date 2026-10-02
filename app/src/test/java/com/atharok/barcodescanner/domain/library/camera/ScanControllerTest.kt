package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanControllerTest {

    private fun code(text: String) = Result(text, null, emptyArray(), BarcodeFormat.QR_CODE)

    @Test fun `nothing seen is searching, a zoom step is approaching`() {
        val c = ScanController()
        assertEquals(ScanUiState.Searching, c.onFrame(emptyList(), null))
        assertEquals(ScanUiState.Approaching(2.5f), c.onFrame(emptyList(), 2.5f))
    }

    @Test fun `a single code is found only on the second consecutive matching frame`() {
        val c = ScanController()
        assertEquals(ScanUiState.Searching, c.onFrame(listOf(code("a")), null))
        val second = c.onFrame(listOf(code("a")), null)
        assertTrue(second is ScanUiState.Found && second.result.text == "a")
    }

    @Test fun `a different code or a gap restarts stability`() {
        val c = ScanController()
        c.onFrame(listOf(code("a")), null)
        assertEquals(ScanUiState.Searching, c.onFrame(listOf(code("b")), null))
        c.onFrame(emptyList(), null)
        assertEquals(ScanUiState.Searching, c.onFrame(listOf(code("b")), null))
    }

    @Test fun `two codes in one frame open the picker immediately`() {
        val state = ScanController().onFrame(listOf(code("a"), code("b")), null)
        assertTrue(state is ScanUiState.Picking && state.results.size == 2)
    }
}
