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

package com.atharok.barcodescanner.domain.library

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.preference.PreferenceManager
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.domain.entity.barcode.QrCodeErrorCorrectionLevel
import com.atharok.barcodescanner.presentation.intent.createSearchUrlIntent
import com.atharok.barcodescanner.presentation.intent.createWebSearchIntent
import androidx.core.content.edit

class SettingsManager(private val context: Context) {

    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Appearance
    private val colorKey = context.getString(R.string.preferences_color_key)
    private val themeKey = context.getString(R.string.preferences_theme_key)
    private var color = prefs.getString(colorKey, getDefaultColorKey())
    private var theme = prefs.getString(themeKey, "system")

    // Remote API
    private val searchOnApiKey = context.getString(R.string.preferences_switch_scan_search_on_api_key)
    private val apiChooseKey = context.getString(R.string.preferences_remote_api_choose_key)

    var useSearchOnApi = prefs.getBoolean(searchOnApiKey, true)
        private set
    var apiChoose = prefs.getString(apiChooseKey, context.getString(R.string.preferences_entry_value_food))
        private set

    private val saveDuplicateKey = context.getString(R.string.preferences_switch_history_save_duplicates_key)

    // Scan
    private val vibrateScanKey = context.getString(R.string.preferences_switch_scan_vibrate_key)
    private val bipScanKey = context.getString(R.string.preferences_switch_scan_bip_key)
    private val autoScreenRotationScanDisabledKey = context.getString(R.string.preferences_switch_scan_screen_rotation_key)
    private val copyBarcodeScanKey = context.getString(R.string.preferences_switch_scan_barcode_copied_key)
    private val addBarcodeToHistoryScanKey = context.getString(R.string.preferences_switch_scan_add_barcode_to_the_history_key)
    private val defaultZoomValueKey = context.getString(R.string.preferences_seek_bar_camera_default_zoom_value_key)
    private val barcodeTypeFilterKey = context.getString(R.string.preferences_switch_scan_barcode_type_filter_key)
    private val rateLimitEnabledKey = context.getString(R.string.preferences_switch_scan_rate_limit_key)
    private val rateLimitDurationKey = context.getString(R.string.preferences_scan_rate_limit_duration_key)

    var saveDuplicates = prefs.getBoolean(saveDuplicateKey, true)
        private set

    var useVibrateScan = prefs.getBoolean(vibrateScanKey, false)
        private set
    var useBipScan = prefs.getBoolean(bipScanKey, false)
        private set
    var isAutoScreenRotationScanDisabled = prefs.getBoolean(autoScreenRotationScanDisabledKey, true)
        private set
    var shouldCopyBarcodeScan = prefs.getBoolean(copyBarcodeScanKey, false)
        private set
    var shouldAddBarcodeScanToHistory = prefs.getBoolean(addBarcodeToHistoryScanKey, true)
        private set

    // Returns a set of allowed barcode formats, empty set means all formats are allowed
    var allowedBarcodeFormats = prefs.getStringSet(barcodeTypeFilterKey, emptySet()) ?: emptySet()
        private set

    var isRateLimitEnabled = prefs.getBoolean(rateLimitEnabledKey, false)
        private set
    var rateLimitDurationSeconds = prefs.getString(rateLimitDurationKey, "10")?.toIntOrNull() ?: 10
        private set

    // Barcode Generation
    private val errorCorrectionLevelKey = context.getString(R.string.preferences_barcode_generation_error_correction_level_key)
    private val addBarcodeToHistoryGenerateKey = context.getString(R.string.preferences_switch_barcode_generation_add_barcode_to_the_history_key)

    private var errorCorrectionLevelEntry = prefs.getString(errorCorrectionLevelKey, "low")
    var shouldAddBarcodeGenerateToHistory = prefs.getBoolean(addBarcodeToHistoryGenerateKey, false)
        private set

    // Search
    private val searchEngineKey = context.getString(R.string.preferences_search_engine_key)
    private var defaultSearchEngine = prefs.getString(searchEngineKey, "google")

    // Additional options
    private val displayBarcodeInResultsViewKey = context.getString(R.string.preferences_display_barcode_image_results_view_key)
    var shouldDisplayBarcodeInResultsView = prefs.getBoolean(displayBarcodeInResultsViewKey, false)
        private set

    fun reload() {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)

        color = prefs.getString(colorKey, getDefaultColorKey())
        theme = prefs.getString(themeKey, "system")
        useSearchOnApi = prefs.getBoolean(searchOnApiKey, true)
        apiChoose = prefs.getString(apiChooseKey, context.getString(R.string.preferences_entry_value_food))
        saveDuplicates = prefs.getBoolean(saveDuplicateKey, true)
        useVibrateScan = prefs.getBoolean(vibrateScanKey, false)
        useBipScan = prefs.getBoolean(bipScanKey, false)
        isAutoScreenRotationScanDisabled = prefs.getBoolean(autoScreenRotationScanDisabledKey, true)
        shouldCopyBarcodeScan = prefs.getBoolean(copyBarcodeScanKey, false)
        shouldAddBarcodeScanToHistory = prefs.getBoolean(addBarcodeToHistoryScanKey, true)
        allowedBarcodeFormats = prefs.getStringSet(barcodeTypeFilterKey, emptySet()) ?: emptySet()
        isRateLimitEnabled = prefs.getBoolean(rateLimitEnabledKey, false)
        rateLimitDurationSeconds = prefs.getString(rateLimitDurationKey, "10")?.toIntOrNull() ?: 10
        errorCorrectionLevelEntry = prefs.getString(errorCorrectionLevelKey, "low")
        shouldAddBarcodeGenerateToHistory = prefs.getBoolean(addBarcodeToHistoryGenerateKey, false)
        defaultSearchEngine = prefs.getString(searchEngineKey, "google")
        shouldDisplayBarcodeInResultsView = prefs.getBoolean(displayBarcodeInResultsViewKey, false)
    }

    fun getTheme(): Int {
        return when(theme) {
            "system" -> if(isDarkThemeSystemOn()) getDarkTheme() else getLightTheme()
            "light" -> getLightTheme()
            "dark" -> getDarkTheme()
            "black" -> getBlackTheme()
            else -> if(isDarkThemeSystemOn()) getDarkTheme() else getLightTheme()
        }
    }

    private fun getLightTheme(): Int {
        return when(color){
            "material_you" -> if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.style.MaterialYouLightTheme else R.style.BlueLightTheme
            "blue" -> R.style.BlueLightTheme
            "orange" -> R.style.OrangeLightTheme
            "green" -> R.style.GreenLightTheme
            "red" -> R.style.RedLightTheme
            "purple" -> R.style.PurpleLightTheme
            else -> R.style.BlueLightTheme
        }
    }

    private fun getDarkTheme(): Int {
        return when(color){
            "material_you" -> if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.style.MaterialYouDarkTheme else R.style.BlueDarkTheme
            "blue" -> R.style.BlueDarkTheme
            "orange" -> R.style.OrangeDarkTheme
            "green" -> R.style.GreenDarkTheme
            "red" -> R.style.RedDarkTheme
            "purple" -> R.style.PurpleDarkTheme
            else -> R.style.BlueDarkTheme
        }
    }

    private fun getBlackTheme(): Int {
        return when(color){
            "material_you" -> if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.style.MaterialYouBlackTheme else R.style.BlueBlackTheme
            "blue" -> R.style.BlueBlackTheme
            "orange" -> R.style.OrangeBlackTheme
            "green" -> R.style.GreenBlackTheme
            "red" -> R.style.RedBlackTheme
            "purple" -> R.style.PurpleBlackTheme
            else -> R.style.BlueBlackTheme
        }
    }

    fun getQrCodeErrorCorrectionLevel(): QrCodeErrorCorrectionLevel = when(errorCorrectionLevelEntry){
        "low" -> QrCodeErrorCorrectionLevel.L
        "medium" -> QrCodeErrorCorrectionLevel.M
        "quartile" -> QrCodeErrorCorrectionLevel.Q
        "high" -> QrCodeErrorCorrectionLevel.H
        else -> QrCodeErrorCorrectionLevel.L
    }

    private fun getSearchEngineUrl(contents: String): String = when(defaultSearchEngine){
        "google" -> context.getString(R.string.search_engine_google_url, contents)
        "bing" -> context.getString(R.string.search_engine_bing_url, contents)
        "duckduckgo" -> context.getString(R.string.search_engine_duck_duck_go_url, contents)
        "startpage" -> context.getString(R.string.search_engine_startpage_url, contents)
        "bravesearch" -> context.getString(R.string.search_engine_brave_search_url, contents)
        "qwant" -> context.getString(R.string.search_engine_qwant_url, contents)
        "ecosia" -> context.getString(R.string.search_engine_ecosia_url, contents)
        "lilo" -> context.getString(R.string.search_engine_lilo_url, contents)
        else -> context.getString(R.string.search_engine_google_url, contents)
    }

    fun getSearchEngineIntent(contents: String): Intent {
        return if(defaultSearchEngine == "default") {
            createWebSearchIntent(contents)
        } else {
            createSearchUrlIntent(getSearchEngineUrl(contents))
        }
    }

    fun useDarkTheme(): Boolean = when(theme){
        "system" -> isDarkThemeSystemOn()
        "light" -> false
        "dark", "black" -> true
        else -> isDarkThemeSystemOn()
    }

    private fun isDarkThemeSystemOn(): Boolean =
        context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES

    private fun getDefaultColorKey(): String =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "material_you" else "blue"

    fun getDefaultZoomValue(): Int = prefs.getInt(defaultZoomValueKey, 50)

    fun updateAllowedBarcodeFormats(formats: Set<String>) {
        prefs.edit { putStringSet(barcodeTypeFilterKey, formats) }
        allowedBarcodeFormats = formats
    }
}