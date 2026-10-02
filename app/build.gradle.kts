plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.devtools.ksp)
}

android {
    namespace = "com.atharok.barcodescanner"
    compileSdk {
        version = release(36)
    }
    // Needed even though app has no native code of its own: without it AGP can't strip the
    // bundled .so files and ships them with full debug info.
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        applicationId = "com.atharok.barcodescanner"
        minSdk = 23
        targetSdk = 36
        versionCode = 53
        versionName = "1.27.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            System.getenv("ANDROID_KEY_STORE_FILE")?.let { storeFile = file(it) }
            System.getenv("ANDROID_KEY_STORE_PASSWORD")?.let { storePassword = it }
            System.getenv("ANDROID_KEY_ALIAS")?.let { keyAlias = it }
            System.getenv("ANDROID_KEY_PASSWORD")?.let { keyPassword = it }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.insert.koin.android)
    implementation(libs.zxing.core)
    implementation(libs.zxing.cpp.android)
    implementation(project(":wechatqr"))
    testImplementation(libs.mockito.core)
    implementation(libs.vanniktech.android.image.cropper)
    implementation(libs.ez.vcard)
    implementation(libs.atharok.color.picker)
}