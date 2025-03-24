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

import com.atharok.barcodescanner.common.utils.FAT_VALUE_HIGH
import com.atharok.barcodescanner.common.utils.FAT_VALUE_LOW
import com.atharok.barcodescanner.common.utils.SALT_VALUE_HIGH
import com.atharok.barcodescanner.common.utils.SALT_VALUE_LOW
import com.atharok.barcodescanner.common.utils.SATURATED_FAT_VALUE_HIGH
import com.atharok.barcodescanner.common.utils.SATURATED_FAT_VALUE_LOW
import com.atharok.barcodescanner.common.utils.SUGAR_VALUE_HIGH
import com.atharok.barcodescanner.common.utils.SUGAR_VALUE_LOW
import com.atharok.barcodescanner.data.model.openFoodFactsResponse.FoodProductResponse
import com.atharok.barcodescanner.data.model.openFoodFactsResponse.IngredientResponse
import com.atharok.barcodescanner.data.model.openFoodFactsResponse.NutrimentsResponse

fun getNutriscore(productResponse: FoodProductResponse?): Nutriscore {
    return when(productResponse?.nutritionGrades){
        "a" -> Nutriscore.A
        "b" -> Nutriscore.B
        "c" -> Nutriscore.C
        "d" -> Nutriscore.D
        "e" -> Nutriscore.E
        else  -> Nutriscore.UNKNOWN
    }
}

fun getEcoScore(productResponse: FoodProductResponse?): EcoScore {
    return when(productResponse?.ecoScoreGrade){
        "a" -> EcoScore.A
        "b" -> EcoScore.B
        "c" -> EcoScore.C
        "d" -> EcoScore.D
        "e" -> EcoScore.E
        else  -> EcoScore.UNKNOWN
    }
}

fun getNovaGroup(productResponse: FoodProductResponse?): NovaGroup {
    return when(productResponse?.novaGroup) {
        1 -> NovaGroup.GROUP_1
        2 -> NovaGroup.GROUP_2
        3 -> NovaGroup.GROUP_3
        4 -> NovaGroup.GROUP_4
        else -> NovaGroup.UNKNOWN
    }
}

fun getVeggieIngredientAnalysisList(productResponse: FoodProductResponse?): List<VeggieIngredientAnalysis>? {

    val ingredientsList: List<IngredientResponse>? = productResponse?.ingredientsResponses

    if (ingredientsList.isNullOrEmpty()) return null

    val veggieIngredientAnalysisList = mutableListOf<VeggieIngredientAnalysis>()

    for(ingredient in ingredientsList){

        veggieIngredientAnalysisList.add(
            VeggieIngredientAnalysis(
                ingredient.text,
                determineVeganStatus(ingredient.vegan),
                determineVegetarianStatus(ingredient.vegetarian)
            )
        )
    }
    return veggieIngredientAnalysisList
}

private fun determineVeganStatus(veganTxt: String?): VeganStatus {
    return when(veganTxt){
        "yes" -> VeganStatus.VEGAN
        "no" -> VeganStatus.NO_VEGAN
        "maybe" -> VeganStatus.MAYBE_VEGAN
        else -> VeganStatus.UNKNOWN_VEGAN
    }
}

private fun determineVegetarianStatus(veganTxt: String?): VegetarianStatus {
    return when(veganTxt){
        "yes" -> VegetarianStatus.VEGETARIAN
        "no" -> VegetarianStatus.NO_VEGETARIAN
        "maybe" -> VegetarianStatus.MAYBE_VEGETARIAN
        else -> VegetarianStatus.UNKNOWN_VEGETARIAN
    }
}

fun getPalmOilStatus(productResponse: FoodProductResponse?): PalmOilStatus {

    var status: PalmOilStatus = PalmOilStatus.UNKNOWN_PALM_OIL
    productResponse?.ingredientsAnalysisTags?.forEach {
        when(it){
            "en:palm-oil-free" -> status = PalmOilStatus.PALM_OIL_FREE
            "en:palm-oil" -> status = PalmOilStatus.PALM_OIL
            "en:may-contain-palm-oil" -> status = PalmOilStatus.MAYBE_PALM_OIL
            "en:palm-oil-content-unknown" -> status = PalmOilStatus.UNKNOWN_PALM_OIL
        }
    }

    return status
}

fun getVeganStatus(productResponse: FoodProductResponse?): VeganStatus {

    var status: VeganStatus = VeganStatus.UNKNOWN_VEGAN
    productResponse?.ingredientsAnalysisTags?.forEach {
        when(it){
            "en:vegan" -> status = VeganStatus.VEGAN
            "en:non-vegan" -> status = VeganStatus.NO_VEGAN
            "en:maybe-vegan" -> status = VeganStatus.MAYBE_VEGAN
            "en:vegan-status-unknown" -> status = VeganStatus.UNKNOWN_VEGAN
        }
    }

    return status
}

fun getVegetarianStatus(productResponse: FoodProductResponse?): VegetarianStatus {

    var status: VegetarianStatus = VegetarianStatus.UNKNOWN_VEGETARIAN
    productResponse?.ingredientsAnalysisTags?.forEach {
        when(it){
            "en:vegetarian" -> status = VegetarianStatus.VEGETARIAN
            "en:non-vegetarian" -> status = VegetarianStatus.NO_VEGETARIAN
            "en:maybe-vegetarian" -> status = VegetarianStatus.MAYBE_VEGETARIAN
            "en:vegetarian-status-unknown" -> status = VegetarianStatus.UNKNOWN_VEGETARIAN
        }
    }

    return status
}

fun createNutrientsList(productResponse: FoodProductResponse?): List<Nutrient>{
    val nutrimentsResponse = productResponse?.nutrimentsResponse
    val isBeverage = productResponse?.nutritionScoreBeverage==1

    val energyKJ = createEnergyKJNutrient(nutrimentsResponse)
    val energyKcal = createEnergyKcalNutrient(nutrimentsResponse)
    val fat = createFatNutrient(nutrimentsResponse, isBeverage)
    val saturatedFat = createSaturatedFatNutrient(nutrimentsResponse, isBeverage)
    val carbohydrates = createCarbohydratesNutrient(nutrimentsResponse)
    val sugars = createSugarsNutrient(nutrimentsResponse, isBeverage)
    val starch = createStarchNutrient(nutrimentsResponse)
    val fiber = createFiberNutrient(nutrimentsResponse)
    val proteins = createProteinsNutrient(nutrimentsResponse)
    val salt = createSaltNutrient(nutrimentsResponse, isBeverage)
    val sodium = createSodiumNutrient(nutrimentsResponse)

    return listOfNotNull(energyKJ, energyKcal, fat, saturatedFat, carbohydrates, sugars, starch, fiber, proteins, salt, sodium)
}

private fun createEnergyKJNutrient(nutrimentsResponse: NutrimentsResponse?): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.ENERGY_KJ,
    value100g = nutrimentsResponse?.energyKj100g,
    valueServing = nutrimentsResponse?.energyKjServing,
    unit = nutrimentsResponse?.energyKjUnit
)

private fun createEnergyKcalNutrient(nutrimentsResponse: NutrimentsResponse?): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.ENERGY_KCAL,
    value100g = nutrimentsResponse?.energyKcal100g,
    valueServing = nutrimentsResponse?.energyKcalServing,
    unit = nutrimentsResponse?.energyKcalUnit
)

private fun createFatNutrient(nutrimentsResponse: NutrimentsResponse?, isBeverage: Boolean): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.FAT,
    value100g = nutrimentsResponse?.fat100g,
    valueServing = nutrimentsResponse?.fatServing,
    unit = nutrimentsResponse?.fatUnit,
    valueLow = FAT_VALUE_LOW,
    valueHigh = FAT_VALUE_HIGH,
    isBeverage = isBeverage
)

private fun createSaturatedFatNutrient(nutrimentsResponse: NutrimentsResponse?, isBeverage: Boolean): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.SATURATED_FAT,
    value100g = nutrimentsResponse?.saturatedFat100g,
    valueServing = nutrimentsResponse?.saturatedFatServing,
    unit = nutrimentsResponse?.saturatedFatUnit,
    valueLow = SATURATED_FAT_VALUE_LOW,
    valueHigh = SATURATED_FAT_VALUE_HIGH,
    isBeverage = isBeverage
)

private fun createCarbohydratesNutrient(nutrimentsResponse: NutrimentsResponse?): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.CARBOHYDRATES,
    value100g = nutrimentsResponse?.carbohydrates100g,
    valueServing = nutrimentsResponse?.carbohydratesServing,
    unit = nutrimentsResponse?.carbohydratesUnit
)

private fun createSugarsNutrient(nutrimentsResponse: NutrimentsResponse?, isBeverage: Boolean): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.SUGARS,
    value100g = nutrimentsResponse?.sugars100g,
    valueServing = nutrimentsResponse?.sugarsServing,
    unit = nutrimentsResponse?.sugarsUnit,
    valueLow = SUGAR_VALUE_LOW,
    valueHigh = SUGAR_VALUE_HIGH,
    isBeverage = isBeverage
)

private fun createStarchNutrient(nutrimentsResponse: NutrimentsResponse?): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.STARCH,
    value100g = nutrimentsResponse?.starch100g,
    valueServing = nutrimentsResponse?.starchServing,
    unit = nutrimentsResponse?.starchUnit
)

private fun createFiberNutrient(nutrimentsResponse: NutrimentsResponse?): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.FIBER,
    value100g = nutrimentsResponse?.fiber100g,
    valueServing = nutrimentsResponse?.fiberServing,
    unit = nutrimentsResponse?.fiberUnit
)

private fun createProteinsNutrient(nutrimentsResponse: NutrimentsResponse?): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.PROTEINS,
    value100g = nutrimentsResponse?.proteins100g,
    valueServing = nutrimentsResponse?.proteinsServing,
    unit = nutrimentsResponse?.proteinsUnit
)

private fun createSaltNutrient(nutrimentsResponse: NutrimentsResponse?, isBeverage: Boolean): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.SALT,
    value100g = nutrimentsResponse?.salt100g,
    valueServing = nutrimentsResponse?.saltServing,
    unit = nutrimentsResponse?.saltUnit,
    valueLow = SALT_VALUE_LOW,
    valueHigh = SALT_VALUE_HIGH,
    isBeverage = isBeverage
)

private fun createSodiumNutrient(nutrimentsResponse: NutrimentsResponse?): Nutrient? = createNutrient(
    nutritionFactsEnum = NutritionFactsEnum.SODIUM,
    value100g = nutrimentsResponse?.sodium100g,
    valueServing = nutrimentsResponse?.sodiumServing,
    unit = nutrimentsResponse?.sodiumUnit
)

private fun createNutrient(nutritionFactsEnum: NutritionFactsEnum,
                           value100g: Number?,
                           valueServing: Number?,
                           unit: String?,
                           valueLow: Float? = null,
                           valueHigh: Float? = null,
                           isBeverage: Boolean? = null): Nutrient? {

    // Dans NutrientsResponse, on récupère les valeurs Number au format com.google.gson.internal.LazilyParsedNumber.
    // Si il n y a pas de valeur on obtient une valeur vide et non null. Si c'est le cas on fait en sorte de récupérer une valeur null.
    val newValue100g = if(value100g.toString().isBlank()) null else value100g
    val newValueServing = if(valueServing.toString().isBlank()) null else valueServing

    if(newValue100g == null && newValueServing == null) return null

    val values = Nutrient.NutrientValues(newValue100g, newValueServing, unit ?: "")
    val quantity = createQuantity(valueLow, valueHigh, isBeverage)
    return Nutrient(nutritionFactsEnum, values, quantity)
}

private fun createQuantity(valueLow: Float? = null,
                           valueHigh: Float? = null,
                           isBeverage: Boolean? = null): Nutrient.Quantity? {
    if(valueLow == null || valueHigh == null || isBeverage == null) return null

    return Nutrient.Quantity(valueLow, valueHigh, isBeverage)
}