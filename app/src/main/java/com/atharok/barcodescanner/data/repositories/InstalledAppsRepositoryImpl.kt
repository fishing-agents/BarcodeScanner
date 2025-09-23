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

package com.atharok.barcodescanner.data.repositories

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.atharok.barcodescanner.domain.repositories.InstalledAppsRepository
import com.atharok.barcodescanner.presentation.views.recyclerView.applications.ApplicationsItem

class InstalledAppsRepositoryImpl(private val context: Context): InstalledAppsRepository {

    override suspend fun getInstalledApps(): List<ApplicationsItem> {
        val pm: PackageManager = context.packageManager
        return listInstalledApps(pm, listResolveInfo(pm)).sortedBy { it.title }
    }

    private fun listInstalledApps(pm: PackageManager, packages: List<ResolveInfo>): List<ApplicationsItem> {
        val applications = mutableListOf<ApplicationsItem>()
        for (pkg in packages) {
            val appName = pkg.activityInfo.applicationInfo.loadLabel(pm).toString()
            val packageName = pkg.activityInfo.packageName
            val appIcon = pm.getApplicationIcon(pkg.activityInfo.applicationInfo)

            applications.add(ApplicationsItem(appName, packageName, appIcon))
        }
        return applications
    }

    @Suppress("DEPRECATION")
    private fun listResolveInfo(pm: PackageManager): List<ResolveInfo> {
        val query = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> pm.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            else -> pm.queryIntentActivities(query, PackageManager.MATCH_ALL)
        }
    }
}