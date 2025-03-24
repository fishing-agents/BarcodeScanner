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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.common.extensions.serializable
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_DEFAULT_SIZE
import com.atharok.barcodescanner.common.utils.BARCODE_IMAGE_GENERATOR_PROPERTIES_KEY
import com.atharok.barcodescanner.databinding.FragmentBarcodeImageEditorBinding
import com.atharok.barcodescanner.domain.entity.barcode.QrCodeErrorCorrectionLevel
import com.atharok.barcodescanner.domain.library.BarcodeImageGeneratorProperties
import com.atharok.barcodescanner.presentation.views.adapters.FragmentPagerAdapter
import com.atharok.barcodescanner.presentation.views.fragments.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.google.zxing.BarcodeFormat

class BarcodeImageEditorFragment : BaseFragment() {

    private var _binding: FragmentBarcodeImageEditorBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBarcodeImageEditorBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageGeneratorProperties = arguments?.serializable(
            BARCODE_IMAGE_GENERATOR_PROPERTIES_KEY, BarcodeImageGeneratorProperties::class.java
        )

        configureEditorView(
            contents = imageGeneratorProperties?.contents ?: "",
            format = imageGeneratorProperties?.format ?: BarcodeFormat.QR_CODE,
            qrCodeErrorCorrectionLevel = imageGeneratorProperties?.qrCodeErrorCorrectionLevel ?: QrCodeErrorCorrectionLevel.NONE,
            marginsPx = imageGeneratorProperties?.marginsPx ?: 0,
            width = imageGeneratorProperties?.width ?: BARCODE_IMAGE_DEFAULT_SIZE,
            height = imageGeneratorProperties?.height ?: BARCODE_IMAGE_DEFAULT_SIZE,
            frontColor = imageGeneratorProperties?.frontColor ?: Color.BLACK,
            backgroundColor = imageGeneratorProperties?.backgroundColor ?: Color.WHITE,
            cornerRadius = imageGeneratorProperties?.cornerRadius ?: 0.0f
        )
    }

    private fun configureEditorView(
        contents: String,
        format: BarcodeFormat,
        qrCodeErrorCorrectionLevel: QrCodeErrorCorrectionLevel,
        marginsPx: Int,
        width: Int,
        height: Int,
        @ColorInt frontColor: Int = Color.BLACK,
        @ColorInt backgroundColor: Int = Color.WHITE,
        cornerRadius: Float = 0.0f
    ) {
        val infoFragment = BarcodeInfoFragment.newInstance(contents, format, qrCodeErrorCorrectionLevel)
        val colorsFragment = BarcodeImageEditorColorsFragment.newInstance(frontColor, backgroundColor)
        val shapesFragment = BarcodeImageEditorShapesFragment.newInstance(cornerRadius)
        val dimensionsFragment = BarcodeImageEditorDimensionsFragment.newInstance(width, height, marginsPx)
        val adapter = FragmentPagerAdapter(childFragmentManager, lifecycle, infoFragment, colorsFragment, shapesFragment, dimensionsFragment)

        val info = TabIcon(requireContext(), R.drawable.outline_info_24, R.string.information_label)
        val colors = TabIcon(requireContext(), R.drawable.outline_palette_24, R.string.colors_label)
        val shapes = TabIcon(requireContext(), R.drawable.outline_rounded_corner_24, R.string.shapes_label)
        val dimensions = TabIcon(requireContext(), R.drawable.outline_aspect_ratio_24, R.string.dimensions_label)

        configureViewPager(adapter, info, colors, shapes, dimensions)
    }

    // ---- ViewPager Configuration ----
    private fun configureViewPager(adapter: FragmentStateAdapter, vararg tabIcon: TabIcon) {
        val viewPager = viewBinding.fragmentBarcodeImageEditorViewPager
        val tabLayout = viewBinding.fragmentBarcodeImageEditorTabLayout

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = tabIcon.size - 1
        viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                hideSoftKeyboard() // Ferme le Keyboard si on change d'onglet
            }
        })

        if(tabIcon.isNotEmpty()) {
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tabIcon[position].apply {
                    tab.icon = icon
                    tab.contentDescription = contentDescription
                }
            }.attach()

            tabLayout.visibility = View.VISIBLE
        } else {
            tabLayout.visibility = View.GONE
        }
    }

    private class TabIcon(
        context: Context,
        @DrawableRes iconResource: Int,
        @StringRes contentDescriptionResource: Int
    ) {
        val icon: Drawable? = AppCompatResources.getDrawable(context, iconResource)
        val contentDescription: String = context.getString(contentDescriptionResource)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            properties: BarcodeImageGeneratorProperties
        ) = BarcodeImageEditorFragment().apply {
            arguments = Bundle().apply {
                putSerializable(BARCODE_IMAGE_GENERATOR_PROPERTIES_KEY, properties)
            }
        }
    }
}