package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/**
 * Runs [primary]; when it decodes nothing, escalates to [fallback] synchronously on the
 * caller's thread. Synchronous because a camera [ImageProxy] is closed as soon as the
 * analyzer returns, and a gallery bitmap gets exactly one scan. Camera frames are
 * rate-limited: one fallback per [candidateRetryFrames] misses while a candidate is visible,
 * per [emptyFrameThreshold] otherwise — except right after the fallback decoded something, when
 * it runs on every miss so a code only it can read keeps being reported frame after frame.
 * Bitmaps always fall back. Camera path is single-threaded.
 */
class TieredScanEngine(
    private val primary: ScanEngine,
    private val fallback: ScanEngine?,
    private val emptyFrameThreshold: Int = 10,
    private val candidateRetryFrames: Int = 2
) : ScanEngine {

    private var missesSinceFallback = 0
    private var fallbackHot = false

    override fun scan(image: ImageProxy): ScanOutcome {
        val outcome = primary.scan(image)
        if (outcome.codes.isNotEmpty() || fallback == null) {
            missesSinceFallback = 0
            fallbackHot = false
            return outcome
        }
        val due = when {
            fallbackHot -> 1
            outcome.candidates.isNotEmpty() -> candidateRetryFrames
            else -> emptyFrameThreshold
        }
        if (++missesSinceFallback < due) return outcome
        missesSinceFallback = 0
        val fallbackOutcome = fallback.scan(image)
        fallbackHot = fallbackOutcome.codes.isNotEmpty()
        return outcome + fallbackOutcome
    }

    override fun scan(bitmap: Bitmap): ScanOutcome {
        val outcome = primary.scan(bitmap)
        if (outcome.codes.isNotEmpty() || fallback == null) return outcome
        return outcome + fallback.scan(bitmap)
    }

    private operator fun ScanOutcome.plus(other: ScanOutcome) =
        ScanOutcome(codes + other.codes, candidates + other.candidates)
}
