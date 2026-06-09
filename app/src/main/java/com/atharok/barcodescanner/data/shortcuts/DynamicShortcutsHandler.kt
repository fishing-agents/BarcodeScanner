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

package com.atharok.barcodescanner.data.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.atharok.barcodescanner.BuildConfig
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.domain.entity.DynamicShortcut
import com.atharok.barcodescanner.presentation.views.activities.BarcodeDetailsActivity
import com.atharok.barcodescanner.presentation.views.activities.BarcodeScanFromImageShortcutActivity
import com.atharok.barcodescanner.presentation.views.activities.MainActivity

class DynamicShortcutsHandler(
    private val context: Context
) {

    companion object {
        private const val SCAN_ID = "scan"
        private const val SCAN_FROM_IMAGE_ID = "scanFromImage"
        private const val HISTORY_ID = "history"
        private const val CREATE_ID = "create"
        private const val CREATE_FROM_CLIPBOARD_ID = "createFromClipboard"
    }

    private val map: Map<String, DynamicShortcut> by lazy {
        mapOf(
            SCAN_ID to DynamicShortcut(
                id = SCAN_ID,
                label = R.string.title_scan,
                drawable = R.mipmap.ic_shortcut_scan,
                icon = R.drawable.baseline_qr_code_scanner_24,
                targetClass = MainActivity::class.java,
                action = "${BuildConfig.APPLICATION_ID}.SCAN"
            ),
            SCAN_FROM_IMAGE_ID to DynamicShortcut(
                id = SCAN_FROM_IMAGE_ID,
                label = R.string.intent_filter_scan_by_image,
                drawable = R.mipmap.ic_shortcut_scan_from_image,
                icon = R.drawable.baseline_image_24,
                targetClass = BarcodeScanFromImageShortcutActivity::class.java,
                action = Intent.ACTION_VIEW
            ),
            HISTORY_ID to DynamicShortcut(
                id = HISTORY_ID,
                label = R.string.title_history,
                drawable = R.mipmap.ic_shortcut_history,
                icon = R.drawable.baseline_history_24,
                targetClass = MainActivity::class.java,
                action = "${BuildConfig.APPLICATION_ID}.HISTORY"
            ),
            CREATE_ID to DynamicShortcut(
                id = CREATE_ID,
                label = R.string.title_bar_code_creator,
                drawable = R.mipmap.ic_shortcut_create,
                icon = R.drawable.baseline_create_24,
                targetClass = MainActivity::class.java,
                action = "${BuildConfig.APPLICATION_ID}.CREATE"
            ),
            CREATE_FROM_CLIPBOARD_ID to DynamicShortcut(
                id = CREATE_FROM_CLIPBOARD_ID,
                label = R.string.create_qr_from_clipboard,
                drawable = R.mipmap.ic_shortcut_create_from_clipboard,
                icon = R.drawable.outline_content_paste_go_24,
                targetClass = BarcodeDetailsActivity::class.java,
                action = "${BuildConfig.APPLICATION_ID}.CREATE_FROM_CLIPBOARD"
            )
        )
    }

    fun createShortcuts() {
        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
        if (shortcuts.isEmpty() || shortcuts.size != map.size) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            createShortcut(0, map[SCAN_ID])
            createShortcut(1, map[SCAN_FROM_IMAGE_ID])
            createShortcut(2, map[HISTORY_ID])
            createShortcut(3, map[CREATE_ID])
            createShortcut(4, map[CREATE_FROM_CLIPBOARD_ID])
        }
    }

    fun updateShortcuts(shortcuts: List<DynamicShortcut>) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        shortcuts.forEachIndexed { index, shortcut ->
            createShortcut(index, shortcut)
        }
    }

    fun getShortcuts(): List<DynamicShortcut> = ShortcutManagerCompat
        .getDynamicShortcuts(context)
        .sortedBy { it.rank }
        .mapNotNull { map[it.id] }

    private fun createShortcut(rank: Int, shortcut: DynamicShortcut?) {
        shortcut?.let {
            val shortcutInfoCompat = ShortcutInfoCompat.Builder(context, shortcut.id)
                .setShortLabel(context.getString(shortcut.label))
                .setLongLabel(context.getString(shortcut.label))
                .setIcon(IconCompat.createWithResource(context, shortcut.drawable))
                .setIntent(
                    Intent(context, shortcut.targetClass).setAction(shortcut.action)
                )
                .setRank(rank)
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfoCompat)
        }
    }
}