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
import com.atharok.barcodescanner.R
import java.io.Serializable

class Nutrient(val entitled: NutritionFactsEnum,
               val values: NutrientValues,
               private val quantity: Quantity? = null): Serializable {

    fun getQuantityValue(): QuantityRate = quantity?.determineQuantity(values.value100g) ?: QuantityRate.NONE
    fun getLowQuantity(): Float? = quantity?.getLowQuantity()
    fun getHighQuantity(): Float? = quantity?.getHighQuantity()

    class NutrientValues(val value100g: Number?, val valueServing: Number?, val unit: String): Serializable {

        fun getValue100gString(): String = getValueString(value100g)
        fun getValueServingString(): String = getValueString(valueServing)

        private fun getValueString(number: Number?): String {
            val numberStr = number.toString()
            return when {
                numberStr == "null" || numberStr.isBlank() -> "-"
                else -> {
                    try {
                        "${round(number?.toFloat())}$unit"
                    } catch (e: Exception) {
                        "$number$unit"
                    }
                }
            }
        }

        //On arrondie à 2 chiffres après la virgule, et on supprime le dernier caractère si il est à 0
        private fun round(value: Float?): String = "%.2f".format(value).removeSuffix("0")
    }

    class Quantity(private val lowQuantity: Float,
                   private val highQuantity: Float,
                   private val isBeverage: Boolean = false): Serializable {

        fun determineQuantity(value100g: Number?): QuantityRate {

            return if(value100g == null){
                QuantityRate.NONE
            }else{

                val div: Int = if(isBeverage) 2 else 1 // -> Pour les boissons on divise toutes les valeures par 2

                when {
                    value100g.toFloat()>highQuantity/div -> QuantityRate.HIGH
                    value100g.toFloat()<lowQuantity/div -> QuantityRate.LOW
                    else -> QuantityRate.MODERATE
                }
            }
        }

        fun getLowQuantity(): Float = if (isBeverage) lowQuantity / 2 else lowQuantity

        fun getHighQuantity(): Float = if (isBeverage) highQuantity / 2 else highQuantity
    }

    enum class QuantityRate(@param:AttrRes val colorResource: Int, val stringResource: Int) {
        LOW(R.attr.colorPositive, R.string.off_quantity_low_label),
        MODERATE(R.attr.colorMedium, R.string.off_quantity_moderate_label),
        HIGH(R.attr.colorNegative, R.string.off_quantity_high_label),
        NONE(R.attr.colorUnknown, R.string.off_quantity_none_label)
    }
}