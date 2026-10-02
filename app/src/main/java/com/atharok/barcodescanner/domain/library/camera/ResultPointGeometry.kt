package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint

/** Axis-aligned bounding-box area of this quad as a fraction (0..1) of a [frameWidth] x [frameHeight] frame. */
fun Array<ResultPoint>.boundingBoxAreaRatio(frameWidth: Int, frameHeight: Int): Float {
    if (isEmpty() || frameWidth <= 0 || frameHeight <= 0) return 0f
    val width = maxOf { it.x } - minOf { it.x }
    val height = maxOf { it.y } - minOf { it.y }
    return (width * height / (frameWidth.toFloat() * frameHeight)).coerceIn(0f, 1f)
}

fun Array<ResultPoint>.centroid(): ResultPoint =
    ResultPoint(sumOf { it.x.toDouble() }.toFloat() / size, sumOf { it.y.toDouble() }.toFloat() / size)
