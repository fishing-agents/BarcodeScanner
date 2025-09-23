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

package com.atharok.barcodescanner.domain.library.wifiSetup.extensions

import android.net.wifi.WifiEnterpriseConfig
import android.os.Build

internal fun String.toEapMethod(): Int? = when (this) {
    "AKA" -> WifiEnterpriseConfig.Eap.AKA
    "AKA_PRIME" -> WifiEnterpriseConfig.Eap.AKA_PRIME
    "NONE" -> WifiEnterpriseConfig.Eap.NONE
    "PEAP" -> WifiEnterpriseConfig.Eap.PEAP
    "PWD" -> WifiEnterpriseConfig.Eap.PWD
    "SIM" -> WifiEnterpriseConfig.Eap.SIM
    "TLS" -> WifiEnterpriseConfig.Eap.TLS
    "TTLS" -> WifiEnterpriseConfig.Eap.TTLS
    "UNAUTH_TLS" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WifiEnterpriseConfig.Eap.UNAUTH_TLS else null
    else -> null
}

internal fun String.toPhase2Method(): Int =
    when (this) {
        "AKA" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WifiEnterpriseConfig.Phase2.AKA else WifiEnterpriseConfig.Phase2.NONE
        "AKA_PRIME" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WifiEnterpriseConfig.Phase2.AKA_PRIME else WifiEnterpriseConfig.Phase2.NONE
        "GTC" -> WifiEnterpriseConfig.Phase2.GTC
        "MSCHAP" -> WifiEnterpriseConfig.Phase2.MSCHAP
        "MSCHAPV2" -> WifiEnterpriseConfig.Phase2.MSCHAPV2
        "NONE" -> WifiEnterpriseConfig.Phase2.NONE
        "PAP" -> WifiEnterpriseConfig.Phase2.PAP
        "SIM" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WifiEnterpriseConfig.Phase2.SIM else WifiEnterpriseConfig.Phase2.NONE
        else -> WifiEnterpriseConfig.Phase2.NONE
    }

/**
 * Avec l'ancienne API de connexion WifiConfiguration, le SSID et le mot de passe doivent être entre guillemets.
 */
internal fun String.quote(): String {
    return if (startsWith("\"") && endsWith("\"")) this else "\"$this\""
}

private fun String.isHex(): Boolean {
    return length == 64 && matches("""^[0-9a-f]+$""".toRegex(RegexOption.IGNORE_CASE))
}

internal fun String.quoteIfNotHex(): String = if(isHex()) this else quote()