package com.atharok.barcodescanner.domain.library.camera

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.atharok.barcodescanner.common.extensions.afterMeasured
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class CameraConfig(private val context: Context) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var postZoom = -1f
    var flashEnabled = false
        private set

    private val resolutionSelector: ResolutionSelector by lazy {
        ResolutionSelector.Builder().build()
    }

    private val cameraSelector: CameraSelector by lazy {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }

    private val preview: Preview by lazy {
        Preview.Builder().build()
    }

    private val imageAnalysis: ImageAnalysis by lazy {
        ImageAnalysis.Builder().apply {
            setResolutionSelector(resolutionSelector)
            setOutputImageRotationEnabled(true)
        }.build()
    }

    fun setAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get().apply {
                try {
                    unbindAll()
                    preview.surfaceProvider = previewView.surfaceProvider
                    camera = bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis).apply {
                        configureAutoFocus(previewView, this)
                        if (postZoom != -1f) {
                            configureZoom(this, postZoom)
                            postZoom = -1f
                        }
                    }
                } catch(exc: Exception) {
                    Log.e("TAG", "Use case binding failed", exc)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        switchOffFlash()
        // Clear analyzer first to stop processing new frames
        imageAnalysis.clearAnalyzer()
        cameraProvider?.let {
            it.unbindAll()
            camera = null
        }
    }

    fun isRunning(): Boolean = camera != null

    private fun configureAutoFocus(previewView: PreviewView, camera: Camera) {

        previewView.afterMeasured {

            val previewViewWidth = previewView.width.toFloat()
            val previewViewHeight = previewView.height.toFloat()

            val autoFocusPoint = SurfaceOrientedMeteringPointFactory(
                previewViewWidth, previewViewHeight
            ).createPoint(previewViewWidth / 2.0f, previewViewHeight / 2.0f)

            try {
                camera.cameraControl.startFocusAndMetering(
                    FocusMeteringAction
                        .Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(2, TimeUnit.SECONDS)
                        .build()
                )
            } catch (e: CameraInfoUnavailableException) {
                Log.d("ERROR", "cannot access camera", e)
            }
        }
    }

    private fun configureZoom(camera: Camera, value: Float) {
        val safeZoom = max(0f, min(value, 1f))
        camera.cameraControl.setLinearZoom(safeZoom)
    }

    fun setLinearZoom(value: Float) {
        val camera = this.camera
        if (camera == null) {
            postZoom = value
            return
        }
        postZoom = -1f
        configureZoom(camera, value)
    }

    fun hasFlash(): Boolean =
        context.applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    fun switchFlash() {
        camera?.let {
            flashEnabled = !flashEnabled
            it.cameraControl.enableTorch(flashEnabled)
        }
    }

    private fun switchOffFlash() {
        if(flashEnabled){
            switchFlash()
        }
    }
}