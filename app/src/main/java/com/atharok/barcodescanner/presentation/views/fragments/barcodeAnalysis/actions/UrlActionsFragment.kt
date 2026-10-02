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

package com.atharok.barcodescanner.presentation.views.fragments.barcodeAnalysis.actions

import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.domain.entity.barcode.Barcode
import com.atharok.barcodescanner.domain.library.payment.PaymentCodeClassifier
import com.atharok.barcodescanner.presentation.views.recyclerView.actionButton.ActionItem
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.URIParsedResult

class UrlActionsFragment: AbstractParsedResultActionsFragment() {
    override fun configureActionItems(barcode: Barcode, parsedResult: ParsedResult) {
        if(parsedResult is URIParsedResult) {
            addActionItem(configureUrlActionItem(parsedResult.uri))
        } else {
            addActionItem(configureSearchOnWebActionItem(barcode))
        }
        addActionItem(configureShareTextActionItem(barcode))
        addActionItem(configureCopyTextActionItem(barcode))
        addActionItem(configureModifyBarcodeActionItem(barcode))
        addActionItem(configureAssignANameToBarcodeActionItem(barcode))
    }

    /** WeChat/Alipay codes get an "Open in <app>" label and Alipay's in-app hand-off URI; other URIs open as links. */
    private fun configureUrlActionItem(uri: String): ActionItem {
        val paymentCode = PaymentCodeClassifier.classify(uri)
        return ActionItem(
            textRes = paymentCode?.openActionRes ?: R.string.action_open_link,
            imageRes = R.drawable.baseline_open_in_browser_24,
            listener = openUrl(paymentCode?.let { PaymentCodeClassifier.handoffUri(uri, it) } ?: uri)
        )
    }
}