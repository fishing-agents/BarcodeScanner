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

package com.atharok.barcodescanner.presentation.views.fragments.barcodeImageEditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.atharok.barcodescanner.common.extensions.fixAnimateLayoutChangesInNestedScroll
import com.atharok.barcodescanner.common.extensions.toLocalString
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_DEFAULT_SIZE
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_HEIGHT_KEY
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_MARGINS_KEY
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_WIDTH_KEY
import com.atharok.barcodescanner.databinding.FragmentBarcodeImageEditorDimensionsBinding
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.roundToInt

class BarcodeImageEditorDimensionsFragment : AbstractBarcodeImageEditorFragment() {

    private var proportionsWidth: Int = 1
        set(value) {
            field = if(value == 0) 1 else value
        }

    private var proportionsHeight: Int = 1
        set(value) {
            field = if(value == 0) 1 else value
        }

    private var _binding: FragmentBarcodeImageEditorDimensionsBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBarcodeImageEditorDimensionsBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.fragmentBarcodeImageEditorDimensionsOuterView.fixAnimateLayoutChangesInNestedScroll()

        val defaultWidth: Int = arguments?.getInt(BARCODE_IMAGE_WIDTH_KEY, BARCODE_IMAGE_DEFAULT_SIZE) ?: BARCODE_IMAGE_DEFAULT_SIZE
        val defaultHeight: Int = arguments?.getInt(BARCODE_IMAGE_HEIGHT_KEY, BARCODE_IMAGE_DEFAULT_SIZE) ?: BARCODE_IMAGE_DEFAULT_SIZE

        val widthEditText = viewBinding.fragmentBarcodeImageEditorDimensionsWidthInputEditText
        val heightEditText = viewBinding.fragmentBarcodeImageEditorDimensionsHeightInputEditText
        val proportionsCheckBox = viewBinding.fragmentBarcodeImageEditorDimensionsKeepProportionsCheckBox

        widthEditText.setText(defaultWidth.toLocalString())
        widthEditText.addTextChangedListener { editable ->
            editable.toString().toIntOrNull()?.let { newWidth ->
                if(widthEditText.isFocused) {
                    if (newWidth > MAX_SIZE) {
                        widthEditText.setText(MAX_SIZE.toLocalString())
                        widthEditText.setSelection(4)
                    } else if (proportionsCheckBox.isChecked) {
                        val newHeight = conserveHeightProportions(newWidth)
                        if(newHeight > MAX_SIZE) {
                            val newWidth2 = conserveWidthProportions(MAX_SIZE)
                            heightEditText.setText(MAX_SIZE.toLocalString())
                            widthEditText.setText(newWidth2.toLocalString())
                            widthEditText.setSelection(4)
                        } else {
                            heightEditText.setText(newHeight.toLocalString())
                            onBarcodeDetailsActivity { activity ->
                                activity.regenerateBitmap(
                                    width = newWidth,
                                    height = newHeight
                                )
                            }
                        }
                    } else {
                        onBarcodeDetailsActivity { activity ->
                            activity.regenerateBitmap(
                                width = newWidth
                            )
                        }
                    }
                }
            }
        }

        heightEditText.setText(defaultHeight.toLocalString())
        heightEditText.addTextChangedListener { editable ->
            editable.toString().toIntOrNull()?.let { newHeight ->
                if(heightEditText.isFocused) {
                    if (newHeight > MAX_SIZE) {
                        heightEditText.setText(MAX_SIZE.toLocalString())
                        heightEditText.setSelection(4)
                    } else if (proportionsCheckBox.isChecked) {
                        val newWidth = conserveWidthProportions(newHeight)
                        if(newWidth > MAX_SIZE) {
                            val newHeight2 = conserveHeightProportions(MAX_SIZE)
                            widthEditText.setText(MAX_SIZE.toLocalString())
                            heightEditText.setText(newHeight2.toLocalString())
                            heightEditText.setSelection(4)
                        } else {
                            widthEditText.setText(newWidth.toLocalString())
                            onBarcodeDetailsActivity { activity ->
                                activity.regenerateBitmap(
                                    width = newWidth,
                                    height = newHeight
                                )
                            }
                        }
                    } else {
                        onBarcodeDetailsActivity { activity ->
                            activity.regenerateBitmap(
                                height = newHeight
                            )
                        }
                    }
                }

            }
        }

        configureCheckBox(proportionsCheckBox, widthEditText, heightEditText)

        configureMarginsSlider()
    }

    private fun configureCheckBox(
        checkBox: MaterialCheckBox,
        widthInputEditText: TextInputEditText,
        heightInputEditText: TextInputEditText
    ) {
        if(checkBox.isChecked) {
            proportionsWidth = widthInputEditText.text.toString().toIntOrNull() ?: 1
            proportionsHeight = heightInputEditText.text.toString().toIntOrNull() ?: 1
        }

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                proportionsWidth = widthInputEditText.text.toString().toIntOrNull() ?: 1
                proportionsHeight = heightInputEditText.text.toString().toIntOrNull() ?: 1
            } else {
                proportionsWidth = 1
                proportionsHeight = 1
            }
        }
    }

    private fun configureMarginsSlider() {
        viewBinding.fragmentBarcodeImageEditorDimensionsMarginsSlider.apply {
            value = arguments?.getFloat(BARCODE_IMAGE_MARGINS_KEY, 0f) ?: 0f
            setLabelFormatter { value -> "${(value * 100f).roundToInt()}%" }
            addOnChangeListener { _, value, _ ->
                onBarcodeDetailsActivity { activity ->
                    activity.regenerateBitmap(marginsPercent = value)
                }
            }
        }
    }

    private fun conserveWidthProportions(newHeightSize: Int): Int {
        return (newHeightSize / proportionsHeight.toFloat() * proportionsWidth).roundToInt()
    }

    private fun conserveHeightProportions(newWidthSize: Int): Int {
        return (newWidthSize / proportionsWidth.toFloat() * proportionsHeight).roundToInt()
    }

    companion object {
        private const val MAX_SIZE = 2048

        @JvmStatic
        fun newInstance(width: Int, height: Int, marginsPercent: Float) = BarcodeImageEditorDimensionsFragment().apply {
            arguments = Bundle().apply {
                putInt(BARCODE_IMAGE_WIDTH_KEY, width)
                putInt(BARCODE_IMAGE_HEIGHT_KEY, height)
                putFloat(BARCODE_IMAGE_MARGINS_KEY, marginsPercent)
            }
        }
    }
}