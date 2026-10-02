package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint
import kotlin.math.sqrt

/**
 * Picks a zoom ratio that grows a too-small candidate (< [triggerAreaRatio] of the frame)
 * toward [targetAreaRatio], at most one step per [stepIntervalMillis]. After
 * [resetTimeoutMillis] with no candidate it restores the ratio that was in effect before it
 * started — it only ever undoes its own zoom. A manual zoom hands control back to the user
 * and suppresses auto-zoom for [manualHoldMillis]. The caller applies the ratio via
 * `CameraControl.setZoomRatio`.
 */
class AutoZoomController(
    private val minZoomRatio: Float,
    private val maxZoomRatio: Float,
    private val targetAreaRatio: Float = 0.25f,
    private val triggerAreaRatio: Float = 0.10f,
    private val stepIntervalMillis: Long = 300L,
    private val resetTimeoutMillis: Long = 5000L,
    private val manualHoldMillis: Long = 3000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private var lastStepMillis = Long.MIN_VALUE / 2
    private var lastManualMillis = Long.MIN_VALUE / 2
    private var lastCandidateMillis = now()
    /** Ratio to restore once the candidate is gone; non-null only while auto-zoom is in effect. */
    private var restoreRatio: Float? = null

    /** Call on every user-initiated zoom change (pinch, double-tap, slider). */
    fun onManualZoom() {
        lastManualMillis = now()
        restoreRatio = null
    }

    /** @return the zoom ratio to apply now, or null for no change. */
    fun onCandidates(candidates: List<Array<ResultPoint>>, currentZoomRatio: Float, frameWidth: Int, frameHeight: Int): Float? {
        val t = now()
        if (t - lastManualMillis < manualHoldMillis) return null
        val area = candidates.minOfOrNull { it.boundingBoxAreaRatio(frameWidth, frameHeight) }
        if (area == null) {
            val restore = restoreRatio ?: return null
            if (t - lastCandidateMillis < resetTimeoutMillis) return null
            restoreRatio = null
            return restore
        }
        lastCandidateMillis = t
        if (area <= 0f || area >= triggerAreaRatio || t - lastStepMillis < stepIntervalMillis) return null
        // Linear size scales with the square root of area.
        val target = (currentZoomRatio * sqrt(targetAreaRatio / area)).coerceIn(minZoomRatio, maxZoomRatio)
        if (target <= currentZoomRatio) return null
        lastStepMillis = t
        if (restoreRatio == null) restoreRatio = currentZoomRatio
        return target
    }
}
