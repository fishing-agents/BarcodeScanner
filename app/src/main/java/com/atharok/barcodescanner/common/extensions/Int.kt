package com.atharok.barcodescanner.common.extensions

import java.util.Locale

fun Int.toLocalString(): String {
    return String.format(Locale.getDefault(), "%d", this)
}