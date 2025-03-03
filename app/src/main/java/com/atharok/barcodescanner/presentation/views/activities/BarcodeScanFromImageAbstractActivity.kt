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

package com.atharok.barcodescanner.presentation.views.activities

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.common.extensions.getColorStateListFromAttrRes
import com.atharok.barcodescanner.databinding.ActivityBarcodeScanFromImageBinding
import com.atharok.barcodescanner.domain.library.BarcodeBitmapAnalyser
import com.google.zxing.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File

abstract class BarcodeScanFromImageAbstractActivity: BaseActivity() {

    private val barcodeBitmapAnalyser: BarcodeBitmapAnalyser by inject()

    private var zxingResult: Result? = null
    private var menuVisibility = false

    private val viewBinding: ActivityBarcodeScanFromImageBinding by lazy {
        ActivityBarcodeScanFromImageBinding.inflate(layoutInflater)
    }
    override val rootView: View get() = viewBinding.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(viewBinding.activityBarcodeScanFromImageToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewBinding.activityBarcodeScanFromImageCropImageView.clearImage()
        configureProgressBarColor()

        setContentView(rootView)
    }

    // ---- Menu ----

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_confirm, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_activity_confirm_item -> onSuccessfulImageScan(zxingResult)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if(menu != null) {
            for (i in 0 until menu.size()) {
                menu.getItem(i).isVisible = menuVisibility
                menu.getItem(i).isEnabled = menuVisibility
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setMenuVisibility(visible: Boolean) {
        if(menuVisibility!=visible) {
            menuVisibility=visible
            invalidateOptionsMenu()
        }
    }

    private fun configureProgressBarColor() {
        val mProgressBar = viewBinding.activityBarcodeScanFromImageCropImageView.findViewById<ProgressBar>(R.id.CropProgressBar)
        mProgressBar.indeterminateTintList = getColorStateListFromAttrRes(R.attr.colorPrimary)
    }

    // ---- Configure CropImageView ----

    /**
     * Configures all the elements necessary for barcode detection in the image.
     * The CropImageView component allows cropping the image, making it easier to analyze a specific area of it.
     */
    protected fun configureCropManagement(uri: Uri) {

        val cropImageView = viewBinding.activityBarcodeScanFromImageCropImageView
        val mProgressBar = cropImageView.findViewById<ProgressBar>(R.id.CropProgressBar)

        // Insert the image into the CropImageView.
        cropImageView.setImageUriAsync(uri)

        var job: Job? = null

        // Executed when the image cropping is finished.
        cropImageView.setOnCropImageCompleteListener { _, result ->
            val bitmap = result.getBitmap(this)

            if(bitmap != null){
                job?.cancel()
                setMenuVisibility(false)
                mProgressBar.visibility = View.VISIBLE
                job = lifecycleScope.launch(Dispatchers.IO) {
                    zxingResult = barcodeBitmapAnalyser.detectBarcodeFromBitmap(bitmap)
                    runOnUiThread {
                        if(zxingResult == null) {
                            Toast.makeText(this@BarcodeScanFromImageAbstractActivity, R.string.barcode_not_found, Toast.LENGTH_SHORT).show()
                        } else {
                            setMenuVisibility(true)
                        }
                        mProgressBar.visibility = View.GONE
                    }
                }
            }
        }

        // Executed when the image has finished loading.
        cropImageView.setOnSetImageUriCompleteListener { _, _, _ ->
            cropImageView.croppedImageAsync()
        }

        // Executed when the overlay is moved.
        cropImageView.setOnSetCropOverlayMovedListener {
            cropImageView.croppedImageAsync()
            job?.cancel()
            setMenuVisibility(false)
        }
    }

    protected abstract fun onSuccessfulImageScan(result: Result?)

    override fun finish() {
        removeTemporariesFiles()
        super.finish()
    }

    /**
     * The "android-image-cropper" library saves cropped images in the application's directory.
     * This method allows deleting these unnecessary files.
     */
    private fun removeTemporariesFiles() {
        val dir = File(getExternalFilesDir(null), "Pictures")
        deleteRecursive(dir)
        dir.delete()
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach {
                deleteRecursive(it)
            }
        }
        fileOrDirectory.delete()
    }
}