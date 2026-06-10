/*
 * Barcode Scanner
 * Copyright (C) 2021  Atharok
 *
 * This file is part of Barcode Scanner.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.atharok.barcodescanner.presentation.views.fragments.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.common.extensions.SCAN_RESULT
import com.atharok.barcodescanner.common.extensions.SCAN_RESULT_ERROR_CORRECTION_LEVEL
import com.atharok.barcodescanner.common.extensions.SCAN_RESULT_FORMAT
import com.atharok.barcodescanner.common.extensions.getDisplayName
import com.atharok.barcodescanner.common.extensions.hasFlash
import com.atharok.barcodescanner.common.extensions.is1DIndustrialBarcode
import com.atharok.barcodescanner.common.extensions.is1DProductBarcode
import com.atharok.barcodescanner.common.extensions.is2DBarcode
import com.atharok.barcodescanner.common.extensions.toIntent
import com.atharok.barcodescanner.common.utils.BARCODE_KEY
import com.atharok.barcodescanner.common.utils.KOIN_NAMED_ERROR_CORRECTION_LEVEL_BY_RESULT
import com.atharok.barcodescanner.common.utils.KOIN_NAMED_ERROR_CORRECTION_LEVEL_BY_STRING
import com.atharok.barcodescanner.databinding.FragmentMainCameraXScannerBinding
import com.atharok.barcodescanner.domain.entity.barcode.Barcode
import com.atharok.barcodescanner.domain.entity.barcode.QrCodeErrorCorrectionLevel
import com.atharok.barcodescanner.domain.library.BeepManager
import com.atharok.barcodescanner.domain.library.VibratorAppCompat
import com.atharok.barcodescanner.domain.library.camera.CameraBarcodeAnalyzer
import com.atharok.barcodescanner.domain.library.camera.CameraZoomGestureDetector
import com.atharok.barcodescanner.presentation.intent.createStartActivityIntent
import com.atharok.barcodescanner.presentation.viewmodel.DatabaseBarcodeViewModel
import com.atharok.barcodescanner.presentation.views.activities.BarcodeAnalysisActivity
import com.atharok.barcodescanner.presentation.views.activities.BarcodeScanFromImageGalleryActivity
import com.atharok.barcodescanner.presentation.views.activities.BaseActivity
import com.atharok.barcodescanner.presentation.views.activities.MainActivity
import com.atharok.barcodescanner.presentation.views.fragments.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

/**
 * A simple [Fragment] subclass.
 */
class MainCameraXScannerFragment : BaseFragment(), CameraBarcodeAnalyzer.BarcodeDetector {

    companion object {
        private const val ZXING_SCAN_INTENT_ACTION = "com.google.zxing.client.android.SCAN"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private val databaseBarcodeViewModel: DatabaseBarcodeViewModel by activityViewModel()

    // ---- Camera ----

    private lateinit var cameraExecutor: ExecutorService
    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
            //.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    private val barcodeAnalyzer by lazy { CameraBarcodeAnalyzer(this) }
    private var isBarcodeAnalyzerRunning = false
    private var camera: Camera? = null
    private var flashEnabled = false
    private var isProcessingBarcode = false
    private var lastProcessingTime = 0L

    // Rate limiting data
    private val recentScans = mutableMapOf<String, Long>()

    // ---- View ----
    private var _binding: FragmentMainCameraXScannerBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureResultBarcodeScanFromImageActivity()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainCameraXScannerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            doPermissionGranted()
        } else {
            requestPermissions()
        }

        configureMenu()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        stopBarcodeAnalyzer()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        // Clear rate limit cache if rate limiting is disabled
        if (!settingsManager.isRateLimitEnabled) {
            recentScans.clear()
        }

        // Reset processing flag when fragment resumes
        isProcessingBarcode = false

       if (allPermissionsGranted() && !isBarcodeAnalyzerRunning) {
           startBarcodeAnalyzer()
        }
    }

    override fun onPause() {
        super.onPause()
        switchOffFlash()
        stopBarcodeAnalyzer()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: Activity = requireActivity()
        if(activity is BaseActivity) {
            if(settingsManager.isAutoScreenRotationScanDisabled) {
                activity.lockDeviceRotation(true)
            }
        }
    }

    // ---- Menu ----

    private fun configureMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_scanner, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when(menuItem.itemId) {
                R.id.menu_scanner_flash -> {
                    switchFlash()
                    requireActivity().invalidateOptionsMenu()
                    true
                }
                R.id.menu_scanner_scan_from_image -> {
                    startBarcodeScanFromImageActivity()
                    true
                }
                else -> false
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)

                if(requireContext().hasFlash() && allPermissionsGranted()) {
                    if (flashEnabled) {
                        menu.getItem(0).icon =
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.baseline_flash_on_24
                            )
                    } else {
                        menu.getItem(0).icon =
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.baseline_flash_off_24
                            )
                    }

                } else {
                    menu.getItem(0).isVisible = false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // ---- Camera Permission ----

    private fun requestPermissions() {
        val activityResultLauncher: ActivityResultLauncher<String> =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    doPermissionGranted()
                } else doPermissionRefused()
            }

        // Gère le resultat de la demande de permission d'accès à la caméra.
        activityResultLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireActivity(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun doPermissionGranted() {
        startCamera()
        viewBinding.fragmentMainCameraXScannerCameraPermissionTextView.visibility = View.GONE
        viewBinding.fragmentMainCameraXScannerPreviewView.visibility = View.VISIBLE
        viewBinding.fragmentMainCameraXScannerScanOverlay.visibility = View.VISIBLE
        viewBinding.fragmentMainCameraXScannerSlider.visibility = View.VISIBLE
        viewBinding.fragmentMainCameraXScannerInformationTextView?.visibility = View.VISIBLE
    }

    private fun doPermissionRefused() {
        stopBarcodeAnalyzer()
        viewBinding.fragmentMainCameraXScannerCameraPermissionTextView.visibility = View.VISIBLE
        viewBinding.fragmentMainCameraXScannerPreviewView.visibility = View.GONE
        viewBinding.fragmentMainCameraXScannerScanOverlay.visibility = View.GONE
        viewBinding.fragmentMainCameraXScannerSlider.visibility = View.GONE
        viewBinding.fragmentMainCameraXScannerInformationTextView?.visibility = View.GONE
    }

    // ---- Camera ----

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({

            // Prevent a rare crash if the view is destroyed before the ProcessCameraProvider callback is executed
            if (_binding == null) {
                return@addListener
            }

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = viewBinding.fragmentMainCameraXScannerPreviewView.surfaceProvider }

            // Analyzer
            startBarcodeAnalyzer()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner = viewLifecycleOwner,
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )

                configureZoom()
            } catch (e: Exception) {
                Log.e("CameraFragment", "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopBarcodeAnalyzer() {
        imageAnalyzer.clearAnalyzer()
        isBarcodeAnalyzerRunning = false
    }

    private fun startBarcodeAnalyzer() {
        imageAnalyzer.also {
            it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
        }
        isBarcodeAnalyzerRunning = true
    }

    override fun onBarcodeFound(result: Result) {
        viewBinding.fragmentMainCameraXScannerPreviewView.post {
            try {
                // Check if processing is stuck (more than 5 seconds)
                val currentTime = System.currentTimeMillis()
                if (isProcessingBarcode && currentTime - lastProcessingTime > 5000) {
                    // Reset the flag if it's been stuck for more than 5 seconds
                    isProcessingBarcode = false
                }

                // Prevent processing multiple barcodes at once
                if(isBarcodeAnalyzerRunning && !isProcessingBarcode) {
                    // Check rate limiting first
                    if (settingsManager.isRateLimitEnabled) {
                        try {
                            if (isWithinRateLimit(result.text)) {
                                // Barcode was recently scanned, ignore it and continue scanning
                                // Don't set isProcessingBarcode = true so scanner keeps running
                                return@post
                            }
                        } catch (e: Exception) {
                            // If rate limiting check fails, continue scanning
                            e.printStackTrace()
                        }
                    }

                    isProcessingBarcode = true
                    lastProcessingTime = currentTime

                    // Check if this barcode type is allowed
                    val allowedFormats = settingsManager.allowedBarcodeFormats
                    val barcodeFormat = result.barcodeFormat?.name

                    // If no formats are specified (empty set), all formats are allowed
                    if (allowedFormats.isEmpty() || (barcodeFormat != null && allowedFormats.contains(barcodeFormat))) {
                        // Record scan time for rate limiting BEFORE stopping camera
                        if (settingsManager.isRateLimitEnabled) {
                            recordScanTime(result.text)
                        }

                        onSuccessfulScanFromCamera(result)
                    } else {
                        // Barcode type is not whitelisted - show popup but don't save to history
                        showNonWhitelistedBarcodePopup(result)
                    }
                }
            } catch (e: Exception) {
                // Reset flag on any error to prevent scanner from getting stuck
                isProcessingBarcode = false
                e.printStackTrace()
            }
        }
    }

    override fun onError(msg: String) {
        viewBinding.fragmentMainCameraXScannerPreviewView.post {
            viewBinding.fragmentMainCameraXScannerCameraPermissionTextView.text = getString(R.string.scan_error_exception_label, msg)
            doPermissionRefused()
        }
    }

    private fun showNonWhitelistedBarcodePopup(result: Result) {
        requireActivity().runOnUiThread {
            stopBarcodeAnalyzer()

            val barcodeFormat = result.barcodeFormat
            val displayName = barcodeFormat?.getDisplayName(requireContext()) ?: "Unknown"
            val iconResource = getBarcodeFormatIcon(barcodeFormat)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.non_whitelisted_barcode_title, displayName))
                .setIcon(iconResource)
                .setMessage(getString(R.string.non_whitelisted_barcode_message))
                .setPositiveButton(R.string.allow_this_time) { dialog, _ ->
                    dialog.dismiss()
                    // Process the barcode as if it was whitelisted (one-time exception)
                    onSuccessfulScanFromCamera(result)
                    // Flag will be reset after processing completes
                }
                .setNegativeButton(R.string.go_back) { dialog, _ ->
                    dialog.dismiss()
                    isProcessingBarcode = false // Reset flag before resuming
                    startBarcodeAnalyzer()
                }
                .setOnCancelListener {
                    isProcessingBarcode = false // Reset flag before resuming
                    startBarcodeAnalyzer()
                }
                .setCancelable(true) // Allow dismissing by tapping outside
                .show()
        }
    }

    private fun getBarcodeFormatIcon(format: BarcodeFormat?): Int {
        return when {
            format == null -> R.drawable.ic_bar_code_24
            format.is1DProductBarcode() || format.is1DIndustrialBarcode() -> R.drawable.ic_bar_code_24
            format.is2DBarcode() -> {
                when(format) {
                    BarcodeFormat.AZTEC -> R.drawable.ic_aztec_code_24
                    BarcodeFormat.DATA_MATRIX -> R.drawable.ic_data_matrix_code_24
                    BarcodeFormat.PDF_417 -> R.drawable.ic_pdf_417_code_24
                    BarcodeFormat.QR_CODE -> R.drawable.baseline_qr_code_24
                    else -> R.drawable.baseline_qr_code_24
                }
            }
            else -> R.drawable.baseline_qr_code_24
        }
    }

    // Zoom

    private fun configureZoom() {
        val slider = viewBinding.fragmentMainCameraXScannerSlider
        slider.value = settingsManager.getDefaultZoomValue() / 100f
        setLinearZoom(slider.value)
        slider.addOnChangeListener { v, value, _ ->
            setLinearZoom(value)
            // BZZZTT!!1!
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        CameraZoomGestureDetector(slider.value)
            .attach(viewBinding.fragmentMainCameraXScannerScanOverlay) { value ->
                slider.value = value
            }
    }

    private fun setLinearZoom(value: Float) {
        this.camera?.let {
            val safeZoom = max(0f, min(value, 1f))
            it.cameraControl.setLinearZoom(safeZoom)
        }
    }

    // Flash

    private fun switchFlash() {
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

    // ---- Scan successful ----

    private inline fun onSuccessfulScan(
        contents: String?,
        formatName: String?,
        errorCorrectionLevel: QrCodeErrorCorrectionLevel,
        crossinline onResult: (barcode: Barcode) -> Unit
    ) = requireActivity().runOnUiThread {

        if(contents != null && formatName != null) {

            stopBarcodeAnalyzer()

            if(settingsManager.shouldCopyBarcodeScan) {
                copyToClipboard("contents", contents)
                showToastText(R.string.barcode_copied_label)
            }

            if(settingsManager.useBipScan)
                get<BeepManager>().playBeepSound(requireActivity())

            if(settingsManager.useVibrateScan)
                get<VibratorAppCompat>().vibrate()

            val barcode: Barcode = get { parametersOf(contents, formatName, errorCorrectionLevel) }

            if(settingsManager.shouldAddBarcodeScanToHistory) {
                // Insert les informations du code-barres dans la base de données (de manière asynchrone)
                databaseBarcodeViewModel.insertBarcode(barcode, settingsManager.saveDuplicates)
            }

            onResult(barcode)
        } else {
            showSnackbar(getString(R.string.scan_cancel_label))
        }
    }

    // ---- Scan from Camera ----

    private fun onSuccessfulScanFromCamera(result: Result) {
        val contents = result.text
        val formatName = result.barcodeFormat?.name
        val errorCorrectionLevel: QrCodeErrorCorrectionLevel =
            get(named(KOIN_NAMED_ERROR_CORRECTION_LEVEL_BY_RESULT)) { parametersOf(result) }

        onSuccessfulScan(contents, formatName, errorCorrectionLevel) { barcode ->
            // Si l'application a été ouverte via une application tierce
            if (requireActivity().intent?.action == ZXING_SCAN_INTENT_ACTION) {
                sendResultToAppIntent(result.toIntent())
            } else {
                startBarcodeAnalysisActivity(barcode)
            }
        }
    }

    /**
     * Démarre l'Activity: BarcodeAnalysisActivity.
     */
    private fun startBarcodeAnalysisActivity(barcode: Barcode) {
        val intent = createStartActivityIntent(requireContext(), BarcodeAnalysisActivity::class).apply {
            putExtra(BARCODE_KEY, barcode)
        }
        startActivity(intent)
    }

    // ---- Scan from Image ----

    private var resultBarcodeScanFromImageActivity: ActivityResultLauncher<Intent>? = null

    private fun configureResultBarcodeScanFromImageActivity() {
        resultBarcodeScanFromImageActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK){
                it.data?.let { intentResult ->
                    onSuccessfulScanFromImage(intentResult)
                }
            }
        }
    }

    private fun onSuccessfulScanFromImage(intentResult: Intent) {
        val contents = intentResult.getStringExtra(SCAN_RESULT)
        val formatName = intentResult.getStringExtra(SCAN_RESULT_FORMAT)
        val errorCorrectionLevel: QrCodeErrorCorrectionLevel =
            get(named(KOIN_NAMED_ERROR_CORRECTION_LEVEL_BY_STRING)) {
                parametersOf(intentResult.getStringExtra(SCAN_RESULT_ERROR_CORRECTION_LEVEL))
            }

        onSuccessfulScan(contents, formatName, errorCorrectionLevel) { barcode ->
            // Si l'application a été ouverte via une application tierce
            if (requireActivity().intent?.action == ZXING_SCAN_INTENT_ACTION) {
                sendResultToAppIntent(intentResult)
            } else {
                startBarcodeAnalysisActivity(barcode)
            }
        }
    }

    private fun startBarcodeScanFromImageActivity() {
        //cameraConfig?.stopCamera()
        resultBarcodeScanFromImageActivity?.let { result ->
            val intent = createStartActivityIntent(requireContext(), BarcodeScanFromImageGalleryActivity::class)
            result.launch(intent)
        }
    }

    // ---- Snackbar ----

    private fun showSnackbar(text: String) {
        val activity = requireActivity()
        if(activity is MainActivity) {
            activity.showSnackbar(text)
        }
    }

    // ---- Intent ----

    private fun sendResultToAppIntent(intent: Intent) {
        requireActivity().apply {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    // ---- Rate Limiting ----

    private fun isWithinRateLimit(barcodeContent: String): Boolean {
        // Don't rate limit empty barcodes
        if (barcodeContent.isBlank()) return false

        val currentTime = System.currentTimeMillis()
        val rateLimitMillis = settingsManager.rateLimitDurationSeconds * 1000L

        // Clean up expired entries first
        val iterator = recentScans.entries.iterator()
        while(iterator.hasNext()) {
            val (_, timestamp) = iterator.next()
            if (currentTime - timestamp >= rateLimitMillis) {
                iterator.remove()
            }
        }

        // Now check if the barcode is still in the map (not expired)
        val lastScanTime = recentScans[barcodeContent] ?: return false
        val isWithinLimit = currentTime - lastScanTime < rateLimitMillis

        // Debug logging
        if (isWithinLimit) {
            val remainingSeconds = (rateLimitMillis - (currentTime - lastScanTime)) / 1000
            println("Rate limit: Barcode blocked for $remainingSeconds more seconds")
        }

        return isWithinLimit
    }

    private fun recordScanTime(barcodeContent: String) {
        // Don't record empty barcodes
        if (barcodeContent.isNotBlank()) {
            recentScans[barcodeContent] = System.currentTimeMillis()
            println("Rate limit: Recorded scan of barcode, will be blocked for ${settingsManager.rateLimitDurationSeconds} seconds")
        }
    }
}