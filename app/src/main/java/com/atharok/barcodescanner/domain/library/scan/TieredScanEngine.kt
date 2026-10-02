package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/**
 * Runs [primary]; when it decodes nothing, escalates to [fallback] synchronously on the
 * caller's thread. Synchronous because a camera [ImageProxy] is closed as soon as the
 * analyzer returns, and a gallery bitmap gets exactly one scan. Camera frames are
 * rate-limited: one fallback per [candidateRetryFrames] misses while a candidate is visible,
 * per [emptyFrameThreshold] otherwise. Bitmaps always fall back. Camera path is single-threaded.
 */
class TieredScanEngine(
    private val primary: ScanEngine,
    private val fallback: ScanEngine?,
    private val emptyFrameThreshold: Int = 10,
    private val candidateRetryFrames: Int = 2
) : ScanEngine {

    private var missesSinceFallback = 0

    override fun scan(image: ImageProxy): ScanOutcome {
        val outcome = primary.scan(image)
        if (outcome.codes.isNotEmpty() || fallback == null) {
            missesSinceFallback = 0
            return outcome
        }
        val due = if (outcome.candidates.isNotEmpty()) candidateRetryFrames else emptyFrameThreshold
        if (++missesSinceFallback < due) return outcome
        missesSinceFallback = 0
        return outcome + fallback.scan(image)
    }

    override fun scan(bitmap: Bitmap): ScanOutcome {
        val outcome = primary.scan(bitmap)
        if (outcome.codes.isNotEmpty() || fallback == null) return outcome
        return outcome + fallback.scan(bitmap)
    }

    private operator fun ScanOutcome.plus(other: ScanOutcome) =
        ScanOutcome(codes + other.codes, candidates + other.candidates)
}
