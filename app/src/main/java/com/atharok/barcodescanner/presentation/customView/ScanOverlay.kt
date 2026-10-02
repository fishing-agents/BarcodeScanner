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

package com.atharok.barcodescanner.presentation.customView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import androidx.core.graphics.toColorInt
import android.view.View

/**
 * Touch surface for zoom/focus gestures, and the multi-code picker: a frozen preview frame
 * with a tappable ring on each detected code. Scanning is full-frame, so there is no viewfinder.
 */
class ScanOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private val density = resources.displayMetrics.density
    private val markerRadius = 16f * density
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#4CAF50".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
    }
    private val frameBounds = Rect()

    private var frozenFrame: Bitmap? = null
    private var markers: List<PointF> = emptyList()

    fun showPicker(frame: Bitmap?, markerPoints: List<PointF>) {
        frozenFrame = frame
        markers = markerPoints
        invalidate()
    }

    fun clearPicker() {
        frozenFrame = null
        markers = emptyList()
        invalidate()
    }

    /** Index of the marker within a finger-sized radius of (x, y), or -1. */
    fun hitTestMarker(x: Float, y: Float): Int {
        val reach = markerRadius * 2
        return markers.indexOfFirst { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) <= reach * reach }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frozenFrame?.let {
            frameBounds.set(0, 0, width, height)
            canvas.drawBitmap(it, null, frameBounds, null)
        }
        markers.forEach { canvas.drawCircle(it.x, it.y, markerRadius, markerPaint) }
    }
}
