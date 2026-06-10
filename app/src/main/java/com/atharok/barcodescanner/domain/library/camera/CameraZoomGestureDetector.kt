package com.atharok.barcodescanner.domain.library.camera

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange
import androidx.core.animation.addListener
import kotlin.math.max
import kotlin.math.min

typealias OnZoomChangeListener = (zoom: Float) -> Unit

class CameraZoomGestureDetector(@FloatRange(from = 0.0, to = 1.0) defaultZoom: Float) :
    GestureDetector.SimpleOnGestureListener(), ScaleGestureDetector.OnScaleGestureListener,
    View.OnTouchListener, ValueAnimator.AnimatorUpdateListener {

    companion object {

        /** Minimum time between calls to zoom listener. */
        private const val ZOOM_MINIMUM_WAIT_MILLIS: Long = 33L

        private const val ZOOM_LEVEL_STEP: Float = 0.5f
        private const val MIN_ZOOM: Float = 0f
        private const val MAX_ZOOM: Float = 1f
        private const val ZOOM_ANIMATOR_DURATION: Long = 300L
    }

    /** Next time zoom change should be sent to listener. */
    private var delayZoomCallUntilMillis: Long = 0L

    private var currentZoom: Float = min(MAX_ZOOM, max(defaultZoom, MIN_ZOOM))

    private var animator: ValueAnimator? = null

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private lateinit var listener: OnZoomChangeListener

    private val Animator.valueFloat: Float get() = (this as ValueAnimator).animatedValue as Float

    fun attach(view: View, listener: OnZoomChangeListener) {
        this.scaleGestureDetector = ScaleGestureDetector(view.context, this)
        this.gestureDetector = GestureDetector(view.context, this).apply {
            setOnDoubleTapListener(this@CameraZoomGestureDetector)
        }
        this.listener = ZoomChangeListener(listener)
        view.setOnTouchListener(this)
    }

    private class ZoomChangeListener(val delegate: OnZoomChangeListener) : OnZoomChangeListener {

        private var lastZoom: Float = -1f
        override fun invoke(zoom: Float) {
            if (lastZoom == zoom) return
            lastZoom = zoom
            delegate.invoke(zoom)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    private fun cancelZoomAnimator() {
        val animator = this.animator
        if (animator != null) {
            animator.cancel()
            this.animator = null
        }
    }

    private fun setZoomWithAnimator(from: Float, target: Float) {
        cancelZoomAnimator()
        this.animator = ValueAnimator.ofFloat(from, target)
            .apply {
                duration = ZOOM_ANIMATOR_DURATION
                interpolator = LinearInterpolator()
                addListener(
                    onEnd = this@CameraZoomGestureDetector::onAnimationFinish,
                    onCancel = this@CameraZoomGestureDetector::onAnimationFinish
                )
                addUpdateListener(this@CameraZoomGestureDetector)
                start()
            }
    }

    private fun getNextLevelZoom(currentZoom: Float): Float {
        if (currentZoom !in MIN_ZOOM..<MAX_ZOOM) {
            return MIN_ZOOM
        }
        var zoom = ((currentZoom / ZOOM_LEVEL_STEP).toInt() + 1) * ZOOM_LEVEL_STEP
        if (zoom > MAX_ZOOM) {
            zoom = MAX_ZOOM
        }
        return zoom
    }

    private fun onAnimationFinish(animation: Animator) {
        performListener(animation.valueFloat, true)
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        performListener(animation.valueFloat)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        val from = this.currentZoom
        setZoomWithAnimator(from, getNextLevelZoom(from))
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        cancelZoomAnimator()
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        performListener(getScaleZoom(detector))
        return true
    }

    private fun getScaleZoom(detector: ScaleGestureDetector): Float {
        val sf = detector.scaleFactor
        var zoom = (0.33f + currentZoom) * sf * sf - 0.33f
        zoom = min(MAX_ZOOM, max(zoom, MIN_ZOOM))
        return zoom
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        performListener(getScaleZoom(detector), true)
    }

    private fun performListener(zoom: Float, immediate: Boolean = false) {
        currentZoom = zoom
        if (immediate) {
            listener.invoke(zoom)
            return
        }

        // Refer to android Camera2, com.android.camera.ui.PreviewOverlay.ZoomProcessor#onScale
        // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r8:packages/apps/Camera2/src/com/android/camera/ui/PreviewOverlay.java;l=364

        // Only call the listener with a certain frequency. This is
        // necessary because these listeners will make repeated
        // applySettings() calls into the portability layer, and doing this
        // too often can back up its handler and result in visible lag in
        // updating the zoom level and other controls.
        val now = SystemClock.uptimeMillis()
        if (now > delayZoomCallUntilMillis) {
            listener.invoke(zoom)
            delayZoomCallUntilMillis = now + ZOOM_MINIMUM_WAIT_MILLIS
        }
    }

}
