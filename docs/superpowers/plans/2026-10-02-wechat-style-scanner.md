# WeChat-style Scanner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fork `references/BarcodeScanner` into this project and extend it with full-frame multi-code scanning, an AI-assisted decode fallback (OpenCV WeChatQRCode) for hard QR codes, auto-torch, auto-zoom, and WeChat Pay / Alipay code recognition with hand-off — while keeping every existing barcode type, screen, and history/Room behavior unchanged.

**Architecture:** `ScanEngine` (zxing-cpp primary, WeChatQRCode fallback via `TieredScanEngine`) replaces the embedded ZXing-Java analyzer in `CameraBarcodeAnalyzer`; pure decision classes (`AutoTorchController`, `AutoZoomController`, `ScanController`) drive torch/zoom/UI state from each frame's `ScanOutcome`; `PaymentCodeClassifier` plugs into the *existing* URI content/actions screens rather than adding a new result pipeline.

**Tech Stack:** Kotlin, CameraX 1.6.1, `io.github.zxing-cpp:android:3.1.1`, OpenCV + opencv_contrib (custom NDK/CMake build, `wechat_qrcode` module only), ZXing core 3.3.3 (kept, for generation/parsing only), Koin, JUnit4, Mockito.

**Spec:** `docs/superpowers/specs/2026-10-02-wechat-style-scanner-design.md`

## Global Constraints

- Fork baseline: `references/BarcodeScanner` (Atharok), GPLv3. `applicationId`/`namespace` stay `com.atharok.barcodescanner` — this is a continuation of that project, not a rebrand. minSdk 23, targetSdk 36, Kotlin, Views + Koin + Room + CameraX 1.6.1 unchanged.
- ZXing core stays at **3.3.3** — ZXing 3.4.0+ requires targetting API 24+, which conflicts with `minSdk 23`; raising minSdk is out of scope. ZXing core is used only for code **generation**, `ResultParser`, and the Java-fallback path — not live camera decode.
- **DRY — reuse, do not wrap:** `com.google.zxing.Result` / `ResultPoint` / `BarcodeFormat` are the only decode currency across both engines and the whole existing UI/history/intent pipeline. Do not introduce parallel `DecodedCode`/`Quad`/`LumaFrame` types. `ScanEngine.scan(image: ImageProxy)` / `scan(bitmap: Bitmap)` — both zxing-cpp and the WeChatQRCode JNI wrapper already accept these Android types directly.
- **DRY — reuse the existing payment-code extension points:** `BarcodeType.URL` already covers all URI-scheme codes (confirmed: UPI is handled as a sub-case of `BarcodeType.URL`, not its own type). Payment hook = one branch in `BarcodeMatrixUriFragment.start()` (new sibling fragment, same container as the existing UPI branch) + one relabel in `UrlActionsFragment.configureActionItems()`. No new `BarcodeType`, no `Modules.kt` change, no new `IntentCreator` function, no new Actions-fragment class, no `AndroidManifest` `<queries>` (confirmed: `openUrl(uri)` → `createSearchUrlIntent` → `ACTION_VIEW` + `mStartActivity`'s existing `ActivityNotFoundException` → toast already does the generic scheme hand-off `wxp://`/`weixin://`/`alipays://`/`https://qr.alipay.com/…` need).
- **DRY — do not touch the working zoom gesture path:** manual zoom (slider/pinch/double-tap in `CameraZoomGestureDetector`) keeps calling `setLinearZoom`, unmodified. `AutoZoomController` calls `setZoomRatio` directly on the same `CameraControl`/`ZoomState` — CameraX keeps `linearZoom` and `zoomRatio` in sync, no conversion needed. The one addition to `CameraZoomGestureDetector` is a single-tap hook (tap-to-focus, ported from Privacy-QR's `FocusMeteringAction` idea) and, when the scan state is `Picking`, marker hit-testing instead of focus.
- Marker coordinates for the multi-code picker use CameraX's own `androidx.camera.view.transform` package (`ImageProxyTransformFactory`, `PreviewView.getOutputTransform()`, `CoordinateTransform.mapPoint`) — confirmed present since CameraX 1.1.0. Do not hand-roll a sensor→view projection.
- Distribution: an alternative F-Droid-compatible store without a reproducible-build/no-binary-blob policy. WeChatCV model assets are committed; no post-install download step required (though the engine still degrades gracefully if they're absent).
- No ML Kit, no Google Play Services, no new Gradle flavors.
- Every new pure-logic class (classifier, controllers, geometry, engines' mapping logic) is unit-testable on the JVM with no Android framework dependency where feasible; Android-framework-bound glue (the analyzer itself, fragment wiring) is verified by an instrumented test or a manual run, not force-fit into a JVM test.

## Review Focus

- **A code whose finder pattern is never found by either engine** (far away, fully out of frame, or genuinely blank) — the scanner must keep running with empty `codes`/`candidates` every frame, no crash, no zoom runaway (covered: `AutoZoomController` reset-after-timeout test; `TieredScanEngineTest`'s empty-frame-threshold test).
- **The user pinches/double-taps while `AutoZoomController` is mid-step** — auto-zoom must pause, not fight the manual gesture (covered: `AutoZoomControllerTest`'s pause test; wired via `onManualZoomActive`/`onManualZoomReleased` in Task 14).
- **A device with no flash unit** (some tablets/foldables) — `AutoTorchController` and the existing flash menu item must no-op, never call `enableTorch` or crash (covered: `AutoTorchControllerTest`'s no-flash test; reuses the existing `Context.hasFlash()` extension already used at `MainCameraXScannerFragment.kt:210`).
- **The WeChatQRCode native lib or models are unavailable** (missing assets, load failure, non-ARM ABI) — `TieredScanEngine` must degrade to zxing-cpp only, not throw (covered: `TieredScanEngineTest`'s missing-fallback test; `WeChatQrEngine`'s init-failure path in Task 7).
- **An adversarial look-alike payment code** (`https://qr.alipay.com.evil.example/`, `https://example.com/?u=wxp://x`) must not be classified as a payment code and must not get a relabeled "Pay with…" button (covered: `PaymentCodeClassifierTest` look-alike cases; `UrlActionsFragment` only relabels on a real classifier match).

---

## Task 1: Fork BarcodeScanner with upstream history preserved

**Files:**
- Create: everything under `app/`, `gradle/`, `fastlane/`, `gradlew`, `gradlew.bat`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitlab-ci.yml`, `privacy_policy.txt`, `LICENSE`, `.gitignore` (all from `references/BarcodeScanner`'s own git history, not a flat copy)

**Interfaces:** None (this task produces the baseline every later task modifies).

This keeps Atharok's commit history and GPL provenance reachable, so `git merge upstream/main` (or cherry-picking a specific upstream fix) stays possible later — a plain file copy would discard that permanently. `references/BarcodeScanner/.git` is a real clone: `origin` is `https://gitlab.com/Atharok/BarcodeScanner`, tag `1.26.3` is commit `b41f56d58978dc228ff87324813004b01d6231c6`.

- [ ] **Step 1: Init, fetch the baseline tag's history, and check it out**

The project root already has `references/` and `docs/`, so a plain `git clone` into it fails (destination not empty); fetch from the local reference clone's objects into a freshly-initialized repo instead — verified to leave unrelated existing files untouched.

```bash
git init
git remote add upstream https://gitlab.com/Atharok/BarcodeScanner
git fetch references/BarcodeScanner/.git '+refs/heads/*:refs/remotes/upstream/*' '+refs/tags/*:refs/tags/*'
git checkout -b main 1.26.3
```

*(This pulls in Atharok's full commit history up to `1.26.3`, not a flat snapshot — `git log` shows the real authorship chain. `upstream` points at the real GitLab URL, so `git fetch upstream && git merge upstream/main` later reaches Atharok's actual repo, not the local `references/` checkout.)*

- [ ] **Step 2: Verify the baseline builds**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. If it fails on missing Android SDK/NDK, install per the error before proceeding — every later task assumes a working build.

- [ ] **Step 3: Verify the baseline's own tests still pass**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` (the template `ExampleUnitTest` passes trivially).

- [ ] **Step 4: Exclude the reference/working directories from the fork's history**

`references/` (both reference apps — pure research scaffolding, not part of BarcodeScanner) must not end up committed into the fork. `docs/superpowers/` (this spec and plan) stays committed — the writing-plans/brainstorming workflow expects the design record to travel with the code it describes. Exclude only `references/`, via `.git/info/exclude` (local-only, since `.gitignore` is itself a tracked file inherited from upstream):

```bash
printf 'references/\n' >> .git/info/exclude
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: confirm baseline build and test" --allow-empty
```

---

## Task 2: PaymentCodeClassifier

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/payment/PaymentCode.kt`
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/payment/PaymentCodeClassifier.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/payment/PaymentCodeClassifierTest.kt`

**Interfaces:**
- Produces: `enum class PaymentCode { WECHAT_PAY, WECHAT_LINK, ALIPAY }`; `object PaymentCodeClassifier { fun classify(uri: String): PaymentCode?; fun handoffUri(uri: String, code: PaymentCode): String }` — consumed by Task 16/17.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentCodeClassifierTest {

    @Test fun `wechat pay scheme classified`() {
        assertEquals(PaymentCode.WECHAT_PAY, PaymentCodeClassifier.classify("wxp://f2f0account/abc123"))
        assertEquals(PaymentCode.WECHAT_PAY, PaymentCodeClassifier.classify("WXP://F2F0ACCOUNT/ABC123"))
    }

    @Test fun `wechat link scheme and host classified`() {
        assertEquals(PaymentCode.WECHAT_LINK, PaymentCodeClassifier.classify("weixin://dl/business/?t=abc"))
        assertEquals(PaymentCode.WECHAT_LINK, PaymentCodeClassifier.classify("https://u.wechat.com/abcdef"))
        assertEquals(PaymentCode.WECHAT_LINK, PaymentCodeClassifier.classify("https://weixin.qq.com/g/xyz"))
    }

    @Test fun `alipay scheme and host classified`() {
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify("https://qr.alipay.com/abc123"))
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify("alipays://platformapi/startapp?saId=10000007"))
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify("alipay://platformapi/startapp?saId=10000007"))
    }

    @Test fun `lookalike hosts are rejected`() {
        assertNull(PaymentCodeClassifier.classify("https://qr.alipay.com.evil.example/steal"))
        assertNull(PaymentCodeClassifier.classify("https://example.com/?u=wxp://x"))
        assertNull(PaymentCodeClassifier.classify("https://notu.wechat.com/abc"))
    }

    @Test fun `ordinary urls and text are not payment codes`() {
        assertNull(PaymentCodeClassifier.classify("https://example.com/"))
        assertNull(PaymentCodeClassifier.classify("Hello world"))
        assertNull(PaymentCodeClassifier.classify("upi://pay?pa=foo@bank"))
    }

    @Test fun `alipay https link is wrapped into the alipays scan intent`() {
        val handoff = PaymentCodeClassifier.handoffUri("https://qr.alipay.com/abc123?x=1", PaymentCode.ALIPAY)
        assertEquals(
            "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2Fqr.alipay.com%2Fabc123%3Fx%3D1",
            handoff
        )
    }

    @Test fun `alipay code already using the alipays scheme passes through unchanged`() {
        val uri = "alipays://platformapi/startapp?saId=10000007"
        assertEquals(uri, PaymentCodeClassifier.handoffUri(uri, PaymentCode.ALIPAY))
    }

    @Test fun `wechat codes pass through unchanged, they already deep-link directly`() {
        assertEquals("wxp://abc", PaymentCodeClassifier.handoffUri("wxp://abc", PaymentCode.WECHAT_PAY))
        assertEquals("weixin://abc", PaymentCodeClassifier.handoffUri("weixin://abc", PaymentCode.WECHAT_LINK))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.PaymentCodeClassifierTest"`
Expected: FAIL — `PaymentCode`/`PaymentCodeClassifier` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.payment

enum class PaymentCode { WECHAT_PAY, WECHAT_LINK, ALIPAY }
```

```kotlin
package com.atharok.barcodescanner.domain.library.payment

/**
 * Recognizes WeChat Pay / WeChat link / Alipay QR codes among ordinary URIs,
 * so [com.atharok.barcodescanner.presentation.views.fragments.barcodeAnalysis.contents.BarcodeMatrixUriFragment]
 * and [com.atharok.barcodescanner.presentation.views.fragments.barcodeAnalysis.actions.UrlActionsFragment]
 * can special-case them without a new [com.atharok.barcodescanner.domain.entity.barcode.BarcodeType].
 */
object PaymentCodeClassifier {

    private val WECHAT_PAY_SCHEME = Regex("^wxp://", RegexOption.IGNORE_CASE)
    private val WECHAT_LINK_SCHEME = Regex("^weixin://", RegexOption.IGNORE_CASE)
    private val WECHAT_LINK_HOST = Regex("^https?://(u\\.wechat\\.com|weixin\\.qq\\.com)(/|$|\\?)", RegexOption.IGNORE_CASE)
    private val ALIPAY_SCHEME = Regex("^alipays?://", RegexOption.IGNORE_CASE)
    private val ALIPAY_HOST = Regex("^https?://qr\\.alipay\\.com(/|$|\\?)", RegexOption.IGNORE_CASE)

    fun classify(uri: String): PaymentCode? = when {
        WECHAT_PAY_SCHEME.containsMatchIn(uri) -> PaymentCode.WECHAT_PAY
        WECHAT_LINK_SCHEME.containsMatchIn(uri) || WECHAT_LINK_HOST.containsMatchIn(uri) -> PaymentCode.WECHAT_LINK
        ALIPAY_SCHEME.containsMatchIn(uri) || ALIPAY_HOST.containsMatchIn(uri) -> PaymentCode.ALIPAY
        else -> null
    }

    /**
     * The URI to hand to `ACTION_VIEW` for a classified payment code.
     * `wxp://`/`weixin://` already deep-link directly. Alipay's
     * `https://qr.alipay.com/...` web link does NOT reliably open the
     * Alipay app (App Links verification for that host is not guaranteed),
     * so it is wrapped into Alipay's own scan-intent scheme; an already
     * `alipays://`/`alipay://` code passes through unchanged.
     */
    fun handoffUri(uri: String, code: PaymentCode): String = when (code) {
        PaymentCode.ALIPAY -> if (ALIPAY_HOST.containsMatchIn(uri)) {
            "alipays://platformapi/startapp?saId=10000007&qrcode=" +
                java.net.URLEncoder.encode(uri, "UTF-8")
        } else {
            uri
        }
        PaymentCode.WECHAT_PAY, PaymentCode.WECHAT_LINK -> uri
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.PaymentCodeClassifierTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/payment app/src/test/java/com/atharok/barcodescanner/domain/library/payment
git commit -m "feat: add PaymentCodeClassifier for WeChat/Alipay QR codes"
```

---

## Task 3: zxing-cpp dependency, test tooling, and the ScanEngine contract

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ScanOutcome.kt`
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ScanEngine.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/scan/ScanOutcomeTest.kt`

**Interfaces:**
- Produces: `data class ScanOutcome(val codes: List<Result>, val candidates: List<Array<ResultPoint>>)` with `ScanOutcome.EMPTY`; `interface ScanEngine { fun scan(image: ImageProxy): ScanOutcome; fun scan(bitmap: Bitmap): ScanOutcome }` — consumed by every later scan-layer task.

- [ ] **Step 1: Add dependencies**

In `gradle/libs.versions.toml`, under `[versions]` add:

```toml
zxingCpp = "3.1.1"
mockitoCore = "5.14.2"
```

Under `[libraries]` add:

```toml
zxing-cpp-android = { group = "io.github.zxing-cpp", name = "android", version.ref = "zxingCpp" }
mockito-core = { group = "org.mockito", name = "mockito-core", version.ref = "mockitoCore" }
```

In `app/build.gradle.kts`, inside `dependencies { }`, add alongside the existing `zxing.core` line:

```kotlin
    implementation(libs.zxing.cpp.android)
    testImplementation(libs.mockito.core)
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanOutcomeTest {

    @Test fun `EMPTY has no codes and no candidates`() {
        assertTrue(ScanOutcome.EMPTY.codes.isEmpty())
        assertTrue(ScanOutcome.EMPTY.candidates.isEmpty())
    }

    @Test fun `holds decoded codes and undecoded candidates independently`() {
        val code = Result("hi", null, arrayOf(ResultPoint(0f, 0f)), BarcodeFormat.QR_CODE)
        val candidate = arrayOf(ResultPoint(0f, 0f), ResultPoint(1f, 0f), ResultPoint(1f, 1f), ResultPoint(0f, 1f))
        val outcome = ScanOutcome(codes = listOf(code), candidates = listOf(candidate))

        assertEquals(1, outcome.codes.size)
        assertEquals(1, outcome.candidates.size)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ScanOutcomeTest"`
Expected: FAIL — `ScanOutcome` unresolved.

- [ ] **Step 4: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.Result
import com.google.zxing.ResultPoint

/**
 * One camera frame's (or one gallery image's) scan result. Reuses ZXing's
 * own [Result]/[ResultPoint] so the rest of the app (history, Room, intents,
 * ResultParser) needs no changes to consume codes found by either engine.
 */
data class ScanOutcome(
    val codes: List<Result>,
    val candidates: List<Array<ResultPoint>>
) {
    companion object {
        val EMPTY = ScanOutcome(emptyList(), emptyList())
    }
}
```

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/** A barcode decoder that can read a live camera frame or a static image. */
interface ScanEngine {
    fun scan(image: ImageProxy): ScanOutcome
    fun scan(bitmap: Bitmap): ScanOutcome
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ScanOutcomeTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/atharok/barcodescanner/domain/library/scan app/src/test/java/com/atharok/barcodescanner/domain/library/scan
git commit -m "feat: add zxing-cpp dependency and the ScanEngine contract"
```

---

## Task 4: ZxingCppFormatMapper

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppFormatMapper.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppFormatMapperTest.kt`

**Interfaces:**
- Consumes: `zxingcpp.BarcodeReader.Format` (library type).
- Produces: `object ZxingCppFormatMapper { fun toBarcodeFormat(format: BarcodeReader.Format): BarcodeFormat? }` — consumed by Task 5.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import zxingcpp.BarcodeReader

class ZxingCppFormatMapperTest {

    @Test fun `maps formats shared with ZXing core`() {
        assertEquals(BarcodeFormat.QR_CODE, ZxingCppFormatMapper.toBarcodeFormat(BarcodeReader.Format.QR_CODE))
        assertEquals(BarcodeFormat.EAN_13, ZxingCppFormatMapper.toBarcodeFormat(BarcodeReader.Format.EAN_13))
        assertEquals(BarcodeFormat.DATA_MATRIX, ZxingCppFormatMapper.toBarcodeFormat(BarcodeReader.Format.DATA_MATRIX))
        assertEquals(BarcodeFormat.MAXICODE, ZxingCppFormatMapper.toBarcodeFormat(BarcodeReader.Format.MAXI_CODE))
    }

    @Test fun `formats absent from ZXing core 3_3_3 BarcodeFormat map to null`() {
        assertNull(ZxingCppFormatMapper.toBarcodeFormat(BarcodeReader.Format.MICRO_QR_CODE))
        assertNull(ZxingCppFormatMapper.toBarcodeFormat(BarcodeReader.Format.RMQR_CODE))
        assertNull(ZxingCppFormatMapper.toBarcodeFormat(BarcodeReader.Format.DATA_BAR))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ZxingCppFormatMapperTest"`
Expected: FAIL — `ZxingCppFormatMapper` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.BarcodeFormat
import zxingcpp.BarcodeReader

/**
 * Maps zxing-cpp's [BarcodeReader.Format] onto ZXing core's [BarcodeFormat],
 * which the rest of the app (history, Room, [Barcode.getBarcodeFormat])
 * already understands. Formats zxing-cpp can read that ZXing core 3.3.3
 * has no equivalent for (MICRO_QR_CODE, RMQR_CODE, the GS1 DataBar family,
 * TELEPEN, DX_FILM_EDGE, OTHER_BARCODE) map to null and are dropped.
 */
object ZxingCppFormatMapper {

    private val MAPPING: Map<BarcodeReader.Format, BarcodeFormat> = mapOf(
        BarcodeReader.Format.AZTEC to BarcodeFormat.AZTEC,
        BarcodeReader.Format.CODABAR to BarcodeFormat.CODABAR,
        BarcodeReader.Format.CODE_39 to BarcodeFormat.CODE_39,
        BarcodeReader.Format.CODE_93 to BarcodeFormat.CODE_93,
        BarcodeReader.Format.CODE_128 to BarcodeFormat.CODE_128,
        BarcodeReader.Format.DATA_MATRIX to BarcodeFormat.DATA_MATRIX,
        BarcodeReader.Format.EAN_8 to BarcodeFormat.EAN_8,
        BarcodeReader.Format.EAN_13 to BarcodeFormat.EAN_13,
        BarcodeReader.Format.ITF to BarcodeFormat.ITF,
        BarcodeReader.Format.MAXI_CODE to BarcodeFormat.MAXICODE,
        BarcodeReader.Format.PDF_417 to BarcodeFormat.PDF_417,
        BarcodeReader.Format.QR_CODE to BarcodeFormat.QR_CODE,
        BarcodeReader.Format.UPC_A to BarcodeFormat.UPC_A,
        BarcodeReader.Format.UPC_E to BarcodeFormat.UPC_E
    )

    /** The set of formats this mapper accepts, for configuring [BarcodeReader.Options.formats]. */
    val SUPPORTED: Set<BarcodeReader.Format> = MAPPING.keys

    fun toBarcodeFormat(format: BarcodeReader.Format): BarcodeFormat? = MAPPING[format]
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ZxingCppFormatMapperTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppFormatMapper.kt app/src/test/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppFormatMapperTest.kt
git commit -m "feat: add ZxingCppFormatMapper"
```

---

## Task 5: ZxingCppEngine

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppResultMapper.kt`
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppEngine.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppResultMapperTest.kt`

**Interfaces:**
- Consumes: `ZxingCppFormatMapper.toBarcodeFormat` (Task 4).
- Produces: `class ZxingCppEngine : ScanEngine` — consumed by Task 8 (`TieredScanEngine`), Task 13 (`CameraBarcodeAnalyzer`), Task 18 (`BarcodeBitmapAnalyser`).

- [ ] **Step 1: Write the failing test**

The pure mapping logic lives in `ZxingCppResultMapper` so it is testable without constructing a real `BarcodeReader` (which calls `System.loadLibrary` in its `init` block and would fail on the JVM test runner).

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Point
import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zxingcpp.BarcodeReader

class ZxingCppResultMapperTest {

    private fun fakeResult(
        format: BarcodeReader.Format = BarcodeReader.Format.QR_CODE,
        text: String? = "hello",
        error: BarcodeReader.Error? = null
    ): BarcodeReader.Result = BarcodeReader.Result(
        format = format,
        bytes = null,
        text = text,
        contentType = BarcodeReader.ContentType.TEXT,
        position = BarcodeReader.Position(Point(0, 0), Point(10, 0), Point(10, 10), Point(0, 10), 0.0),
        rotation = 0,
        ecLevel = null,
        symbologyIdentifier = null,
        extraJsonString = null,
        sequenceSize = -1,
        sequenceIndex = -1,
        sequenceId = null,
        readerInit = false,
        lineCount = 0,
        error = error
    )

    @Test fun `decoded result becomes a code`() {
        val outcome = ZxingCppResultMapper.toOutcome(listOf(fakeResult()))
        assertEquals(1, outcome.codes.size)
        assertEquals(0, outcome.candidates.size)
        assertEquals(BarcodeFormat.QR_CODE, outcome.codes[0].barcodeFormat)
        assertEquals("hello", outcome.codes[0].text)
    }

    @Test fun `errored result becomes a candidate, not a code`() {
        val outcome = ZxingCppResultMapper.toOutcome(
            listOf(fakeResult(text = null, error = BarcodeReader.Error(BarcodeReader.ErrorType.FORMAT, "bad")))
        )
        assertTrue(outcome.codes.isEmpty())
        assertEquals(1, outcome.candidates.size)
    }

    @Test fun `unsupported format is dropped, not surfaced as a code`() {
        val outcome = ZxingCppResultMapper.toOutcome(listOf(fakeResult(format = BarcodeReader.Format.RMQR_CODE)))
        assertTrue(outcome.codes.isEmpty())
        assertTrue(outcome.candidates.isEmpty())
    }

    @Test fun `multiple results map independently`() {
        val outcome = ZxingCppResultMapper.toOutcome(
            listOf(
                fakeResult(text = "a"),
                fakeResult(text = "b"),
                fakeResult(text = null, error = BarcodeReader.Error(BarcodeReader.ErrorType.CHECKSUM, "x"))
            )
        )
        assertEquals(2, outcome.codes.size)
        assertEquals(1, outcome.candidates.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ZxingCppResultMapperTest"`
Expected: FAIL — `ZxingCppResultMapper` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import com.google.zxing.Result
import com.google.zxing.ResultPoint
import zxingcpp.BarcodeReader

/** Converts zxing-cpp's own result type into [ScanOutcome], independent of any native call. */
internal object ZxingCppResultMapper {

    fun toOutcome(results: List<BarcodeReader.Result>): ScanOutcome {
        val codes = mutableListOf<Result>()
        val candidates = mutableListOf<Array<ResultPoint>>()

        for (r in results) {
            val points = toResultPoints(r.position)
            if (r.error == null) {
                val format = ZxingCppFormatMapper.toBarcodeFormat(r.format) ?: continue
                codes.add(Result(r.text.orEmpty(), r.bytes, points, format))
            } else {
                candidates.add(points)
            }
        }
        return ScanOutcome(codes, candidates)
    }

    private fun toResultPoints(position: BarcodeReader.Position): Array<ResultPoint> = arrayOf(
        ResultPoint(position.topLeft.x.toFloat(), position.topLeft.y.toFloat()),
        ResultPoint(position.topRight.x.toFloat(), position.topRight.y.toFloat()),
        ResultPoint(position.bottomRight.x.toFloat(), position.bottomRight.y.toFloat()),
        ResultPoint(position.bottomLeft.x.toFloat(), position.bottomLeft.y.toFloat())
    )
}
```

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import zxingcpp.BarcodeReader

/** Live/gallery decode backed by zxing-cpp: every ZXing-mappable format (see [ZxingCppFormatMapper]), multi-code, inverted/rotated retries. */
class ZxingCppEngine : ScanEngine {

    private val reader = BarcodeReader(
        BarcodeReader.Options(
            formats = ZxingCppFormatMapper.SUPPORTED,
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
            maxNumberOfSymbols = 8,
            returnErrors = true
        )
    )

    override fun scan(image: ImageProxy): ScanOutcome = ZxingCppResultMapper.toOutcome(reader.read(image))
    override fun scan(bitmap: Bitmap): ScanOutcome = ZxingCppResultMapper.toOutcome(reader.read(bitmap))
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ZxingCppResultMapperTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppResultMapper.kt app/src/main/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppEngine.kt app/src/test/java/com/atharok/barcodescanner/domain/library/scan/ZxingCppResultMapperTest.kt
git commit -m "feat: add ZxingCppEngine"
```

---

## Task 6: `:wechatqr` native module scaffold (OpenCV + opencv_contrib, WeChatQRCode)

**Files:**
- Modify: `settings.gradle.kts`
- Create: `wechatqr/build.gradle.kts`
- Create: `wechatqr/src/main/cpp/CMakeLists.txt`
- Create: `wechatqr/src/main/cpp/wechatqr_jni.cpp`
- Create: `wechatqr/src/main/java/com/atharok/barcodescanner/wechatqr/WeChatQrNative.kt`
- Create: `wechatqr/.gitmodules` entries for `third_party/opencv`, `third_party/opencv_contrib` (added via `git submodule add`)
- Test: `wechatqr/src/androidTest/java/com/atharok/barcodescanner/wechatqr/WeChatQrNativeTest.kt`
- Test asset: `wechatqr/src/androidTest/assets/qr_test.png` (a plain, clearly decodable QR code, for build-smoke verification only)

**Interfaces:**
- Produces: `interface WeChatQrNative { fun nativeInit(modelDir: String?): Boolean; fun nativeDetectAndDecode(yBuffer: ByteBuffer, width: Int, height: Int, rowStride: Int): WeChatQrNativeResult }`, its JNI implementation `class WeChatQrNativeJni : WeChatQrNative`, and `class WeChatQrNativeResult(texts, decodedQuads, candidateQuads)` — consumed by Task 7. The interface/impl split exists so Task 7's JVM unit test can mock the interface: Mockito cannot intercept `external` (native) methods, so mocking a class whose methods are `external` would fall through to the real JNI call and fail.

- [ ] **Step 1: Add the module and submodules**

```bash
git submodule add --depth 1 -b 4.14.0 https://github.com/opencv/opencv.git wechatqr/third_party/opencv
git submodule add --depth 1 -b 4.14.0 https://github.com/opencv/opencv_contrib.git wechatqr/third_party/opencv_contrib
```

In `settings.gradle.kts`, after `include(":app")`:

```kotlin
include(":wechatqr")
```

- [ ] **Step 2: `wechatqr/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.library")
}

android {
    namespace = "com.atharok.barcodescanner.wechatqr"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 23
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.0"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    androidTestImplementation(libs.androidx.junit)
}
```

- [ ] **Step 3: `wechatqr/src/main/cpp/CMakeLists.txt`**

```cmake
cmake_minimum_required(VERSION 3.22)
project(wechatqr)

set(OPENCV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/opencv)
set(OPENCV_CONTRIB_MODULES ${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/opencv_contrib/modules)

# Build only the modules wechat_qrcode needs, nothing else (no Java bindings,
# no highgui/videoio/apps/tests) to keep the native library small.
set(BUILD_LIST "core,imgproc,dnn,wechat_qrcode" CACHE STRING "" FORCE)
set(BUILD_JAVA OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_PERF_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_opencv_apps OFF CACHE BOOL "" FORCE)
set(OPENCV_EXTRA_MODULES_PATH ${OPENCV_CONTRIB_MODULES} CACHE PATH "" FORCE)

add_subdirectory(${OPENCV_DIR} opencv_build)

add_library(wechatqr_android SHARED wechatqr_jni.cpp)

target_include_directories(wechatqr_android PRIVATE
    # Internal, module-private header: SSDDetector is not part of OpenCV's
    # public include/opencv2 surface, so we reach into the module's own
    # src/ tree. This is pinned to the opencv_contrib submodule commit
    # recorded in .gitmodules; a contrib bump that moves/renames this
    # header needs this path (and wechatqr_jni.cpp) updated together.
    ${OPENCV_CONTRIB_MODULES}/wechat_qrcode/src
    ${OPENCV_CONTRIB_MODULES}/wechat_qrcode/src/detector
)

target_link_libraries(wechatqr_android
    opencv_core
    opencv_imgproc
    opencv_dnn
    opencv_wechat_qrcode
    log
)
```

- [ ] **Step 4: `wechatqr/src/main/cpp/wechatqr_jni.cpp`**

```cpp
// JNI bridge: calls the public WeChatQRCode::detectAndDecode for decoded
// text+points, and separately calls the module-internal SSDDetector::forward
// for detected-but-undecoded candidate quads (needed for auto-zoom; the
// public API drops those — see design spec, "verified external facts").
#include <jni.h>
#include <string>
#include <vector>
#include <opencv2/core.hpp>
#include <opencv2/wechat_qrcode.hpp>
#include "detector/ssd_detector.hpp"

using cv::wechat_qrcode::WeChatQRCode;
using cv::wechat_qrcode::SSDDetector;

namespace {
cv::Ptr<WeChatQRCode> g_decoder;
cv::Ptr<SSDDetector> g_detector;

std::vector<float> matToQuad(const cv::Mat& point) {
    std::vector<float> quad(8);
    for (int i = 0; i < 4; ++i) {
        quad[i * 2] = point.at<float>(i, 0);
        quad[i * 2 + 1] = point.at<float>(i, 1);
    }
    return quad;
}

jfloatArray toJavaFloatArray(JNIEnv* env, const std::vector<float>& values) {
    jfloatArray array = env->NewFloatArray(static_cast<jsize>(values.size()));
    env->SetFloatArrayRegion(array, 0, static_cast<jsize>(values.size()), values.data());
    return array;
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_atharok_barcodescanner_wechatqr_WeChatQrNativeJni_jniInit(
    JNIEnv* env, jobject /* this */, jstring modelDir) {
    const char* dir = modelDir ? env->GetStringUTFChars(modelDir, nullptr) : nullptr;
    std::string detectorPrototxt, detectorModel, srPrototxt, srModel;
    if (dir != nullptr) {
        detectorPrototxt = std::string(dir) + "/detect.prototxt";
        detectorModel = std::string(dir) + "/detect.caffemodel";
        srPrototxt = std::string(dir) + "/sr.prototxt";
        srModel = std::string(dir) + "/sr.caffemodel";
        env->ReleaseStringUTFChars(modelDir, dir);
    }

    try {
        g_decoder = cv::makePtr<WeChatQRCode>(detectorPrototxt, detectorModel, srPrototxt, srModel);
        if (!detectorPrototxt.empty()) {
            g_detector = cv::makePtr<SSDDetector>();
            if (g_detector->init(detectorPrototxt, detectorModel) != 0) {
                g_detector = nullptr;
            }
        }
        return JNI_TRUE;
    } catch (const cv::Exception&) {
        g_decoder = nullptr;
        g_detector = nullptr;
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_atharok_barcodescanner_wechatqr_WeChatQrNativeJni_jniDetectAndDecode(
    JNIEnv* env, jobject /* this */, jobject yBuffer, jint width, jint height, jint rowStride) {
    auto* data = static_cast<uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    cv::Mat gray(height, width, CV_8UC1, data, rowStride);

    std::vector<std::string> texts;
    std::vector<cv::Mat> decodedPoints;
    std::vector<std::vector<float>> candidateQuads;

    if (g_decoder) {
        texts = g_decoder->detectAndDecode(gray, decodedPoints);
    }
    if (g_detector) {
        // Detector-only pass: every candidate box the CNN proposed, whether
        // or not it went on to decode successfully.
        const int detectW = std::min(width, 400);
        const int detectH = std::min(height, 400);
        for (const cv::Mat& box : g_detector->forward(gray, detectW, detectH)) {
            candidateQuads.push_back(matToQuad(box));
        }
    }

    jclass resultClass = env->FindClass("com/atharok/barcodescanner/wechatqr/WeChatQrNativeResult");
    jmethodID ctor = env->GetMethodID(
        resultClass, "<init>", "([Ljava/lang/String;[[F[[F)V");

    jobjectArray textArray = env->NewObjectArray(
        static_cast<jsize>(texts.size()), env->FindClass("java/lang/String"), nullptr);
    for (size_t i = 0; i < texts.size(); ++i) {
        env->SetObjectArrayElement(textArray, static_cast<jsize>(i), env->NewStringUTF(texts[i].c_str()));
    }

    jclass floatArrayClass = env->FindClass("[F");
    jobjectArray decodedArray = env->NewObjectArray(static_cast<jsize>(decodedPoints.size()), floatArrayClass, nullptr);
    for (size_t i = 0; i < decodedPoints.size(); ++i) {
        env->SetObjectArrayElement(decodedArray, static_cast<jsize>(i), toJavaFloatArray(env, matToQuad(decodedPoints[i])));
    }

    jobjectArray candidateArray = env->NewObjectArray(static_cast<jsize>(candidateQuads.size()), floatArrayClass, nullptr);
    for (size_t i = 0; i < candidateQuads.size(); ++i) {
        env->SetObjectArrayElement(candidateArray, static_cast<jsize>(i), toJavaFloatArray(env, candidateQuads[i]));
    }

    return env->NewObject(resultClass, ctor, textArray, decodedArray, candidateArray);
}
```

- [ ] **Step 5: `wechatqr/src/main/java/com/atharok/barcodescanner/wechatqr/WeChatQrNative.kt`**

```kotlin
package com.atharok.barcodescanner.wechatqr

import java.nio.ByteBuffer

/** One native detectAndDecode call's raw output: decoded texts, their quads (parallel arrays), and all detector candidate quads (decoded or not). */
class WeChatQrNativeResult(
    val texts: Array<String>,
    val decodedQuads: Array<FloatArray>,
    val candidateQuads: Array<FloatArray>
)

/** Native-boundary contract. An interface (not the JNI class itself) so JVM unit tests can mock it — Mockito cannot stub `external` methods. */
interface WeChatQrNative {
    /** @param modelDir directory containing detect.prototxt/.caffemodel and sr.prototxt/.caffemodel, or null to run without a CNN (full-frame, cubic-resize fallback). */
    fun nativeInit(modelDir: String?): Boolean

    fun nativeDetectAndDecode(yBuffer: ByteBuffer, width: Int, height: Int, rowStride: Int): WeChatQrNativeResult
}

/** JNI implementation over the custom `wechatqr_android` native library (OpenCV + opencv_contrib's wechat_qrcode module, built from source — see CMakeLists.txt). */
class WeChatQrNativeJni : WeChatQrNative {

    init {
        System.loadLibrary("wechatqr_android")
    }

    override fun nativeInit(modelDir: String?): Boolean = jniInit(modelDir)

    override fun nativeDetectAndDecode(yBuffer: ByteBuffer, width: Int, height: Int, rowStride: Int): WeChatQrNativeResult =
        jniDetectAndDecode(yBuffer, width, height, rowStride)

    private external fun jniInit(modelDir: String?): Boolean

    private external fun jniDetectAndDecode(yBuffer: ByteBuffer, width: Int, height: Int, rowStride: Int): WeChatQrNativeResult
}
```

- [ ] **Step 6: Build-verification step (no JVM unit test possible — this is a native/instrumented boundary)**

Run: `./gradlew :wechatqr:assembleDebug`
Expected: `BUILD SUCCESSFUL`, producing `wechatqr/build/outputs/aar/wechatqr-debug.aar` containing `libwechatqr_android.so` for `arm64-v8a` and `armeabi-v7a`.

- [ ] **Step 7: Instrumented smoke test**

```kotlin
package com.atharok.barcodescanner.wechatqr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class WeChatQrNativeTest {

    @Test fun nativeLibraryLoadsAndRunsWithoutModels() {
        val native = WeChatQrNativeJni()
        val initialized = native.nativeInit(modelDir = null)
        assertTrue(initialized)

        // A trivial 8x8 all-white buffer: no QR present, must not crash.
        val buffer = ByteBuffer.allocateDirect(64).apply { repeat(64) { put(0xFF.toByte()) }; rewind() }
        val result = native.nativeDetectAndDecode(buffer, width = 8, height = 8, rowStride = 8)

        assertTrue(result.texts.isEmpty())
    }
}
```

Run: `./gradlew :wechatqr:connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL` on a connected `arm64-v8a`/`armeabi-v7a` device or emulator (x86/x86_64 emulators are out of scope — ABI filters only include ARM).

- [ ] **Step 8: Commit**

```bash
git add .gitmodules settings.gradle.kts wechatqr
git commit -m "feat: add :wechatqr native module (OpenCV + opencv_contrib wechat_qrcode)"
```

---

## Task 7: WeChatQrEngine (Kotlin wrapper)

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/scan/WeChatQrEngine.kt`
- Create: `app/src/main/java/com/atharok/barcodescanner/wechatqr/WeChatQrModelInstaller.kt`
- Modify: `app/build.gradle.kts` (add `implementation(project(":wechatqr"))`)
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/scan/WeChatQrEngineTest.kt`

**Interfaces:**
- Consumes: `WeChatQrNative`, `WeChatQrNativeResult` (Task 6).
- Produces: `class WeChatQrEngine(native: WeChatQrNative, modelDir: String?) : ScanEngine`; `object WeChatQrModelInstaller { fun ensureInstalled(context: Context): String? }` — both consumed by Task 8/14 (Koin registration).

- [ ] **Step 1: Write the failing test**

The native boundary (`WeChatQrNative`) is mocked with Mockito; the test targets `WeChatQrEngine`'s own merging/mapping logic (decoded texts+quads → `Result`s; leftover candidate quads not covered by a decode → `candidates`).

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import com.atharok.barcodescanner.wechatqr.WeChatQrNative
import com.atharok.barcodescanner.wechatqr.WeChatQrNativeResult
import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.nio.ByteBuffer

class WeChatQrEngineTest {

    private val native = mock(WeChatQrNative::class.java)

    @Before
    fun setUp() {
        `when`(native.nativeInit(null)).thenReturn(true)
    }

    private fun quad(side: Float) = floatArrayOf(0f, 0f, side, 0f, side, side, 0f, side)

    @Test fun `decoded text becomes a QR code result`() {
        `when`(native.nativeDetectAndDecode(any(ByteBuffer::class.java), anyInt(), anyInt(), anyInt())).thenReturn(
            WeChatQrNativeResult(arrayOf("hello"), arrayOf(quad(50f)), arrayOf(quad(50f)))
        )
        val engine = WeChatQrEngine(native, modelDir = null)

        val outcome = engine.scanBuffer(width = 100, height = 100, rowStride = 100)

        assertEquals(1, outcome.codes.size)
        assertEquals(BarcodeFormat.QR_CODE, outcome.codes[0].barcodeFormat)
        assertEquals("hello", outcome.codes[0].text)
        assertTrue(outcome.candidates.isEmpty())
    }

    @Test fun `a detector candidate with no matching decode is reported as a candidate`() {
        `when`(native.nativeDetectAndDecode(any(ByteBuffer::class.java), anyInt(), anyInt(), anyInt())).thenReturn(
            WeChatQrNativeResult(arrayOf(), arrayOf(), arrayOf(quad(10f)))
        )
        val engine = WeChatQrEngine(native, modelDir = null)

        val outcome = engine.scanBuffer(width = 100, height = 100, rowStride = 100)

        assertTrue(outcome.codes.isEmpty())
        assertEquals(1, outcome.candidates.size)
    }

    @Test fun `init failure degrades to empty outcomes without throwing`() {
        `when`(native.nativeInit(null)).thenReturn(false)
        val engine = WeChatQrEngine(native, modelDir = null)

        val outcome = engine.scanBuffer(width = 100, height = 100, rowStride = 100)

        assertTrue(outcome.codes.isEmpty())
        assertTrue(outcome.candidates.isEmpty())
    }
}
```

*(Uses mockito-core's `ArgumentMatchers.any(ByteBuffer::class.java)` and `anyInt()` — plain `any()` returns null and would NPE when unboxed into the `Int` parameters. `WeChatQrNative` is an interface (Task 6), so Mockito can stub it; the JNI class itself is never loaded on the JVM.)*

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.WeChatQrEngineTest"`
Expected: FAIL — `WeChatQrEngine` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.atharok.barcodescanner.wechatqr.WeChatQrNative
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint

/**
 * QR-only decode backed by the custom OpenCV wechat_qrcode build (CNN
 * detector + super-resolution). Used only as [TieredScanEngine]'s fallback.
 * Without [modelDir] it still runs (full-frame, cubic-resize), but then
 * reports no useful candidate quads (see design spec) and effectively
 * contributes nothing beyond what zxing-cpp already tried.
 */
class WeChatQrEngine(
    private val native: WeChatQrNative,
    modelDir: String?
) : ScanEngine {

    private val ready: Boolean = native.nativeInit(modelDir)

    override fun scan(image: ImageProxy): ScanOutcome {
        if (!ready) return ScanOutcome.EMPTY
        val plane = image.planes[0]
        return scanBuffer(image.width, image.height, plane.rowStride, plane.buffer)
    }

    override fun scan(bitmap: Bitmap): ScanOutcome {
        if (!ready) return ScanOutcome.EMPTY
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val gray = java.nio.ByteBuffer.allocateDirect(pixels.size).apply {
            for (p in pixels) {
                val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
                put(((r * 299 + g * 587 + b * 114) / 1000).toByte())
            }
            rewind()
        }
        return scanBuffer(bitmap.width, bitmap.height, bitmap.width, gray)
    }

    internal fun scanBuffer(
        width: Int,
        height: Int,
        rowStride: Int,
        buffer: java.nio.ByteBuffer = java.nio.ByteBuffer.allocateDirect(width * height)
    ): ScanOutcome {
        if (!ready) return ScanOutcome.EMPTY
        val native = native.nativeDetectAndDecode(buffer, width, height, rowStride)

        val codes = native.texts.mapIndexed { i, text ->
            Result(text, null, native.decodedQuads.getOrNull(i)?.let(::toResultPoints) ?: emptyArray(), BarcodeFormat.QR_CODE)
        }

        val decodedQuadKeys = native.decodedQuads.map { it.toList() }.toSet()
        val candidates = native.candidateQuads
            .filter { it.toList() !in decodedQuadKeys }
            .map(::toResultPoints)

        return ScanOutcome(codes, candidates)
    }

    private fun toResultPoints(quad: FloatArray): Array<ResultPoint> = arrayOf(
        ResultPoint(quad[0], quad[1]),
        ResultPoint(quad[2], quad[3]),
        ResultPoint(quad[4], quad[5]),
        ResultPoint(quad[6], quad[7])
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.WeChatQrEngineTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Add `WeChatQrModelInstaller`**

Copies the bundled model assets (Task 19) to a real filesystem path — OpenCV's C++ file APIs need a path, not an asset stream — idempotently and synchronously. The caller (Task 14's Koin registration) is responsible for calling this off the UI thread; there is no separate async copy step to race against.

```kotlin
package com.atharok.barcodescanner.wechatqr

import android.content.Context
import java.io.File

/**
 * Returns the directory containing the WeChatQRCode model files, copying
 * them from APK assets on first call, or null if the assets aren't bundled
 * (e.g. before Task 19's model-license check clears). Idempotent: already-
 * copied files are left alone, so a crash mid-copy recovers cleanly on the
 * next call. Call off the UI thread — this does file I/O.
 */
object WeChatQrModelInstaller {

    private const val ASSET_DIR = "wechat_qrcode"
    private val MODEL_FILES = listOf("detect.prototxt", "detect.caffemodel", "sr.prototxt", "sr.caffemodel")

    fun ensureInstalled(context: Context): String? {
        val assetNames = context.assets.list(ASSET_DIR)?.toSet() ?: return null
        if (!assetNames.containsAll(MODEL_FILES)) return null

        val targetDir = File(context.filesDir, ASSET_DIR).apply { mkdirs() }
        for (name in MODEL_FILES) {
            val target = File(targetDir, name)
            if (target.exists()) continue
            context.assets.open("$ASSET_DIR/$name").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return targetDir.path
    }
}
```

*(No JVM unit test here — `android.content.Context`/`AssetManager` aren't available on the plain JVM test runner and this project has no Robolectric. Covered instead by Task 20's instrumented test, which calls `ensureInstalled` for real against the bundled assets.)*

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/atharok/barcodescanner/domain/library/scan/WeChatQrEngine.kt app/src/main/java/com/atharok/barcodescanner/wechatqr/WeChatQrModelInstaller.kt app/src/test/java/com/atharok/barcodescanner/domain/library/scan/WeChatQrEngineTest.kt
git commit -m "feat: add WeChatQrEngine and WeChatQrModelInstaller"
```

---

## Task 8: TieredScanEngine

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/scan/TieredScanEngine.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/scan/TieredScanEngineTest.kt`

**Interfaces:**
- Consumes: `ScanEngine` (Task 3).
- Produces: `class TieredScanEngine(primary: ScanEngine, fallback: ScanEngine?, emptyFrameThreshold: Int = 10, candidateRetryFrames: Int = 2) : ScanEngine` — consumed by Tasks 14 and 18.

**Design note — synchronous, not async.** The fallback runs inline on the caller's thread, for two reasons: (1) the camera path's `ImageProxy` is closed in `CameraBarcodeAnalyzer.analyze()`'s `finally` as soon as `scan()` returns, so handing it to another executor would read a released buffer; (2) the gallery/share path (Task 18) makes exactly one `scan(bitmap)` call, so an async result "delivered on the next scan" would be silently discarded. On the camera path the fallback is rate-limited (at most once every `candidateRetryFrames` missed frames when a candidate is visible, every `emptyFrameThreshold` otherwise); while it runs, `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` simply drops stale frames. For bitmaps the fallback always runs when zxing-cpp finds nothing.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

private val FAKE_IMAGE: ImageProxy = mock(ImageProxy::class.java)
private val FAKE_BITMAP: Bitmap = mock(Bitmap::class.java)

private class FakeEngine(private val outcomes: MutableList<ScanOutcome>) : ScanEngine {
    var calls = 0
    private fun next(): ScanOutcome { calls++; return outcomes.removeFirstOrNull() ?: ScanOutcome.EMPTY }
    override fun scan(image: ImageProxy): ScanOutcome = next()
    override fun scan(bitmap: Bitmap): ScanOutcome = next()
}

private fun code() = Result("x", null, arrayOf(ResultPoint(0f, 0f)), BarcodeFormat.QR_CODE)
private fun candidate() = arrayOf(ResultPoint(0f, 0f), ResultPoint(1f, 0f), ResultPoint(1f, 1f), ResultPoint(0f, 1f))
private fun withCandidate() = ScanOutcome(emptyList(), listOf(candidate()))

class TieredScanEngineTest {

    @Test fun `primary decode short-circuits, fallback never runs`() {
        val primary = FakeEngine(mutableListOf(ScanOutcome(listOf(code()), emptyList())))
        val fallback = FakeEngine(mutableListOf())
        val outcome = TieredScanEngine(primary, fallback).scan(FAKE_IMAGE)

        assertEquals(1, outcome.codes.size)
        assertEquals(0, fallback.calls)
    }

    @Test fun `a visible candidate triggers the fallback and its result is returned in the same call`() {
        val primary = FakeEngine(MutableList(5) { withCandidate() })
        val fallback = FakeEngine(mutableListOf(ScanOutcome(listOf(code()), emptyList())))
        val tiered = TieredScanEngine(primary, fallback, candidateRetryFrames = 2)

        assertTrue(tiered.scan(FAKE_IMAGE).codes.isEmpty())
        val second = tiered.scan(FAKE_IMAGE)

        assertEquals(1, fallback.calls)
        assertEquals(1, second.codes.size)
    }

    @Test fun `fallback is rate-limited while the candidate stays undecodable`() {
        val primary = FakeEngine(MutableList(6) { withCandidate() })
        val fallback = FakeEngine(mutableListOf())
        val tiered = TieredScanEngine(primary, fallback, candidateRetryFrames = 2)

        repeat(6) { tiered.scan(FAKE_IMAGE) }

        assertEquals(3, fallback.calls)
    }

    @Test fun `no candidate waits for the empty-frame threshold before falling back`() {
        val primary = FakeEngine(MutableList(20) { ScanOutcome.EMPTY })
        val fallback = FakeEngine(mutableListOf())
        val tiered = TieredScanEngine(primary, fallback, emptyFrameThreshold = 3)

        repeat(2) { tiered.scan(FAKE_IMAGE) }
        assertEquals(0, fallback.calls)

        tiered.scan(FAKE_IMAGE)
        assertEquals(1, fallback.calls)
    }

    @Test fun `missing fallback degrades to primary-only without throwing`() {
        val primary = FakeEngine(mutableListOf(withCandidate()))
        val outcome = TieredScanEngine(primary, fallback = null).scan(FAKE_IMAGE)

        assertTrue(outcome.codes.isEmpty())
    }

    @Test fun `bitmap miss runs the fallback inline and returns its codes`() {
        val primary = FakeEngine(mutableListOf(ScanOutcome.EMPTY))
        val fallback = FakeEngine(mutableListOf(ScanOutcome(listOf(code()), emptyList())))

        val outcome = TieredScanEngine(primary, fallback).scan(FAKE_BITMAP)

        assertEquals(1, outcome.codes.size)
    }

    @Test fun `bitmap hit skips the fallback`() {
        val primary = FakeEngine(mutableListOf(ScanOutcome(listOf(code()), emptyList())))
        val fallback = FakeEngine(mutableListOf())

        TieredScanEngine(primary, fallback).scan(FAKE_BITMAP)

        assertEquals(0, fallback.calls)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.TieredScanEngineTest"`
Expected: FAIL — `TieredScanEngine` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/**
 * Runs [primary] first. When it decodes nothing, escalates to [fallback]
 * synchronously on the caller's thread — for camera frames at most once
 * every [candidateRetryFrames] missed frames while a candidate is visible
 * (every [emptyFrameThreshold] otherwise); for bitmaps always.
 * Not thread-safe for the camera path: call from one analyzer thread.
 */
class TieredScanEngine(
    private val primary: ScanEngine,
    private val fallback: ScanEngine?,
    private val emptyFrameThreshold: Int = 10,
    private val candidateRetryFrames: Int = 2
) : ScanEngine {

    private var missesSinceFallback = 0

    override fun scan(image: ImageProxy): ScanOutcome {
        val outcome = primary.scan(image)
        if (outcome.codes.isNotEmpty() || fallback == null) {
            missesSinceFallback = 0
            return outcome
        }
        missesSinceFallback++
        val due = if (outcome.candidates.isNotEmpty()) candidateRetryFrames else emptyFrameThreshold
        if (missesSinceFallback < due) return outcome
        missesSinceFallback = 0
        return outcome + fallback.scan(image)
    }

    override fun scan(bitmap: Bitmap): ScanOutcome {
        val outcome = primary.scan(bitmap)
        if (outcome.codes.isNotEmpty() || fallback == null) return outcome
        return outcome + fallback.scan(bitmap)
    }

    private operator fun ScanOutcome.plus(other: ScanOutcome) =
        ScanOutcome(codes + other.codes, candidates + other.candidates)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.TieredScanEngineTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/scan/TieredScanEngine.kt app/src/test/java/com/atharok/barcodescanner/domain/library/scan/TieredScanEngineTest.kt
git commit -m "feat: add TieredScanEngine"
```

---

## Task 9: ResultPoint geometry helpers

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/camera/ResultPointGeometry.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/camera/ResultPointGeometryTest.kt`

**Interfaces:**
- Produces: `fun Array<ResultPoint>.boundingBoxAreaRatio(frameWidth: Int, frameHeight: Int): Float`, `fun Array<ResultPoint>.centroid(): PointF` — consumed by Task 11 (`AutoZoomController`), Task 15 (`ScanOverlay` markers).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultPointGeometryTest {

    @Test fun `bounding box area ratio of a quarter-frame square is one quarter`() {
        val quad = arrayOf(ResultPoint(0f, 0f), ResultPoint(50f, 0f), ResultPoint(50f, 50f), ResultPoint(0f, 50f))
        assertEquals(0.25f, quad.boundingBoxAreaRatio(100, 100), 0.001f)
    }

    @Test fun `full frame quad is ratio one`() {
        val quad = arrayOf(ResultPoint(0f, 0f), ResultPoint(100f, 0f), ResultPoint(100f, 100f), ResultPoint(0f, 100f))
        assertEquals(1f, quad.boundingBoxAreaRatio(100, 100), 0.001f)
    }

    @Test fun `empty quad has zero area`() {
        assertEquals(0f, emptyArray<ResultPoint>().boundingBoxAreaRatio(100, 100), 0.001f)
    }

    @Test fun `centroid of a square is its center`() {
        val quad = arrayOf(ResultPoint(0f, 0f), ResultPoint(10f, 0f), ResultPoint(10f, 10f), ResultPoint(0f, 10f))
        val centroid = quad.centroid()
        assertEquals(5f, centroid.x, 0.001f)
        assertEquals(5f, centroid.y, 0.001f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ResultPointGeometryTest"`
Expected: FAIL — extension functions unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

import android.graphics.PointF
import com.google.zxing.ResultPoint

/** Axis-aligned bounding box area of this quad, as a fraction of a [frameWidth]x[frameHeight] frame. */
fun Array<ResultPoint>.boundingBoxAreaRatio(frameWidth: Int, frameHeight: Int): Float {
    if (isEmpty() || frameWidth <= 0 || frameHeight <= 0) return 0f
    val minX = minOf { it.x }; val maxX = maxOf { it.x }
    val minY = minOf { it.y }; val maxY = maxOf { it.y }
    val area = (maxX - minX) * (maxY - minY)
    return (area / (frameWidth.toFloat() * frameHeight.toFloat())).coerceIn(0f, 1f)
}

fun Array<ResultPoint>.centroid(): PointF {
    val cx = sumOf { it.x.toDouble() }.toFloat() / size
    val cy = sumOf { it.y.toDouble() }.toFloat() / size
    return PointF(cx, cy)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ResultPointGeometryTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/camera/ResultPointGeometry.kt app/src/test/java/com/atharok/barcodescanner/domain/library/camera/ResultPointGeometryTest.kt
git commit -m "feat: add ResultPoint geometry helpers"
```

---

## Task 10: AutoTorchController

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/camera/AutoTorchController.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/camera/AutoTorchControllerTest.kt`

**Interfaces:**
- Produces: `class AutoTorchController(hasFlash: Boolean, ...) { fun onManualTorchToggle(); fun onLuma(meanLuma: Int): Boolean? }` — consumed by Task 14.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoTorchControllerTest {

    @Test fun `requests torch on after sustained darkness`() {
        var clock = 0L
        val controller = AutoTorchController(hasFlash = true, now = { clock })

        assertNull(controller.onLuma(10))
        clock += 1500
        assertEquals(true, controller.onLuma(10))
    }

    @Test fun `does not request torch before the debounce window elapses`() {
        var clock = 0L
        val controller = AutoTorchController(hasFlash = true, now = { clock })

        assertNull(controller.onLuma(10))
        clock += 500
        assertNull(controller.onLuma(10))
    }

    @Test fun `requests torch off after sustained brightness`() {
        var clock = 0L
        val controller = AutoTorchController(hasFlash = true, now = { clock })

        assertNull(controller.onLuma(150))
        clock += 1500
        assertEquals(false, controller.onLuma(150))
    }

    @Test fun `manual toggle disables auto behavior for the session`() {
        var clock = 0L
        val controller = AutoTorchController(hasFlash = true, now = { clock })
        controller.onManualTorchToggle()

        controller.onLuma(10)
        clock += 2000
        assertNull(controller.onLuma(10))
    }

    @Test fun `device without a flash unit never requests a change`() {
        var clock = 0L
        val controller = AutoTorchController(hasFlash = false, now = { clock })

        controller.onLuma(1)
        clock += 5000
        assertNull(controller.onLuma(1))
    }

    @Test fun `mid-range luma resets both timers`() {
        var clock = 0L
        val controller = AutoTorchController(hasFlash = true, now = { clock })

        controller.onLuma(10)
        clock += 1000
        controller.onLuma(75)
        clock += 1000
        assertNull(controller.onLuma(10))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.AutoTorchControllerTest"`
Expected: FAIL — `AutoTorchController` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

/**
 * Turns the torch on below [lowThreshold] and off above [highThreshold],
 * each sustained for [debounceMillis] to avoid flicker. A manual torch
 * toggle disables auto behavior for the rest of the session. Decoupled from
 * CameraX: the caller applies the returned decision via `CameraControl.enableTorch`.
 */
class AutoTorchController(
    private val hasFlash: Boolean,
    private val lowThreshold: Int = 40,
    private val highThreshold: Int = 110,
    private val debounceMillis: Long = 1500L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private var manuallyDisabled = false
    private var belowSinceMillis: Long? = null
    private var aboveSinceMillis: Long? = null

    fun onManualTorchToggle() {
        manuallyDisabled = true
    }

    /** Returns true/false to request a torch change, or null for no change. */
    fun onLuma(meanLuma: Int): Boolean? {
        if (!hasFlash || manuallyDisabled) return null

        val t = now()
        when {
            meanLuma < lowThreshold -> {
                aboveSinceMillis = null
                val since = belowSinceMillis ?: t.also { belowSinceMillis = it }
                if (t - since >= debounceMillis) return true
            }
            meanLuma > highThreshold -> {
                belowSinceMillis = null
                val since = aboveSinceMillis ?: t.also { aboveSinceMillis = it }
                if (t - since >= debounceMillis) return false
            }
            else -> {
                belowSinceMillis = null
                aboveSinceMillis = null
            }
        }
        return null
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.AutoTorchControllerTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/camera/AutoTorchController.kt app/src/test/java/com/atharok/barcodescanner/domain/library/camera/AutoTorchControllerTest.kt
git commit -m "feat: add AutoTorchController"
```

---

## Task 11: AutoZoomController

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/camera/AutoZoomController.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/camera/AutoZoomControllerTest.kt`

**Interfaces:**
- Consumes: `boundingBoxAreaRatio` (Task 9).
- Produces: `class AutoZoomController(minZoomRatio: Float, maxZoomRatio: Float, ...) { fun onManualZoomActive(); fun onManualZoomReleased(); fun onCandidates(candidates: List<Array<ResultPoint>>, currentZoomRatio: Float, frameWidth: Int, frameHeight: Int): Float? }` — consumed by Task 14.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoZoomControllerTest {

    private fun quad(side: Float) = arrayOf(ResultPoint(0f, 0f), ResultPoint(side, 0f), ResultPoint(side, side), ResultPoint(0f, side))

    @Test fun `small candidate below the trigger ratio requests a zoom step`() {
        var clock = 0L
        val controller = AutoZoomController(minZoomRatio = 1f, maxZoomRatio = 6f, now = { clock })
        val target = controller.onCandidates(listOf(quad(10f)), currentZoomRatio = 1f, frameWidth = 100, frameHeight = 100)
        assertTrue(target != null && target > 1f)
    }

    @Test fun `candidate already above the trigger ratio does not zoom further`() {
        var clock = 0L
        val controller = AutoZoomController(minZoomRatio = 1f, maxZoomRatio = 6f, now = { clock })
        val target = controller.onCandidates(listOf(quad(50f)), currentZoomRatio = 1f, frameWidth = 100, frameHeight = 100)
        assertNull(target)
    }

    @Test fun `steps are throttled to the configured interval`() {
        var clock = 0L
        val controller = AutoZoomController(minZoomRatio = 1f, maxZoomRatio = 6f, stepIntervalMillis = 300, now = { clock })

        val first = controller.onCandidates(listOf(quad(10f)), 1f, 100, 100)
        assertTrue(first != null)

        val second = controller.onCandidates(listOf(quad(10f)), first!!, 100, 100)
        assertNull(second)

        clock += 300
        val third = controller.onCandidates(listOf(quad(10f)), first, 100, 100)
        assertTrue(third != null)
    }

    @Test fun `target is capped at maxZoomRatio`() {
        var clock = 0L
        val controller = AutoZoomController(minZoomRatio = 1f, maxZoomRatio = 2f, now = { clock })
        val target = controller.onCandidates(listOf(quad(1f)), currentZoomRatio = 1.9f, 100, 100)
        assertEquals(2f, target)
    }

    @Test fun `pausing during a manual gesture suppresses zoom steps`() {
        var clock = 0L
        val controller = AutoZoomController(minZoomRatio = 1f, maxZoomRatio = 6f, now = { clock })
        controller.onManualZoomActive()
        assertNull(controller.onCandidates(listOf(quad(10f)), 1f, 100, 100))
    }

    @Test fun `resets to minimum zoom after the candidate disappears for the timeout`() {
        var clock = 0L
        val controller = AutoZoomController(minZoomRatio = 1f, maxZoomRatio = 6f, resetTimeoutMillis = 5000, now = { clock })

        controller.onCandidates(listOf(quad(10f)), 1f, 100, 100)
        clock += 5000
        val resetTarget = controller.onCandidates(emptyList(), currentZoomRatio = 3f, 100, 100)

        assertEquals(1f, resetTarget)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.AutoZoomControllerTest"`
Expected: FAIL — `AutoZoomController` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.ResultPoint
import kotlin.math.sqrt

/**
 * Computes a target zoom ratio that grows a too-small candidate code toward
 * [targetAreaRatio] of the frame, in bounded steps — the "zoom in on a code
 * too small/far to read" behavior. Decoupled from CameraX: the caller
 * applies the returned ratio via `CameraControl.setZoomRatio`.
 */
class AutoZoomController(
    private val minZoomRatio: Float,
    private val maxZoomRatio: Float,
    private val targetAreaRatio: Float = 0.25f,
    private val triggerAreaRatio: Float = 0.10f,
    private val stepIntervalMillis: Long = 300L,
    private val resetTimeoutMillis: Long = 5000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private var lastStepMillis = 0L
    private var lastCandidateMillis = 0L
    private var paused = false

    fun onManualZoomActive() { paused = true }
    fun onManualZoomReleased() { paused = false }

    /** Returns the next zoom ratio to apply, or null when no step should be taken this frame. */
    fun onCandidates(
        candidates: List<Array<ResultPoint>>,
        currentZoomRatio: Float,
        frameWidth: Int,
        frameHeight: Int
    ): Float? {
        if (paused) return null
        val t = now()

        val smallest = candidates.minByOrNull { it.boundingBoxAreaRatio(frameWidth, frameHeight) }
        if (smallest == null) {
            return if (t - lastCandidateMillis >= resetTimeoutMillis && currentZoomRatio != minZoomRatio) {
                minZoomRatio
            } else null
        }
        lastCandidateMillis = t

        val areaRatio = smallest.boundingBoxAreaRatio(frameWidth, frameHeight)
        if (areaRatio >= triggerAreaRatio || areaRatio <= 0f) return null
        if (t - lastStepMillis < stepIntervalMillis) return null

        // Linear size scales as sqrt(area); grow toward targetAreaRatio.
        val growth = sqrt(targetAreaRatio / areaRatio)
        val target = (currentZoomRatio * growth).coerceIn(minZoomRatio, maxZoomRatio)
        if (target <= currentZoomRatio) return null

        lastStepMillis = t
        return target
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.AutoZoomControllerTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/camera/AutoZoomController.kt app/src/test/java/com/atharok/barcodescanner/domain/library/camera/AutoZoomControllerTest.kt
git commit -m "feat: add AutoZoomController"
```

---

## Task 12: ScanController state machine

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/domain/library/camera/ScanController.kt`
- Test: `app/src/test/java/com/atharok/barcodescanner/domain/library/camera/ScanControllerTest.kt`

**Interfaces:**
- Produces: `sealed interface ScanUiState { Searching, Approaching(targetZoomRatio), Picking(results), Found(result) }`; `class ScanController(stabilityFrameCount: Int = 2) { fun onFrame(codes: List<Result>, zoomStep: Float?): ScanUiState }` — consumed by Task 14.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import org.junit.Assert.assertTrue
import org.junit.Test

private fun result(text: String) = Result(text, null, arrayOf(ResultPoint(0f, 0f)), BarcodeFormat.QR_CODE)

class ScanControllerTest {

    @Test fun `no codes and no zoom step stays searching`() {
        val controller = ScanController()
        assertTrue(controller.onFrame(emptyList(), null) is ScanUiState.Searching)
    }

    @Test fun `a zoom step while searching reports approaching`() {
        val controller = ScanController()
        val state = controller.onFrame(emptyList(), zoomStep = 2.5f)
        assertTrue(state is ScanUiState.Approaching && state.targetZoomRatio == 2.5f)
    }

    @Test fun `two codes freeze into a picker`() {
        val controller = ScanController()
        val state = controller.onFrame(listOf(result("a"), result("b")), null)
        assertTrue(state is ScanUiState.Picking && state.results.size == 2)
    }

    @Test fun `a single code needs two consecutive matching frames before it is found`() {
        val controller = ScanController()
        assertTrue(controller.onFrame(listOf(result("a")), null) is ScanUiState.Searching)
        val second = controller.onFrame(listOf(result("a")), null)
        assertTrue(second is ScanUiState.Found && second.result.text == "a")
    }

    @Test fun `a different code resets the stability counter`() {
        val controller = ScanController()
        controller.onFrame(listOf(result("a")), null)
        val state = controller.onFrame(listOf(result("b")), null)
        assertTrue(state is ScanUiState.Searching)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*.ScanControllerTest"`
Expected: FAIL — `ScanController`/`ScanUiState` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.atharok.barcodescanner.domain.library.camera

import com.google.zxing.Result

sealed interface ScanUiState {
    data object Searching : ScanUiState
    data class Approaching(val targetZoomRatio: Float) : ScanUiState
    data class Picking(val results: List<Result>) : ScanUiState
    data class Found(val result: Result) : ScanUiState
}

/**
 * Turns a per-frame decode result into a [ScanUiState], requiring the same
 * text on [stabilityFrameCount] consecutive frames before committing to a
 * single result, and reporting a picker when multiple codes are visible.
 */
class ScanController(private val stabilityFrameCount: Int = 2) {

    private var lastText: String? = null
    private var stableCount = 0

    fun onFrame(codes: List<Result>, zoomStep: Float?): ScanUiState = when {
        codes.size >= 2 -> { resetStability(); ScanUiState.Picking(codes) }
        codes.size == 1 -> onSingleCode(codes[0])
        zoomStep != null -> { resetStability(); ScanUiState.Approaching(zoomStep) }
        else -> { resetStability(); ScanUiState.Searching }
    }

    private fun onSingleCode(result: Result): ScanUiState {
        if (result.text == lastText) stableCount++ else { lastText = result.text; stableCount = 1 }
        return if (stableCount >= stabilityFrameCount) {
            resetStability()
            ScanUiState.Found(result)
        } else {
            ScanUiState.Searching
        }
    }

    private fun resetStability() {
        lastText = null
        stableCount = 0
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*.ScanControllerTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/camera/ScanController.kt app/src/test/java/com/atharok/barcodescanner/domain/library/camera/ScanControllerTest.kt
git commit -m "feat: add ScanController state machine"
```

---

## Task 13: Rewrite CameraBarcodeAnalyzer for full-frame, tiered decode

**Files:**
- Modify: `app/src/main/java/com/atharok/barcodescanner/domain/library/camera/CameraBarcodeAnalyzer.kt`

**Interfaces:**
- Consumes: `ScanEngine` (Task 3/8).
- Produces: a **single-callback** `CameraBarcodeAnalyzer.BarcodeDetector` interface (`onFrame(codes, candidates, meanLuma, frameWidth, frameHeight, outputTransform)`; `onBarcodeFound`/`onMultipleBarcodesFound`/`onFrameAnalyzed`/`onError` all removed) — consumed by Task 14. **Collapsed to one callback deliberately**: calling three separate `ScanController.onFrame`-driving callbacks per analyzed frame (the original design) stomps `ScanController`'s stability counter every frame before the codes-specific callback ever sees it, since the "no codes" callback always fires first — single-code stability tracking would never reach 2 consecutive frames. One callback carrying everything means `MainCameraXScannerFragment` calls `scanController.onFrame` exactly once per frame.
- `CameraBarcodeAnalyzer`'s constructor takes `Lazy<ScanEngine>`, not a realized `ScanEngine` — so constructing the analyzer itself is cheap, and the expensive engine build (native lib load, `WeChatQrModelInstaller` file copy) only happens the first time `analyze()` runs, which `ImageAnalysis` always calls on the executor passed to `setAnalyzer` (Task 14 passes `cameraExecutor`) — never the UI thread.

- [ ] **Step 1: Replace the file**

```kotlin
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

@file:OptIn(androidx.camera.view.TransformExperimental::class)
package com.atharok.barcodescanner.domain.library.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import com.atharok.barcodescanner.domain.library.scan.ScanEngine
import com.google.zxing.Result
import com.google.zxing.ResultPoint

class CameraBarcodeAnalyzer(
    private val engine: Lazy<ScanEngine>,
    private val barcodeDetector: BarcodeDetector
) : ImageAnalysis.Analyzer {

    interface BarcodeDetector {
        /** Called exactly once per analyzed frame, whether or not any code decoded. */
        fun onFrame(
            codes: List<Result>,
            candidates: List<Array<ResultPoint>>,
            meanLuma: Int,
            frameWidth: Int,
            frameHeight: Int,
            outputTransform: OutputTransform
        )
    }

    override fun analyze(image: ImageProxy) {
        try {
            val outcome = engine.value.scan(image)
            val outputTransform = ImageProxyTransformFactory().getOutputTransform(image)
            barcodeDetector.onFrame(outcome.codes, outcome.candidates, meanLuma(image), image.width, image.height, outputTransform)
        } catch (e: IllegalStateException) {
            // Surface abandoned errors are expected when camera is stopping; ignore them.
        } catch (e: Exception) {
            // Per-frame decode exceptions are logged and dropped; they no longer hide the
            // camera (the old onError/doPermissionRefused path is gone — a single bad frame
            // must not end scanning).
            Log.w("CameraBarcodeAnalyzer", "Frame analysis failed", e)
        } finally {
            image.close()
        }
    }

    /** Mean brightness of the Y plane, sampled every 8th pixel on each axis. */
    private fun meanLuma(image: ImageProxy): Int {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        var sum = 0L
        var count = 0
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                sum += buffer.get(y * rowStride + x).toInt() and 0xFF
                count++
                x += 8
            }
            y += 8
        }
        return if (count == 0) 0 else (sum / count).toInt()
    }
}
```

- [ ] **Step 2: Do not build or commit yet**

`MainCameraXScannerFragment` still implements the old `BarcodeDetector`, so the module won't compile until Task 14. Leave this file uncommitted; Task 14 commits it together with the fragment so every commit in the fork's history builds (keeps `git bisect` usable).

---

## Task 14: Wire ScanController/AutoTorch/AutoZoom + tap-to-focus into the camera fragment

**Files:**
- Modify: `app/src/main/java/com/atharok/barcodescanner/domain/library/camera/CameraZoomGestureDetector.kt`
- Modify: `app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/main/MainCameraXScannerFragment.kt`
- Modify: `app/src/main/java/com/atharok/barcodescanner/common/injections/Modules.kt` (register the shared `ScanEngine` single — Task 18 reuses it, no duplicate construction)

**Interfaces:**
- Consumes: `CameraBarcodeAnalyzer.BarcodeDetector` (Task 13, single `onFrame` callback), `AutoTorchController`/`AutoZoomController`/`ScanController`/`ScanUiState` (Tasks 10–12), `TieredScanEngine`/`ZxingCppEngine`/`WeChatQrEngine`/`WeChatQrModelInstaller` (Tasks 5/7/8).
- Produces: `CameraZoomGestureDetector.setOnSingleTapListener(onSingleTap: (x: Float, y: Float) -> Unit)` — consumed by Task 15 (picker tap routing shares this same callback with tap-to-focus; `onDoubleTap` already returns `true` unconditionally in the existing code, so a double-tap is never also delivered as two single taps).

- [ ] **Step 1: Opt in to CameraX's experimental coordinate-transform API**

Both `MainCameraXScannerFragment.kt`'s `onFrame` override (Step 5) and Task 15's `showMultiCodePicker` reference `androidx.camera.view.transform.OutputTransform`, which (like `CoordinateTransform` and `ImageProxyTransformFactory`) is marked `@TransformExperimental` (`androidx.camera.view.TransformExperimental`) in CameraX. File annotations must precede `package` (Kotlin requires this — file annotations after `package` is a compile error), so add this between the existing license header comment and the `package` line, NOT after it:

```kotlin
@file:OptIn(androidx.camera.view.TransformExperimental::class)
```

- [ ] **Step 2: Add a single-tap hook to `CameraZoomGestureDetector`**

In `CameraZoomGestureDetector.kt`, add a field and override (alongside the existing `onDoubleTap`):

```kotlin
    private var onSingleTap: ((x: Float, y: Float) -> Unit)? = null

    fun setOnSingleTapListener(listener: (x: Float, y: Float) -> Unit) {
        onSingleTap = listener
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        onSingleTap?.invoke(e.x, e.y)
        return true
    }
```

(`GestureDetector.SimpleOnGestureListener` already declares `onSingleTapConfirmed`; this class extends it, so this is an override of an inherited no-op.)

- [ ] **Step 3: Register the shared `ScanEngine` as a Koin single**

In `Modules.kt`'s `libraryModule`, alongside the existing `single<BarcodeBitmapAnalyser>`:

```kotlin
    single<com.atharok.barcodescanner.domain.library.scan.ScanEngine> {
        val modelDir = com.atharok.barcodescanner.wechatqr.WeChatQrModelInstaller.ensureInstalled(androidContext())
        com.atharok.barcodescanner.domain.library.scan.TieredScanEngine(
            primary = com.atharok.barcodescanner.domain.library.scan.ZxingCppEngine(),
            fallback = runCatching {
                com.atharok.barcodescanner.domain.library.scan.WeChatQrEngine(
                    com.atharok.barcodescanner.wechatqr.WeChatQrNativeJni(),
                    modelDir = modelDir
                )
            }.getOrNull()
        )
    }
```

*(This is the one place `ScanEngine` is constructed — Task 18 changes `BarcodeBitmapAnalyser`'s wiring to `get<ScanEngine>()` instead of building its own, removing that duplication. `WeChatQrModelInstaller.ensureInstalled` does synchronous file I/O; Koin only runs this factory once, on whichever thread first resolves `ScanEngine` — Step 4 below ensures that is `cameraExecutor` for the camera path, never the UI thread.)*

- [ ] **Step 4: Build fields in `MainCameraXScannerFragment`**

In the existing `imageAnalyzer` builder (baseline line 105), un-comment the backpressure line so it reads `.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)` — the synchronous WeChatQRCode fallback (Task 8) blocks the analyzer thread for a frame or two, and stale frames must be dropped rather than queued.

Replace the existing `private val barcodeAnalyzer by lazy { CameraBarcodeAnalyzer(this) }` and add the other fields next to it:

```kotlin
    private val barcodeAnalyzer by lazy { CameraBarcodeAnalyzer(lazy { get<com.atharok.barcodescanner.domain.library.scan.ScanEngine>() }, this) }
    private val scanController = ScanController()
    private val autoTorchController by lazy { AutoTorchController(hasFlash = requireContext().hasFlash()) }
    private val autoZoomController by lazy {
        val zoomState = camera?.cameraInfo?.zoomState?.value
        AutoZoomController(minZoomRatio = zoomState?.minZoomRatio ?: 1f, maxZoomRatio = zoomState?.maxZoomRatio ?: 1f)
    }
    /** Non-null while the multi-code picker (Task 15) is showing; index into these results resolves a tap. */
    private var pickerResults: List<Result>? = null
```

*(`CameraBarcodeAnalyzer(lazy { get<ScanEngine>() }, this)` constructs a `Lazy<ScanEngine>` whose body — the Koin `get()` call, which runs Step 3's factory — is not evaluated here. It only runs the first time `CameraBarcodeAnalyzer.analyze()` dereferences `engine.value`, which CameraX always calls on `cameraExecutor` (never the UI thread), satisfying the "native lib/models unavailable → degrade" requirement safely off-thread: any `UnsatisfiedLinkError` from `WeChatQrNative`'s `init` block is still caught by the `runCatching` in Step 3's factory, and `TieredScanEngine` runs zxing-cpp only.)*

- [ ] **Step 5: Replace the `BarcodeDetector` implementation**

Replace the existing `override fun onBarcodeFound(result: Result)`, and delete the existing `override fun onError(msg: String)` entirely (the interface no longer declares either — per-frame failures are logged and dropped in `CameraBarcodeAnalyzer`, Task 13, never surfaced to this fragment). Replace with the single callback:

```kotlin
    override fun onFrame(
        codes: List<Result>,
        candidates: List<Array<ResultPoint>>,
        meanLuma: Int,
        frameWidth: Int,
        frameHeight: Int,
        outputTransform: androidx.camera.view.transform.OutputTransform
    ) {
        autoTorchController.onLuma(meanLuma)?.let { shouldBeOn ->
            if (shouldBeOn != flashEnabled) { flashEnabled = shouldBeOn; camera?.cameraControl?.enableTorch(flashEnabled) }
        }
        val currentRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
        val zoomStep = autoZoomController.onCandidates(candidates, currentRatio, frameWidth, frameHeight)
        zoomStep?.let { camera?.cameraControl?.setZoomRatio(it) }

        viewBinding.fragmentMainCameraXScannerPreviewView.post {
            // Exactly one scanController.onFrame call per analyzed frame — codes and
            // zoomStep together, not split across separate callbacks (that would reset
            // the stability counter on every frame before a single code could ever
            // reach 2 consecutive matches).
            handleScanState(scanController.onFrame(codes, zoomStep), outputTransform)
        }
    }

    private fun handleScanState(state: ScanUiState, outputTransform: androidx.camera.view.transform.OutputTransform) {
        when (state) {
            is ScanUiState.Found -> processFoundResult(state.result)
            is ScanUiState.Picking -> showMultiCodePicker(state.results, outputTransform)
            is ScanUiState.Approaching, ScanUiState.Searching -> Unit
        }
    }

    private fun processFoundResult(result: Result) {
        try {
            // Check if processing is stuck (more than 5 seconds)
            val currentTime = System.currentTimeMillis()
            if (isProcessingBarcode && currentTime - lastProcessingTime > 5000) {
                // Reset the flag if it's been stuck for more than 5 seconds
                isProcessingBarcode = false
            }

            // Prevent processing multiple barcodes at once
            if(isBarcodeAnalyzerRunning && !isProcessingBarcode) {
                // Check rate limiting first
                if (settingsManager.isRateLimitEnabled) {
                    try {
                        if (isWithinRateLimit(result.text)) {
                            // Barcode was recently scanned, ignore it and continue scanning
                            // Don't set isProcessingBarcode = true so scanner keeps running
                            return
                        }
                    } catch (e: Exception) {
                        // If rate limiting check fails, continue scanning
                        e.printStackTrace()
                    }
                }

                isProcessingBarcode = true
                lastProcessingTime = currentTime

                // Check if this barcode type is allowed
                val allowedFormats = settingsManager.allowedBarcodeFormats
                val barcodeFormat = result.barcodeFormat?.name

                // If no formats are specified (empty set), all formats are allowed
                if (allowedFormats.isEmpty() || (barcodeFormat != null && allowedFormats.contains(barcodeFormat))) {
                    // Record scan time for rate limiting BEFORE stopping camera
                    if (settingsManager.isRateLimitEnabled) {
                        recordScanTime(result.text)
                    }

                    onSuccessfulScanFromCamera(result)
                } else {
                    // Barcode type is not whitelisted - show popup but don't save to history
                    showNonWhitelistedBarcodePopup(result)
                }
            }
        } catch (e: Exception) {
            // Reset flag on any error to prevent scanner from getting stuck
            isProcessingBarcode = false
            e.printStackTrace()
        }
    }

```

*(The multi-code picker, `showMultiCodePicker`, is added in Task 15 alongside the overlay changes it needs; stub it as `private fun showMultiCodePicker(results: List<Result>, outputTransform: androidx.camera.view.transform.OutputTransform) { processFoundResult(results.first()) }` for now so this task compiles standalone, then replace in Task 15.)*


- [ ] **Step 6: Wire manual-zoom pause and tap-to-focus/tap-to-pick into `configureZoom()`**

```kotlin
    private fun configureZoom() {
        val slider = viewBinding.fragmentMainCameraXScannerSlider
        slider.value = settingsManager.getDefaultZoomValue() / 100f
        setLinearZoom(slider.value)
        slider.addOnChangeListener { v, value, _ ->
            setLinearZoom(value)
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        val gestureDetector = CameraZoomGestureDetector(slider.value)
        gestureDetector.attach(viewBinding.fragmentMainCameraXScannerScanOverlay) { value ->
            slider.value = value
            autoZoomController.onManualZoomActive()
        }
        gestureDetector.setOnSingleTapListener { x, y -> onScanOverlaySingleTap(x, y) }
    }

    private fun onScanOverlaySingleTap(x: Float, y: Float) {
        val results = pickerResults
        if (results != null) {
            val index = viewBinding.fragmentMainCameraXScannerScanOverlay.hitTestMarker(x, y)
            viewBinding.fragmentMainCameraXScannerScanOverlay.clearPicker()
            pickerResults = null
            if (index >= 0) processFoundResult(results[index]) else startBarcodeAnalyzer()
            return
        }
        val cam = camera ?: return
        val point = viewBinding.fragmentMainCameraXScannerPreviewView.meteringPointFactory.createPoint(x, y)
        cam.cameraControl.startFocusAndMetering(androidx.camera.core.FocusMeteringAction.Builder(point).build())
        autoZoomController.onManualZoomReleased()
    }
```

- [ ] **Step 7: Build-verification step**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Manual smoke run**

Run: `./gradlew installDebug`, launch the app, point the camera at a QR code.
Expected: the result screen opens after the code is held steady for 2 frames; no crash on backgrounding/foregrounding the app mid-scan.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/camera/CameraBarcodeAnalyzer.kt app/src/main/java/com/atharok/barcodescanner/domain/library/camera/CameraZoomGestureDetector.kt app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/main/MainCameraXScannerFragment.kt app/src/main/java/com/atharok/barcodescanner/common/injections/Modules.kt
git commit -m "feat: wire ScanController/AutoTorch/AutoZoom and tap-to-focus into the camera fragment"
```

---

## Task 15: ScanOverlay — remove the ROI box, add the multi-code marker picker

**Files:**
- Modify: `app/src/main/java/com/atharok/barcodescanner/presentation/customView/ScanOverlay.kt`
- Modify: `app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/main/MainCameraXScannerFragment.kt` (replace the `showMultiCodePicker` stub from Task 14)

**Interfaces:**
- Consumes: `centroid()` (Task 9), `CameraZoomGestureDetector.setOnSingleTapListener` (Task 14), `androidx.camera.view.transform.{ImageProxyTransformFactory, CoordinateTransform}` (CameraX, confirmed present since 1.1.0).
- Produces: `ScanOverlay.showPicker(bitmap, markers)`, `ScanOverlay.clearPicker()`, `ScanOverlay.hitTestMarker(x, y): Int`.

- [ ] **Step 1: Replace `ScanOverlay`'s drawing logic**

```kotlin
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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRect

/**
 * Full-frame scanning has no fixed viewfinder box. This view's drawing job
 * is now limited to the multi-code picker: freeze the last preview frame
 * and draw a tappable marker at each code's centroid.
 */
class ScanOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private val markerPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeWidth = getDP(4f)
    }
    private val markerRadiusPx = getDP(16f)

    private var frozenBitmap: Bitmap? = null
    private var markers: List<PointF> = emptyList()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun getDP(value: Float): Float = value * resources.displayMetrics.density

    fun showPicker(bitmap: Bitmap?, markerPoints: List<PointF>) {
        frozenBitmap = bitmap
        markers = markerPoints
        invalidate()
    }

    fun clearPicker() {
        frozenBitmap = null
        markers = emptyList()
        invalidate()
    }

    /** Index of the marker under (x, y) within a generous tap radius, or -1. */
    fun hitTestMarker(x: Float, y: Float): Int = markers.indexOfFirst {
        val dx = it.x - x; val dy = it.y - y
        dx * dx + dy * dy <= (markerRadiusPx * 2f) * (markerRadiusPx * 2f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frozenBitmap?.let { canvas.drawBitmap(it, null, Rect(0, 0, width, height), null) }
        for (marker in markers) {
            canvas.drawCircle(marker.x, marker.y, markerRadiusPx, markerPaint)
        }
    }
}
```

*(The old `RATIO`, `viewfinderRadius`/`backgroundPaint`/`viewfinderPaint`/`viewfinderCornerPaint`/`viewfinderRect`, `getViewfinderRect()`, `onMeasure`, `calculateRectangleDimension` are all removed — confirmed via grep that `RATIO` and `getViewfinderRect()` have no other callers once `CameraBarcodeAnalyzer`'s crop was removed in Task 13.)*

- [ ] **Step 2: Delete the now-unused style attributes**

In `app/src/main/res/values/attrs.xml`, remove the `ScanOverlay` declare-styleable's `viewfinder_radius`, `viewfinder_corner_thickness`, `viewfinder_corner_color`, `overlay_mask_color` entries if this file declares nothing else for `ScanOverlay` (check the file; if other attrs share the block, remove only these four). In `fragment_main_camera_x_scanner.xml`, remove the now-nonexistent `app:viewfinder_radius`, `app:viewfinder_corner_thickness`, `app:viewfinder_corner_color`, `app:overlay_mask_color` attributes from the `ScanOverlay` tag.

- [ ] **Step 3: Implement the real `showMultiCodePicker` in `MainCameraXScannerFragment`**

Replace the Task-14 stub (the `showMultiCodePicker(results, outputTransform)` signature is already final as declared in Task 14 — nothing further to change there):

```kotlin
    private fun showMultiCodePicker(results: List<Result>, outputTransform: androidx.camera.view.transform.OutputTransform) {
        stopBarcodeAnalyzer()
        val previewView = viewBinding.fragmentMainCameraXScannerPreviewView
        val overlay = viewBinding.fragmentMainCameraXScannerScanOverlay
        val previewOutputTransform = previewView.outputTransform ?: run {
            // No transform yet (preview not started) — fall back to the first result.
            processFoundResult(results.first())
            return
        }
        val coordinateTransform = androidx.camera.view.transform.CoordinateTransform(outputTransform, previewOutputTransform)

        val markerPoints = results.map { result ->
            @Suppress("UNCHECKED_CAST")
            val points = result.resultPoints as? Array<com.google.zxing.ResultPoint>
            val centroid = points?.takeIf { it.isNotEmpty() }
                ?.let { com.atharok.barcodescanner.domain.library.camera.centroid(it) }
                ?: android.graphics.PointF(0f, 0f)
            val point = android.graphics.PointF(centroid.x, centroid.y)
            coordinateTransform.mapPoint(point)
            point
        }

        pickerResults = results
        overlay.showPicker(previewView.bitmap, markerPoints)
    }
```

*(Tapping a marker or empty space is handled by `onScanOverlaySingleTap` from Task 14, which already checks `pickerResults` first.)*

- [ ] **Step 4: Build-verification step**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual smoke run**

Print two different QR codes side by side; scan both in frame.
Expected: the preview freezes, two green ring markers appear near each code's actual position; tapping one opens its result; tapping empty space / back resumes scanning.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/presentation/customView/ScanOverlay.kt app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/main/MainCameraXScannerFragment.kt app/src/main/java/com/atharok/barcodescanner/domain/library/camera/CameraBarcodeAnalyzer.kt app/src/main/res/values/attrs.xml app/src/main/res/layout/fragment_main_camera_x_scanner.xml
git commit -m "feat: multi-code marker picker, remove the fixed ROI box"
```

---

## Task 16: Payment content screen hook

**Files:**
- Create: `app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/barcodeAnalysis/contents/BarcodeMatrixPaymentParsedFragment.kt`
- Create: `app/src/main/res/layout/fragment_barcode_matrix_payment_parsed.xml`
- Modify: `app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/barcodeAnalysis/contents/BarcodeMatrixUriFragment.kt`
- New strings in `app/src/main/res/values/strings.xml`: `matrix_uri_payment_wechat_pay_label`, `matrix_uri_payment_wechat_link_label`, `matrix_uri_payment_alipay_label`

**Interfaces:**
- Consumes: `PaymentCodeClassifier` (Task 2).

- [ ] **Step 1: `fragment_barcode_matrix_payment_parsed.xml`** (modeled 1:1 on `fragment_barcode_matrix_upi_parsed.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    tools:context=".presentation.views.fragments.barcodeAnalysis.contents.BarcodeMatrixPaymentParsedFragment">

    <View
        android:id="@+id/fragment_barcode_matrix_payment_parsed_separator"
        android:layout_width="wrap_content"
        android:layout_height="@dimen/stroke_width"
        android:background="?colorOutline"
        android:paddingTop="@dimen/large_margin"
        android:layout_centerHorizontal="true"/>

    <com.atharok.barcodescanner.presentation.customView.BarcodeParsedView
        android:id="@+id/fragment_barcode_matrix_payment_parsed_brand_layout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingTop="@dimen/large_margin"
        android:layout_below="@id/fragment_barcode_matrix_payment_parsed_separator"
        app:title_text="@string/matrix_uri_url_label" />

</RelativeLayout>
```

- [ ] **Step 2: Strings**

```xml
    <string name="matrix_uri_payment_wechat_pay_label">WeChat Pay code</string>
    <string name="matrix_uri_payment_wechat_link_label">WeChat link</string>
    <string name="matrix_uri_payment_alipay_label">Alipay code</string>
```

- [ ] **Step 3: `BarcodeMatrixPaymentParsedFragment.kt`**

```kotlin
package com.atharok.barcodescanner.presentation.views.fragments.barcodeAnalysis.contents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.databinding.FragmentBarcodeMatrixPaymentParsedBinding
import com.atharok.barcodescanner.domain.library.payment.PaymentCode
import com.atharok.barcodescanner.presentation.views.fragments.BaseFragment

/** Display-only, sibling to [BarcodeMatrixUpiParsedFragment]: brand label for a classified payment code. */
class BarcodeMatrixPaymentParsedFragment : BaseFragment() {

    private var paymentCode: PaymentCode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(PAYMENT_CODE_BUNDLE_KEY)?.let {
            paymentCode = PaymentCode.valueOf(it)
        }
    }

    private var _binding: FragmentBarcodeMatrixPaymentParsedBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBarcodeMatrixPaymentParsedBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val labelRes = when (paymentCode) {
            PaymentCode.WECHAT_PAY -> R.string.matrix_uri_payment_wechat_pay_label
            PaymentCode.WECHAT_LINK -> R.string.matrix_uri_payment_wechat_link_label
            PaymentCode.ALIPAY -> R.string.matrix_uri_payment_alipay_label
            null -> { viewBinding.root.visibility = View.GONE; return }
        }
        viewBinding.fragmentBarcodeMatrixPaymentParsedBrandLayout.setContentsText(getString(labelRes))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PAYMENT_CODE_BUNDLE_KEY = "paymentCodeBundleKey"

        @JvmStatic
        fun newInstance(paymentCode: PaymentCode) = BarcodeMatrixPaymentParsedFragment().apply {
            arguments = Bundle().apply { putString(PAYMENT_CODE_BUNDLE_KEY, paymentCode.name) }
        }
    }
}
```

- [ ] **Step 4: Hook into `BarcodeMatrixUriFragment.start()`**

```kotlin
    override fun start(product: BarcodeAnalysis, parsedResult: ParsedResult) {
        if(parsedResult is URIParsedResult && parsedResult.type == ParsedResultType.URI) {
            val uri = parsedResult.uri
            viewBinding.fragmentBarcodeMatrixUriUrlLayout.setContentsText(uri)
            configureIsPossiblyMaliciousURI(parsedResult.isPossiblyMaliciousURI)
            val paymentCode = com.atharok.barcodescanner.domain.library.payment.PaymentCodeClassifier.classify(uri)
            when {
                uri.startsWith("upi") -> applyFragment(
                    containerViewId = viewBinding.fragmentBarcodeMatrixUriParsedLayout.id,
                    fragment = BarcodeMatrixUpiParsedFragment.newInstance(uri)
                )
                paymentCode != null -> applyFragment(
                    containerViewId = viewBinding.fragmentBarcodeMatrixUriParsedLayout.id,
                    fragment = BarcodeMatrixPaymentParsedFragment.newInstance(paymentCode)
                )
                else -> viewBinding.fragmentBarcodeMatrixUriParsedLayout.visibility = View.GONE
            }
        } else {
            viewBinding.root.visibility = View.GONE
        }
    }
```

- [ ] **Step 5: Build-verification step**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/barcodeAnalysis/contents app/src/main/res/layout/fragment_barcode_matrix_payment_parsed.xml app/src/main/res/values/strings.xml
git commit -m "feat: show WeChat/Alipay brand on classified payment codes"
```

---

## Task 17: Payment action hand-off (relabel the existing Open Link button)

**Files:**
- Modify: `app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/barcodeAnalysis/actions/UrlActionsFragment.kt`
- New strings: `action_pay_with_alipay`, `action_open_wechat`

**Interfaces:**
- Consumes: `PaymentCodeClassifier` (Task 2).

- [ ] **Step 1: Strings**

```xml
    <string name="action_pay_with_alipay">Pay with Alipay</string>
    <string name="action_open_wechat">Open WeChat</string>
```

- [ ] **Step 2: Modify `UrlActionsFragment`**

```kotlin
package com.atharok.barcodescanner.presentation.views.fragments.barcodeAnalysis.actions

import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.domain.entity.barcode.Barcode
import com.atharok.barcodescanner.domain.library.payment.PaymentCode
import com.atharok.barcodescanner.domain.library.payment.PaymentCodeClassifier
import com.atharok.barcodescanner.presentation.views.recyclerView.actionButton.ActionItem
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.URIParsedResult

class UrlActionsFragment: AbstractParsedResultActionsFragment() {
    override fun configureActionItems(barcode: Barcode, parsedResult: ParsedResult) {
        if(parsedResult is URIParsedResult) {
            addActionItem(configureUrlActionItem(parsedResult.uri))
        } else {
            addActionItem(configureSearchOnWebActionItem(barcode))
        }
        addActionItem(configureShareTextActionItem(barcode))
        addActionItem(configureCopyTextActionItem(barcode))
        addActionItem(configureModifyBarcodeActionItem(barcode))
        addActionItem(configureAssignANameToBarcodeActionItem(barcode))
    }

    private fun configureUrlActionItem(uri: String): ActionItem {
        val paymentCode = PaymentCodeClassifier.classify(uri)
        val textRes = when (paymentCode) {
            PaymentCode.ALIPAY -> R.string.action_pay_with_alipay
            PaymentCode.WECHAT_PAY, PaymentCode.WECHAT_LINK -> R.string.action_open_wechat
            null -> R.string.action_open_link
        }
        val handoffUri = paymentCode?.let { PaymentCodeClassifier.handoffUri(uri, it) } ?: uri
        return ActionItem(textRes = textRes, imageRes = R.drawable.baseline_open_in_browser_24, listener = openUrl(handoffUri))
    }
}
```

*(`handoffUri` wraps Alipay's `https://qr.alipay.com/...` web link into `alipays://platformapi/startapp?...` before it reaches `openUrl` → `ACTION_VIEW`, since a plain `https` link isn't guaranteed to open the Alipay app over a browser — see `PaymentCodeClassifier.handoffUri`'s doc comment, Task 2. WeChat codes and already-`alipays://` codes pass through unchanged. The listener call itself, `openUrl(...)`, is still the one unmodified existing function — only the URI handed to it differs for Alipay's https form.)*

- [ ] **Step 3: Build-verification step**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual smoke run**

Generate (or find online) a `wxp://` test string and an `https://qr.alipay.com/...` test string, encode them as QR codes on a second device/screen, scan each.
Expected: the Actions screen's single link button reads "Open WeChat" / "Pay with Alipay" respectively; tapping it either opens the target app or shows the existing "no compatible application found" toast if not installed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/presentation/views/fragments/barcodeAnalysis/actions/UrlActionsFragment.kt app/src/main/res/values/strings.xml
git commit -m "feat: relabel the open-link action for classified payment codes"
```

---

## Task 18: Gallery/share path uses the tiered engine

**Files:**
- Modify: `app/src/main/java/com/atharok/barcodescanner/domain/library/BarcodeBitmapAnalyser.kt`
- Modify: `app/src/main/java/com/atharok/barcodescanner/common/injections/Modules.kt` (reuse the `ScanEngine` single registered in Task 14 — no duplicate construction)

**Interfaces:**
- Consumes: `ScanEngine` (Task 3), the Koin single registered in Task 14.
- Produces: `BarcodeBitmapAnalyser.detectBarcodeFromBitmap(bitmap: Bitmap): Result?` (signature unchanged — existing callers in `BarcodeScanFromImageGalleryActivity`/share/shortcut activities need no changes).

- [ ] **Step 1: Rewrite `BarcodeBitmapAnalyser`**

```kotlin
package com.atharok.barcodescanner.domain.library

import android.graphics.Bitmap
import com.atharok.barcodescanner.domain.library.scan.ScanEngine
import com.google.zxing.Result

/** Search for a barcode in a static image (gallery/share/shortcut), via the same tiered engine the camera uses. */
class BarcodeBitmapAnalyser(private val engine: ScanEngine) {

    fun detectBarcodeFromBitmap(bitmap: Bitmap): Result? = engine.scan(bitmap).codes.firstOrNull()
}
```

- [ ] **Step 2: Reuse the shared `ScanEngine` single in `Modules.kt`**

Replace:

```kotlin
    single<BarcodeBitmapAnalyser>{ BarcodeBitmapAnalyser() }
```

with:

```kotlin
    single<BarcodeBitmapAnalyser> { BarcodeBitmapAnalyser(get<com.atharok.barcodescanner.domain.library.scan.ScanEngine>()) }
```

*(Reuses the exact `ScanEngine` single Task 14 registered — one engine instance, one model-copy, shared by camera and gallery/share, instead of building a second `TieredScanEngine` here.)*

- [ ] **Step 3: Build-verification step**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual smoke run**

Open the app's "scan from image" flow with a saved QR code screenshot.
Expected: identical result to the previous ZXing-only behavior.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/atharok/barcodescanner/domain/library/BarcodeBitmapAnalyser.kt app/src/main/java/com/atharok/barcodescanner/common/injections/Modules.kt
git commit -m "feat: gallery/share scan uses the tiered decode engine"
```

---

## Task 19: Model asset + F-Droid/build metadata

**Files:**
- Create: `app/src/main/assets/wechat_qrcode/{detect.prototxt,detect.caffemodel,sr.prototxt,sr.caffemodel}` (fetched from `WeChatCV/opencv_3rdparty`, with a checksum-verification Gradle task)
- Modify: nothing further for the model-copy mechanism — `WeChatQrModelInstaller` (Task 7) and its Koin wiring (Task 14) already handle it idempotently and synchronously off the UI thread; this task only has to supply the asset files themselves
- Modify: `fastlane/metadata/android/en-US/changelogs/` — add the next version-code changelog entry
- Modify: F-Droid build metadata (wherever this project's F-Droid recipe lives once published) — add `submodules: true` and pin the NDK version used in Task 6

**Interfaces:** None (build/release configuration; no new code contract).

- [ ] **Step 1: Download and pin the models**

```bash
mkdir -p app/src/main/assets/wechat_qrcode
curl -fsSL -o app/src/main/assets/wechat_qrcode/detect.prototxt https://raw.githubusercontent.com/WeChatCV/opencv_3rdparty/wechat_qrcode/detect.prototxt
curl -fsSL -o app/src/main/assets/wechat_qrcode/detect.caffemodel https://raw.githubusercontent.com/WeChatCV/opencv_3rdparty/wechat_qrcode/detect.caffemodel
curl -fsSL -o app/src/main/assets/wechat_qrcode/sr.prototxt https://raw.githubusercontent.com/WeChatCV/opencv_3rdparty/wechat_qrcode/sr.prototxt
curl -fsSL -o app/src/main/assets/wechat_qrcode/sr.caffemodel https://raw.githubusercontent.com/WeChatCV/opencv_3rdparty/wechat_qrcode/sr.caffemodel
sha256sum app/src/main/assets/wechat_qrcode/* > app/src/main/assets/wechat_qrcode/SHA256SUMS
```

**Before committing these**, confirm the model files' license (check the `WeChatCV/opencv_3rdparty` repo's LICENSE) is compatible with GPLv3 distribution. If unclear, hold this step and ship with `fallback = null` (zxing-cpp only) until resolved — `TieredScanEngine` already degrades cleanly (Task 8).

- [ ] **Step 2: Confirm the installer picks the models up**

No new code here — `WeChatQrModelInstaller.ensureInstalled` (Task 7) already copies `assets/wechat_qrcode/*` to `context.filesDir` on first resolution of the `ScanEngine` Koin single (Task 14), synchronously, before `WeChatQrEngine` is constructed. Before this step, `context.assets.list("wechat_qrcode")` returned null/empty, so `ensureInstalled` returned null and the engine ran zxing-cpp-only; once Step 1's files exist, it returns the real directory and `WeChatQrEngine` gets real models with no further wiring.

- [ ] **Step 3: Fastlane changelog**

Add `fastlane/metadata/android/en-US/changelogs/53.txt` (next version code after the imported baseline's 52):

```
- Full-frame multi-code scanning with a tap-to-pick marker overlay
- Auto-torch in low light and auto-zoom on small/distant codes
- AI-assisted recovery for hard-to-read QR codes
- WeChat Pay and Alipay QR codes are recognized and handed off to the app
```

- [ ] **Step 4: Bump `versionCode`/`versionName`**

In `app/build.gradle.kts`, bump `versionCode = 53` and `versionName` to the next value per this project's existing convention (check `fastlane/metadata` history for the pattern already in use).

- [ ] **Step 5: Verify the release build**

Run: `./gradlew assembleRelease`
Expected: `BUILD SUCCESSFUL`. Note the resulting per-ABI APK sizes (`app/build/outputs/apk/release/`) against the design spec's "+8 MB per ABI" budget; if exceeded, report the actual delta rather than silently accepting it.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/wechat_qrcode app/build.gradle.kts fastlane/metadata/android/en-US/changelogs/53.txt
git commit -m "chore: bundle WeChatQRCode models, bump version, add changelog"
```

---

## Task 20: Instrumented fixture tests and manual device checklist

**Files:**
- Create: `app/src/androidTest/assets/scan_fixtures/{normal_qr.png,ean13.png,data_matrix.png,inverted_qr.png,two_qr.png,small_blurred_qr.png,wechat_pay_qr.png,alipay_qr.png}`
- Create: `app/src/androidTest/java/com/atharok/barcodescanner/domain/library/scan/TieredScanEngineInstrumentedTest.kt`

**Interfaces:**
- Consumes: `TieredScanEngine`, `ZxingCppEngine`, `WeChatQrEngine` (Tasks 5/7/8), `PaymentCodeClassifier` (Task 2).

- [ ] **Step 1: Generate the fixture images**

Use any QR/barcode generator (including this app's own `BarcodeFormCreator*` screens, run once manually) to produce: a normal QR, an EAN-13, a DataMatrix, a QR with inverted (light-on-dark) colors, two distinct QR codes composited into one image, a QR downscaled/blurred to the point zxing-cpp alone cannot read it, a QR encoding a `wxp://` test string, and a QR encoding an `https://qr.alipay.com/...` test string. Place them under `app/src/androidTest/assets/scan_fixtures/`.

- [ ] **Step 2: Write the instrumented test**

```kotlin
package com.atharok.barcodescanner.domain.library.scan

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atharok.barcodescanner.domain.library.payment.PaymentCode
import com.atharok.barcodescanner.domain.library.payment.PaymentCodeClassifier
import com.atharok.barcodescanner.wechatqr.WeChatQrModelInstaller
import com.atharok.barcodescanner.wechatqr.WeChatQrNativeJni
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TieredScanEngineInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    /** The app under test: owns the bundled `wechat_qrcode/` model assets (Task 19). */
    private val appContext = instrumentation.targetContext

    /** Fixtures live in src/androidTest/assets, i.e. the *test* APK — read via the instrumentation context, not the app's. */
    private fun fixture(name: String) = instrumentation.context.assets.open("scan_fixtures/$name").use {
        BitmapFactory.decodeStream(it)
    }

    @Test fun `zxing-cpp alone decodes the common formats`() {
        val engine = ZxingCppEngine()
        assertTrue(engine.scan(fixture("normal_qr.png")).codes.isNotEmpty())
        assertTrue(engine.scan(fixture("ean13.png")).codes.isNotEmpty())
        assertTrue(engine.scan(fixture("data_matrix.png")).codes.isNotEmpty())
        assertTrue(engine.scan(fixture("inverted_qr.png")).codes.isNotEmpty())
        assertEquals(2, engine.scan(fixture("two_qr.png")).codes.size)
    }

    @Test fun `wechat pay and alipay sample codes classify correctly end to end`() {
        val engine = ZxingCppEngine()
        val wechatText = engine.scan(fixture("wechat_pay_qr.png")).codes.first().text
        val alipayText = engine.scan(fixture("alipay_qr.png")).codes.first().text

        assertEquals(PaymentCode.WECHAT_PAY, PaymentCodeClassifier.classify(wechatText))
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify(alipayText))
    }

    @Test fun `small blurred QR unreadable by zxing-cpp is recovered by the CNN-backed WeChatQRCode fallback`() {
        // Real models, copied the same way production does (Task 7/14) — NOT modelDir = null,
        // which would silently exercise the no-CNN fallback and prove nothing.
        val modelDir = WeChatQrModelInstaller.ensureInstalled(appContext)
        assumeTrue("WeChatQRCode models not bundled yet (Task 19 license check pending)", modelDir != null)

        val bitmap = fixture("small_blurred_qr.png")
        // Guard: the fixture must actually defeat zxing-cpp, otherwise this test proves nothing about the fallback.
        assertTrue("fixture is too easy — zxing-cpp alone decodes it", ZxingCppEngine().scan(bitmap).codes.isEmpty())

        val tiered = TieredScanEngine(
            primary = ZxingCppEngine(),
            fallback = WeChatQrEngine(WeChatQrNativeJni(), modelDir = modelDir)
        )

        // Bitmap path is synchronous (Task 8): one call, fallback included.
        val outcome = tiered.scan(bitmap)

        assertTrue("expected the WeChatQRCode fallback to decode the blurred fixture", outcome.codes.isNotEmpty())
    }
}
```

- [ ] **Step 3: Run the instrumented tests**

Run: `./gradlew connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL` on an `arm64-v8a`/`armeabi-v7a` device or emulator.

- [ ] **Step 4: Manual device checklist**

Perform each, on a real device:
1. Dark room: cover the camera partially; confirm the torch turns on automatically within ~1.5 s and off again in a lit room.
2. Hold a small/distant QR code (occupying <10% of the frame); confirm the preview zooms in automatically and the code decodes once large enough.
3. Pinch-zoom manually mid-auto-zoom; confirm auto-zoom pauses and does not fight the gesture.
4. Present two different QR codes at once; confirm the marker picker appears with markers roughly over each code, and tapping one opens its result.
5. Scan a `wxp://` test code; confirm the Actions screen reads "Open WeChat" and, if WeChat is installed, opens it (or shows WeChat's own "scan inside the app" message — this is WeChat's behavior, not this app's, per the design spec's noted risk).
6. Scan an `https://qr.alipay.com/...` test code with Alipay installed; confirm "Pay with Alipay" opens Alipay.
7. Repeat 5–6 with the target app **not** installed; confirm the existing "no compatible application found" toast appears, no crash.
8. If models are bundled (Step 2's instrumented test didn't skip): hold a small, blurred QR code that step 2 of this checklist's auto-zoom cannot fully recover; confirm it eventually decodes via the WeChatQRCode fallback (expect a brief delay) — this is the real-device counterpart to Step 2's instrumented fixture test above.

- [ ] **Step 5: Run the full existing JVM unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` — every test added in Tasks 2–12 plus the pre-existing template test.

- [ ] **Step 6: Commit**

```bash
git add app/src/androidTest
git commit -m "test: instrumented fixture coverage for tiered decode and payment classification"
```
