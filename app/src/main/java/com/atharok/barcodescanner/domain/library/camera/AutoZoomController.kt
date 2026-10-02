package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint
import kotlin.math.sqrt

/**
 * Picks a zoom ratio that grows a too-small candidate (< [triggerAreaRatio] of the frame)
 * toward [targetAreaRatio], at most one step per [stepIntervalMillis], and resets to
 * [minZoomRatio] after [resetTimeoutMillis] with no candidate. Paused during manual zoom.
 * The caller applies the ratio via `CameraControl.setZoomRatio`.
 */
class AutoZoomController(
    private val minZoomRatio: Float,
    private val maxZoomRatio: Float,
    private val targetAreaRatio: Float = 0.25f,
    private val triggerAreaRatio: Float = 0.10f,
    private val stepIntervalMillis: Long = 300L,
    private val resetTimeoutMillis: Long = 5000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private var paused = false
    private var lastStepMillis = Long.MIN_VALUE / 2
    private var lastCandidateMillis = now()

    fun onManualZoomActive() { paused = true }
    fun onManualZoomReleased() { paused = false }

    /** @return the zoom ratio to apply now, or null for no change. */
    fun onCandidates(candidates: List<Array<ResultPoint>>, currentZoomRatio: Float, frameWidth: Int, frameHeight: Int): Float? {
        if (paused) return null
        val t = now()
        val area = candidates.minOfOrNull { it.boundingBoxAreaRatio(frameWidth, frameHeight) }
        if (area == null) {
            val idle = t - lastCandidateMillis >= resetTimeoutMillis
            return minZoomRatio.takeIf { idle && currentZoomRatio != minZoomRatio }
        }
        lastCandidateMillis = t
        if (area <= 0f || area >= triggerAreaRatio || t - lastStepMillis < stepIntervalMillis) return null
        // Linear size scales with the square root of area.
        val target = (currentZoomRatio * sqrt(targetAreaRatio / area)).coerceIn(minZoomRatio, maxZoomRatio)
        if (target <= currentZoomRatio) return null
        lastStepMillis = t
        return target
    }
}
