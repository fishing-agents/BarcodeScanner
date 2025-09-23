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

package com.atharok.barcodescanner.presentation.views.recyclerView.aboutThirdPartyLibrary

import androidx.annotation.StringRes
import com.atharok.barcodescanner.R

enum class ThirdPartyLibrary(
    @param:StringRes val title: Int,
    @param:StringRes val author: Int,
    @param:StringRes val id: Int,
    @param:StringRes val license: Int,
    @param:StringRes val licenseUrl: Int,
    @param:StringRes val sourceCode: Int,
    @param:StringRes val sourceCodeUrl: Int
) {

    ACTIVITY_KTX(
        title = R.string.dependency_activity_ktx_title_label,
        author = R.string.google_label,
        id = R.string.dependency_activity_ktx_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    PREFERENCE_KTX(
        title = R.string.dependency_preference_ktx_title_label,
        author = R.string.google_label,
        id = R.string.dependency_preference_ktx_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    LIFECYCLE_LIVEDATA_KTX(
        title = R.string.dependency_lifecycle_livedata_ktx_title_label,
        author = R.string.google_label,
        id = R.string.dependency_lifecycle_livedata_ktx_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    APP_COMPAT(
        title = R.string.dependency_appcompat_title_label,
        author = R.string.google_label,
        id = R.string.dependency_appcompat_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    CONSTRAINT_LAYOUT(
        title = R.string.dependency_constraintlayout_title_label,
        author = R.string.google_label,
        id = R.string.dependency_constraintlayout_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    RECYCLER_VIEW(
        title = R.string.dependency_recyclerview_title_label,
        author = R.string.google_label,
        id = R.string.dependency_recyclerview_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    MATERIAL_COMPONENTS(
        title = R.string.dependency_material_components_title_label,
        author = R.string.dependency_material_components_author_label,
        id = R.string.dependency_material_components_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_material_components_github_link_label
    ),

    CAMERA_X(
        title = R.string.dependency_camera_x_title_label,
        author = R.string.google_label,
        id = R.string.dependency_camera_x_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    ROOM(
        title = R.string.dependency_room_title_label,
        author = R.string.google_label,
        id = R.string.dependency_room_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_x_github_link_label
    ),

    RETROFIT(
        title = R.string.dependency_retrofit_title_label,
        author = R.string.dependency_retrofit_author_label,
        id = R.string.dependency_retrofit_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_retrofit_github_link_label
    ),

    GSON(
        title = R.string.dependency_gson_title_label,
        author = R.string.google_label,
        id = R.string.dependency_gson_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_gson_github_link_label
    ),

    COIL(
        title = R.string.dependency_coil_title_label,
        author = R.string.dependency_coil_author_label,
        id = R.string.dependency_coil_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_coil_github_link_label
    ),

    KOIN(
        title = R.string.dependency_koin_title_label,
        author = R.string.dependency_koin_author_label,
        id = R.string.dependency_koin_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_koin_github_link_label
    ),

    ZXING(
        title = R.string.dependency_zxing_title_label,
        author = R.string.dependency_zxing_author_label,
        id = R.string.dependency_zxing_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_zxing_github_link_label
    ),

    ANDROID_IMAGE_CROPPER(
        title = R.string.dependency_android_image_cropper_title_label,
        author = R.string.dependency_android_image_cropper_author_label,
        id = R.string.dependency_android_image_cropper_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_android_image_cropper_github_link_label
    ),

    EZ_VCARD(
        title = R.string.dependency_ez_vcard_title_label,
        author = R.string.dependency_ez_vcard_author_label,
        id = R.string.dependency_ez_vcard_id_label,
        license = R.string.free_bsd,
        licenseUrl = R.string.free_bsd_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_ez_vcard_github_link_label
    ),

    COLOR_PICKER(
        title = R.string.dependency_color_picker_title_label,
        author = R.string.dependency_color_picker_author_label,
        id = R.string.dependency_color_picker_id_label,
        license = R.string.apache_license_2_0,
        licenseUrl = R.string.apache_license_2_0_url,
        sourceCode = R.string.github,
        sourceCodeUrl = R.string.dependency_color_picker_github_link_label
    )
}