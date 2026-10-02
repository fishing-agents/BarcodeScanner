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

import android.graphics.Bitmap
import com.atharok.barcodescanner.domain.library.scan.ScanEngine
import com.google.zxing.Result

/** Finds a barcode in a static image (gallery, share, shortcut) with the same engine the camera uses. */
class BarcodeBitmapAnalyser(private val engine: ScanEngine) {

    fun detectBarcodeFromBitmap(bitmap: Bitmap): Result? = engine.scan(bitmap).codes.firstOrNull()
}
