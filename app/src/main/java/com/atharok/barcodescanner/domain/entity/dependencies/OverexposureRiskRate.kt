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

package com.atharok.barcodescanner.domain.entity.dependencies

import androidx.annotation.AttrRes
import com.atharok.barcodescanner.R

enum class OverexposureRiskRate(val id: String, @param:AttrRes val colorResource: Int, val stringResource: Int) {
    LOW("en:no", R.attr.colorPositive, R.string.off_overexposure_risk_low_label),
    MODERATE("en:moderate", R.attr.colorMedium, R.string.off_overexposure_risk_moderate_label),
    HIGH("en:high",R.attr.colorNegative, R.string.off_overexposure_risk_high_label),
    NONE("", R.attr.colorUnknown, R.string.off_overexposure_risk_none_label)
}