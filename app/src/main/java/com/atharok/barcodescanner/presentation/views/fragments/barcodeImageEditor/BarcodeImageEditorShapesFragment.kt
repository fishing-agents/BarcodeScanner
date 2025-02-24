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
import com.atharok.barcodescanner.common.extensions.fixAnimateLayoutChangesInNestedScroll
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_CORNER_RADIUS_KEY
import com.atharok.barcodescanner.databinding.FragmentBarcodeImageEditorShapesBinding
import kotlin.math.roundToInt

class BarcodeImageEditorShapesFragment : AbstractBarcodeImageEditorFragment() {

    private var _binding: FragmentBarcodeImageEditorShapesBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBarcodeImageEditorShapesBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.fragmentBarcodeImageEditorShapesOuterView.fixAnimateLayoutChangesInNestedScroll()

        viewBinding.fragmentBarcodeImageEditorShapesCornerRadiusSlider.apply {
            value = arguments?.getFloat(BARCODE_IMAGE_CORNER_RADIUS_KEY, 0f) ?: 0f
            setLabelFormatter { value -> "${(value * 100f).roundToInt()}%" }
            addOnChangeListener { _, value, _ ->
                onBarcodeDetailsActivity { activity ->
                    activity.regenerateBitmap(cornerRadius = value)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(cornerRadius: Float) = BarcodeImageEditorShapesFragment().apply {
            arguments = Bundle().apply {
                putFloat(BARCODE_IMAGE_CORNER_RADIUS_KEY, cornerRadius)
            }
        }
    }
}