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
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.common.extensions.fixAnimateLayoutChangesInNestedScroll
import com.atharok.barcodescanner.common.extensions.toLocalString
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_DEFAULT_SIZE
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_HEIGHT_KEY
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_MARGINS_KEY
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_WIDTH_KEY
import com.atharok.barcodescanner.databinding.FragmentBarcodeImageEditorDimensionsBinding
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.max
import kotlin.math.min
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

        var defaultWidth: Int = arguments?.getInt(BARCODE_IMAGE_WIDTH_KEY, BARCODE_IMAGE_DEFAULT_SIZE) ?: BARCODE_IMAGE_DEFAULT_SIZE
        var defaultHeight: Int = arguments?.getInt(BARCODE_IMAGE_HEIGHT_KEY, BARCODE_IMAGE_DEFAULT_SIZE) ?: BARCODE_IMAGE_DEFAULT_SIZE

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

                    updateMarginsSlider(
                        bitmapWidth = widthEditText.text?.toString()?.toIntOrNull(),
                        bitmapHeight = heightEditText.text?.toString()?.toIntOrNull()
                    )
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

                    updateMarginsSlider(
                        bitmapWidth = widthEditText.text?.toString()?.toIntOrNull(),
                        bitmapHeight = heightEditText.text?.toString()?.toIntOrNull()
                    )
                }
            }
        }

        configureCheckBox(proportionsCheckBox, widthEditText, heightEditText)

        configureMarginsSlider(defaultWidth.toFloat(), defaultHeight.toFloat())
        configureMarginsButtons()
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

    private fun configureMarginsSlider(bitmapWidth: Float, bitmapHeight: Float) {
        val defaultMargin = arguments?.getInt(BARCODE_IMAGE_MARGINS_KEY, 0) ?: 0

        val slider = viewBinding.fragmentBarcodeImageEditorDimensionsMarginsSlider
        val pxTextView = viewBinding.fragmentBarcodeImageEditorDimensionsMarginsPxTextView.apply {
            text = getString(R.string.value_px, "$defaultMargin")
        }

        slider.apply {
            valueFrom = 0f
            valueTo = minOf(bitmapWidth, bitmapHeight) / 2f // margins max (px)
            value = defaultMargin.toFloat()

            setLabelFormatter { value -> getString(R.string.value_px, "${value.roundToInt()}") }

            addOnChangeListener { _, value, _ ->
                onBarcodeDetailsActivity { activity ->
                    activity.regenerateBitmap(marginsPx = value.roundToInt())
                }
                pxTextView.text = getString(R.string.value_px, "${value.roundToInt()}")
            }
        }
    }

    private fun updateMarginsSlider(bitmapWidth: Int?, bitmapHeight: Int?) {
        if(bitmapWidth != null && bitmapHeight != null) {
            viewBinding.fragmentBarcodeImageEditorDimensionsMarginsSlider.apply {
                val fromPx = valueFrom
                val toPx = valueTo
                val valuePx = value
                val valuePercent = (valuePx - fromPx) / (toPx - fromPx) * 100

                val marginsMaxPx: Float = minOf(bitmapWidth, bitmapHeight) / 2f
                valueFrom = 0f
                valueTo = marginsMaxPx
                value = (valuePercent / 100f * marginsMaxPx)
            }
        }
    }

    private fun configureMarginsButtons() {
        val slider = viewBinding.fragmentBarcodeImageEditorDimensionsMarginsSlider

        viewBinding.fragmentBarcodeImageEditorDimensionsMarginsDecreaseIconButton.setOnClickListener {
            slider.value = max(slider.value - 1f, slider.valueFrom)
        }

        viewBinding.fragmentBarcodeImageEditorDimensionsMarginsIncreaseIconButton.setOnClickListener {
            slider.value = min(slider.value + 1f, slider.valueTo)
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
        fun newInstance(width: Int, height: Int, marginsPx: Int) = BarcodeImageEditorDimensionsFragment().apply {
            arguments = Bundle().apply {
                putInt(BARCODE_IMAGE_WIDTH_KEY, width)
                putInt(BARCODE_IMAGE_HEIGHT_KEY, height)
                putInt(BARCODE_IMAGE_MARGINS_KEY, marginsPx)
            }
        }
    }
}