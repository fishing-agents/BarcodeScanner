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

import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.atharok.barcodescanner.R

fun Preference.initializeCustomResourcesValues(holder: PreferenceViewHolder?) {
    holder?.itemView?.let {
        val titleView: TextView? = it.findViewById(android.R.id.title)
        titleView?.setTextAppearance(R.style.AppTheme_TextView_Appearance_Normal_Primary)

        val summaryView: TextView? = it.findViewById(android.R.id.summary)
        summaryView?.setTextAppearance(R.style.AppTheme_TextView_Appearance_Normal_Secondary)
    }
}