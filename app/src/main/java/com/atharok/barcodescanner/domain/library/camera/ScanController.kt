package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.Result

sealed interface ScanUiState {
    data object Searching : ScanUiState
    data class Approaching(val targetZoomRatio: Float) : ScanUiState
    data class Picking(val results: List<Result>) : ScanUiState
    data class Found(val result: Result) : ScanUiState
}

/**
 * Turns one analyzed frame into a [ScanUiState]: 2+ codes -> picker; a single code only once
 * the same text is seen on [stabilityFrameCount] consecutive frames; otherwise approaching
 * (auto-zoom stepping) or searching. Call exactly once per frame.
 */
class ScanController(private val stabilityFrameCount: Int = 2) {

    private var lastText: String? = null
    private var stableCount = 0

    fun onFrame(codes: List<Result>, zoomStep: Float?): ScanUiState {
        if (codes.size == 1) return onSingleCode(codes[0])
        lastText = null
        stableCount = 0
        return when {
            codes.size > 1 -> ScanUiState.Picking(codes)
            zoomStep != null -> ScanUiState.Approaching(zoomStep)
            else -> ScanUiState.Searching
        }
    }

    private fun onSingleCode(result: Result): ScanUiState {
        stableCount = if (result.text == lastText) stableCount + 1 else 1
        lastText = result.text
        if (stableCount < stabilityFrameCount) return ScanUiState.Searching
        lastText = null
        stableCount = 0
        return ScanUiState.Found(result)
    }
}
