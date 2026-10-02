package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.Result
import com.google.zxing.ResultPoint

/**
 * One camera frame's (or one gallery image's) scan result. Reuses ZXing's own
 * [Result]/[ResultPoint] so history, Room, intents and ResultParser consume codes from
 * either engine unchanged. [candidates] are symbols detected but not decoded.
 */
data class ScanOutcome(
    val codes: List<Result>,
    val candidates: List<Array<ResultPoint>>
) {
    companion object {
        val EMPTY = ScanOutcome(emptyList(), emptyList())
    }
}
