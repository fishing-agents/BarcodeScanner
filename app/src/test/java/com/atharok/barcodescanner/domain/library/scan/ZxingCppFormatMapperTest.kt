package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import zxingcpp.BarcodeReader.Format

class ZxingCppFormatMapperTest {

    @Test fun `decoded variants map to their ZXing core format`() {
        // zxing-cpp 3.x only ever returns specific variants from a read, never the family.
        assertEquals(BarcodeFormat.QR_CODE, ZxingCppFormatMapper.toBarcodeFormat(Format.QR_CODE_MODEL_2))
        assertEquals(BarcodeFormat.QR_CODE, ZxingCppFormatMapper.toBarcodeFormat(Format.QR_CODE_MODEL_1))
        assertEquals(BarcodeFormat.EAN_13, ZxingCppFormatMapper.toBarcodeFormat(Format.ISBN))
        assertEquals(BarcodeFormat.ITF, ZxingCppFormatMapper.toBarcodeFormat(Format.ITF_14))
        assertEquals(BarcodeFormat.AZTEC, ZxingCppFormatMapper.toBarcodeFormat(Format.AZTEC_CODE))
        assertEquals(BarcodeFormat.PDF_417, ZxingCppFormatMapper.toBarcodeFormat(Format.COMPACT_PDF_417))
        assertEquals(BarcodeFormat.RSS_14, ZxingCppFormatMapper.toBarcodeFormat(Format.DATA_BAR_OMNI))
        assertEquals(BarcodeFormat.RSS_EXPANDED, ZxingCppFormatMapper.toBarcodeFormat(Format.DATA_BAR_EXP_STK))
        assertEquals(BarcodeFormat.MAXICODE, ZxingCppFormatMapper.toBarcodeFormat(Format.MAXI_CODE))
    }

    @Test fun `formats with no ZXing core 3_3_3 counterpart map to null`() {
        assertNull(ZxingCppFormatMapper.toBarcodeFormat(Format.MICRO_QR_CODE))
        assertNull(ZxingCppFormatMapper.toBarcodeFormat(Format.RMQR_CODE))
        assertNull(ZxingCppFormatMapper.toBarcodeFormat(Format.DATA_BAR_LTD))
        assertNull(ZxingCppFormatMapper.toBarcodeFormat(Format.TELEPEN))
    }

    @Test fun `reader is not asked to search for formats that would be dropped`() {
        assertTrue(ZxingCppFormatMapper.SUPPORTED.none { ZxingCppFormatMapper.toBarcodeFormat(it) == null })
        assertTrue(Format.MICRO_QR_CODE !in ZxingCppFormatMapper.SUPPORTED)
    }
}
