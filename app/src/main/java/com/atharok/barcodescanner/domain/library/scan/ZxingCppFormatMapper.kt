package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.BarcodeFormat
import zxingcpp.BarcodeReader.Format

/**
 * Maps zxing-cpp's [Format] onto ZXing core's [BarcodeFormat], which history, Room and
 * BarcodeType already understand. zxing-cpp 3.x returns *specific variants* from a read
 * (e.g. QR_CODE_MODEL_2, ISBN, ITF_14), so every variant is listed, not just families.
 * Formats ZXing core 3.3.3 cannot represent (Micro QR, rMQR, DataBar Limited, Telepen,
 * Micro PDF417, Aztec Rune, DX film edge, EAN-2/5 add-ons) are absent and dropped.
 */
object ZxingCppFormatMapper {

    private val MAPPING: Map<Format, BarcodeFormat> = mapOf(
        Format.AZTEC to BarcodeFormat.AZTEC,
        Format.AZTEC_CODE to BarcodeFormat.AZTEC,
        Format.CODABAR to BarcodeFormat.CODABAR,
        Format.CODE_39 to BarcodeFormat.CODE_39,
        Format.CODE_39_STD to BarcodeFormat.CODE_39,
        Format.CODE_39_EXT to BarcodeFormat.CODE_39,
        Format.CODE_32 to BarcodeFormat.CODE_39,
        Format.PZN to BarcodeFormat.CODE_39,
        Format.CODE_93 to BarcodeFormat.CODE_93,
        Format.CODE_128 to BarcodeFormat.CODE_128,
        Format.DATA_BAR_OMNI to BarcodeFormat.RSS_14,
        Format.DATA_BAR_STK to BarcodeFormat.RSS_14,
        Format.DATA_BAR_STK_OMNI to BarcodeFormat.RSS_14,
        Format.DATA_BAR_EXP to BarcodeFormat.RSS_EXPANDED,
        Format.DATA_BAR_EXP_STK to BarcodeFormat.RSS_EXPANDED,
        Format.DATA_MATRIX to BarcodeFormat.DATA_MATRIX,
        Format.EAN_8 to BarcodeFormat.EAN_8,
        Format.EAN_13 to BarcodeFormat.EAN_13,
        Format.ISBN to BarcodeFormat.EAN_13,
        Format.ITF to BarcodeFormat.ITF,
        Format.ITF_14 to BarcodeFormat.ITF,
        Format.MAXI_CODE to BarcodeFormat.MAXICODE,
        Format.PDF_417 to BarcodeFormat.PDF_417,
        Format.COMPACT_PDF_417 to BarcodeFormat.PDF_417,
        Format.QR_CODE to BarcodeFormat.QR_CODE,
        Format.QR_CODE_MODEL_1 to BarcodeFormat.QR_CODE,
        Format.QR_CODE_MODEL_2 to BarcodeFormat.QR_CODE,
        Format.UPC_A to BarcodeFormat.UPC_A,
        Format.UPC_E to BarcodeFormat.UPC_E
    )

    /** Formats to request via [zxingcpp.BarcodeReader.Options.formats]: only ones that survive mapping. */
    val SUPPORTED: Set<Format> = MAPPING.keys

    fun toBarcodeFormat(format: Format): BarcodeFormat? = MAPPING[format]
}
