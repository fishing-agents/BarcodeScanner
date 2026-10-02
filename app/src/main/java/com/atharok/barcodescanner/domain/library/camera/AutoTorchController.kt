package com.atharok.barcodescanner.domain.library.camera

/**
 * Requests the torch on below [lowThreshold] mean luma and off above [highThreshold], each
 * sustained for [debounceMillis] so it never flickers. A manual torch toggle disables auto
 * behavior for the session. The caller applies the decision via `CameraControl.enableTorch`.
 */
class AutoTorchController(
    private val hasFlash: Boolean,
    private val lowThreshold: Int = 40,
    private val highThreshold: Int = 110,
    private val debounceMillis: Long = 1500L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private var manuallyControlled = false
    private var pendingState: Boolean? = null
    private var pendingSinceMillis = 0L

    fun onManualTorchToggle() {
        manuallyControlled = true
    }

    /** @return true/false to switch the torch on/off, or null for no change. */
    fun onLuma(meanLuma: Int): Boolean? {
        if (!hasFlash || manuallyControlled) return null
        val wanted = when {
            meanLuma < lowThreshold -> true
            meanLuma > highThreshold -> false
            else -> null
        }
        if (wanted != pendingState) {
            pendingState = wanted
            pendingSinceMillis = now()
            return null
        }
        return wanted?.takeIf { now() - pendingSinceMillis >= debounceMillis }
    }
}
