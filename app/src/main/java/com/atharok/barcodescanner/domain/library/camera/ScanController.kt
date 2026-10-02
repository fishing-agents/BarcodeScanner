package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.Result

sealed interface ScanUiState {
    data object Searching : ScanUiState
    data class Approaching(val targetZoomRatio: Float) : ScanUiState
    data class Picking(val results: List<Result>) : ScanUiState
    data class Found(val result: Result) : ScanUiState
}

/**
 * Turns one analyzed frame into a [ScanUiState]: 2+ codes -> picker; a single code once the
 * same text has been seen on [stabilityFrameCount] frames, allowing up to [maxGapFrames] empty
 * frames in between (the WeChatQRCode fallback only runs on some frames, so a code only it can
 * read alternates with empty frames); otherwise approaching (auto-zoom) or searching.
 * Call exactly once per frame.
 */
class ScanController(private val stabilityFrameCount: Int = 2, private val maxGapFrames: Int = 2) {

    private var lastText: String? = null
    private var sightings = 0
    private var gapFrames = 0

    fun onFrame(codes: List<Result>, zoomStep: Float?): ScanUiState {
        if (codes.size == 1) return onSingleCode(codes[0])
        if (codes.isEmpty() && ++gapFrames <= maxGapFrames) {
            return zoomStep?.let(ScanUiState::Approaching) ?: ScanUiState.Searching
        }
        reset()
        return when {
            codes.size > 1 -> ScanUiState.Picking(codes)
            zoomStep != null -> ScanUiState.Approaching(zoomStep)
            else -> ScanUiState.Searching
        }
    }

    private fun onSingleCode(result: Result): ScanUiState {
        sightings = if (result.text == lastText) sightings + 1 else 1
        lastText = result.text
        gapFrames = 0
        if (sightings < stabilityFrameCount) return ScanUiState.Searching
        reset()
        return ScanUiState.Found(result)
    }

    private fun reset() {
        lastText = null
        sightings = 0
        gapFrames = 0
    }
}
