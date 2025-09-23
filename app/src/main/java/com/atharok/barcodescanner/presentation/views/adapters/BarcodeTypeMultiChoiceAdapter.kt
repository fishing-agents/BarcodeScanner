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

package com.atharok.barcodescanner.presentation.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.atharok.barcodescanner.R

class BarcodeTypeMultiChoiceAdapter(
    context: Context,
    private val items: List<Triple<String, String, Int>>, // format name, display name, icon resource
    private val checkedItems: BooleanArray
): ArrayAdapter<Triple<String, String, Int>>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.template_dialog_multi_choice_item, parent, false
        )

        val checkbox = view.findViewById<CheckBox>(R.id.template_dialog_multi_choice_checkbox)
        val textView = view.findViewById<TextView>(R.id.template_dialog_multi_choice_text_view)
        val imageView = view.findViewById<ImageView>(R.id.template_dialog_multi_choice_image_view)

        val item = items[position]
        textView.text = item.second
        imageView.setImageResource(item.third)
        checkbox.isChecked = checkedItems[position]

        // Handle checkbox clicks
        checkbox.setOnClickListener {
            checkedItems[position] = checkbox.isChecked
        }

        // Make the entire row clickable
        view.setOnClickListener {
            checkbox.isChecked = !checkbox.isChecked
            checkedItems[position] = checkbox.isChecked
        }

        return view
    }
}