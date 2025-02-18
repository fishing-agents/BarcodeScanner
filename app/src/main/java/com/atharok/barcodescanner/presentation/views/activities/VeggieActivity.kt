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

package com.atharok.barcodescanner.presentation.views.activities

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.common.extensions.serializable
import com.atharok.barcodescanner.common.utils.BARCODE_ANALYSIS_KEY
import com.atharok.barcodescanner.databinding.ActivityVeggieBinding
import com.atharok.barcodescanner.domain.entity.analysis.FoodBarcodeAnalysis
import com.atharok.barcodescanner.domain.entity.product.foodProduct.VeggieIngredientAnalysis
import com.atharok.barcodescanner.presentation.views.recyclerView.veggie.VeggieItemAdapter

class VeggieActivity : BaseActivity() {

    private val viewBinding: ActivityVeggieBinding by lazy { ActivityVeggieBinding.inflate(layoutInflater) }
    override val rootView: View get() = viewBinding.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureToolbar()

        val foodProduct: FoodBarcodeAnalysis? = intent.serializable(BARCODE_ANALYSIS_KEY, FoodBarcodeAnalysis::class.java)

        val veggieIngredientsList = foodProduct?.veggieIngredientList
        if(veggieIngredientsList.isNullOrEmpty()) {
            viewBinding.activityVeggieNestedScrollView.visibility = View.GONE
            viewBinding.activityVeggieNoIngredientsTextView.visibility = View.VISIBLE
        } else {
            configureRecyclerView(veggieIngredientsList)
            viewBinding.activityVeggieNestedScrollView.visibility = View.VISIBLE
            viewBinding.activityVeggieNoIngredientsTextView.visibility = View.GONE
        }

        setContentView(rootView)
    }

    private fun configureToolbar() {
        setSupportActionBar(viewBinding.activityVeggieActivityLayout.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)// On affiche l'icone "retour"
            it.setTitle(R.string.ingredients_analysis_label)
        }
    }

    private fun configureRecyclerView(veggieIngredientList: List<VeggieIngredientAnalysis>) {
        val linearLayoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = DividerItemDecoration(this, linearLayoutManager.orientation)
        val adapter = VeggieItemAdapter(veggieIngredientList)
        viewBinding.activityVeggieRecyclerView.let { recyclerView ->
            recyclerView.isNestedScrollingEnabled = false
            recyclerView.adapter = adapter
            recyclerView.layoutManager = linearLayoutManager
            recyclerView.addItemDecoration(dividerItemDecoration)
        }
    }
}