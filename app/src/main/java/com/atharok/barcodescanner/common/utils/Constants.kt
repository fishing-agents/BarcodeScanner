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

package com.atharok.barcodescanner.common.utils

// -------------------------------------- Scope Name Session ---------------------------------------

const val KOIN_NAMED_ERROR_CORRECTION_LEVEL_BY_RESULT = "koinNamedErrorCorrectionLevelByResult"
const val KOIN_NAMED_ERROR_CORRECTION_LEVEL_BY_STRING = "koinNamedErrorCorrectionLevelByString"

// ------------------------------------------ BUNDLE KEY -------------------------------------------

// Clé du Bundle associé à l'URI d'une image.
const val IMAGE_URI_KEY = "imageUriKey"

// Clé du Bundle associé au type (et sous type) BarcodeAnalysis
const val BARCODE_ANALYSIS_KEY = "barcodeAnalysisKey"

// Clé du Bundle associé au type Barcode
const val BARCODE_KEY = "barcodeKey"

// Clé de l'intent permettant la récupération des données de Bank entre BarcodeFormCreatorQrEpcFragment et BarcodeEpcTemplateListActivity.
const val BANK_KEY = "bankKey"

const val CUSTOM_URL_KEY = "customUrlKey"

// Clé du Bundle associé au type AllBarCodeCreatorType
const val BARCODE_TYPE_ENUM_KEY = "barcodeTypeEnumKey" // Clé de l'intent contenant le type de code-barres à générer (AllBarCodeCreatorType: QR_TEXT, QR_AGENDA, AZTEC, EAN_13, EAN_8, UPC_A, etc...)

// Clé du Bundle associé au type String, contenant le contents du Barcode
const val BARCODE_CONTENTS_KEY = "barcodeStringKey" // Clé de l'intent contenant le contenu du code-barres dans la transition entre le formulaire de création de code-barres et le résultat

// Clé du Bundle associé au type BarcodeFormat
const val BARCODE_FORMAT_KEY = "barcodeFormatKey"

// Clé du Bundle associé au type QrCodeErrorCorrectionLevel
const val QR_CODE_ERROR_CORRECTION_LEVEL_KEY = "qrCodeErrorCorrectionLevelKey"

// Clé de l'intent associé au type BarcodeImageGeneratorSettings (Serializable)
const val BARCODE_IMAGE_GENERATOR_PROPERTIES_KEY = "barcodeImageGeneratorPropertiesKey"

const val BARCODE_IMAGE_FRONT_COLOR_KEY = "barcodeImageFrontColorKey"
const val BARCODE_IMAGE_BACKGROUND_COLOR_KEY = "barcodeImageBackgroundColorKey"
const val BARCODE_IMAGE_CORNER_RADIUS_KEY = "barcodeImageCornerRadiusKey"
const val BARCODE_IMAGE_WIDTH_KEY = "barcodeImageWidthKey"
const val BARCODE_IMAGE_HEIGHT_KEY = "barcodeImageHeightKey"
const val BARCODE_IMAGE_MARGINS_KEY = "barcodeImageMarginsKey"

// ------------------------------------------- API Links -------------------------------------------

// ---- URL des fichiers complémentaires pour OpenFoodFacts ----
const val LABELS_LOCALE_FILE_NAME = "labels.json"
const val LABELS_URL = "https://world.openfoodfacts.org/labels.json"

const val ADDITIVES_LOCALE_FILE_NAME = "additives.json"
const val ADDITIVES_URL = "https://world.openfoodfacts.org/data/taxonomies/additives.json"

const val ADDITIVES_CLASSES_LOCALE_FILE_NAME = "additives_classes.json"
const val ADDITIVES_CLASSES_URL = "https://world.openfoodfacts.org/data/taxonomies/additives_classes.json"

const val ALLERGENS_LOCALE_FILE_NAME = "allergens.json"
const val ALLERGENS_URL = "https://world.openfoodfacts.org/data/taxonomies/allergens.json"

const val COUNTRIES_LOCALE_FILE_NAME = "countries.json"
const val COUNTRIES_URL = "https://world.openfoodfacts.org/data/taxonomies/countries.json"

/*const val INGREDIENTS_ANALYSIS_LOCALE_FILE_NAME = "ingredients_analysis.json"
const val INGREDIENTS_ANALYSIS_URL = "https://world.openfoodfacts.org/data/taxonomies/ingredients_analysis.json"*/

// ----------------------------------------- Static Values -----------------------------------------

const val DATABASE_NAME = "scan_history.db"
const val ENCODING_UTF_8 = "UTF-8"
const val ENCODING_ISO_8859_1 = "ISO-8859-1"
const val BARCODE_IMAGE_DEFAULT_SIZE = 1024
const val BARCODE_2_DIMENSION_IMAGE_DEFAULT_MARGINS_PX = 128
const val BARCODE_1_DIMENSION_IMAGE_DEFAULT_MARGINS_PX = 32

// ---- Valeures indicatives de la quantité des substances dans les produits alimentaires ----

const val FAT_VALUE_LOW = 3.0f
const val FAT_VALUE_HIGH = 20.0f

const val SATURATED_FAT_VALUE_LOW = 1.5f
const val SATURATED_FAT_VALUE_HIGH = 5.0f

const val SUGAR_VALUE_LOW = 5.0f
const val SUGAR_VALUE_HIGH = 12.5f

const val SALT_VALUE_LOW = 0.3f
const val SALT_VALUE_HIGH = 1.5f

// ---- Barcode contents length ----

const val EAN_13_LENGTH = 13
const val EAN_8_LENGTH = 8
const val UPC_A_LENGTH = 12
const val UPC_E_LENGTH = 8
const val CODE_39_LENGTH = 80
const val CODE_93_LENGTH = 80
const val CODE_128_LENGTH = 80
const val ITF_LENGTH = 80
const val PDF_417_LENGTH = 2710

// ----

const val CUSTOM_SEARCH_URL_BARCODE_KEY_WORD = "{barcode}"
