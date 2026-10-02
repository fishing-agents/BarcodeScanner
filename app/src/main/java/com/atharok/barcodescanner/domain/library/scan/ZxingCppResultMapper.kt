package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.Result
import com.google.zxing.ResultPoint
import zxingcpp.BarcodeReader

/** Converts zxing-cpp results into a [ScanOutcome]; kept separate from the native reader so it is JVM-testable. */
internal object ZxingCppResultMapper {

    fun toOutcome(results: List<BarcodeReader.Result>): ScanOutcome {
        val codes = mutableListOf<Result>()
        val candidates = mutableListOf<Array<ResultPoint>>()
        for (r in results) {
            val points = r.position.toResultPoints()
            if (r.error != null) {
                candidates += points
                continue
            }
            val format = ZxingCppFormatMapper.toBarcodeFormat(r.format) ?: continue
            codes += Result(r.text.orEmpty(), r.bytes, points, format)
        }
        return ScanOutcome(codes, candidates)
    }

    private fun BarcodeReader.Position.toResultPoints(): Array<ResultPoint> = arrayOf(
        ResultPoint(topLeft.x.toFloat(), topLeft.y.toFloat()),
        ResultPoint(topRight.x.toFloat(), topRight.y.toFloat()),
        ResultPoint(bottomRight.x.toFloat(), bottomRight.y.toFloat()),
        ResultPoint(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())
    )
}
