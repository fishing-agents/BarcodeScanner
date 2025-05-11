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

package com.atharok.barcodescanner.domain.library.wifiSetup.configuration

import com.atharok.barcodescanner.domain.library.wifiSetup.data.WifiSetupData
import com.atharok.barcodescanner.domain.library.wifiSetup.extensions.toEapMethod
import com.atharok.barcodescanner.domain.library.wifiSetup.extensions.toPhase2Method
import java.util.Locale

interface WifiSetup<CONFIGURATION> {

    fun configure(data: WifiSetupData): CONFIGURATION? {
        try {
            return when (data.authType.uppercase(Locale.getDefault())){
                "", "NOPASS" -> configureOpenNetwork(data.name, data.isHidden)
                "WPA", "WPA2" -> configureWpa2Network(data.name, data.password, data.isHidden)
                "WPA2-EAP" -> configureWpa2EapNetwork(data.name, data.password, data.isHidden, data.anonymousIdentity, data.identity, data.eapMethod.toEapMethod(), data.phase2Method.toPhase2Method())
                "WPA3", "SAE" -> configureWpa3Network(data.name, data.password, data.isHidden)
                "WPA3-EAP" -> configureWpa3EapNetwork(data.name, data.password, data.isHidden, data.anonymousIdentity, data.identity, data.eapMethod.toEapMethod(), data.phase2Method.toPhase2Method())
                "WEP" -> configureWepNetwork(data.name, data.password, data.isHidden)
                else -> null
            }
        } catch (e: IllegalArgumentException) {
            // Thrown when passphrase is not ASCII encodable.
            return null
        }
    }

    fun configureOpenNetwork(name: String, isHidden: Boolean): CONFIGURATION?

    fun configureWpa2Network(name: String, password: String, isHidden: Boolean): CONFIGURATION?

    fun configureWpa2EapNetwork(name: String,
                                password: String,
                                isHidden: Boolean,
                                anonymousIdentity: String,
                                identity: String,
                                eapMethod: Int?,
                                phase2Method: Int?): CONFIGURATION?

    fun configureWpa3Network(name: String, password: String, isHidden: Boolean): CONFIGURATION?

    fun configureWpa3EapNetwork(name: String,
                                password: String,
                                isHidden: Boolean,
                                anonymousIdentity: String,
                                identity: String,
                                eapMethod: Int?,
                                phase2Method: Int?): CONFIGURATION?

    fun configureWepNetwork(name: String,
                            password: String,
                            isHidden: Boolean): CONFIGURATION?
}