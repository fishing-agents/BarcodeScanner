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

package com.atharok.barcodescanner.common.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat

fun Context.resolveThemeAttr(@AttrRes attrRes: Int): TypedValue {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue
}

@ColorRes
fun Context.getColorRes(@AttrRes attrRes: Int): Int {
    val resolvedAttr = resolveThemeAttr(attrRes)
    // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
    return if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
}

@ColorInt
fun Context.getColorInt(@AttrRes attrRes: Int): Int {
    @ColorRes val colorRes = getColorRes(attrRes)
    return ContextCompat.getColor(this, colorRes)
}


fun Context.getColorStateListFromAttrRes(@AttrRes attrRes: Int): ColorStateList {
    @ColorRes val colorRes = getColorRes(attrRes)
    return AppCompatResources.getColorStateList(this, colorRes)
}

fun Context.hasFlash(): Boolean = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)