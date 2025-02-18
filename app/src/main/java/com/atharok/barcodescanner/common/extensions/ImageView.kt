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

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import coil3.load

fun ImageView.setImageFromWeb(url: String?, layout: View? = null){
    if(url.isNullOrBlank()){
        layout?.visibility = View.GONE
        this.visibility = View.GONE
    } else {
        this.load(url)
    }
}

fun ImageView.setImageColorFromAttrRes(@AttrRes attrRes: Int){
    @ColorInt val color = context.getColorInt(attrRes)
    imageTintList = ColorStateList.valueOf(color)
}