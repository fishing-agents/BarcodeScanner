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

package com.atharok.barcodescanner.data.file.image

import com.atharok.barcodescanner.common.extensions.toColorAlpha
import com.atharok.barcodescanner.common.extensions.toColorHex
import com.atharok.barcodescanner.domain.library.BarcodeImageGeneratorProperties
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

/**
 * Génère l'image d'un code-barres à partir d'un texte.
 */
class BarcodeSvgGenerator(multiFormatWriter: MultiFormatWriter): BarcodeImageGenerator<String>(multiFormatWriter) {

    override fun createImageBarcode(
        properties: BarcodeImageGeneratorProperties,
        matrix: BitMatrix
    ): String {
        val builder = StringBuilder()
        buildSvgBegin(builder, totalWidth = properties.width, totalHeight = properties.height)
        buildSvgImageContent(builder, properties = properties, matrix = matrix)
        if(!properties.is2DBarcode)
            buildSvgTextContent(builder, properties = properties)
        buildSvgEnd(builder)
        return builder.toString()
    }

    // ---- SVG build ----

    private fun buildSvgBegin(builder: StringBuilder, totalWidth: Int, totalHeight: Int) {
        builder.append("<svg width=\"$totalWidth\" height=\"$totalHeight\" viewBox=\"0 0 $totalWidth $totalHeight\" xmlns=\"http://www.w3.org/2000/svg\">\n")
    }

    private fun buildSvgEnd(builder: StringBuilder) {
        builder.append("</svg>\n")
    }

    private fun buildSvgImageContent(builder: StringBuilder, properties: BarcodeImageGeneratorProperties, matrix: BitMatrix) {

        // ---- Background ----
        val backgroundFillColor = properties.backgroundColor.toColorHex()
        val backgroundAlpha: Float = properties.backgroundColor.toColorAlpha()
        builder.append("<rect x=\"0\" y=\"0\" width=\"${properties.width}\" height=\"${properties.height}\" style=\"fill:$backgroundFillColor;fill-opacity:$backgroundAlpha\"/>\n")

        // ---- Foreground ----
        val margins = properties.marginsPx

        val bitWidth: Float = (properties.widthF - 2 * margins) / matrix.width.toFloat()
        val bitHeight: Float = (properties.heightF - properties.contentsHeight - 2 * margins) / matrix.height.toFloat()
        val cornerRadius = bitWidth / 2f * properties.cornerRadius
        val foregroundFillColor = properties.frontColor.toColorHex()
        val foregroundAlpha: Float = properties.frontColor.toColorAlpha()

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) {
                    val posX = x * bitWidth + margins
                    val posY = y * bitHeight + margins
                    builder.append("<rect x=\"$posX\" y=\"$posY\" width=\"$bitWidth\" height=\"$bitHeight\" rx=\"$cornerRadius\" ry=\"$cornerRadius\" style=\"fill:$foregroundFillColor;fill-opacity:$foregroundAlpha\"/>\n")
                }
            }
        }
    }

    private fun buildSvgTextContent(builder: StringBuilder, properties: BarcodeImageGeneratorProperties) {
        val content = properties.contents
        val textSize = properties.contentsHeight
        val posX = properties.width / 2f
        val posY = properties.height - properties.marginsPx
        val fillColor = properties.frontColor.toColorHex()
        val fillAlpha: Float = properties.frontColor.toColorAlpha()

        builder.append("<text x=\"$posX\" y=\"$posY\" text-anchor=\"middle\" font-size=\"$textSize\" style=\"fill:$fillColor;fill-opacity:$fillAlpha\">$content</text>\n")
    }
}