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

package com.atharok.barcodescanner.domain.entity.product.foodProduct

import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import com.atharok.barcodescanner.R

enum class PalmOilStatus(@param:StringRes val stringResource: Int, @param:AttrRes val colorResource: Int) {
    PALM_OIL_FREE(R.string.palm_oil_free_label, R.attr.colorPositive),
    PALM_OIL(R.string.contain_palm_oil_label, R.attr.colorNegative),
    MAYBE_PALM_OIL(R.string.may_contain_palm_oil_label, R.attr.colorMedium),
    UNKNOWN_PALM_OIL(R.string.palm_oil_content_unknown_label, R.attr.colorUnknown)
}