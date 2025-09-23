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

package com.atharok.barcodescanner.presentation.views.fragments.barcodeAnalysis.contents

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import com.atharok.barcodescanner.databinding.FragmentBarcodeMatrixUpiParsedBinding
import com.atharok.barcodescanner.presentation.views.fragments.BaseFragment

class BarcodeMatrixUpiParsedFragment : BaseFragment() {

    private var uri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uri = it.getString(URI_BUNDLE_KEY)
        }
    }

    private var _binding: FragmentBarcodeMatrixUpiParsedBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBarcodeMatrixUpiParsedBinding.inflate(inflater, container, false)
        if (uri?.startsWith("upi") != true) {
            viewBinding.root.visibility = View.GONE
        }
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uri?.let {
            val uriParsed: Uri = it.toUri()

            val upiIdView = viewBinding.fragmentBarcodeMatrixUpiParsedUpiIdLayout
            val payeeNameView = viewBinding.fragmentBarcodeMatrixUpiParsedPayeeNameLayout
            val amountView = viewBinding.fragmentBarcodeMatrixUpiParsedAmountLayout
            val currencyView = viewBinding.fragmentBarcodeMatrixUpiParsedCurrencyLayout
            val descriptionView = viewBinding.fragmentBarcodeMatrixUpiParsedDescriptionLayout

            upiIdView.setContentsText(uriParsed.getQueryParameter("pa"))
            payeeNameView.setContentsText(uriParsed.getQueryParameter("pn"))
            amountView.setContentsText(uriParsed.getQueryParameter("am"))
            currencyView.setContentsText(uriParsed.getQueryParameter("cu"))
            descriptionView.setContentsText(uriParsed.getQueryParameter("tn"))
        } ?: run {
            viewBinding.root.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val URI_BUNDLE_KEY = "uriBundleKey"

        @JvmStatic
        fun newInstance(uri: String) =
            BarcodeMatrixUpiParsedFragment().apply {
                arguments = Bundle().apply {
                    putString(URI_BUNDLE_KEY, uri)
                }
            }
    }
}